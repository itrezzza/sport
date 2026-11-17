package com.example.data.model

enum class BroadcastSource(
    val id: String,
    val titleFa: String,
    val subtitleFa: String,
    val badgeText: String,
    val defaultUrl: String
) {
    ANTEN(
        id = "anten",
        titleFa = "آنتن",
        subtitleFa = "مسابقات و پخش زنده",
        badgeText = "آنتن",
        defaultUrl = "https://www.anten.ir/mapi/v1/v4.3/programs"
    ),
    APARAT(
        id = "aparat",
        titleFa = "آپارات اسپرت",
        subtitleFa = "برنامهٔ مسابقات امروز",
        badgeText = "آپارات",
        defaultUrl = "https://www.aparatsport.com/api/fa/v1/sport/sport/get_today_match_data?uuid="
    )
}

enum class BroadcastFilter(val labelFa: String) {
    ALL("همهٔ بازی‌ها"),
    ANTEN("آنتن"),
    APARAT("آپارات اسپرت")
}

data class BroadcastProgram(
    val id: String,
    val title: String,
    val time: String,
    val isLive: Boolean,
    val source: BroadcastSource,
    val primaryStreamUrl: String = "",
    val secondaryStreamUrl: String = "",
    val posterUrl: String = "",
    val hostLogoUrl: String = "",
    val guestLogoUrl: String = "",
    val leagueTitle: String = "",
    val isDemo: Boolean = false
)

data class MergedBroadcastGame(
    val key: String,
    val title: String,
    val time: String,
    val isLive: Boolean,
    val posterUrl: String = "",
    val hostLogoUrl: String = "",
    val guestLogoUrl: String = "",
    val leagueTitle: String = "",
    val newsPhotoUrl: String = "",
    val sources: Map<BroadcastSource, BroadcastProgram>
)

data class StreamQuality(
    val label: String,
    val directUrl: String? = null,
    val accessUrl: String? = null
)

data class HlsTrackOption(
    val id: String,
    val label: String,
    val groupIndex: Int = -1,
    val trackIndex: Int = -1
)

data class TeamInfo(
    val id: String = "",
    val name: String,
    val logoUrl: String = ""
)

data class MatchGoals(
    val host: Int? = null,
    val guest: Int? = null
)

data class MatchSummary(
    val id: String,
    val host: TeamInfo,
    val guest: TeamInfo,
    val goals: MatchGoals? = null,
    val isLive: Boolean = false,
    val liveTime: String = "",
    val statusTitle: String = "",
    val time: String = "",
    val date: String = "",
    val startOnUtc: String = "",
    val link: String = "",
    val liveStreamLink: String = ""
) {
    fun stateLabel(): String = when {
        isLive -> "زنده ${liveTime}".trim()
        goals != null -> statusTitle.ifBlank { "پایان بازی" }
        else -> time.ifBlank { "در انتظار شروع" }
    }
}

data class MatchEntry(
    val match: MatchSummary,
    val leagueTitle: String,
    val sport: Int
)

data class LeagueGroup(
    val title: String,
    val sport: Int,
    val matches: List<MatchSummary>
)

data class MatchEvent(
    val id: String,
    val time: String,
    val rawTime: Int,
    val eventType: Int,
    val side: Int, // 0 = host, 1 = away
    val description: String,
    val decisionTitle: String = "",
    val strikerName: String = "",
    val assisterName: String = "",
    val hostScore: Int? = null,
    val guestScore: Int? = null,
    val videoId: String = "",
    val videoLink: String = ""
) {
    val periodLabel: String
        get() = when {
            rawTime > 90 -> "وقت‌های اضافه"
            rawTime > 45 -> "نیمهٔ دوم"
            else -> "نیمهٔ اول"
        }

    val titleLabel: String
        get() = when {
            eventType == 1 -> if (strikerName.isNotBlank()) "گل · $strikerName" else "گل"
            eventType == 4 -> "تعویض"
            eventType == 5 -> "بررسی VAR"
            description.contains("کارت قرمز") -> "کارت قرمز"
            description.contains("کارت زرد") -> "کارت زرد"
            else -> "رخداد بازی"
        }
}

data class StatEntry(
    val title: String,
    val hostValue: String,
    val guestValue: String,
    val hostPercent: Float
)

data class LineupPlayer(
    val name: String,
    val shirtNumber: String = "",
    val isCaptain: Boolean = false,
    val isManOfTheMatch: Boolean = false,
    val score: String = "",
    val portraitUrl: String = ""
)

data class LineupSide(
    val formation: String = "",
    val coach: String = "",
    val starters: List<LineupPlayer> = emptyList(),
    val bench: List<LineupPlayer> = emptyList()
)

data class MatchDetailInfo(
    val stadium: String = "",
    val referee: String = "",
    val week: String = "",
    val pageUrl: String = "",
    val statsTitle: String = "مجموع بازی",
    val stats: List<StatEntry> = emptyList(),
    val hostLineup: LineupSide? = null,
    val guestLineup: LineupSide? = null
)

data class HighlightVideo(
    val id: String,
    val title: String,
    val mp4Url: String,
    val posterUrl: String = ""
)

data class ConnectionSettings(
    val selectedSource: BroadcastSource = BroadcastSource.ANTEN,
    val customAntenUrl: String = BroadcastSource.ANTEN.defaultUrl,
    val antenBearerToken: String = "",
    val isDarkTheme: Boolean = true
)

enum class AppTab(val route: String, val labelFa: String) {
    BROADCAST("broadcast", "پخش زنده"),
    SCORES("scores", "نتایج زنده"),
    NEWS("news", "اخبار")
}

enum class NewsCategory(val labelFa: String) {
    ALL("همه"),
    IRAN_FOOTBALL("فوتبال ایران"),
    WORLD_FOOTBALL("فوتبال جهان"),
    OTHER("سایر رشته‌ها")
}

data class NewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val link: String,
    val imageUrl: String = "",
    val publishedAtFa: String = "",
    val sourceName: String = "ورزش سه",
    val category: NewsCategory = NewsCategory.ALL
)

enum class ScoreFilter(val labelFa: String) {
    LIVE("در جریان"),
    ALL("همهٔ بازی‌ها")
}

enum class SportCategory(val sportId: Int?, val labelFa: String) {
    ALL(null, "همه"),
    FOOTBALL(1, "فوتبال"),
    VOLLEYBALL(2, "والیبال"),
    BASKETBALL(3, "بسکتبال"),
    FUTSAL(4, "فوتسال")
}

data class WatchedMatch(
    val id: String,
    val hostName: String,
    val guestName: String,
    val leagueTitle: String,
    val startOnUtc: String = "",
    val lastHostGoals: Int? = null,
    val lastGuestGoals: Int? = null,
    val wasLive: Boolean = false,
    val wasFinished: Boolean = false
)

data class NotificationSettings(
    val enabled: Boolean = true,
    val notifyOnKickoff: Boolean = true,
    val notifyOnScoreChange: Boolean = true,
    val kickoffLeadMinutes: Int = 5
)

enum class InstagramMediaType {
    IMAGE,
    VIDEO,
    CAROUSEL_ALBUM
}

data class InstagramStoryItem(
    val id: String,
    val mediaType: InstagramMediaType,
    val mediaUrl: String,
    val thumbnailUrl: String = "",
    val caption: String = "",
    val permalink: String = "",
    val timestampFa: String = "",
    val isLiveStory: Boolean = true
)

data class InstagramStoryTrayGroup(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val isActiveStoryBucket: Boolean = true,
    val badgeLabel: String = "استوری",
    val items: List<InstagramStoryItem>
)

data class InstagramSession(
    val cookieHeader: String = "",
    val csrfToken: String = "",
    val userId: String = "",
    val username: String = "",
    val userAgent: String = ""
) {
    val isAuthenticated: Boolean
        get() = cookieHeader.contains("sessionid=") && cookieHeader.isNotBlank()
}



