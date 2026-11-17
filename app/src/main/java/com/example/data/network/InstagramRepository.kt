package com.example.data.network

import com.example.data.model.InstagramMediaType
import com.example.data.model.InstagramSession
import com.example.data.model.InstagramStoryItem
import com.example.data.model.InstagramStoryTrayGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class InstagramAuthRequiredException(message: String) : Exception(message)

data class InstagramFetchResult(
    val groups: List<InstagramStoryTrayGroup>,
    val resolvedAccountUsername: String = ""
)

data class ParsedTrayEntry(
    val reelId: String,
    val group: InstagramStoryTrayGroup
)

class InstagramRepository {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchStoriesWithSession(
        session: InstagramSession,
        pinnedUsernames: List<String>
    ): InstagramFetchResult = withContext(Dispatchers.IO) {
        if (!session.isAuthenticated) {
            throw InstagramAuthRequiredException("برای دیدن استوری‌های اینستاگرام ابتدا وارد حسابت شو.")
        }

        val ua = session.userAgent.ifBlank { DEFAULT_MOBILE_USER_AGENT }
        var activeSession = if (session.csrfToken.isBlank()) {
            hydrateSessionWithCookies(session, ua)
        } else {
            session
        }

        // 1. Fetch the user's real-time home Stories Tray from Instagram
        val trayJson = try {
            val raw = executeAuthenticatedGet(
                url = "https://www.instagram.com/api/v1/feed/reels_tray/",
                session = activeSession,
                userAgent = ua
            )
            checkAuthResponse(raw)
            raw
        } catch (authErr: InstagramAuthRequiredException) {
            val encodedAlt = encodeColonSessionIdIfNeeded(activeSession)
            if (encodedAlt != null) {
                val hydratedAlt = hydrateSessionWithCookies(encodedAlt, ua)
                val rawAlt = executeAuthenticatedGet(
                    url = "https://www.instagram.com/api/v1/feed/reels_tray/",
                    session = hydratedAlt,
                    userAgent = ua
                )
                checkAuthResponse(rawAlt)
                activeSession = hydratedAlt
                rawAlt
            } else {
                throw authErr
            }
        }

        val trayEntries = parseReelsTrayJson(trayJson).toMutableList()

        // 2. Hydrate tray entries that did not include inline items using reels_media batch endpoint
        val missingIds = trayEntries
            .filter { it.group.items.isEmpty() && it.reelId.isNotBlank() }
            .map { it.reelId }
            .distinct()
            .take(18)

        if (missingIds.isNotEmpty()) {
            val hydratedByReelId = mutableMapOf<String, List<InstagramStoryItem>>()
            val chunks = missingIds.chunked(8)
            val semaphore = Semaphore(2)
            coroutineScope {
                chunks.map { chunk ->
                    async {
                        semaphore.withPermit {
                            val query = chunk.joinToString("&") {
                                "reel_ids=${URLEncoder.encode(it, "UTF-8")}"
                            }
                            val raw = runCatching {
                                executeAuthenticatedGet(
                                    url = "https://www.instagram.com/api/v1/feed/reels_media/?$query",
                                    session = activeSession,
                                    userAgent = ua
                                )
                            }.getOrNull()
                            if (!raw.isNullOrBlank()) {
                                checkAuthResponse(raw)
                                parseReelsMediaJson(raw)
                            } else {
                                emptyMap()
                            }
                        }
                    }
                }.awaitAll().forEach { chunkMap ->
                    hydratedByReelId.putAll(chunkMap)
                }
            }

            for (idx in trayEntries.indices) {
                val entry = trayEntries[idx]
                if (entry.group.items.isEmpty()) {
                    val fetchedItems = hydratedByReelId[entry.reelId].orEmpty()
                    if (fetchedItems.isNotEmpty()) {
                        trayEntries[idx] = entry.copy(
                            group = entry.group.copy(
                                items = fetchedItems,
                                badgeLabel = buildStoryBadgeLabel(fetchedItems, isLiveStory = true)
                            )
                        )
                    }
                }
            }
        }

        val trayGroups = trayEntries
            .map { it.group }
            .filter { it.items.isNotEmpty() }
            .toMutableList()

        // 3. Resolve signed-in user's own @username if missing
        var resolvedUsername = activeSession.username
        if (resolvedUsername.isBlank()) {
            val currentUserRaw = runCatching {
                executeAuthenticatedGet(
                    url = "https://www.instagram.com/api/v1/accounts/current_user/?edit=true",
                    session = activeSession,
                    userAgent = ua
                )
            }.getOrNull()
            if (!currentUserRaw.isNullOrBlank()) {
                resolvedUsername = runCatching {
                    JSONObject(currentUserRaw)
                        .optJSONObject("user")
                        ?.optString("username")
                        ?.trim()
                        .orEmpty()
                }.getOrDefault("")
            }
        }

        // 4. Fetch active 24h stories (or latest posts) for pinned sports accounts not already in tray
        val cleanedPinned = pinnedUsernames
            .mapNotNull { normalizeUsername(it).takeIf(String::isNotBlank) }
            .distinct()
        val existingUsernames = trayGroups.map { it.username.lowercase(Locale.US) }.toSet()
        val remainingPinned = cleanedPinned.filter { it !in existingUsernames }

        if (remainingPinned.isNotEmpty()) {
            val semaphore = Semaphore(3)
            val pinnedGroups = coroutineScope {
                remainingPinned.map { username ->
                    async {
                        semaphore.withPermit {
                            runCatching {
                                fetchPinnedAccountAuthenticated(
                                    username = username,
                                    session = activeSession,
                                    userAgent = ua
                                )
                            }.getOrNull()
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            trayGroups.addAll(pinnedGroups)
        }

        InstagramFetchResult(
            groups = trayGroups.distinctBy { it.username.lowercase(Locale.US) },
            resolvedAccountUsername = resolvedUsername
        )
    }

    private fun fetchPinnedAccountAuthenticated(
        username: String,
        session: InstagramSession,
        userAgent: String
    ): InstagramStoryTrayGroup? {
        val encoded = URLEncoder.encode(username, "UTF-8")
        val profileRaw = executeAuthenticatedGet(
            url = "https://www.instagram.com/api/v1/users/web_profile_info/?username=$encoded",
            session = session,
            userAgent = userAgent
        )
        checkAuthResponse(profileRaw)

        val root = runCatching { JSONObject(profileRaw) }.getOrNull() ?: return null
        val user = root.optJSONObject("data")?.optJSONObject("user")
            ?: root.optJSONObject("user")
            ?: return null
        val pk = user.optString("id").trim()
        val resolvedName = user.optString("username").trim().ifBlank { username }
        val fullName = user.optString("full_name").trim().ifBlank { resolvedName }
        val avatar = unescapeIgUrl(
            user.optString("profile_pic_url_hd").ifBlank { user.optString("profile_pic_url") }
        )

        // Try active 24h story for this account first
        if (pk.isNotBlank()) {
            val storyRaw = runCatching {
                executeAuthenticatedGet(
                    url = "https://www.instagram.com/api/v1/feed/reels_media/?reel_ids=${URLEncoder.encode(pk, "UTF-8")}",
                    session = session,
                    userAgent = userAgent
                )
            }.getOrNull()
            if (!storyRaw.isNullOrBlank()) {
                val activeItems = parseReelsMediaJson(storyRaw)[pk].orEmpty()
                if (activeItems.isNotEmpty()) {
                    return InstagramStoryTrayGroup(
                        id = resolvedName,
                        username = resolvedName,
                        displayName = fullName,
                        avatarUrl = avatar.ifBlank { activeItems.first().thumbnailUrl.ifBlank { activeItems.first().mediaUrl } },
                        isActiveStoryBucket = true,
                        badgeLabel = buildStoryBadgeLabel(activeItems, isLiveStory = true),
                        items = activeItems
                    )
                }
            }
        }

        // Fall back to recent posts/reels if the pinned page has no active 24h story right now
        return parseWebProfileInfoJson(profileRaw, username)
    }

    suspend fun fetchPublicGuestPreview(
        pinnedUsernames: List<String>
    ): List<InstagramStoryTrayGroup> = withContext(Dispatchers.IO) {
        val cleaned = pinnedUsernames
            .mapNotNull { normalizeUsername(it).takeIf(String::isNotBlank) }
            .distinct()
            .take(5)
        if (cleaned.isEmpty()) return@withContext emptyList()

        val guestCookies = harvestGuestCookies()
        val csrf = guestCookies["csrftoken"].orEmpty()
        val cookieHeader = guestCookies.entries.joinToString("; ") { "${it.key}=${it.value}" }

        val semaphore = Semaphore(2)
        coroutineScope {
            cleaned.map { username ->
                async {
                    semaphore.withPermit {
                        runCatching {
                            val encoded = URLEncoder.encode(username, "UTF-8")
                            val builder = Request.Builder()
                                .url("https://www.instagram.com/api/v1/users/web_profile_info/?username=$encoded")
                                .header("User-Agent", DEFAULT_MOBILE_USER_AGENT)
                                .header("X-IG-App-ID", IG_WEB_APP_ID)
                                .header("X-ASBD-ID", "129477")
                                .header("X-Requested-With", "XMLHttpRequest")
                                .header("Referer", "https://www.instagram.com/$username/")
                                .header("Accept", "*/*")
                                .header("Accept-Language", "fa-IR,fa;q=0.9,en-US;q=0.8,en;q=0.7")
                            if (cookieHeader.isNotBlank()) builder.header("Cookie", cookieHeader)
                            if (csrf.isNotBlank()) builder.header("X-CSRFToken", csrf)

                            client.newCall(builder.build()).execute().use { resp ->
                                val body = resp.body?.string() ?: ""
                                if (!resp.isSuccessful || !body.trimStart().startsWith("{")) return@use null
                                parseWebProfileInfoJson(body, username)
                            }
                        }.getOrNull()
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    private fun hydrateSessionWithCookies(
        session: InstagramSession,
        userAgent: String
    ): InstagramSession {
        val cookieMap = parseCookieMap(session.cookieHeader).toMutableMap()
        val harvested = harvestGuestCookies(existingCookieHeader = session.cookieHeader, userAgent = userAgent)
        harvested.forEach { (k, v) ->
            if (cookieMap[k].isNullOrBlank()) {
                cookieMap[k] = v
            }
        }
        val mergedHeader = cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
        val mergedCsrf = cookieMap["csrftoken"].orEmpty().ifBlank { session.csrfToken }
        return session.copy(
            cookieHeader = mergedHeader.ifBlank { session.cookieHeader },
            csrfToken = mergedCsrf
        )
    }

    private fun encodeColonSessionIdIfNeeded(session: InstagramSession): InstagramSession? {
        val cookieMap = parseCookieMap(session.cookieHeader).toMutableMap()
        val rawSid = cookieMap["sessionid"].orEmpty()
        if (!rawSid.contains(':') || rawSid.contains("%3A", ignoreCase = true)) return null
        val encodedSid = rawSid.replace(":", "%3A")
        cookieMap["sessionid"] = encodedSid
        val uid = cookieMap["ds_user_id"].orEmpty().ifBlank { extractNumericUserIdFromSessionId(encodedSid) }
        if (uid.isNotBlank()) cookieMap["ds_user_id"] = uid
        return session.copy(
            cookieHeader = cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" },
            userId = uid.ifBlank { session.userId }
        )
    }

    private fun harvestGuestCookies(
        existingCookieHeader: String = "",
        userAgent: String = DEFAULT_MOBILE_USER_AGENT
    ): Map<String, String> {
        val cookieMap = linkedMapOf<String, String>()
        listOf(
            "https://www.instagram.com/api/v1/web/login_page/",
            "https://www.instagram.com/"
        ).forEach { seedUrl ->
            runCatching {
                val builder = Request.Builder()
                    .url(seedUrl)
                    .header("User-Agent", userAgent)
                    .header("Accept", "text/html,application/json,*/*")
                    .header("Accept-Language", "fa-IR,fa;q=0.9,en-US;q=0.8,en;q=0.7")
                if (existingCookieHeader.isNotBlank()) {
                    builder.header("Cookie", existingCookieHeader)
                }
                client.newCall(builder.build()).execute().use { resp ->
                    resp.headers("Set-Cookie").forEach { setCookie ->
                        val pair = setCookie.substringBefore(';')
                        val idx = pair.indexOf('=')
                        if (idx > 0) {
                            val k = pair.substring(0, idx).trim()
                            val v = pair.substring(idx + 1).trim()
                            if (k.isNotBlank() && v.isNotBlank() && !v.equals("deleted", ignoreCase = true)) {
                                cookieMap[k] = v
                            }
                        }
                    }
                    if (!cookieMap.containsKey("csrftoken")) {
                        val body = resp.body?.string().orEmpty()
                        val embeddedCsrf = Regex("\"csrf_token\"\\s*:\\s*\"([A-Za-z0-9_-]{8,})\"")
                            .find(body)
                            ?.groupValues
                            ?.getOrNull(1)
                            .orEmpty()
                        if (embeddedCsrf.isNotBlank()) {
                            cookieMap["csrftoken"] = embeddedCsrf
                        }
                    }
                }
            }
            if (cookieMap.containsKey("csrftoken")) return cookieMap
        }
        return cookieMap
    }

    private fun executeAuthenticatedGet(
        url: String,
        session: InstagramSession,
        userAgent: String
    ): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Cookie", session.cookieHeader)
            .header("X-IG-App-ID", IG_WEB_APP_ID)
            .header("X-ASBD-ID", "129477")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", "https://www.instagram.com/")
            .header("Origin", "https://www.instagram.com")
            .header("Accept", "*/*")
            .header("Accept-Language", "fa-IR,fa;q=0.9,en-US;q=0.8,en;q=0.7")

        if (session.csrfToken.isNotBlank()) {
            requestBuilder.header("X-CSRFToken", session.csrfToken)
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (response.code == 401 || response.code == 403) {
                throw InstagramAuthRequiredException("نشست اینستاگرام منقضی شده است؛ لطفاً دوباره وارد شو.")
            }
            if (!response.isSuccessful) {
                throw HttpStatusException(response.code, "HTTP ${response.code}")
            }
            return body
        }
    }

    private fun checkAuthResponse(rawJson: String) {
        if (rawJson.contains("\"login_required\"") || rawJson.contains("\"checkpoint_required\"")) {
            throw InstagramAuthRequiredException("نیاز به ورود مجدد به حساب اینستاگرام است.")
        }
        val trimmed = rawJson.trimStart()
        if (trimmed.startsWith("<!DOCTYPE html", ignoreCase = true) || trimmed.startsWith("<html", ignoreCase = true)) {
            throw InstagramAuthRequiredException("نشست اینستاگرام معتبر نیست؛ دوباره وارد شو.")
        }
    }

    companion object {
        private const val IG_WEB_APP_ID = "936619743392459"
        const val DEFAULT_MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36"

        private val INVISIBLE_MARKS_REGEX = Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF]")
        private val COOKIE_HEADER_PREFIX_REGEX = Regex("(?im)^\\s*cookie\\s*:\\s*")
        private val CANONICAL_COOKIE_KEYS = setOf(
            "sessionid", "csrftoken", "ds_user_id", "mid", "ig_did", "rur", "datr", "ig_nrcb", "ps_l", "ps_n"
        )

        private fun sanitizeCookieValue(raw: String): String {
            return raw
                .replace(INVISIBLE_MARKS_REGEX, "")
                .trim()
                .trim('"', '\'', ';', ',')
                .filter { it.code in 0x21..0x7E }
        }

        private fun extractNumericUserIdFromSessionId(sessionId: String): String {
            return sessionId
                .substringBefore("%3A")
                .substringBefore("%3a")
                .substringBefore(':')
                .takeIf { it.isNotBlank() && it.all(Char::isDigit) }
                .orEmpty()
        }

        private fun parseCookieMap(rawCookie: String?): Map<String, String> {
            if (rawCookie.isNullOrBlank()) return emptyMap()
            val cleanedInput = COOKIE_HEADER_PREFIX_REGEX
                .replace(rawCookie.replace(INVISIBLE_MARKS_REGEX, ""), "")
                .trim()
            val cookieMap = linkedMapOf<String, String>()
            cleanedInput.split(';', '\n', '\r').forEach { rawPart ->
                val part = rawPart.trim()
                if (part.isBlank()) return@forEach
                val idx = part.indexOf('=')
                if (idx > 0) {
                    val rawKey = part.substring(0, idx).trim()
                    val lowerKey = rawKey.lowercase(Locale.US)
                    val key = if (lowerKey in CANONICAL_COOKIE_KEYS) lowerKey else rawKey
                    val value = sanitizeCookieValue(part.substring(idx + 1))
                    if (
                        key.isNotBlank() &&
                        value.isNotBlank() &&
                        !value.equals("deleted", ignoreCase = true) &&
                        !value.equals("null", ignoreCase = true)
                    ) {
                        cookieMap[key] = value
                    }
                }
            }
            return cookieMap
        }

        fun parseInstagramCookieString(
            rawCookie: String?,
            userAgent: String = ""
        ): InstagramSession? {
            val cookieMap = parseCookieMap(rawCookie).toMutableMap()
            val sessionId = cookieMap["sessionid"].orEmpty()
            if (sessionId.isBlank()) return null

            val uid = cookieMap["ds_user_id"].orEmpty().ifBlank {
                extractNumericUserIdFromSessionId(sessionId)
            }
            if (uid.isNotBlank() && cookieMap["ds_user_id"].isNullOrBlank()) {
                cookieMap["ds_user_id"] = uid
            }

            val normalizedHeader = cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
            return InstagramSession(
                cookieHeader = normalizedHeader,
                csrfToken = cookieMap["csrftoken"].orEmpty(),
                userId = uid,
                username = "",
                userAgent = userAgent.ifBlank { DEFAULT_MOBILE_USER_AGENT }
            )
        }

        fun buildSessionFromManualToken(rawInput: String): InstagramSession? {
            val cleaned = rawInput
                .replace(INVISIBLE_MARKS_REGEX, "")
                .trim()
            if (cleaned.length < 8) return null

            // 1. JSON export from browser extensions (Cookie-Editor / EditThisCookie)
            if (cleaned.startsWith("[") || cleaned.startsWith("{")) {
                parseManualCookiesFromJson(cleaned)?.let { return it }
            }

            // 2. Chrome/Edge DevTools copied table rows (tab-separated)
            if (cleaned.contains('\t')) {
                val tsvMap = linkedMapOf<String, String>()
                cleaned.lines().forEach { line ->
                    val cols = line.split('\t')
                    if (cols.size >= 2) {
                        val key = cols[0].trim().lowercase(Locale.US)
                        val value = sanitizeCookieValue(cols[1])
                        if (key in CANONICAL_COOKIE_KEYS && value.isNotBlank() && !value.equals("deleted", ignoreCase = true)) {
                            tsvMap[key] = value
                        }
                    }
                }
                if (tsvMap["sessionid"] != null) {
                    val header = tsvMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
                    return parseInstagramCookieString(header)
                }
            }

            // 3. Key=Value / "Cookie: sessionid=..." / "sessionid: ..." / quoted pairs / multi-line headers
            val withoutCookiePrefix = COOKIE_HEADER_PREFIX_REGEX.replace(cleaned, "").trim()
            if (Regex("(?i)(?:^|[;\\s,\"'])sessionid[\"']?\\s*[=:]").containsMatchIn(withoutCookiePrefix)) {
                val normalizedPairs = withoutCookiePrefix
                    .replace(Regex("(?i)[\"']?(sessionid|csrftoken|ds_user_id|mid|ig_did|rur|datr)[\"']?\\s*[=:]\\s*")) { m ->
                        "${m.groupValues[1].lowercase(Locale.US)}="
                    }
                parseInstagramCookieString(normalizedPairs)?.let { return it }

                val fallbackSessionId = Regex("(?i)[\"']?sessionid[\"']?\\s*[=:]\\s*[\"']?([^\\s;\"',]+)")
                    .find(withoutCookiePrefix)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.let(::sanitizeCookieValue)
                    .orEmpty()
                if (fallbackSessionId.length >= 8 && !fallbackSessionId.equals("deleted", ignoreCase = true)) {
                    val uid = extractNumericUserIdFromSessionId(fallbackSessionId)
                    return InstagramSession(
                        cookieHeader = if (uid.isNotBlank()) "sessionid=$fallbackSessionId; ds_user_id=$uid" else "sessionid=$fallbackSessionId",
                        csrfToken = "",
                        userId = uid,
                        username = "",
                        userAgent = DEFAULT_MOBILE_USER_AGENT
                    )
                }
            }

            // 4. Bare token (allows line-wrap removal / quotes / trailing semicolon, rejects spaced sentences)
            val unwrapped = cleaned.trim('"', '\'', ';', ',', ' ')
            if (unwrapped.contains(' ')) return null
            val token = unwrapped
                .replace("\r", "")
                .replace("\n", "")
                .let(::sanitizeCookieValue)
            if (token.length < 8 || token.equals("deleted", ignoreCase = true) || token.equals("null", ignoreCase = true)) {
                return null
            }
            val extractedUid = extractNumericUserIdFromSessionId(token)
            return InstagramSession(
                cookieHeader = if (extractedUid.isNotBlank()) {
                    "sessionid=$token; ds_user_id=$extractedUid"
                } else {
                    "sessionid=$token"
                },
                csrfToken = "",
                userId = extractedUid,
                username = "",
                userAgent = DEFAULT_MOBILE_USER_AGENT
            )
        }

        private fun parseManualCookiesFromJson(jsonText: String): InstagramSession? {
            val cookieMap = linkedMapOf<String, String>()
            runCatching {
                when {
                    jsonText.startsWith("[") -> {
                        val arr = JSONArray(jsonText)
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            val name = obj.optString("name").trim().lowercase(Locale.US)
                            val value = sanitizeCookieValue(obj.optString("value"))
                            if (name.isNotBlank() && value.isNotBlank() && (name in CANONICAL_COOKIE_KEYS || name == "sessionid")) {
                                cookieMap[name] = value
                            }
                        }
                    }
                    jsonText.startsWith("{") -> {
                        val obj = JSONObject(jsonText)
                        val nestedArray = obj.optJSONArray("cookies")
                        if (nestedArray != null) {
                            for (i in 0 until nestedArray.length()) {
                                val item = nestedArray.optJSONObject(i) ?: continue
                                val name = item.optString("name").trim().lowercase(Locale.US)
                                val value = sanitizeCookieValue(item.optString("value"))
                                if (name.isNotBlank() && value.isNotBlank()) {
                                    cookieMap[name] = value
                                }
                            }
                        } else if (obj.has("name") && obj.has("value")) {
                            val name = obj.optString("name").trim().lowercase(Locale.US)
                            val value = sanitizeCookieValue(obj.optString("value"))
                            if (name.isNotBlank() && value.isNotBlank()) {
                                cookieMap[name] = value
                            }
                        } else {
                            val keys = obj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next().trim().lowercase(Locale.US)
                                val v = sanitizeCookieValue(obj.optString(k))
                                if (v.isNotBlank()) cookieMap[k] = v
                            }
                        }
                    }
                }
            }
            if (cookieMap["sessionid"].isNullOrBlank()) return null
            val header = cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
            return parseInstagramCookieString(header)
        }

        fun normalizeUsername(raw: String): String {
            var s = raw.trim()
            if (s.contains("instagram.com/", ignoreCase = true)) {
                s = s.substringAfter("instagram.com/", "")
                    .substringBefore('?')
                    .substringBefore('#')
                    .trim('/')
                    .substringBefore('/')
            }
            return s.removePrefix("@")
                .trim()
                .lowercase(Locale.US)
                .replace(Regex("[^a-z0-9._]"), "")
        }

        fun parseReelsTrayJson(jsonText: String): List<ParsedTrayEntry> {
            if (jsonText.isBlank()) return emptyList()
            return runCatching {
                val root = JSONObject(jsonText)
                val tray = root.optJSONArray("tray") ?: return@runCatching emptyList()
                val result = mutableListOf<ParsedTrayEntry>()
                for (i in 0 until tray.length()) {
                    val obj = tray.optJSONObject(i) ?: continue
                    val userObj = obj.optJSONObject("user") ?: continue
                    val username = userObj.optString("username").trim()
                    if (username.isBlank()) continue
                    val reelId = obj.optString("id").trim()
                        .ifBlank { userObj.optString("pk").trim() }
                    val fullName = userObj.optString("full_name").trim().ifBlank { username }
                    val avatar = unescapeIgUrl(userObj.optString("profile_pic_url"))

                    val items = parseReelMediaItems(
                        itemsArray = obj.optJSONArray("items"),
                        fallbackUsername = username
                    )
                    result.add(
                        ParsedTrayEntry(
                            reelId = reelId,
                            group = InstagramStoryTrayGroup(
                                id = username,
                                username = username,
                                displayName = fullName,
                                avatarUrl = avatar,
                                isActiveStoryBucket = true,
                                badgeLabel = if (items.isNotEmpty()) {
                                    buildStoryBadgeLabel(items, isLiveStory = true)
                                } else {
                                    "استوری"
                                },
                                items = items
                            )
                        )
                    )
                }
                result
            }.getOrDefault(emptyList())
        }

        fun parseReelsMediaJson(jsonText: String): Map<String, List<InstagramStoryItem>> {
            if (jsonText.isBlank()) return emptyMap()
            return runCatching {
                val root = JSONObject(jsonText)
                val map = mutableMapOf<String, List<InstagramStoryItem>>()

                val reelsObj = root.optJSONObject("reels")
                if (reelsObj != null) {
                    val keys = reelsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val reel = reelsObj.optJSONObject(key) ?: continue
                        val username = reel.optJSONObject("user")?.optString("username")?.trim().orEmpty()
                        val items = parseReelMediaItems(reel.optJSONArray("items"), username)
                        if (items.isNotEmpty()) {
                            map[key] = items
                        }
                    }
                }

                val reelsArr = root.optJSONArray("reels_media")
                if (reelsArr != null) {
                    for (i in 0 until reelsArr.length()) {
                        val reel = reelsArr.optJSONObject(i) ?: continue
                        val id = reel.optString("id").trim()
                            .ifBlank { reel.optJSONObject("user")?.optString("pk")?.trim().orEmpty() }
                        val username = reel.optJSONObject("user")?.optString("username")?.trim().orEmpty()
                        val items = parseReelMediaItems(reel.optJSONArray("items"), username)
                        if (id.isNotBlank() && items.isNotEmpty()) {
                            map[id] = items
                        }
                    }
                }
                map
            }.getOrDefault(emptyMap())
        }

        fun parseReelMediaItems(
            itemsArray: JSONArray?,
            fallbackUsername: String
        ): List<InstagramStoryItem> {
            if (itemsArray == null || itemsArray.length() == 0) return emptyList()
            val items = mutableListOf<InstagramStoryItem>()
            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.optJSONObject(i) ?: continue
                val id = item.optString("id").trim()
                    .ifBlank { item.optString("pk").trim() }
                    .ifBlank { "${fallbackUsername}_story_$i" }
                val mediaTypeInt = item.optInt("media_type", 1)

                val imageUrl = extractBestCandidateUrl(
                    item.optJSONObject("image_versions2")?.optJSONArray("candidates")
                )
                val videoUrl = extractBestCandidateUrl(item.optJSONArray("video_versions"))
                val isVideo = mediaTypeInt == 2 || videoUrl.isNotBlank()

                val finalMediaUrl = if (isVideo) videoUrl.ifBlank { imageUrl } else imageUrl
                if (finalMediaUrl.isBlank()) continue

                val caption = item.optJSONObject("caption")?.optString("text")?.trim().orEmpty()
                val takenAt = item.optLong("taken_at", 0L)
                val timestampFa = if (takenAt > 0L) formatUnixTimeFa(takenAt) else "استوری زنده"
                val storyPk = item.optString("pk").trim().ifBlank { id.substringBefore("_") }
                val permalink = if (fallbackUsername.isNotBlank() && storyPk.isNotBlank()) {
                    "https://www.instagram.com/stories/$fallbackUsername/$storyPk/"
                } else if (fallbackUsername.isNotBlank()) {
                    "https://www.instagram.com/$fallbackUsername/"
                } else {
                    ""
                }

                items.add(
                    InstagramStoryItem(
                        id = id,
                        mediaType = if (isVideo) InstagramMediaType.VIDEO else InstagramMediaType.IMAGE,
                        mediaUrl = finalMediaUrl,
                        thumbnailUrl = imageUrl,
                        caption = caption,
                        permalink = permalink,
                        timestampFa = timestampFa,
                        isLiveStory = true
                    )
                )
            }
            return items
        }

        private fun extractBestCandidateUrl(array: JSONArray?): String {
            if (array == null || array.length() == 0) return ""
            var bestUrl = ""
            var bestScore = -1L
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val url = unescapeIgUrl(obj.optString("url"))
                if (url.isBlank()) continue
                val width = obj.optLong("width", 0L)
                val height = obj.optLong("height", 0L)
                val score = width * height
                if (bestUrl.isBlank() || score > bestScore) {
                    bestScore = score
                    bestUrl = url
                }
            }
            return bestUrl
        }

        private fun buildStoryBadgeLabel(
            items: List<InstagramStoryItem>,
            isLiveStory: Boolean
        ): String {
            val hasVideo = items.any { it.mediaType == InstagramMediaType.VIDEO }
            return when {
                isLiveStory && items.size > 1 -> "${SportsRepository.toPersianDigits(items.size)} استوری"
                isLiveStory -> "استوری"
                hasVideo -> "ریلز و پست"
                else -> "${SportsRepository.toPersianDigits(items.size)} پست"
            }
        }

        fun parseWebProfileInfoJson(
            jsonText: String,
            fallbackUsername: String
        ): InstagramStoryTrayGroup? {
            if (jsonText.isBlank()) return null
            return runCatching {
                val root = JSONObject(jsonText)
                val user = root.optJSONObject("data")?.optJSONObject("user")
                    ?: root.optJSONObject("user")
                    ?: return@runCatching null

                val username = user.optString("username").trim().ifBlank { fallbackUsername }
                val fullName = user.optString("full_name").trim()
                val avatar = unescapeIgUrl(
                    user.optString("profile_pic_url_hd")
                        .ifBlank { user.optString("profile_pic_url") }
                )

                val collected = mutableListOf<InstagramStoryItem>()
                val seenIds = mutableSetOf<String>()

                fun addEdgeArray(edges: JSONArray?) {
                    if (edges == null) return
                    for (i in 0 until edges.length()) {
                        val node = edges.optJSONObject(i)?.optJSONObject("node") ?: continue
                        val baseId = node.optString("id").trim().ifBlank { "${username}_$i" }
                        val shortcode = node.optString("shortcode").trim()
                        val isVideo = node.optBoolean("is_video", false)
                        val permalink = if (shortcode.isNotBlank()) {
                            if (node.optString("product_type") == "clips") {
                                "https://www.instagram.com/reel/$shortcode/"
                            } else {
                                "https://www.instagram.com/p/$shortcode/"
                            }
                        } else {
                            "https://www.instagram.com/$username/"
                        }

                        val caption = node.optJSONObject("edge_media_to_caption")
                            ?.optJSONArray("edges")
                            ?.optJSONObject(0)
                            ?.optJSONObject("node")
                            ?.optString("text")
                            ?.trim()
                            .orEmpty()

                        val timestampSec = node.optLong("taken_at_timestamp", 0L)
                        val timestampFa = if (timestampSec > 0L) formatUnixTimeFa(timestampSec) else "تازه"

                        val sidecarEdges = node.optJSONObject("edge_sidecar_to_children")
                            ?.optJSONArray("edges")
                        if (sidecarEdges != null && sidecarEdges.length() > 0) {
                            for (c in 0 until sidecarEdges.length()) {
                                val child = sidecarEdges.optJSONObject(c)?.optJSONObject("node") ?: continue
                                val childId = child.optString("id").ifBlank { "${baseId}_$c" }
                                if (!seenIds.add(childId)) continue
                                val childIsVideo = child.optBoolean("is_video", false)
                                val display = unescapeIgUrl(child.optString("display_url"))
                                val video = unescapeIgUrl(child.optString("video_url"))
                                val mediaUrl = if (childIsVideo) video.ifBlank { display } else display
                                if (mediaUrl.isBlank()) continue
                                collected.add(
                                    InstagramStoryItem(
                                        id = childId,
                                        mediaType = if (childIsVideo) InstagramMediaType.VIDEO else InstagramMediaType.IMAGE,
                                        mediaUrl = mediaUrl,
                                        thumbnailUrl = display,
                                        caption = caption,
                                        permalink = permalink,
                                        timestampFa = timestampFa,
                                        isLiveStory = false
                                    )
                                )
                            }
                            continue
                        }

                        if (!seenIds.add(baseId)) continue
                        val display = unescapeIgUrl(
                            node.optString("display_url").ifBlank { node.optString("thumbnail_src") }
                        )
                        val video = unescapeIgUrl(node.optString("video_url"))
                        val mediaUrl = if (isVideo) video.ifBlank { display } else display
                        if (mediaUrl.isBlank()) continue

                        collected.add(
                            InstagramStoryItem(
                                id = baseId,
                                mediaType = if (isVideo) InstagramMediaType.VIDEO else InstagramMediaType.IMAGE,
                                mediaUrl = mediaUrl,
                                thumbnailUrl = display,
                                caption = caption,
                                permalink = permalink,
                                timestampFa = timestampFa,
                                isLiveStory = false
                            )
                        )
                    }
                }

                addEdgeArray(
                    user.optJSONObject("edge_owner_to_timeline_media")?.optJSONArray("edges")
                )
                addEdgeArray(
                    user.optJSONObject("edge_felix_video_timeline")?.optJSONArray("edges")
                )

                val trimmed = collected.take(8)
                if (trimmed.isEmpty()) return@runCatching null

                InstagramStoryTrayGroup(
                    id = username,
                    username = username,
                    displayName = fullName.ifBlank { username },
                    avatarUrl = avatar.ifBlank { trimmed.first().thumbnailUrl.ifBlank { trimmed.first().mediaUrl } },
                    isActiveStoryBucket = false,
                    badgeLabel = buildStoryBadgeLabel(trimmed, isLiveStory = false),
                    items = trimmed
                )
            }.getOrNull()
        }

        fun unescapeIgString(raw: String): String {
            if (raw.isBlank()) return ""
            return raw
                .replace("\\/", "/")
                .replace("\\u0026", "&")
                .replace("\\u003c", "<")
                .replace("\\u003e", ">")
                .replace("\\\"", "\"")
                .replace("&amp;", "&")
                .trim()
        }

        fun unescapeIgUrl(raw: String): String {
            val cleaned = unescapeIgString(raw)
            if (cleaned.isBlank() || cleaned == "null") return ""
            return when {
                cleaned.startsWith("http://") -> "https://" + cleaned.removePrefix("http://")
                cleaned.startsWith("//") -> "https:$cleaned"
                else -> cleaned
            }
        }

        fun formatUnixTimeFa(epochSeconds: Long): String {
            val diffMin = ((System.currentTimeMillis() / 1000L - epochSeconds) / 60L).coerceAtLeast(0L)
            return when {
                diffMin < 2L -> "هم‌اکنون"
                diffMin < 60L -> "${SportsRepository.toPersianDigits(diffMin.toInt())} دقیقه پیش"
                diffMin < 24L * 60L -> "${SportsRepository.toPersianDigits((diffMin / 60L).toInt())} ساعت پیش"
                else -> "${SportsRepository.toPersianDigits((diffMin / (24L * 60L)).toInt())} روز پیش"
            }
        }

        fun formatIgTimestampFa(rawIso: String): String {
            if (rawIso.isBlank()) return "تازه"
            val patterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
            )
            val millis = patterns.firstNotNullOfOrNull { pattern ->
                runCatching {
                    SimpleDateFormat(pattern, Locale.US).parse(rawIso.trim())?.time
                }.getOrNull()
            } ?: return "تازه"

            val diffMin = ((System.currentTimeMillis() - millis) / 60_000L).coerceAtLeast(0L)
            return when {
                diffMin < 2L -> "هم‌اکنون"
                diffMin < 60L -> "${SportsRepository.toPersianDigits(diffMin.toInt())} دقیقه پیش"
                diffMin < 24L * 60L -> "${SportsRepository.toPersianDigits((diffMin / 60L).toInt())} ساعت پیش"
                else -> "${SportsRepository.toPersianDigits((diffMin / (24L * 60L)).toInt())} روز پیش"
            }
        }
    }
}
