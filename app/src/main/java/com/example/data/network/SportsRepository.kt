package com.example.data.network

import android.util.Xml
import androidx.core.text.HtmlCompat
import com.example.data.model.BroadcastProgram
import com.example.data.model.BroadcastSource
import com.example.data.model.HighlightVideo
import com.example.data.model.LeagueGroup
import com.example.data.model.LineupPlayer
import com.example.data.model.LineupSide
import com.example.data.model.MatchDetailInfo
import com.example.data.model.MatchEvent
import com.example.data.model.MatchGoals
import com.example.data.model.MatchSummary
import com.example.data.model.MergedBroadcastGame
import com.example.data.model.NewsArticle
import com.example.data.model.NewsCategory
import com.example.data.model.StatEntry
import com.example.data.model.StreamQuality
import com.example.data.model.TeamInfo
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
import org.json.JSONTokener
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class SportsRepository {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchProgramsForSource(
        source: BroadcastSource,
        customAntenUrl: String,
        antenToken: String
    ): List<BroadcastProgram> = withContext(Dispatchers.IO) {
        when (source) {
            BroadcastSource.ANTEN -> {
                val url = customAntenUrl.ifBlank { BroadcastSource.ANTEN.defaultUrl }
                val jsonText = executeGet(url, bearerToken = antenToken)
                parseAntenPrograms(jsonText)
            }
            BroadcastSource.APARAT -> {
                val jsonText = executeGet(BroadcastSource.APARAT.defaultUrl, bearerToken = antenToken)
                parseAparatPrograms(jsonText)
            }
        }
    }

    fun mergeBroadcastPrograms(
        antenItems: List<BroadcastProgram>,
        aparatItems: List<BroadcastProgram>
    ): List<MergedBroadcastGame> {
        val merged = linkedMapOf<String, MutableMap<BroadcastSource, BroadcastProgram>>()
        val titles = mutableMapOf<String, String>()
        val times = mutableMapOf<String, String>()
        val liveMap = mutableMapOf<String, Boolean>()
        val posters = mutableMapOf<String, String>()
        val hostLogos = mutableMapOf<String, String>()
        val guestLogos = mutableMapOf<String, String>()
        val leagues = mutableMapOf<String, String>()

        fun addItems(items: List<BroadcastProgram>) {
            for (program in items) {
                val cleanTitle = cleanGameName(program.title)
                val key = gameKey(cleanTitle).ifBlank { "${program.source.id}:${program.id}" }
                val bucket = merged.getOrPut(key) { linkedMapOf() }
                bucket[program.source] = program
                if (!titles.containsKey(key) && cleanTitle.isNotBlank()) titles[key] = cleanTitle
                if (times[key].isNullOrBlank() && program.time.isNotBlank()) times[key] = program.time
                if (posters[key].isNullOrBlank() && program.posterUrl.isNotBlank()) posters[key] = program.posterUrl
                if (hostLogos[key].isNullOrBlank() && program.hostLogoUrl.isNotBlank()) hostLogos[key] = program.hostLogoUrl
                if (guestLogos[key].isNullOrBlank() && program.guestLogoUrl.isNotBlank()) guestLogos[key] = program.guestLogoUrl
                if (leagues[key].isNullOrBlank() && program.leagueTitle.isNotBlank()) leagues[key] = program.leagueTitle
                if (program.isLive) liveMap[key] = true
            }
        }

        addItems(antenItems)
        addItems(aparatItems)

        return merged.entries.map { (key, sourceMap) ->
            MergedBroadcastGame(
                key = key,
                title = titles[key] ?: "مسابقه",
                time = times[key] ?: "",
                isLive = liveMap[key] == true,
                posterUrl = posters[key] ?: "",
                hostLogoUrl = hostLogos[key] ?: "",
                guestLogoUrl = guestLogos[key] ?: "",
                leagueTitle = leagues[key] ?: "",
                sources = sourceMap
            )
        }
    }

    suspend fun fetchProgramVisualDetails(
        program: BroadcastProgram,
        customAntenUrl: String,
        antenToken: String
    ): BroadcastProgram = withContext(Dispatchers.IO) {
        when (program.source) {
            BroadcastSource.APARAT -> {
                val encoded = URLEncoder.encode(program.id, "UTF-8")
                val url = "https://www.aparatsport.com/api/fa/v1/sport/sport/get_match_with_uuid?uuid=$encoded"
                val raw = executeGet(url, bearerToken = antenToken)
                val root = JSONObject(raw)
                val data = root.optJSONObject("data") ?: root
                val poster = extractPosterUrl(data, "https://www.aparatsport.com")
                    .ifBlank { extractPosterUrl(root, "https://www.aparatsport.com") }
                    .ifBlank { program.posterUrl }
                val (hLogo, gLogo) = extractTeamLogos(data, "https://www.aparatsport.com")
                program.copy(
                    posterUrl = poster,
                    hostLogoUrl = program.hostLogoUrl.ifBlank { hLogo },
                    guestLogoUrl = program.guestLogoUrl.ifBlank { gLogo },
                    leagueTitle = program.leagueTitle.ifBlank { extractLeagueTitle(data) }
                )
            }
            BroadcastSource.ANTEN -> {
                val base = (customAntenUrl.ifBlank { BroadcastSource.ANTEN.defaultUrl })
                    .substringBefore("/programs")
                    .trimEnd('/')
                val raw = try {
                    executeGet("$base/programs/details/${program.id}", bearerToken = antenToken)
                } catch (e: HttpStatusException) {
                    if (e.code == 404) {
                        executeGet("$base/programs/${program.id}", bearerToken = antenToken)
                    } else {
                        throw e
                    }
                }
                val root = JSONObject(raw)
                val data = root.optJSONObject("data") ?: root
                val progObj = data.optJSONObject("program") ?: data
                val poster = extractPosterUrl(progObj, "https://www.anten.ir")
                    .ifBlank { extractPosterUrl(data, "https://www.anten.ir") }
                    .ifBlank { extractPosterUrl(root, "https://www.anten.ir") }
                    .ifBlank { program.posterUrl }
                val (hLogo, gLogo) = extractTeamLogos(progObj, "https://www.anten.ir")
                program.copy(
                    posterUrl = poster,
                    hostLogoUrl = program.hostLogoUrl.ifBlank { hLogo },
                    guestLogoUrl = program.guestLogoUrl.ifBlank { gLogo },
                    leagueTitle = program.leagueTitle.ifBlank { extractLeagueTitle(progObj) }
                )
            }
        }
    }

    suspend fun fetchQualitiesForProgram(
        program: BroadcastProgram,
        customAntenUrl: String,
        antenToken: String
    ): List<StreamQuality> = withContext(Dispatchers.IO) {
        when (program.source) {
            BroadcastSource.APARAT -> {
                val encoded = URLEncoder.encode(program.id, "UTF-8")
                val url = "https://www.aparatsport.com/api/fa/v1/sport/sport/get_match_with_uuid?uuid=$encoded"
                val raw = executeGet(url, bearerToken = antenToken)
                val root = JSONObject(raw)
                val data = root.optJSONObject("data") ?: root
                val list = mutableListOf<StreamQuality>()
                val m3u8 = data.optString("m3u8").trim()
                if (m3u8.isNotBlank() && m3u8 != "null") {
                    list.add(StreamQuality(label = "پخش اصلی", directUrl = m3u8))
                }
                val otherLangs = data.optJSONArray("other_language")
                if (otherLangs != null) {
                    for (i in 0 until otherLangs.length()) {
                        val item = otherLangs.optJSONObject(i) ?: continue
                        val altUrl = item.optString("m3u8").trim()
                        if (altUrl.isNotBlank() && altUrl != "null") {
                            val label = item.optString("reporter_name").ifBlank {
                                item.optString("title").ifBlank { "گزارش ${toPersianDigits(i + 1)}" }
                            }
                            list.add(StreamQuality(label = label, directUrl = altUrl))
                        }
                    }
                }
                list
            }
            BroadcastSource.ANTEN -> {
                val base = (customAntenUrl.ifBlank { BroadcastSource.ANTEN.defaultUrl })
                    .substringBefore("/programs")
                    .trimEnd('/')
                val raw = try {
                    executeGet("$base/programs/details/${program.id}", bearerToken = antenToken)
                } catch (e: HttpStatusException) {
                    if (e.code == 404) {
                        executeGet("$base/programs/${program.id}", bearerToken = antenToken)
                    } else {
                        throw e
                    }
                }
                val root = JSONObject(raw)
                val data = root.optJSONObject("data") ?: root
                val streams = data.optJSONArray("streams")
                    ?: data.optJSONObject("program")?.optJSONArray("streams")
                    ?: JSONArray()
                val qualities = mutableListOf<StreamQuality>()
                for (i in 0 until streams.length()) {
                    val stream = streams.optJSONObject(i) ?: continue
                    val hq = stream.optString("hqUrlAccess").trim()
                    val low = stream.optString("lowUrlAccess").trim()
                    if (hq.isNotBlank() && hq != "null") {
                        qualities.add(StreamQuality(label = "کیفیت بالا", accessUrl = hq))
                    }
                    if (low.isNotBlank() && low != "null") {
                        qualities.add(StreamQuality(label = "مصرف کمتر", accessUrl = low))
                    }
                }
                qualities
            }
        }
    }

    suspend fun resolveStreamUrl(
        quality: StreamQuality,
        antenToken: String
    ): String = withContext(Dispatchers.IO) {
        if (!quality.directUrl.isNullOrBlank()) return@withContext quality.directUrl
        val accessUrl = quality.accessUrl ?: throw IllegalStateException("آدرس دسترسی پخش موجود نیست.")
        val raw = executeGet(accessUrl, bearerToken = antenToken)
        val root = JSONObject(raw)
        val data = root.optJSONObject("data") ?: root
        val url = data.optString("url").trim()
        if (url.isBlank() || url == "null") {
            throw IllegalStateException("لینک پخش دریافت نشد.")
        }
        url
    }

    suspend fun fetchVarzesh3LiveScores(): List<LeagueGroup> = withContext(Dispatchers.IO) {
        val raw = executeGet("https://web-api.varzesh3.ir/v2.0/livescore/today")
        val array = JSONArray(raw)
        val leagues = mutableListOf<LeagueGroup>()
        for (i in 0 until array.length()) {
            val leagueObj = array.optJSONObject(i) ?: continue
            val leagueTitle = leagueObj.optString("title").ifBlank { "مسابقات" }
            val sport = leagueObj.optInt("sport", 1)
            val dates = leagueObj.optJSONArray("dates") ?: JSONArray()
            val matches = mutableListOf<MatchSummary>()
            for (d in 0 until dates.length()) {
                val dateObj = dates.optJSONObject(d) ?: continue
                val dateMatches = dateObj.optJSONArray("matches") ?: continue
                for (m in 0 until dateMatches.length()) {
                    val matchObj = dateMatches.optJSONObject(m) ?: continue
                    matches.add(parseMatchSummary(matchObj))
                }
            }
            if (matches.isNotEmpty()) {
                leagues.add(LeagueGroup(title = leagueTitle, sport = sport, matches = matches))
            }
        }
        leagues
    }

    suspend fun fetchVarzesh3Events(matchId: String): List<MatchEvent> = withContext(Dispatchers.IO) {
        val raw = executeGet("https://web-api.varzesh3.ir/v2.0/livescore/football/matches/$matchId/events")
        val array = JSONArray(raw)
        val events = mutableListOf<MatchEvent>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val timeStr = obj.opt("time")?.toString()?.takeIf { it != "null" } ?: "—"
            val rawTime = obj.optInt("rawTime", timeStr.filter { it.isDigit() }.toIntOrNull() ?: 0)
            val videoLink = safeHttpsUrl(obj.optString("videoLink"), setOf("video.varzesh3.ir"))
            val rawVideoId = obj.opt("videoId")?.toString()?.takeIf { it != "null" && it.all(Char::isDigit) }
                ?: Regex("""/video/(\d+)""").find(videoLink)?.groupValues?.getOrNull(1)
                ?: ""
            val resultObj = obj.optJSONObject("matchResult")
            events.add(
                MatchEvent(
                    id = "${i}_${rawTime}_${obj.optInt("eventType")}",
                    time = timeStr,
                    rawTime = rawTime,
                    eventType = obj.optInt("eventType", 0),
                    side = obj.optInt("side", 0),
                    description = obj.optString("description").takeIf { it != "null" } ?: "",
                    decisionTitle = obj.optString("decisionTitle").takeIf { it != "null" } ?: "",
                    strikerName = obj.optString("strickerName").takeIf { it != "null" } ?: "",
                    assisterName = obj.optString("assisterName").takeIf { it != "null" } ?: "",
                    hostScore = if (resultObj != null && !resultObj.isNull("host")) resultObj.optInt("host") else null,
                    guestScore = if (resultObj != null && !resultObj.isNull("guest")) resultObj.optInt("guest") else null,
                    videoId = rawVideoId,
                    videoLink = videoLink
                )
            )
        }
        events.sortedBy { it.rawTime }
    }

    suspend fun fetchVarzesh3MatchDetails(matchId: String): MatchDetailInfo = withContext(Dispatchers.IO) {
        val pageUrl = "https://www.varzesh3.ir/football/match/$matchId/"
        val html = executeGet(pageUrl, accept = "text/html")
        val matchJson = extractNextFlightMatchData(html, matchId.toLongOrNull() ?: -1L)
            ?: throw IllegalStateException("ساختار جزئیات مسابقه در صفحه پیدا نشد.")

        val stadium = matchJson.optString("stadium").takeIf { it != "null" } ?: ""
        val refereeObj = matchJson.opt("referee")
        val referee = when (refereeObj) {
            is JSONObject -> refereeObj.optString("name").takeIf { it != "null" } ?: ""
            is String -> refereeObj.takeIf { it != "null" } ?: ""
            else -> ""
        }
        val week = matchJson.opt("week")?.toString()?.takeIf { it != "null" && it.isNotBlank() } ?: ""

        val statsArray = matchJson.optJSONArray("stats") ?: JSONArray()
        var chosenStatsSection: JSONObject? = null
        for (i in 0 until statsArray.length()) {
            val sec = statsArray.optJSONObject(i) ?: continue
            val title = sec.optString("title")
            if (title.contains("مجموع") || title.contains("کل")) {
                chosenStatsSection = sec
                break
            }
            chosenStatsSection = sec
        }

        val statEntries = mutableListOf<StatEntry>()
        val statsTitle = chosenStatsSection?.optString("title")?.ifBlank { "مجموع بازی" } ?: "مجموع بازی"
        val innerStats = chosenStatsSection?.optJSONObject("stats")
        if (innerStats != null) {
            parseStatEntry(innerStats.optJSONObject("possession"))?.let { statEntries.add(it) }
            val items = innerStats.optJSONArray("items")
            if (items != null) {
                for (i in 0 until items.length()) {
                    parseStatEntry(items.optJSONObject(i))?.let { statEntries.add(it) }
                }
            }
        }

        val lineupObj = matchJson.optJSONObject("lineup")
        val hostLineup = parseLineupSide(lineupObj?.optJSONObject("host"))
        val guestLineup = parseLineupSide(lineupObj?.optJSONObject("guest"))

        MatchDetailInfo(
            stadium = stadium,
            referee = referee,
            week = week,
            pageUrl = pageUrl,
            statsTitle = statsTitle,
            stats = statEntries,
            hostLineup = hostLineup,
            guestLineup = guestLineup
        )
    }

    suspend fun fetchVarzesh3HighlightVideo(videoId: String): HighlightVideo = withContext(Dispatchers.IO) {
        val html = executeGet("https://video.varzesh3.ir/video/$videoId/", accept = "text/html")
        val ldRegex = Regex(
            """<script\b[^>]*type=["']application/ld\+json["'][^>]*>([\s\S]*?)</script>""",
            setOf(RegexOption.IGNORE_CASE)
        )
        var videoObj: JSONObject? = null
        for (match in ldRegex.findAll(html)) {
            val rawJson = match.groupValues[1].trim()
            runCatching {
                val parsed = JSONTokener(rawJson).nextValue()
                val candidates = mutableListOf<JSONObject>()
                when (parsed) {
                    is JSONArray -> {
                        for (i in 0 until parsed.length()) {
                            parsed.optJSONObject(i)?.let { candidates.add(it) }
                        }
                    }
                    is JSONObject -> {
                        val graph = parsed.optJSONArray("@graph")
                        if (graph != null) {
                            for (i in 0 until graph.length()) {
                                graph.optJSONObject(i)?.let { candidates.add(it) }
                            }
                        } else {
                            candidates.add(parsed)
                        }
                    }
                }
                videoObj = candidates.firstOrNull { it.optString("@type") == "VideoObject" }
            }
            if (videoObj != null) break
        }

        val found = videoObj ?: throw IllegalStateException("اطلاعات ویدئوی صحنه پیدا نشد.")
        val contentUrl = found.optString("contentUrl").trim()
        val safeSrc = safeHttpsUrl(contentUrl, setOf("video-vcdn.varzesh3.ir"))
        if (safeSrc.isBlank() || !safeSrc.lowercase().substringBefore('?').endsWith(".mp4")) {
            throw IllegalStateException("فایل قابل پخش برای این صحنه پیدا نشد.")
        }
        val thumbRaw = when (val t = found.opt("thumbnailUrl")) {
            is JSONArray -> t.optString(0)
            is String -> t
            else -> ""
        }
        val poster = safeHttpsUrl(thumbRaw, setOf("video-icdn.varzesh3.ir"))
        HighlightVideo(
            id = videoId,
            title = found.optString("name").ifBlank { "ویدئوی صحنه" },
            mp4Url = safeSrc,
            posterUrl = poster
        )
    }

    suspend fun fetchSportsNews(): List<NewsArticle> = withContext(Dispatchers.IO) {
        coroutineScope {
            val rssDeferred = async {
                runCatching {
                    val rssXml = executeGet(
                        url = "https://www.varzesh3.ir/rss/all",
                        accept = "application/rss+xml, application/xml, text/xml"
                    )
                    parseVarzesh3Rss(rssXml)
                }.getOrDefault(emptyList())
            }
            val htmlDeferred = async {
                runCatching {
                    executeGet("https://www.varzesh3.ir/news", accept = "text/html")
                }.getOrDefault("")
            }

            val rssArticles = rssDeferred.await()
            val newsHtml = htmlDeferred.await()
            val htmlImageMap = if (newsHtml.isNotBlank()) {
                extractVarzesh3NewsImageMap(newsHtml)
            } else {
                emptyMap()
            }
            val htmlArticles = if (newsHtml.isNotBlank()) {
                parseVarzesh3NewsHtml(newsHtml, htmlImageMap)
            } else {
                emptyList()
            }

            val baseArticles = if (rssArticles.isNotEmpty()) {
                val rssIds = rssArticles.mapTo(mutableSetOf()) { it.id }
                val enrichedRss = rssArticles.map { article ->
                    if (article.imageUrl.isBlank()) {
                        val mappedImg = htmlImageMap[article.id].orEmpty()
                        if (mappedImg.isNotBlank()) article.copy(imageUrl = mappedImg) else article
                    } else {
                        article
                    }
                }
                val extraHtml = htmlArticles.filter { it.id !in rssIds && it.imageUrl.isNotBlank() }
                (enrichedRss + extraHtml).take(36)
            } else {
                htmlArticles.take(36)
            }

            if (baseArticles.isEmpty()) {
                throw IllegalStateException("اخبار ورزشی در حال حاضر دریافت نشد.")
            }

            // Enrich any top articles still missing a real image using each article's og:image / lead photo
            val missingIndices = baseArticles.indices
                .take(18)
                .filter { baseArticles[it].imageUrl.isBlank() }

            if (missingIndices.isEmpty()) {
                return@coroutineScope baseArticles
            }

            val semaphore = Semaphore(6)
            val resolvedMap = missingIndices.map { idx ->
                async {
                    semaphore.withPermit {
                        val img = runCatching {
                            fetchNewsArticleOgImage(baseArticles[idx].link)
                        }.getOrDefault("")
                        idx to img
                    }
                }
            }.awaitAll().toMap()

            baseArticles.mapIndexed { idx, article ->
                val foundImg = resolvedMap[idx].orEmpty()
                if (article.imageUrl.isBlank() && foundImg.isNotBlank()) {
                    article.copy(imageUrl = foundImg)
                } else {
                    article
                }
            }
        }
    }

    suspend fun fetchNewsArticleOgImage(articleUrl: String): String = withContext(Dispatchers.IO) {
        val safeUrl = safeVarzesh3MatchUrl(articleUrl)
        if (safeUrl.isBlank()) return@withContext ""
        val html = executeGet(safeUrl, accept = "text/html")
        extractOgImageUrl(html)
    }

    suspend fun fetchNewsArticleDetail(articleUrl: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val safeUrl = safeVarzesh3MatchUrl(articleUrl)
        if (safeUrl.isBlank()) throw IllegalStateException("نشانی خبر نامعتبر است.")
        val html = executeGet(safeUrl, accept = "text/html")
        val ogImage = extractOgImageUrl(html)
        val body = extractArticleBodyFromHtml(html)
        body to ogImage
    }

    suspend fun fetchNewsArticleBody(articleUrl: String): String =
        fetchNewsArticleDetail(articleUrl).first

    private fun extractArticleBodyFromHtml(html: String): String {
        val ldRegex = Regex(
            """<script\b[^>]*type=["']application/ld\+json["'][^>]*>([\s\S]*?)</script>""",
            setOf(RegexOption.IGNORE_CASE)
        )
        for (match in ldRegex.findAll(html)) {
            val body = runCatching {
                val parsed = JSONTokener(match.groupValues[1].trim()).nextValue()
                when (parsed) {
                    is JSONObject -> parsed.optString("articleBody").takeIf { it.isNotBlank() }
                    else -> null
                }
            }.getOrNull()
            if (!body.isNullOrBlank()) {
                return cleanHtmlText(body)
            }
        }

        val newsTextRegex = Regex(
            """<div[^>]*class=["'][^"']*news-text[^"']*["'][^>]*>([\s\S]*?)</div>""",
            setOf(RegexOption.IGNORE_CASE)
        )
        val divMatch = newsTextRegex.find(html)?.groupValues?.getOrNull(1)
        if (!divMatch.isNullOrBlank()) {
            val cleaned = cleanHtmlText(divMatch)
            if (cleaned.length > 40) return cleaned
        }

        val paragraphs = Regex("""<p[^>]*>([\s\S]*?)</p>""", setOf(RegexOption.IGNORE_CASE))
            .findAll(html)
            .map { cleanHtmlText(it.groupValues[1]) }
            .filter { it.length > 45 && !it.contains("کپی‌رایت") && !it.contains("ورزش سه") }
            .take(8)
            .toList()
        if (paragraphs.isNotEmpty()) {
            return paragraphs.joinToString("\n\n")
        }
        return ""
    }

    private fun parseVarzesh3Rss(xml: String): List<NewsArticle> =
        parseVarzesh3RssFeed(xml)

    private fun parseVarzesh3NewsHtml(
        html: String,
        imageMap: Map<String, String> = extractVarzesh3NewsImageMap(html)
    ): List<NewsArticle> = parseVarzesh3NewsPageHtml(html, imageMap)

    private fun parseAntenPrograms(raw: String): List<BroadcastProgram> {
        val parsed = JSONTokener(raw).nextValue()
        val sliderPosterMap = mutableMapOf<String, String>()
        if (parsed is JSONObject) {
            listOf("sliders", "banners", "featured", "topPrograms").forEach { key ->
                val arr = parsed.optJSONArray(key) ?: return@forEach
                for (i in 0 until arr.length()) {
                    val sObj = arr.optJSONObject(i) ?: continue
                    val pObj = sObj.optJSONObject("program") ?: sObj
                    val pid = pObj.opt("programId")?.toString()?.takeIf { it != "null" }
                        ?: pObj.opt("id")?.toString()?.takeIf { it != "null" }
                        ?: ""
                    val pUrl = extractPosterUrl(sObj, defaultHost = "https://www.anten.ir")
                        .ifBlank { extractPosterUrl(pObj, defaultHost = "https://www.anten.ir") }
                    if (pid.isNotBlank() && pUrl.isNotBlank()) {
                        sliderPosterMap[pid] = pUrl
                    }
                }
            }
        }

        val itemsArray: JSONArray = when (parsed) {
            is JSONArray -> parsed
            is JSONObject -> {
                parsed.optJSONArray("data")
                    ?: parsed.optJSONArray("programs")
                    ?: parsed.optJSONArray("items")
                    ?: run {
                        val cal = parsed.optJSONArray("calendar")
                        if (cal != null) {
                            val flattened = JSONArray()
                            for (d in 0 until cal.length()) {
                                val day = cal.optJSONObject(d) ?: continue
                                val dayDate = day.optString("date")
                                val dayItems = day.optJSONArray("items") ?: continue
                                for (k in 0 until dayItems.length()) {
                                    val entry = dayItems.optJSONObject(k) ?: continue
                                    val prog = entry.optJSONObject("program") ?: entry
                                    if (prog.optString("time").isBlank()) {
                                        val time = entry.optString("time").ifBlank { dayDate }
                                        prog.put("time", time)
                                    }
                                    flattened.put(prog)
                                }
                            }
                            flattened
                        } else {
                            JSONArray()
                        }
                    }
            }
            else -> JSONArray()
        }

        val result = mutableListOf<BroadcastProgram>()
        for (i in 0 until itemsArray.length()) {
            val rawObj = itemsArray.optJSONObject(i) ?: continue
            val obj = rawObj.optJSONObject("program") ?: rawObj
            val id = obj.opt("programId")?.toString()?.takeIf { it != "null" }
                ?: obj.opt("id")?.toString()?.takeIf { it != "null" }
                ?: i.toString()
            val title = obj.optString("titleProgram").ifBlank {
                obj.optString("title").ifBlank {
                    obj.optString("name").ifBlank { "برنامه" }
                }
            }
            val time = obj.optString("time").takeIf { it != "null" && it.isNotBlank() }
                ?: obj.optString("date").takeIf { it != "null" && it.isNotBlank() }
                ?: "جزئیات پخش"
            val live = obj.optBoolean("live", false) || obj.optBoolean("isLive", false)
            val poster = sliderPosterMap[id].orEmpty()
                .ifBlank { extractPosterUrl(obj, defaultHost = "https://www.anten.ir") }
                .ifBlank { extractPosterUrl(rawObj, defaultHost = "https://www.anten.ir") }
            val (hostLogo, guestLogo) = extractTeamLogos(obj, defaultHost = "https://www.anten.ir")
            result.add(
                BroadcastProgram(
                    id = id,
                    title = title,
                    time = time,
                    isLive = live,
                    source = BroadcastSource.ANTEN,
                    posterUrl = poster,
                    hostLogoUrl = hostLogo,
                    guestLogoUrl = guestLogo,
                    leagueTitle = extractLeagueTitle(obj)
                )
            )
        }
        return result
    }

    private fun parseAparatPrograms(raw: String): List<BroadcastProgram> {
        val parsed = JSONTokener(raw).nextValue()
        val array = when (parsed) {
            is JSONArray -> parsed
            is JSONObject -> parsed.optJSONArray("data")
                ?: parsed.optJSONArray("matches")
                ?: JSONArray()
            else -> JSONArray()
        }

        val result = mutableListOf<BroadcastProgram>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val detailArray = item.optJSONArray("match_detail")
            if (detailArray != null) {
                for (j in 0 until detailArray.length()) {
                    detailArray.optJSONObject(j)?.let { m ->
                        result.add(aparatMatchToProgram(m, fallbackObj = item))
                    }
                }
            } else {
                val matchObj = item.optJSONObject("match") ?: item
                result.add(aparatMatchToProgram(matchObj, fallbackObj = item))
            }
        }
        return result
    }

    private fun aparatMatchToProgram(obj: JSONObject, fallbackObj: JSONObject? = null): BroadcastProgram {
        val id = obj.optString("uuid").ifBlank { obj.opt("id")?.toString() ?: "" }
        val title = obj.optString("title").ifBlank {
            obj.optString("match_title").ifBlank {
                obj.optString("team1_name").ifBlank { "مسابقه" }
            }
        }
        val time = obj.optString("start_time_date").ifBlank {
            obj.optString("start_time").ifBlank { "" }
        }
        val live = obj.optBoolean("is_live", false) || obj.optBoolean("live", false)
        val poster = extractPosterUrl(obj, defaultHost = "https://www.aparatsport.com")
            .ifBlank {
                fallbackObj?.let { extractPosterUrl(it, defaultHost = "https://www.aparatsport.com") }.orEmpty()
            }
        val (hostLogo, guestLogo) = extractTeamLogos(obj, defaultHost = "https://www.aparatsport.com")
        val leagueTitle = extractLeagueTitle(obj)
            .ifBlank { fallbackObj?.let { extractLeagueTitle(it) }.orEmpty() }
        return BroadcastProgram(
            id = id,
            title = title,
            time = time,
            isLive = live,
            source = BroadcastSource.APARAT,
            posterUrl = poster,
            hostLogoUrl = hostLogo,
            guestLogoUrl = guestLogo,
            leagueTitle = leagueTitle
        )
    }

    private fun parseMatchSummary(obj: JSONObject): MatchSummary {
        val hostObj = obj.optJSONObject("host")
        val guestObj = obj.optJSONObject("guest")
        val goalsObj = obj.optJSONObject("goals")
        val goals = if (goalsObj != null && (!goalsObj.isNull("host") || !goalsObj.isNull("guest"))) {
            MatchGoals(
                host = if (goalsObj.isNull("host")) null else goalsObj.optInt("host"),
                guest = if (goalsObj.isNull("guest")) null else goalsObj.optInt("guest")
            )
        } else {
            null
        }
        val liveStreamLink = obj.optJSONObject("liveStream")
            ?.optString("link")
            ?.let { safeHttpsUrl(it, setOf("anten.ir", "www.anten.ir")) }
            ?: ""

        return MatchSummary(
            id = obj.opt("id")?.toString() ?: "",
            host = TeamInfo(
                id = hostObj?.opt("id")?.toString() ?: "",
                name = hostObj?.optString("name")?.ifBlank { "میزبان" } ?: "میزبان",
                logoUrl = safeHttpsUrl(hostObj?.optString("logo") ?: "", setOf("match-cdn.varzesh3.ir"))
            ),
            guest = TeamInfo(
                id = guestObj?.opt("id")?.toString() ?: "",
                name = guestObj?.optString("name")?.ifBlank { "میهمان" } ?: "میهمان",
                logoUrl = safeHttpsUrl(guestObj?.optString("logo") ?: "", setOf("match-cdn.varzesh3.ir"))
            ),
            goals = goals,
            isLive = obj.optBoolean("isLive", false),
            liveTime = obj.optString("liveTime").takeIf { it != "null" } ?: "",
            statusTitle = obj.optString("statusTitle").takeIf { it != "null" } ?: "",
            time = obj.optString("time").takeIf { it != "null" } ?: "",
            date = obj.optString("date").takeIf { it != "null" } ?: "",
            startOnUtc = obj.optString("startOnUtc").takeIf { it != "null" } ?: "",
            link = obj.optString("link").takeIf { it != "null" } ?: "",
            liveStreamLink = liveStreamLink
        )
    }

    private fun parseStatEntry(obj: JSONObject?): StatEntry? {
        if (obj == null) return null
        val title = obj.optString("title").takeIf { it != "null" && it.isNotBlank() } ?: return null
        val hostObj = obj.optJSONObject("host")
        val guestObj = obj.optJSONObject("guest")
        val hostVal = hostObj?.opt("value")?.toString()?.takeIf { it != "null" } ?: "—"
        val guestVal = guestObj?.opt("value")?.toString()?.takeIf { it != "null" } ?: "—"
        var hostPct = hostObj?.optDouble("percent", Double.NaN) ?: Double.NaN
        if (hostPct.isNaN() || hostPct <= 0.0) {
            val hNum = hostVal.replace("%", "").trim().toDoubleOrNull()
            val gNum = guestVal.replace("%", "").trim().toDoubleOrNull()
            hostPct = if (hNum != null && gNum != null && (hNum + gNum) > 0.0) {
                (100.0 * hNum) / (hNum + gNum)
            } else {
                50.0
            }
        }
        return StatEntry(
            title = title,
            hostValue = hostVal,
            guestValue = guestVal,
            hostPercent = hostPct.toFloat().coerceIn(0f, 100f)
        )
    }

    private fun parseLineupSide(obj: JSONObject?): LineupSide? {
        if (obj == null) return null
        val formation = obj.optString("formation").takeIf { it != "null" } ?: ""
        val coachRaw = obj.opt("coach")
        val coach = when (coachRaw) {
            is JSONObject -> coachRaw.optString("name").takeIf { it != "null" } ?: ""
            is String -> coachRaw.takeIf { it != "null" } ?: ""
            else -> ""
        }
        val starters = mutableListOf<LineupPlayer>()
        val lines = obj.optJSONArray("formationLines")
        if (lines != null) {
            for (i in 0 until lines.length()) {
                val lineObj = lines.optJSONObject(i) ?: continue
                val players = lineObj.optJSONArray("players") ?: continue
                for (p in 0 until players.length()) {
                    parseLineupPlayer(players.optJSONObject(p))?.let { starters.add(it) }
                }
            }
        }
        val bench = mutableListOf<LineupPlayer>()
        val benchArr = obj.optJSONArray("benchedPlayers")
        if (benchArr != null) {
            for (p in 0 until benchArr.length()) {
                parseLineupPlayer(benchArr.optJSONObject(p))?.let { bench.add(it) }
            }
        }
        if (starters.isEmpty() && bench.isEmpty() && formation.isBlank() && coach.isBlank()) return null
        return LineupSide(
            formation = formation,
            coach = coach,
            starters = starters,
            bench = bench
        )
    }

    private fun parseLineupPlayer(obj: JSONObject?): LineupPlayer? {
        if (obj == null) return null
        val name = obj.optString("name").takeIf { it != "null" && it.isNotBlank() } ?: "بازیکن"
        val number = obj.opt("shirtNumber")?.toString()?.takeIf { it != "null" } ?: ""
        val score = obj.opt("score")?.toString()?.takeIf { it != "null" && it != "0" } ?: ""
        return LineupPlayer(
            name = name,
            shirtNumber = number,
            isCaptain = obj.optBoolean("isCaptain", false),
            isManOfTheMatch = obj.optBoolean("isManOfTheMatch", false),
            score = score,
            portraitUrl = safeHttpsUrl(obj.optString("portrait"), setOf("match-cdn.varzesh3.ir"))
        )
    }

    private fun extractNextFlightMatchData(html: String, targetId: Long): JSONObject? {
        val scriptRegex = Regex("""<script>self\.__next_f\.push\((\[1,[\s\S]*?\])\)</script>""")
        for (match in scriptRegex.findAll(html)) {
            val rawFrame = match.groupValues[1]
            if (!rawFrame.contains("lineup") && !rawFrame.contains("stats")) continue
            val result = runCatching {
                val frame = JSONArray(rawFrame)
                val content = frame.optString(1)
                val sep = content.indexOf(':')
                if (sep == -1) return@runCatching null
                val flightValue = JSONTokener(content.substring(sep + 1)).nextValue()
                findMatchJson(flightValue, targetId, 0)
            }.getOrNull()
            if (result != null) return result
        }
        return null
    }

    private fun findMatchJson(value: Any?, targetId: Long, depth: Int): JSONObject? {
        if (value == null || depth > 16) return null
        when (value) {
            is JSONObject -> {
                if (value.optLong("id", -1L) == targetId &&
                    value.has("host") &&
                    value.has("guest") &&
                    (value.has("lineup") || value.has("stats"))
                ) {
                    return value
                }
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val found = findMatchJson(value.opt(key), targetId, depth + 1)
                    if (found != null) return found
                }
            }
            is JSONArray -> {
                for (i in 0 until value.length()) {
                    val found = findMatchJson(value.opt(i), targetId, depth + 1)
                    if (found != null) return found
                }
            }
        }
        return null
    }

    private fun executeGet(
        url: String,
        bearerToken: String = "",
        accept: String = "application/json"
    ): String {
        val builder = Request.Builder()
            .url(url)
            .header("Accept", accept)
            .header("User-Agent", "KhateBazi-Android/1.0")
        if (bearerToken.isNotBlank()) {
            builder.header("Authorization", "Bearer ${bearerToken.trim()}")
        }
        return executeRequest(builder.build())
    }

    private fun executeRequest(request: Request): String {
        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val errMsg = runCatching { JSONObject(bodyStr).optString("error") }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: "خطای سرور (${response.code})"
                throw HttpStatusException(response.code, errMsg)
            }
            return bodyStr
        }
    }

    companion object {
        fun normalizeRemoteImageUrl(raw: String, defaultHost: String = "https://www.varzesh3.ir"): String {
            val cleaned = raw.trim()
                .replace("&amp;", "&")
                .removeSurrounding("\"")
                .removeSurrounding("'")
            if (cleaned.isBlank() || cleaned.equals("null", ignoreCase = true) || cleaned.startsWith("data:")) {
                return ""
            }
            return when {
                cleaned.startsWith("https://", ignoreCase = true) -> cleaned
                cleaned.startsWith("http://", ignoreCase = true) -> "https://" + cleaned.substring(7)
                cleaned.startsWith("//") -> "https:$cleaned"
                cleaned.startsWith("/") -> "${defaultHost.trimEnd('/')}$cleaned"
                !cleaned.contains(' ') && (
                    cleaned.contains(".jpg", ignoreCase = true) ||
                        cleaned.contains(".jpeg", ignoreCase = true) ||
                        cleaned.contains(".png", ignoreCase = true) ||
                        cleaned.contains(".webp", ignoreCase = true) ||
                        cleaned.contains("/")
                    ) -> "${defaultHost.trimEnd('/')}/${cleaned.trimStart('/')}"
                else -> ""
            }
        }

        fun extractPosterUrl(obj: JSONObject, defaultHost: String): String {
            val keys = listOf(
                "big_poster", "poster", "poster_url", "posterUrl", "mobilePoster",
                "desktopPoster", "posterImage", "appBanner", "sliderImage", "bannerImage",
                "coverImage", "banner", "cover", "cover_image", "coverUrl", "share_image",
                "sport_image", "image", "image_url", "imageUrl", "thumbnail", "thumb",
                "small_poster", "pic", "imagePath", "photo", "photo_url", "img", "background"
            )
            val nestedKeys = listOf(
                "url", "src", "large", "medium", "original", "path",
                "image", "poster", "mobile", "desktop", "file"
            )
            for (k in keys) {
                val value = obj.opt(k) ?: continue
                when (value) {
                    is String -> {
                        val normalized = normalizeRemoteImageUrl(value, defaultHost)
                        if (normalized.isNotBlank()) return normalized
                    }
                    is JSONObject -> {
                        for (nk in nestedKeys) {
                            val normalized = normalizeRemoteImageUrl(value.optString(nk), defaultHost)
                            if (normalized.isNotBlank()) return normalized
                        }
                    }
                    is JSONArray -> {
                        for (idx in 0 until value.length()) {
                            val item = value.opt(idx) ?: continue
                            when (item) {
                                is String -> {
                                    val normalized = normalizeRemoteImageUrl(item, defaultHost)
                                    if (normalized.isNotBlank()) return normalized
                                }
                                is JSONObject -> {
                                    for (nk in nestedKeys) {
                                        val normalized = normalizeRemoteImageUrl(item.optString(nk), defaultHost)
                                        if (normalized.isNotBlank()) return normalized
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return ""
        }

        fun extractTeamLogos(obj: JSONObject, defaultHost: String): Pair<String, String> {
            fun fromKeys(vararg keys: String): String {
                for (k in keys) {
                    val v = obj.opt(k) ?: continue
                    when (v) {
                        is String -> {
                            val u = normalizeRemoteImageUrl(v, defaultHost)
                            if (u.isNotBlank()) return u
                        }
                        is JSONObject -> {
                            val u = normalizeRemoteImageUrl(
                                v.optString("logo").ifBlank {
                                    v.optString("logoUrl").ifBlank { v.optString("image") }
                                },
                                defaultHost
                            )
                            if (u.isNotBlank()) return u
                        }
                    }
                }
                return ""
            }

            var hostLogo = fromKeys("team1_logo", "host_logo", "hostLogo", "homeLogo", "team1Logo", "team1", "host", "homeTeam")
            var guestLogo = fromKeys("team2_logo", "guest_logo", "guestLogo", "awayLogo", "team2Logo", "team2", "guest", "awayTeam")

            if (hostLogo.isBlank() || guestLogo.isBlank()) {
                val teamsArr = obj.optJSONArray("teams") ?: obj.optJSONArray("competitors")
                if (teamsArr != null && teamsArr.length() >= 2) {
                    if (hostLogo.isBlank()) {
                        val t0 = teamsArr.optJSONObject(0)
                        if (t0 != null) {
                            hostLogo = normalizeRemoteImageUrl(
                                t0.optString("logo").ifBlank { t0.optString("image") },
                                defaultHost
                            )
                        }
                    }
                    if (guestLogo.isBlank()) {
                        val t1 = teamsArr.optJSONObject(1)
                        if (t1 != null) {
                            guestLogo = normalizeRemoteImageUrl(
                                t1.optString("logo").ifBlank { t1.optString("image") },
                                defaultHost
                            )
                        }
                    }
                }
            }
            return hostLogo to guestLogo
        }

        fun extractLeagueTitle(obj: JSONObject): String {
            val keys = listOf("league_name", "tournament_name", "competition_title", "league", "competition", "category", "sportName")
            for (k in keys) {
                val v = obj.opt(k) ?: continue
                when (v) {
                    is String -> if (v.isNotBlank() && v != "null") return v.trim()
                    is JSONObject -> {
                        val t = v.optString("title").ifBlank { v.optString("name") }
                        if (t.isNotBlank() && t != "null") return t.trim()
                    }
                }
            }
            return ""
        }

        private fun extractFirstImageFromHtmlSnippet(snippet: String): String {
            if (snippet.isBlank()) return ""
            val attrRegex = Regex(
                """(?:src|data-src|data-original|data-lazy-src|poster)=["']([^"']+)["']""",
                RegexOption.IGNORE_CASE
            )
            for (m in attrRegex.findAll(snippet)) {
                val url = normalizeRemoteImageUrl(m.groupValues[1], "https://www.varzesh3.ir")
                if (isUsefulNewsPhotoUrl(url)) return url
            }
            val srcsetMatch = Regex("""srcset=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(snippet)
            if (srcsetMatch != null) {
                val firstEntry = srcsetMatch.groupValues[1]
                    .split(',')
                    .firstOrNull()
                    ?.trim()
                    ?.substringBefore(' ')
                    .orEmpty()
                val url = normalizeRemoteImageUrl(firstEntry, "https://www.varzesh3.ir")
                if (isUsefulNewsPhotoUrl(url)) return url
            }
            val cdnMatch = Regex(
                """https?://[^"'\s)]+(?:news-cdn|video-icdn|cdn)\.varzesh3\.ir[^"'\s)]+\.(?:jpg|jpeg|png|webp)(?:\?[^"'\s)]*)?""",
                RegexOption.IGNORE_CASE
            ).find(snippet)
            if (cdnMatch != null) {
                return normalizeRemoteImageUrl(cdnMatch.value)
            }
            return ""
        }

        private fun isUsefulNewsPhotoUrl(url: String): Boolean {
            if (url.isBlank()) return false
            val lower = url.lowercase(Locale.US)
            if (!lower.startsWith("https://")) return false
            if (lower.endsWith(".svg") || lower.contains("favicon") || lower.contains("/logo")) return false
            return lower.contains(".jpg") || lower.contains(".jpeg") ||
                lower.contains(".png") || lower.contains(".webp") ||
                lower.contains("news-cdn.varzesh3.ir") || lower.contains("video-icdn.varzesh3.ir")
        }

        fun extractOgImageUrl(html: String): String {
            if (html.isBlank()) return ""
            val metaPatterns = listOf(
                Regex("""<meta[^>]+(?:property|name)=["'](?:og:image|og:image:secure_url|twitter:image)["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["'](?:og:image|og:image:secure_url|twitter:image)["']""", RegexOption.IGNORE_CASE)
            )
            for (pattern in metaPatterns) {
                for (m in pattern.findAll(html)) {
                    val candidate = normalizeRemoteImageUrl(m.groupValues[1], "https://www.varzesh3.ir")
                    if (isUsefulNewsPhotoUrl(candidate)) return candidate
                }
            }
            val cdnPhoto = Regex(
                """https?://news-cdn\.varzesh3\.ir/[^"'\s>]+?\.(?:jpg|jpeg|png|webp)(?:\?[^"'\s>]*)?""",
                RegexOption.IGNORE_CASE
            ).find(html)?.value
            if (!cdnPhoto.isNullOrBlank()) {
                return normalizeRemoteImageUrl(cdnPhoto)
            }
            return ""
        }

        fun extractVarzesh3NewsImageMap(html: String): Map<String, String> {
            if (html.isBlank()) return emptyMap()
            val map = mutableMapOf<String, String>()
            val refRegex = Regex("""/news/(\d+)/""")
            for (match in refRegex.findAll(html)) {
                val id = match.groupValues[1]
                if (map.containsKey(id)) continue
                val start = (match.range.first - 600).coerceAtLeast(0)
                val end = (match.range.last + 600).coerceAtMost(html.length)
                val window = html.substring(start, end)
                val img = extractFirstImageFromHtmlSnippet(window)
                if (img.isNotBlank()) {
                    map[id] = img
                }
            }
            return map
        }

        fun parseVarzesh3RssFeed(xml: String): List<NewsArticle> {
            val parser = runCatching { Xml.newPullParser() }.getOrElse {
                XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }.newPullParser()
            }
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))

            val items = mutableListOf<NewsArticle>()
            var insideItem = false
            var title = ""
            var link = ""
            var description = ""
            var contentEncoded = ""
            var pubDate = ""
            var imageUrl = ""
            var currentTag = ""

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT && items.size < 40) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name.orEmpty().lowercase()
                        if (currentTag == "item") {
                            insideItem = true
                            title = ""
                            link = ""
                            description = ""
                            contentEncoded = ""
                            pubDate = ""
                            imageUrl = ""
                        } else if (insideItem && (
                                currentTag == "enclosure" ||
                                    currentTag == "media:content" ||
                                    currentTag == "media:thumbnail" ||
                                    currentTag == "itunes:image"
                                )
                        ) {
                            val attrUrl = parser.getAttributeValue(null, "url")
                                ?: parser.getAttributeValue(null, "href")
                                ?: ""
                            val normalized = normalizeRemoteImageUrl(attrUrl, "https://www.varzesh3.ir")
                            if (isUsefulNewsPhotoUrl(normalized) && imageUrl.isBlank()) {
                                imageUrl = normalized
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (insideItem) {
                            val txt = parser.text.orEmpty()
                            when (currentTag) {
                                "title" -> title += txt
                                "link" -> link += txt
                                "description" -> description += txt
                                "content:encoded", "encoded" -> contentEncoded += txt
                                "pubdate" -> pubDate += txt
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tag = parser.name.orEmpty().lowercase()
                        if (tag == "item" && insideItem) {
                            insideItem = false
                            val cleanTitle = cleanHtmlText(title)
                            val cleanLink = safeVarzesh3MatchUrl(link.trim())
                            if (imageUrl.isBlank()) {
                                imageUrl = extractFirstImageFromHtmlSnippet("$contentEncoded $description")
                            }
                            val cleanSummary = cleanHtmlText(description)
                            if (cleanTitle.isNotBlank() && cleanLink.isNotBlank()) {
                                val id = Regex("""/news/(\d+)""").find(cleanLink)?.groupValues?.getOrNull(1)
                                    ?: cleanLink.hashCode().toString()
                                items.add(
                                    NewsArticle(
                                        id = id,
                                        title = cleanTitle,
                                        summary = cleanSummary,
                                        link = cleanLink,
                                        imageUrl = imageUrl,
                                        publishedAtFa = formatNewsTimeFa(pubDate),
                                        sourceName = "ورزش سه",
                                        category = classifyNewsCategory(cleanTitle, cleanLink)
                                    )
                                )
                            }
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
            return items
        }

        fun parseVarzesh3NewsPageHtml(
            html: String,
            imageMap: Map<String, String> = extractVarzesh3NewsImageMap(html)
        ): List<NewsArticle> {
            val articles = mutableListOf<NewsArticle>()
            val seenIds = mutableSetOf<String>()

            val linkRegex = Regex(
                """<a[^>]+href=["'](https?://www\.varzesh3\.ir)?(/news/(\d+)/[^"']*)["'][^>]*>([\s\S]{4,360}?)</a>""",
                setOf(RegexOption.IGNORE_CASE)
            )
            for (match in linkRegex.findAll(html)) {
                val path = match.groupValues[2]
                val id = match.groupValues[3]
                if (id in seenIds) continue
                val inner = match.groupValues[4]
                val headline = cleanHtmlText(inner)
                if (headline.length < 14) continue
                val fullLink = safeVarzesh3MatchUrl(path)
                if (fullLink.isBlank()) continue
                seenIds.add(id)
                val imgUrl = extractFirstImageFromHtmlSnippet(inner).ifBlank {
                    imageMap[id].orEmpty()
                }
                articles.add(
                    NewsArticle(
                        id = id,
                        title = headline,
                        summary = "",
                        link = fullLink,
                        imageUrl = imgUrl,
                        publishedAtFa = "",
                        sourceName = "ورزش سه",
                        category = classifyNewsCategory(headline, fullLink)
                    )
                )
                if (articles.size >= 36) break
            }
            return articles
        }

        fun cleanHtmlText(raw: String): String {
            if (raw.isBlank()) return ""
            val decoded = runCatching {
                HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
            }.getOrElse {
                raw.replace(Regex("""<[^>]+>"""), " ")
                    .replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&zwnj;", "\u200C")
            }
            return decoded
                .replace('\uFFFC', ' ')
                .replace(Regex("""[ \t]+"""), " ")
                .replace(Regex("""\n{3,}"""), "\n\n")
                .trim()
        }

        private fun classifyNewsCategory(title: String, link: String): NewsCategory {
            val text = "$title $link"
            val otherKeywords = listOf(
                "والیبال", "بسکتبال", "کشتی", "تکواندو", "وزنه‌برداری",
                "فوتسال", "تنیس", "شطرنج", "شنا", "کاراته", "فرمول", "المپیک"
            )
            if (otherKeywords.any { text.contains(it) }) return NewsCategory.OTHER

            val worldKeywords = listOf(
                "رئال", "بارسلونا", "لیورپول", "آرسنال", "منچستر", "چلسی",
                "بایرن", "میلان", "اینتر", "یوونتوس", "پاری", "اروپا",
                "لیگ قهرمانان", "لیگ برتر انگلیس", "لالیگا", "سری آ", "بوندسلیگا",
                "مسی", "رونالدو", "امباپه", "هالند"
            )
            if (worldKeywords.any { text.contains(it) }) return NewsCategory.WORLD_FOOTBALL

            return NewsCategory.IRAN_FOOTBALL
        }

        private fun formatNewsTimeFa(rawDate: String): String {
            if (rawDate.isBlank()) return ""
            val patterns = listOf(
                "EEE, dd MMM yyyy HH:mm:ss Z",
                "EEE, dd MMM yyyy HH:mm:ss z",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX"
            )
            val millis = patterns.firstNotNullOfOrNull { pattern ->
                runCatching {
                    SimpleDateFormat(pattern, Locale.US).parse(rawDate.trim())?.time
                }.getOrNull()
            } ?: return ""

            val diffMin = ((System.currentTimeMillis() - millis) / 60_000L).coerceAtLeast(0L)
            return when {
                diffMin < 2L -> "هم‌اکنون"
                diffMin < 60L -> "${toPersianDigits(diffMin.toInt())} دقیقه پیش"
                diffMin < 24L * 60L -> "${toPersianDigits((diffMin / 60L).toInt())} ساعت پیش"
                else -> "${toPersianDigits((diffMin / (24L * 60L)).toInt())} روز پیش"
            }
        }

        fun cleanGameName(value: String): String {
            return value
                .replace(Regex("""^.*?:\s*"""), "")
                .replace(Regex("""^(فوتبال|بسکتبال|والیبال|اسنوکر)\s+"""), "")
                .replace(Regex("""\s*\([^)]*\)\s*$"""), "")
                .trim()
        }

        fun gameKey(value: String): String {
            return Normalizer.normalize(cleanGameName(value), Normalizer.Form.NFKC)
                .lowercase()
                .replace('ي', 'ی')
                .replace('ك', 'ک')
                .replace('–', '-')
                .replace('—', '-')
                .replace(Regex("""\s+"""), " ")
                .replace(Regex("""\s*-\s*"""), "|")
        }

        fun safeHttpsUrl(raw: String, allowedHosts: Set<String>): String {
            return runCatching {
                val url = URL(raw)
                if (url.protocol == "https" && url.host in allowedHosts) url.toExternalForm() else ""
            }.getOrDefault("")
        }

        fun safeVarzesh3MatchUrl(pathOrUrl: String): String {
            if (pathOrUrl.isBlank()) return ""
            val normalized = if (pathOrUrl.startsWith("http://")) {
                "https://" + pathOrUrl.removePrefix("http://")
            } else {
                pathOrUrl
            }
            return runCatching {
                if (normalized.startsWith("/") && !normalized.startsWith("//")) {
                    val u = URL(URL("https://www.varzesh3.ir"), normalized)
                    if (u.host == "www.varzesh3.ir") u.toExternalForm() else ""
                } else {
                    safeHttpsUrl(normalized, setOf("www.varzesh3.ir", "varzesh3.ir"))
                }
            }.getOrDefault("")
        }

        fun toPersianDigits(input: Int): String = toPersianDigits(input.toString())

        fun toPersianDigits(input: String): String {
            val persian = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
            val sb = StringBuilder(input.length)
            for (ch in input) {
                if (ch in '0'..'9') sb.append(persian[ch - '0']) else sb.append(ch)
            }
            return sb.toString()
        }
    }
}

class HttpStatusException(val code: Int, override val message: String) : Exception(message)
