package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.BroadcastSource
import com.example.data.model.ConnectionSettings
import com.example.data.model.InstagramSession
import com.example.data.model.NotificationSettings
import com.example.data.model.WatchedMatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "khatebazi_prefs")

class SettingsStore(private val context: Context) {

    private object Keys {
        val SELECTED_SOURCE = stringPreferencesKey("selected_source")
        val CUSTOM_ANTEN_URL = stringPreferencesKey("custom_anten_url")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
        val NOTIF_KICKOFF = booleanPreferencesKey("notif_kickoff")
        val NOTIF_SCORE_CHANGE = booleanPreferencesKey("notif_score_change")
        val NOTIF_LEAD_MINUTES = intPreferencesKey("notif_lead_minutes")
        val WATCHED_MATCHES_JSON = stringPreferencesKey("watched_matches_json")
        val INSTAGRAM_USERNAMES = stringPreferencesKey("instagram_usernames_json")
        val INSTAGRAM_COOKIE = stringPreferencesKey("instagram_cookie")
        val INSTAGRAM_CSRF = stringPreferencesKey("instagram_csrf")
        val INSTAGRAM_USER_ID = stringPreferencesKey("instagram_user_id")
        val INSTAGRAM_USERNAME = stringPreferencesKey("instagram_username")
        val INSTAGRAM_USER_AGENT = stringPreferencesKey("instagram_user_agent")
    }

    val settingsFlow: Flow<ConnectionSettings> = context.dataStore.data.map { prefs ->
        val sourceId = prefs[Keys.SELECTED_SOURCE] ?: BroadcastSource.ANTEN.id
        val source = BroadcastSource.entries.firstOrNull { it.id == sourceId } ?: BroadcastSource.ANTEN
        val antenUrl = prefs[Keys.CUSTOM_ANTEN_URL]?.takeIf { it.isNotBlank() }
            ?: BroadcastSource.ANTEN.defaultUrl
        val isDark = prefs[Keys.DARK_THEME] ?: true
        ConnectionSettings(
            selectedSource = source,
            customAntenUrl = antenUrl,
            isDarkTheme = isDark
        )
    }

    val notificationSettingsFlow: Flow<NotificationSettings> = context.dataStore.data.map { prefs ->
        NotificationSettings(
            enabled = prefs[Keys.NOTIF_ENABLED] ?: true,
            notifyOnKickoff = prefs[Keys.NOTIF_KICKOFF] ?: true,
            notifyOnScoreChange = prefs[Keys.NOTIF_SCORE_CHANGE] ?: true,
            kickoffLeadMinutes = prefs[Keys.NOTIF_LEAD_MINUTES] ?: 5
        )
    }

    val watchedMatchesFlow: Flow<Map<String, WatchedMatch>> = context.dataStore.data.map { prefs ->
        decodeWatchedMatches(prefs[Keys.WATCHED_MATCHES_JSON])
    }

    val instagramUsernamesFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        decodeInstagramUsernames(prefs[Keys.INSTAGRAM_USERNAMES])
    }

    val instagramSessionFlow: Flow<InstagramSession> = context.dataStore.data.map { prefs ->
        InstagramSession(
            cookieHeader = prefs[Keys.INSTAGRAM_COOKIE].orEmpty(),
            csrfToken = prefs[Keys.INSTAGRAM_CSRF].orEmpty(),
            userId = prefs[Keys.INSTAGRAM_USER_ID].orEmpty(),
            username = prefs[Keys.INSTAGRAM_USERNAME].orEmpty(),
            userAgent = prefs[Keys.INSTAGRAM_USER_AGENT].orEmpty()
        )
    }

    suspend fun setSelectedSource(source: BroadcastSource) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_SOURCE] = source.id
        }
    }

    suspend fun setCustomAntenUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_ANTEN_URL] = url.trim().ifBlank { BroadcastSource.ANTEN.defaultUrl }
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = isDark
        }
    }

    suspend fun updateNotificationSettings(settings: NotificationSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIF_ENABLED] = settings.enabled
            prefs[Keys.NOTIF_KICKOFF] = settings.notifyOnKickoff
            prefs[Keys.NOTIF_SCORE_CHANGE] = settings.notifyOnScoreChange
            prefs[Keys.NOTIF_LEAD_MINUTES] = settings.kickoffLeadMinutes
        }
    }

    suspend fun saveWatchedMatches(matches: Map<String, WatchedMatch>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WATCHED_MATCHES_JSON] = encodeWatchedMatches(matches)
        }
    }

    suspend fun saveInstagramUsernames(usernames: List<String>) {
        context.dataStore.edit { prefs ->
            val arr = JSONArray()
            usernames.forEach { arr.put(it) }
            prefs[Keys.INSTAGRAM_USERNAMES] = arr.toString()
        }
    }

    suspend fun saveInstagramSession(session: InstagramSession) {
        context.dataStore.edit { prefs ->
            prefs[Keys.INSTAGRAM_COOKIE] = session.cookieHeader
            prefs[Keys.INSTAGRAM_CSRF] = session.csrfToken
            prefs[Keys.INSTAGRAM_USER_ID] = session.userId
            prefs[Keys.INSTAGRAM_USERNAME] = session.username
            prefs[Keys.INSTAGRAM_USER_AGENT] = session.userAgent
        }
    }

    suspend fun clearInstagramSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.INSTAGRAM_COOKIE)
            prefs.remove(Keys.INSTAGRAM_CSRF)
            prefs.remove(Keys.INSTAGRAM_USER_ID)
            prefs.remove(Keys.INSTAGRAM_USERNAME)
            prefs.remove(Keys.INSTAGRAM_USER_AGENT)
        }
    }

    private fun encodeWatchedMatches(map: Map<String, WatchedMatch>): String {
        val array = JSONArray()
        map.values.forEach { m ->
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("hostName", m.hostName)
            obj.put("guestName", m.guestName)
            obj.put("leagueTitle", m.leagueTitle)
            obj.put("startOnUtc", m.startOnUtc)
            if (m.lastHostGoals != null) obj.put("lastHostGoals", m.lastHostGoals)
            if (m.lastGuestGoals != null) obj.put("lastGuestGoals", m.lastGuestGoals)
            obj.put("wasLive", m.wasLive)
            obj.put("wasFinished", m.wasFinished)
            array.put(obj)
        }
        return array.toString()
    }

    private fun decodeWatchedMatches(raw: String?): Map<String, WatchedMatch> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val array = JSONArray(raw)
            val map = linkedMapOf<String, WatchedMatch>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                if (id.isBlank()) continue
                map[id] = WatchedMatch(
                    id = id,
                    hostName = obj.optString("hostName", "میزبان"),
                    guestName = obj.optString("guestName", "میهمان"),
                    leagueTitle = obj.optString("leagueTitle", "مسابقه"),
                    startOnUtc = obj.optString("startOnUtc", ""),
                    lastHostGoals = if (obj.has("lastHostGoals") && !obj.isNull("lastHostGoals")) obj.optInt("lastHostGoals") else null,
                    lastGuestGoals = if (obj.has("lastGuestGoals") && !obj.isNull("lastGuestGoals")) obj.optInt("lastGuestGoals") else null,
                    wasLive = obj.optBoolean("wasLive", false),
                    wasFinished = obj.optBoolean("wasFinished", false)
                )
            }
            map
        }.getOrDefault(emptyMap())
    }

    private fun decodeInstagramUsernames(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return DEFAULT_INSTAGRAM_USERNAMES
        return runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val u = arr.optString(i).trim()
                if (u.isNotBlank() && u !in list) list.add(u)
            }
            list
        }.getOrDefault(DEFAULT_INSTAGRAM_USERNAMES)
    }

    companion object {
        val DEFAULT_INSTAGRAM_USERNAMES = listOf(
            "varzesh3",
            "perspolis",
            "esteghlal.fc.official",
            "footbalbartar",
            "433"
        )
    }
}
