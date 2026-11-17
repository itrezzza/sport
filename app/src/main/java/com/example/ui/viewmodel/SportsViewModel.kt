package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.webkit.CookieManager
import com.example.data.model.AppTab
import com.example.data.model.BroadcastFilter
import com.example.data.model.BroadcastProgram
import com.example.data.model.BroadcastSource
import com.example.data.model.ConnectionSettings
import com.example.data.model.FavoriteTeam
import com.example.data.model.HighlightVideo
import com.example.data.model.InstagramSession
import com.example.data.model.InstagramStoryTrayGroup
import com.example.data.model.LeagueGroup
import com.example.data.model.LineupPlayer
import com.example.data.model.MatchDetailInfo
import com.example.data.model.MatchEntry
import com.example.data.model.MatchEvent
import com.example.data.model.MatchSummary
import com.example.data.model.MergedBroadcastGame
import com.example.data.model.NewsArticle
import com.example.data.model.NewsCategory
import com.example.data.model.NotificationSettings
import com.example.data.model.PlayerMatchContribution
import com.example.data.model.PlayerProfileData
import com.example.data.model.ScoreFilter
import com.example.data.model.SportCategory
import com.example.data.model.StreamQuality
import com.example.data.model.TeamProfileData
import com.example.data.model.WatchedMatch
import com.example.data.network.HttpStatusException
import com.example.data.network.InstagramAuthRequiredException
import com.example.data.network.InstagramRepository
import com.example.data.network.SportsRepository
import com.example.data.preferences.SettingsStore
import com.example.notifications.MatchNotificationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DetailTab(val labelFa: String) {
    TIMELINE("لحظه‌به‌لحظه"),
    STATS("آمار بازی"),
    LINEUP("ترکیب تیم‌ها")
}

data class BroadcastUiState(
    val filter: BroadcastFilter = BroadcastFilter.ALL,
    val mergedGames: List<MergedBroadcastGame> = emptyList(),
    val isLoadingPrograms: Boolean = false,
    val programsStatus: String = "در حال دریافت بازی‌ها…",
    val activeProgramId: String? = null,
    val activeProgramTitle: String = "پخش موردنظرت را انتخاب کن",
    val activeSourceLabel: String = "آنتن و آپارات اسپرت",
    val playerStatus: String = "یک بازی را انتخاب کن تا پخش زنده شروع شود.",
    val qualities: List<StreamQuality> = emptyList(),
    val activeQualityIndex: Int = 0,
    val streamUrl: String? = null,
    val isFullscreen: Boolean = false,
    val watchChooserGame: MergedBroadcastGame? = null,
    val searchQuery: String = ""
)

data class ScoresUiState(
    val leagues: List<LeagueGroup> = emptyList(),
    val featuredMatches: List<MatchEntry> = emptyList(),
    val liveCount: Int = 0,
    val filter: ScoreFilter = ScoreFilter.LIVE,
    val filterTouched: Boolean = false,
    val sportCategory: SportCategory = SportCategory.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isManualScoreSyncing: Boolean = false,
    val statusText: String = "در حال دریافت نتایج…",
    val lastUpdatedTime: String = "",
    val lastUpdatedEpochMs: Long = 0L,
    val pollIntervalSeconds: Int = 15,
    val instagramUsernames: List<String> = SettingsStore.DEFAULT_INSTAGRAM_USERNAMES,
    val instagramSession: InstagramSession = InstagramSession(),
    val needsInstagramLogin: Boolean = true,
    val storyGroups: List<InstagramStoryTrayGroup> = emptyList(),
    val seenStoryItemIds: Set<String> = emptySet(),
    val isLoadingStories: Boolean = false,
    val storiesError: String? = null,
    val activeStoryGroupIndex: Int? = null,
    val activeStorySlideIndex: Int = 0
)

data class MatchDetailUiState(
    val isOpen: Boolean = false,
    val match: MatchSummary? = null,
    val leagueTitle: String = "",
    val sport: Int = 1,
    val activeTab: DetailTab = DetailTab.TIMELINE,
    val events: List<MatchEvent> = emptyList(),
    val isLoadingEvents: Boolean = false,
    val eventsMessage: String? = null,
    val detailInfo: MatchDetailInfo? = null,
    val isLoadingDetails: Boolean = false,
    val detailsMessage: String? = null,
    val activeHighlightEventId: String? = null,
    val highlightVideo: HighlightVideo? = null,
    val isLoadingHighlight: Boolean = false,
    val highlightStatus: String = ""
)

data class NewsUiState(
    val articles: List<NewsArticle> = emptyList(),
    val selectedCategory: NewsCategory = NewsCategory.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedArticle: NewsArticle? = null,
    val articleBody: String = "",
    val isLoadingArticleBody: Boolean = false
)

class SportsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SportsRepository()
    private val instagramRepository = InstagramRepository()
    private val settingsStore = SettingsStore(application.applicationContext)

    private val _currentTab = MutableStateFlow(AppTab.BROADCAST)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _settings = MutableStateFlow(ConnectionSettings())
    val settings: StateFlow<ConnectionSettings> = _settings.asStateFlow()

    private val _broadcast = MutableStateFlow(BroadcastUiState())
    val broadcast: StateFlow<BroadcastUiState> = _broadcast.asStateFlow()

    private val _scores = MutableStateFlow(ScoresUiState())
    val scores: StateFlow<ScoresUiState> = _scores.asStateFlow()

    private val _detail = MutableStateFlow(MatchDetailUiState())
    val detail: StateFlow<MatchDetailUiState> = _detail.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _notificationSettings = MutableStateFlow(NotificationSettings())
    val notificationSettings: StateFlow<NotificationSettings> = _notificationSettings.asStateFlow()

    private val _watchedMatches = MutableStateFlow<Map<String, WatchedMatch>>(emptyMap())
    val watchedMatches: StateFlow<Map<String, WatchedMatch>> = _watchedMatches.asStateFlow()

    private val _news = MutableStateFlow(NewsUiState())
    val news: StateFlow<NewsUiState> = _news.asStateFlow()

    private val _favoriteTeams = MutableStateFlow<List<FavoriteTeam>>(emptyList())
    val favoriteTeams: StateFlow<List<FavoriteTeam>> = _favoriteTeams.asStateFlow()

    private val _activeTeamProfile = MutableStateFlow<TeamProfileData?>(null)
    val activeTeamProfile: StateFlow<TeamProfileData?> = _activeTeamProfile.asStateFlow()

    private val _activePlayerProfile = MutableStateFlow<PlayerProfileData?>(null)
    val activePlayerProfile: StateFlow<PlayerProfileData?> = _activePlayerProfile.asStateFlow()

    private var pendingDeepLinkMatchId: String? = null

    private var programsJob: Job? = null
    private var playbackJob: Job? = null
    private var detailJob: Job? = null
    private var highlightJob: Job? = null
    private var newsJob: Job? = null
    private var articleBodyJob: Job? = null
    private var storiesJob: Job? = null
    private var pollingJob: Job? = null

    init {
        MatchNotificationManager.ensureChannels(application.applicationContext)
        viewModelScope.launch {
            val saved = settingsStore.settingsFlow.first()
            _settings.value = saved
            _notificationSettings.value = settingsStore.notificationSettingsFlow.first()
            val savedWatched = settingsStore.watchedMatchesFlow.first()
            _watchedMatches.value = savedWatched
            _favoriteTeams.value = settingsStore.favoriteTeamsFlow.first()
            val savedAccounts = settingsStore.instagramUsernamesFlow.first()
            val savedIgSession = settingsStore.instagramSessionFlow.first()
            _scores.update {
                it.copy(
                    instagramUsernames = savedAccounts,
                    instagramSession = savedIgSession,
                    needsInstagramLogin = !savedIgSession.isAuthenticated
                )
            }
            syncWatchedRemindersAndMonitor(savedWatched, _notificationSettings.value)
            loadPrograms()
            refreshLiveScores()
            refreshInstagramStories()
            loadNews()
        }
        startOrRestartScorePolling()
    }

    private fun currentPollIntervalMs(): Long {
        return if (_currentTab.value == AppTab.SCORES || _detail.value.isOpen) {
            15_000L
        } else {
            45_000L
        }
    }

    private fun startOrRestartScorePolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                val intervalMs = currentPollIntervalMs()
                val intervalSec = (intervalMs / 1000L).toInt()
                if (_scores.value.pollIntervalSeconds != intervalSec) {
                    _scores.update { it.copy(pollIntervalSeconds = intervalSec) }
                }
                delay(intervalMs)
                refreshLiveScores(silent = true)
            }
        }
    }

    fun selectTab(tab: AppTab) {
        val previous = _currentTab.value
        _currentTab.value = tab
        if (tab != previous) {
            startOrRestartScorePolling()
            if (tab == AppTab.SCORES || (tab == AppTab.BROADCAST && _scores.value.liveCount > 0)) {
                val ageMs = System.currentTimeMillis() - _scores.value.lastUpdatedEpochMs
                if (ageMs > 10_000L) {
                    refreshLiveScores(silent = true)
                }
            }
        }
    }

    fun toggleTheme() {
        setDarkTheme(!_settings.value.isDarkTheme)
    }

    fun setDarkTheme(isDark: Boolean) {
        if (_settings.value.isDarkTheme == isDark) return
        _settings.update { it.copy(isDarkTheme = isDark) }
        viewModelScope.launch { settingsStore.setDarkTheme(isDark) }
    }

    fun setBroadcastFilter(filter: BroadcastFilter) {
        _broadcast.update { it.copy(filter = filter) }
    }

    fun updateBroadcastSearch(query: String) {
        _broadcast.update { it.copy(searchQuery = query) }
    }

    fun updateScoreSearch(query: String) {
        _scores.update { it.copy(searchQuery = query) }
    }

    fun setScoreFilter(filter: ScoreFilter) {
        _scores.update { it.copy(filter = filter, filterTouched = true) }
    }

    fun setSportCategory(category: SportCategory) {
        _scores.update { it.copy(sportCategory = category) }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun toggleWatchMatch(match: MatchSummary, leagueTitle: String) {
        if (match.id.isBlank()) {
            showToast("شناسهٔ این مسابقه برای اعلان موجود نیست.")
            return
        }
        val appContext = getApplication<Application>().applicationContext
        val currentMap = _watchedMatches.value.toMutableMap()
        if (currentMap.containsKey(match.id)) {
            currentMap.remove(match.id)
            _watchedMatches.value = currentMap
            MatchNotificationManager.cancelKickoffReminder(appContext, match.id)
            MatchNotificationManager.syncBackgroundScoreMonitor(
                appContext,
                _notificationSettings.value.enabled && currentMap.isNotEmpty()
            )
            viewModelScope.launch { settingsStore.saveWatchedMatches(currentMap) }
            showToast("اعلان مسابقهٔ ${match.host.name} و ${match.guest.name} غیرفعال شد.")
        } else {
            val isAlreadyFinished = !match.isLive && match.goals != null && match.statusTitle.contains("پایان")
            val watched = WatchedMatch(
                id = match.id,
                hostName = match.host.name,
                guestName = match.guest.name,
                leagueTitle = leagueTitle.ifBlank { "مسابقه" },
                startOnUtc = match.startOnUtc,
                lastHostGoals = match.goals?.host,
                lastGuestGoals = match.goals?.guest,
                wasLive = match.isLive,
                wasFinished = isAlreadyFinished
            )
            currentMap[match.id] = watched
            _watchedMatches.value = currentMap
            val notifSettings = _notificationSettings.value
            if (notifSettings.enabled && notifSettings.notifyOnKickoff && !match.isLive && !isAlreadyFinished) {
                MatchNotificationManager.scheduleKickoffReminder(
                    context = appContext,
                    match = watched,
                    leadMinutes = notifSettings.kickoffLeadMinutes
                )
            }
            MatchNotificationManager.syncBackgroundScoreMonitor(
                appContext,
                notifSettings.enabled && currentMap.isNotEmpty()
            )
            viewModelScope.launch { settingsStore.saveWatchedMatches(currentMap) }
            showToast("یادآور و اعلان نتیجه برای ${match.host.name} ـ ${match.guest.name} فعال شد.")
        }
    }

    fun unwatchMatch(matchId: String) {
        val appContext = getApplication<Application>().applicationContext
        val currentMap = _watchedMatches.value.toMutableMap()
        val removed = currentMap.remove(matchId) ?: return
        _watchedMatches.value = currentMap
        MatchNotificationManager.cancelKickoffReminder(appContext, matchId)
        MatchNotificationManager.syncBackgroundScoreMonitor(
            appContext,
            _notificationSettings.value.enabled && currentMap.isNotEmpty()
        )
        viewModelScope.launch { settingsStore.saveWatchedMatches(currentMap) }
        showToast("اعلان ${removed.hostName} ـ ${removed.guestName} حذف شد.")
    }

    fun updateNotificationSettings(newSettings: NotificationSettings) {
        val appContext = getApplication<Application>().applicationContext
        _notificationSettings.value = newSettings
        syncWatchedRemindersAndMonitor(_watchedMatches.value, newSettings)
        viewModelScope.launch {
            settingsStore.updateNotificationSettings(newSettings)
        }
        val msg = if (newSettings.enabled) "تنظیمات اعلان ذخیره شد." else "اعلان‌های مسابقات خاموش شدند."
        showToast(msg)
    }

    fun sendTestNotification() {
        val appContext = getApplication<Application>().applicationContext
        if (!MatchNotificationManager.hasNotificationPermission(appContext)) {
            showToast("دسترسی اعلان در تنظیمات گوشی فعال نیست.")
            return
        }
        MatchNotificationManager.showTestNotification(appContext)
        showToast("اعلان آزمایشی ارسال شد.")
    }

    fun openMatchDetailById(matchId: String) {
        if (matchId.isBlank()) return
        val found = findMatchEntryById(_scores.value.leagues, matchId)
        if (found != null) {
            pendingDeepLinkMatchId = null
            _currentTab.value = AppTab.SCORES
            openMatchDetail(found.match, found.leagueTitle, found.sport)
        } else {
            pendingDeepLinkMatchId = matchId
            refreshLiveScores(silent = false)
        }
    }

    private fun findMatchEntryById(leagues: List<LeagueGroup>, matchId: String): MatchEntry? {
        leagues.forEach { league ->
            league.matches.firstOrNull { it.id == matchId }?.let { m ->
                return MatchEntry(match = m, leagueTitle = league.title, sport = league.sport)
            }
        }
        return null
    }

    private fun syncWatchedRemindersAndMonitor(
        watchedMap: Map<String, WatchedMatch>,
        notifSettings: NotificationSettings
    ) {
        val appContext = getApplication<Application>().applicationContext
        watchedMap.values.forEach { watched ->
            if (notifSettings.enabled && notifSettings.notifyOnKickoff && !watched.wasLive && !watched.wasFinished) {
                MatchNotificationManager.scheduleKickoffReminder(
                    context = appContext,
                    match = watched,
                    leadMinutes = notifSettings.kickoffLeadMinutes
                )
            } else {
                MatchNotificationManager.cancelKickoffReminder(appContext, watched.id)
            }
        }
        MatchNotificationManager.syncBackgroundScoreMonitor(
            context = appContext,
            active = notifSettings.enabled && watchedMap.isNotEmpty()
        )
    }

    private fun processWatchedMatchesDiff(leagues: List<LeagueGroup>) {
        val currentMap = _watchedMatches.value
        if (currentMap.isEmpty()) return

        val appContext = getApplication<Application>().applicationContext
        val notifSettings = _notificationSettings.value
        val updatedMap = currentMap.toMutableMap()
        var changed = false

        leagues.forEach { league ->
            league.matches.forEach { match ->
                val prev = updatedMap[match.id] ?: return@forEach
                val newHost = match.goals?.host
                val newGuest = match.goals?.guest
                val isFinished = !match.isLive && match.goals != null &&
                    (match.statusTitle.contains("پایان") || prev.wasLive)

                if (notifSettings.enabled && notifSettings.notifyOnKickoff && !prev.wasLive && match.isLive) {
                    MatchNotificationManager.showMatchStartedNotification(
                        context = appContext,
                        matchId = match.id,
                        leagueTitle = league.title,
                        hostName = match.host.name,
                        guestName = match.guest.name
                    )
                }

                val scoreChanged = newHost != null && newGuest != null && (
                    (prev.lastHostGoals != null && prev.lastGuestGoals != null &&
                        (newHost != prev.lastHostGoals || newGuest != prev.lastGuestGoals)) ||
                        (prev.lastHostGoals == null && (newHost > 0 || newGuest > 0))
                    )
                if (notifSettings.enabled && notifSettings.notifyOnScoreChange && scoreChanged && newHost != null && newGuest != null) {
                    MatchNotificationManager.showScoreChangeNotification(
                        context = appContext,
                        matchId = match.id,
                        leagueTitle = league.title,
                        hostName = match.host.name,
                        guestName = match.guest.name,
                        hostGoals = newHost,
                        guestGoals = newGuest
                    )
                }

                if (notifSettings.enabled && notifSettings.notifyOnScoreChange && !prev.wasFinished && isFinished) {
                    MatchNotificationManager.showMatchFinishedNotification(
                        context = appContext,
                        matchId = match.id,
                        leagueTitle = league.title,
                        hostName = match.host.name,
                        guestName = match.guest.name,
                        hostGoals = newHost,
                        guestGoals = newGuest
                    )
                }

                val updated = prev.copy(
                    lastHostGoals = newHost ?: prev.lastHostGoals,
                    lastGuestGoals = newGuest ?: prev.lastGuestGoals,
                    wasLive = match.isLive,
                    wasFinished = prev.wasFinished || isFinished
                )
                if (updated != prev) {
                    updatedMap[match.id] = updated
                    changed = true
                }
            }
        }

        if (changed) {
            _watchedMatches.value = updatedMap
            viewModelScope.launch { settingsStore.saveWatchedMatches(updatedMap) }
        }
    }

    fun setFullscreen(fullscreen: Boolean) {
        _broadcast.update { it.copy(isFullscreen = fullscreen) }
    }

    fun loadNews() {
        if (_news.value.isLoading) return
        newsJob?.cancel()
        newsJob = viewModelScope.launch {
            _news.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.fetchSportsNews()
            }.onSuccess { articles ->
                _news.update {
                    it.copy(
                        articles = articles,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                enrichMergedGamesVisuals()
            }.onFailure { err ->
                _news.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "خطا در دریافت اخبار: ${err.message ?: "اتصال برقرار نشد"}"
                    )
                }
            }
        }
    }

    fun setNewsCategory(category: NewsCategory) {
        _news.update { it.copy(selectedCategory = category) }
    }

    fun updateNewsSearch(query: String) {
        _news.update { it.copy(searchQuery = query) }
    }

    fun openNewsArticle(article: NewsArticle) {
        articleBodyJob?.cancel()
        _news.update {
            it.copy(
                selectedArticle = article,
                articleBody = "",
                isLoadingArticleBody = true
            )
        }
        articleBodyJob = viewModelScope.launch {
            val (body, ogImage) = runCatching {
                repository.fetchNewsArticleDetail(article.link)
            }.getOrDefault("" to "")
            _news.update { state ->
                val resolvedImg = if (article.imageUrl.isBlank() && ogImage.isNotBlank()) ogImage else article.imageUrl
                val updatedSelected = state.selectedArticle?.let { sel ->
                    if (sel.id == article.id) sel.copy(imageUrl = resolvedImg) else sel
                }
                val updatedList = if (ogImage.isNotBlank() && article.imageUrl.isBlank()) {
                    state.articles.map { item ->
                        if (item.id == article.id && item.imageUrl.isBlank()) item.copy(imageUrl = ogImage) else item
                    }
                } else {
                    state.articles
                }
                state.copy(
                    articles = updatedList,
                    selectedArticle = updatedSelected,
                    articleBody = body,
                    isLoadingArticleBody = false
                )
            }
        }
    }

    fun closeNewsArticle() {
        articleBodyJob?.cancel()
        _news.update {
            it.copy(
                selectedArticle = null,
                articleBody = "",
                isLoadingArticleBody = false
            )
        }
    }

    fun loadPrograms() {
        programsJob?.cancel()
        val currentSettings = _settings.value

        programsJob = viewModelScope.launch {
            _broadcast.update {
                it.copy(
                    isLoadingPrograms = true,
                    programsStatus = "در حال دریافت بازی‌های آنتن و آپارات…"
                )
            }

            val antenDeferred = async {
                runCatching {
                    repository.fetchProgramsForSource(
                        source = BroadcastSource.ANTEN,
                        customAntenUrl = currentSettings.customAntenUrl,
                        antenToken = currentSettings.antenBearerToken
                    )
                }
            }
            val aparatDeferred = async {
                runCatching {
                    repository.fetchProgramsForSource(
                        source = BroadcastSource.APARAT,
                        customAntenUrl = currentSettings.customAntenUrl,
                        antenToken = currentSettings.antenBearerToken
                    )
                }
            }
            val antenRes = antenDeferred.await()
            val aparatRes = aparatDeferred.await()
            val merged = repository.mergeBroadcastPrograms(
                antenItems = antenRes.getOrDefault(emptyList()),
                aparatItems = aparatRes.getOrDefault(emptyList())
            )
            val failures = listOf(antenRes, aparatRes).count { it.isFailure }
            val countFa = SportsRepository.toPersianDigits(merged.size)
            val statusMsg = buildString {
                append("$countFa بازی")
                if (failures > 0) {
                    append(" · دریافت ${SportsRepository.toPersianDigits(failures)} منبع ناموفق بود")
                }
            }
            _broadcast.update {
                it.copy(
                    mergedGames = merged,
                    isLoadingPrograms = false,
                    programsStatus = statusMsg
                )
            }
            enrichMergedGamesVisuals()

            // Concurrently prefetch real posters & team crests from program detail endpoints for top featured games
            val candidates = merged.take(8).filter {
                it.posterUrl.isBlank() || it.hostLogoUrl.isBlank() || it.guestLogoUrl.isBlank()
            }
            if (candidates.isNotEmpty()) {
                val enrichedByKey = candidates.map { game ->
                    async {
                        val primaryProgram = game.sources[BroadcastSource.APARAT]
                            ?: game.sources[BroadcastSource.ANTEN]
                            ?: game.sources.values.firstOrNull()
                        if (primaryProgram == null) {
                            null
                        } else {
                            runCatching {
                                val detailed = repository.fetchProgramVisualDetails(
                                    program = primaryProgram,
                                    customAntenUrl = currentSettings.customAntenUrl,
                                    antenToken = currentSettings.antenBearerToken
                                )
                                game.key to detailed
                            }.getOrNull()
                        }
                    }
                }.awaitAll().filterNotNull().toMap()

                if (enrichedByKey.isNotEmpty()) {
                    _broadcast.update { state ->
                        state.copy(
                            mergedGames = state.mergedGames.map { game ->
                                val detailed = enrichedByKey[game.key] ?: return@map game
                                game.copy(
                                    posterUrl = game.posterUrl.ifBlank { detailed.posterUrl },
                                    hostLogoUrl = game.hostLogoUrl.ifBlank { detailed.hostLogoUrl },
                                    guestLogoUrl = game.guestLogoUrl.ifBlank { detailed.guestLogoUrl },
                                    leagueTitle = game.leagueTitle.ifBlank { detailed.leagueTitle }
                                )
                            }
                        )
                    }
                    enrichMergedGamesVisuals()
                }
            }
        }
    }

    private fun enrichMergedGamesVisuals() {
        val leagues = _scores.value.leagues
        val articles = _news.value.articles
        val currentGames = _broadcast.value.mergedGames
        if (currentGames.isEmpty()) return

        fun normalizeFa(value: String): String =
            value.replace('ي', 'ی').replace('ك', 'ک').replace('\u200C', ' ').trim()

        val allMatches = leagues.flatMap { league ->
            league.matches.map { match -> league.title to match }
        }
        val photoArticles = articles.filter { it.imageUrl.isNotBlank() }

        val updated = currentGames.map { game ->
            val normTitle = normalizeFa(game.title)
            var hLogo = game.hostLogoUrl
            var gLogo = game.guestLogoUrl
            var leagueName = game.leagueTitle

            if (hLogo.isBlank() || gLogo.isBlank() || leagueName.isBlank()) {
                val matched = allMatches.firstOrNull { (_, m) ->
                    val h = normalizeFa(m.host.name)
                    val g = normalizeFa(m.guest.name)
                    h.length >= 3 && g.length >= 3 && normTitle.contains(h) && normTitle.contains(g)
                } ?: allMatches.firstOrNull { (_, m) ->
                    val h = normalizeFa(m.host.name)
                    val g = normalizeFa(m.guest.name)
                    (h.length >= 4 && normTitle.contains(h)) || (g.length >= 4 && normTitle.contains(g))
                }
                if (matched != null) {
                    if (leagueName.isBlank()) leagueName = matched.first
                    if (hLogo.isBlank()) hLogo = matched.second.host.logoUrl
                    if (gLogo.isBlank()) gLogo = matched.second.guest.logoUrl
                }
            }

            var newsPhoto = game.newsPhotoUrl
            if (game.posterUrl.isBlank() && newsPhoto.isBlank() && photoArticles.isNotEmpty()) {
                val tokens = normTitle
                    .split('-', '–', '—', ' ')
                    .map { it.trim() }
                    .filter { it.length >= 4 && it !in setOf("مقابل", "برابر", "زنده", "مسابقه", "پخش", "فوتبال") }
                val matchedArticle = photoArticles.firstOrNull { art ->
                    val artTitle = normalizeFa(art.title)
                    tokens.any { token -> artTitle.contains(token) }
                }
                if (matchedArticle != null) {
                    newsPhoto = matchedArticle.imageUrl
                }
            }

            game.copy(
                hostLogoUrl = hLogo,
                guestLogoUrl = gLogo,
                leagueTitle = leagueName,
                newsPhotoUrl = newsPhoto
            )
        }

        if (updated != currentGames) {
            _broadcast.update { it.copy(mergedGames = updated) }
        }
    }

    fun onMergedGameClicked(game: MergedBroadcastGame) {
        when {
            _broadcast.value.filter == BroadcastFilter.ANTEN && game.sources.containsKey(BroadcastSource.ANTEN) -> {
                val prog = game.sources.getValue(BroadcastSource.ANTEN)
                playProgramFromSource(game.title, BroadcastSource.ANTEN, prog)
            }
            _broadcast.value.filter == BroadcastFilter.APARAT && game.sources.containsKey(BroadcastSource.APARAT) -> {
                val prog = game.sources.getValue(BroadcastSource.APARAT)
                playProgramFromSource(game.title, BroadcastSource.APARAT, prog)
            }
            game.sources.size == 1 -> {
                val (source, program) = game.sources.entries.first()
                playProgramFromSource(game.title, source, program)
            }
            else -> {
                _broadcast.update { it.copy(watchChooserGame = game) }
            }
        }
    }

    fun dismissWatchChooser() {
        _broadcast.update { it.copy(watchChooserGame = null) }
    }

    fun selectProgramFromChooser(game: MergedBroadcastGame, source: BroadcastSource) {
        val program = game.sources[source] ?: return
        dismissWatchChooser()
        playProgramFromSource(game.title, source, program)
    }

    private fun playProgramFromSource(
        displayTitle: String,
        source: BroadcastSource,
        program: BroadcastProgram
    ) {
        clearMedia()
        _broadcast.update {
            it.copy(
                activeProgramId = program.id,
                activeProgramTitle = displayTitle,
                activeSourceLabel = "پخش از ${source.titleFa}",
                playerStatus = "در حال بررسی دسترسی پخش…"
            )
        }

        val currentSettings = _settings.value
        playbackJob = viewModelScope.launch {
            runCatching {
                repository.fetchQualitiesForProgram(
                    program = program,
                    customAntenUrl = currentSettings.customAntenUrl,
                    antenToken = currentSettings.antenBearerToken
                )
            }.onSuccess { qualities ->
                if (qualities.isEmpty()) {
                    _broadcast.update {
                        it.copy(playerStatus = "کیفیتی برای این پخش پیدا نشد.")
                    }
                } else {
                    _broadcast.update {
                        it.copy(qualities = qualities, activeQualityIndex = 0)
                    }
                    activateStreamQuality(0)
                }
            }.onFailure { err ->
                val msg = if (err is HttpStatusException && err.code == 404) {
                    "این پخش فعلاً در دسترس نیست."
                } else {
                    "خطا: ${err.message ?: "عدم دریافت پخش"}"
                }
                _broadcast.update { it.copy(playerStatus = msg) }
            }
        }
    }

    fun selectStreamQuality(index: Int) {
        if (index !in _broadcast.value.qualities.indices) return
        activateStreamQuality(index)
    }

    private fun activateStreamQuality(index: Int) {
        val quality = _broadcast.value.qualities.getOrNull(index) ?: return
        val token = _settings.value.antenBearerToken
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            _broadcast.update {
                it.copy(
                    activeQualityIndex = index,
                    playerStatus = "در حال آماده‌سازی پخش…"
                )
            }
            runCatching {
                repository.resolveStreamUrl(quality, token)
            }.onSuccess { url ->
                _broadcast.update {
                    it.copy(
                        streamUrl = url,
                        playerStatus = "${quality.label} آمادهٔ پخش است."
                    )
                }
            }.onFailure { err ->
                _broadcast.update {
                    it.copy(playerStatus = "خطا در پخش: ${err.message ?: "نامشخص"}")
                }
            }
        }
    }

    fun clearMedia() {
        playbackJob?.cancel()
        _broadcast.update {
            it.copy(
                streamUrl = null,
                qualities = emptyList(),
                activeQualityIndex = 0,
                isFullscreen = false,
                activeProgramId = null,
                activeProgramTitle = "پخش موردنظرت را انتخاب کن",
                activeSourceLabel = "آنتن و آپارات اسپرت",
                playerStatus = "یک بازی را انتخاب کن تا پخش زنده شروع شود."
            )
        }
    }

    fun onPlayerError(message: String) {
        _broadcast.update {
            it.copy(playerStatus = "خطا در پخش: $message")
        }
    }

    fun refreshLiveScores(silent: Boolean = false, isUserInitiated: Boolean = false) {
        if (_scores.value.isLoading) {
            if (isUserInitiated) {
                _scores.update { it.copy(isManualScoreSyncing = true) }
            }
            return
        }
        viewModelScope.launch {
            _scores.update {
                it.copy(
                    isLoading = true,
                    isManualScoreSyncing = isUserInitiated,
                    statusText = if (it.leagues.isEmpty() && !silent) "در حال دریافت نتایج…" else it.statusText
                )
            }
            runCatching {
                repository.fetchVarzesh3LiveScores()
            }.onSuccess { leagues ->
                val allEntries = leagues.flatMap { league ->
                    league.matches.map { match ->
                        MatchEntry(match = match, leagueTitle = league.title, sport = league.sport)
                    }
                }
                val liveEntries = allEntries.filter { it.match.isLive }
                val upcomingEntries = allEntries
                    .filter { !it.match.isLive && it.match.goals == null }
                    .sortedBy { it.match.startOnUtc }
                val finishedEntries = allEntries.filter { !it.match.isLive && it.match.goals != null }
                val featured = (liveEntries + upcomingEntries + finishedEntries).take(7)

                val nowMs = System.currentTimeMillis()
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(nowMs))
                val autoFilter = if (!_scores.value.filterTouched) {
                    if (liveEntries.isNotEmpty()) ScoreFilter.LIVE else ScoreFilter.ALL
                } else {
                    _scores.value.filter
                }

                val shownCount = if (autoFilter == ScoreFilter.LIVE) liveEntries.size else allEntries.size
                val label = if (autoFilter == ScoreFilter.LIVE) "بازی در جریان" else "بازی در فهرست"
                _scores.update {
                    it.copy(
                        leagues = leagues,
                        featuredMatches = featured,
                        liveCount = liveEntries.size,
                        filter = autoFilter,
                        isLoading = false,
                        isManualScoreSyncing = false,
                        lastUpdatedTime = SportsRepository.toPersianDigits(timeStr),
                        lastUpdatedEpochMs = nowMs,
                        statusText = "${SportsRepository.toPersianDigits(shownCount)} $label · ${SportsRepository.toPersianDigits(timeStr)}"
                    )
                }

                processWatchedMatchesDiff(leagues)
                enrichMergedGamesVisuals()
                syncOpenMatchDetailFromLeagues(leagues)

                pendingDeepLinkMatchId?.let { targetId ->
                    pendingDeepLinkMatchId = null
                    findMatchEntryById(leagues, targetId)?.let { entry ->
                        _currentTab.value = AppTab.SCORES
                        openMatchDetail(entry.match, entry.leagueTitle, entry.sport)
                    }
                }
            }.onFailure { err ->
                _scores.update {
                    it.copy(
                        isLoading = false,
                        isManualScoreSyncing = false,
                        statusText = "خطا در دریافت: ${err.message ?: "اتصال برقرار نشد"}"
                    )
                }
            }
        }
    }

    fun refreshInstagramStories() {
        val currentState = _scores.value
        val session = currentState.instagramSession
        if (currentState.isLoadingStories) return

        if (!session.isAuthenticated) {
            storiesJob?.cancel()
            storiesJob = viewModelScope.launch {
                _scores.update {
                    it.copy(
                        isLoadingStories = true,
                        storiesError = null
                    )
                }
                val previewGroups = runCatching {
                    instagramRepository.fetchPublicGuestPreview(currentState.instagramUsernames)
                }.getOrDefault(emptyList())

                _scores.update {
                    it.copy(
                        storyGroups = previewGroups,
                        isLoadingStories = false,
                        needsInstagramLogin = previewGroups.isEmpty(),
                        storiesError = null
                    )
                }
            }
            return
        }

        storiesJob?.cancel()
        storiesJob = viewModelScope.launch {
            _scores.update {
                it.copy(
                    isLoadingStories = true,
                    needsInstagramLogin = false,
                    storiesError = null
                )
            }
            runCatching {
                instagramRepository.fetchStoriesWithSession(
                    session = session,
                    pinnedUsernames = currentState.instagramUsernames
                )
            }.onSuccess { result ->
                val updatedSession = if (result.resolvedAccountUsername.isNotBlank() &&
                    result.resolvedAccountUsername != session.username
                ) {
                    val next = session.copy(username = result.resolvedAccountUsername)
                    settingsStore.saveInstagramSession(next)
                    next
                } else {
                    session
                }
                _scores.update {
                    it.copy(
                        instagramSession = updatedSession,
                        needsInstagramLogin = false,
                        storyGroups = result.groups,
                        isLoadingStories = false,
                        storiesError = if (result.groups.isEmpty()) {
                            "استوری فعالی در حساب یا پیج‌های انتخابی نیست."
                        } else null
                    )
                }
            }.onFailure { err ->
                if (err is InstagramAuthRequiredException) {
                    settingsStore.clearInstagramSession()
                    val previewGroups = runCatching {
                        instagramRepository.fetchPublicGuestPreview(currentState.instagramUsernames)
                    }.getOrDefault(emptyList())
                    _scores.update {
                        it.copy(
                            instagramSession = InstagramSession(),
                            needsInstagramLogin = previewGroups.isEmpty(),
                            storyGroups = previewGroups,
                            isLoadingStories = false,
                            storiesError = if (previewGroups.isEmpty()) err.message else null
                        )
                    }
                } else {
                    _scores.update {
                        it.copy(
                            isLoadingStories = false,
                            storiesError = "خطا در دریافت از اینستاگرام: ${err.message ?: "اتصال برقرار نشد"}"
                        )
                    }
                }
            }
        }
    }

    fun onInstagramSessionCaptured(session: InstagramSession) {
        if (!session.isAuthenticated) return
        storiesJob?.cancel()
        _scores.update {
            it.copy(
                instagramSession = session,
                needsInstagramLogin = false,
                isLoadingStories = false,
                storiesError = null
            )
        }
        viewModelScope.launch {
            settingsStore.saveInstagramSession(session)
            refreshInstagramStories()
        }
        showToast("حساب اینستاگرام متصل شد؛ در حال دریافت استوری‌ها…")
    }

    fun connectWithManualSessionId(rawInput: String): Boolean {
        val parsed = InstagramRepository.buildSessionFromManualToken(rawInput)
        if (parsed == null) {
            showToast("مقدار Session ID معتبر نیست.")
            return false
        }
        onInstagramSessionCaptured(parsed)
        return true
    }

    fun logoutInstagramAccount() {
        storiesJob?.cancel()
        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
        _scores.update {
            it.copy(
                instagramSession = InstagramSession(),
                needsInstagramLogin = true,
                storyGroups = emptyList(),
                isLoadingStories = false,
                storiesError = null
            )
        }
        viewModelScope.launch {
            settingsStore.clearInstagramSession()
            refreshInstagramStories()
        }
        showToast("خروج از حساب اینستاگرام انجام شد.")
    }

    fun addInstagramUsername(raw: String) {
        val normalized = InstagramRepository.normalizeUsername(raw)
        if (normalized.length < 2) {
            showToast("نام کاربری معتبر اینستاگرام را وارد کن.")
            return
        }
        val current = _scores.value.instagramUsernames
        if (normalized in current) {
            showToast("پیج @$normalized قبلاً اضافه شده است.")
            return
        }
        val updated = listOf(normalized) + current
        _scores.update { it.copy(instagramUsernames = updated) }
        viewModelScope.launch {
            settingsStore.saveInstagramUsernames(updated)
        }
        storiesJob?.cancel()
        _scores.update { it.copy(isLoadingStories = false) }
        refreshInstagramStories()
        showToast("پیج @$normalized به پیج‌های استوری اضافه شد.")
    }

    fun removeInstagramUsername(username: String) {
        val current = _scores.value.instagramUsernames
        val updated = current.filterNot { it.equals(username, ignoreCase = true) }
        _scores.update { state ->
            state.copy(
                instagramUsernames = updated,
                storyGroups = state.storyGroups.filterNot { it.username.equals(username, ignoreCase = true) }
            )
        }
        viewModelScope.launch {
            settingsStore.saveInstagramUsernames(updated)
        }
        showToast("پیج @$username حذف شد.")
    }

    fun openStoryGroup(groupIndex: Int) {
        val groups = _scores.value.storyGroups
        val group = groups.getOrNull(groupIndex) ?: return
        val firstId = group.items.firstOrNull()?.id
        _scores.update { state ->
            state.copy(
                activeStoryGroupIndex = groupIndex,
                activeStorySlideIndex = 0,
                seenStoryItemIds = if (firstId != null) state.seenStoryItemIds + firstId else state.seenStoryItemIds
            )
        }
    }

    fun nextStorySlide() {
        _scores.update { state ->
            val gIdx = state.activeStoryGroupIndex ?: return@update state
            val currentGroup = state.storyGroups.getOrNull(gIdx)
                ?: return@update state.copy(activeStoryGroupIndex = null, activeStorySlideIndex = 0)
            val nextSlide = state.activeStorySlideIndex + 1
            if (nextSlide < currentGroup.items.size) {
                val id = currentGroup.items[nextSlide].id
                state.copy(
                    activeStorySlideIndex = nextSlide,
                    seenStoryItemIds = state.seenStoryItemIds + id
                )
            } else {
                val nextGroup = gIdx + 1
                val targetGroup = state.storyGroups.getOrNull(nextGroup)
                if (targetGroup != null && targetGroup.items.isNotEmpty()) {
                    state.copy(
                        activeStoryGroupIndex = nextGroup,
                        activeStorySlideIndex = 0,
                        seenStoryItemIds = state.seenStoryItemIds + targetGroup.items.first().id
                    )
                } else {
                    state.copy(activeStoryGroupIndex = null, activeStorySlideIndex = 0)
                }
            }
        }
    }

    fun prevStorySlide() {
        _scores.update { state ->
            val gIdx = state.activeStoryGroupIndex ?: return@update state
            val prevSlide = state.activeStorySlideIndex - 1
            if (prevSlide >= 0) {
                state.copy(activeStorySlideIndex = prevSlide)
            } else {
                val prevGroupIdx = gIdx - 1
                val prevGroup = state.storyGroups.getOrNull(prevGroupIdx)
                if (prevGroup != null && prevGroup.items.isNotEmpty()) {
                    val lastSlide = prevGroup.items.lastIndex
                    state.copy(
                        activeStoryGroupIndex = prevGroupIdx,
                        activeStorySlideIndex = lastSlide,
                        seenStoryItemIds = state.seenStoryItemIds + prevGroup.items[lastSlide].id
                    )
                } else {
                    state.copy(activeStorySlideIndex = 0)
                }
            }
        }
    }

    fun closeStoryViewer() {
        _scores.update { it.copy(activeStoryGroupIndex = null, activeStorySlideIndex = 0) }
    }

    fun openMatchDetail(match: MatchSummary, leagueTitle: String, sport: Int) {
        if (match.id.isBlank() || !match.id.all(Char::isDigit)) {
            showToast("شناسهٔ این مسابقه برای نمایش جزئیات موجود نیست.")
            return
        }
        detailJob?.cancel()
        highlightJob?.cancel()

        _detail.value = MatchDetailUiState(
            isOpen = true,
            match = match,
            leagueTitle = leagueTitle,
            sport = sport,
            activeTab = DetailTab.TIMELINE,
            isLoadingEvents = sport == 1,
            isLoadingDetails = sport == 1,
            eventsMessage = if (sport != 1) "رویدادهای این رشته در منبع فعلی موجود نیست." else null,
            detailsMessage = if (sport != 1) "آمار و ترکیب این رشته در API فعلی موجود نیست." else null
        )

        startOrRestartScorePolling()
        if (sport != 1) return

        detailJob = viewModelScope.launch {
            val eventsDeferred = async { runCatching { repository.fetchVarzesh3Events(match.id) } }
            val detailsDeferred = async { runCatching { repository.fetchVarzesh3MatchDetails(match.id) } }

            val eventsResult = eventsDeferred.await()
            _detail.update { state ->
                eventsResult.fold(
                    onSuccess = { list ->
                        state.copy(
                            events = list,
                            isLoadingEvents = false,
                            eventsMessage = if (list.isEmpty()) "هنوز رخدادی برای این بازی ثبت نشده است." else null
                        )
                    },
                    onFailure = {
                        state.copy(
                            isLoadingEvents = false,
                            eventsMessage = "دریافت رخدادها در حال حاضر ممکن نیست."
                        )
                    }
                )
            }

            val detailsResult = detailsDeferred.await()
            _detail.update { state ->
                detailsResult.fold(
                    onSuccess = { info ->
                        state.copy(
                            detailInfo = info,
                            isLoadingDetails = false,
                            detailsMessage = null
                        )
                    },
                    onFailure = {
                        state.copy(
                            isLoadingDetails = false,
                            detailsMessage = "آمار و ترکیب این مسابقه فعلاً در دسترس نیست."
                        )
                    }
                )
            }
        }
    }

    fun selectDetailTab(tab: DetailTab) {
        if (tab != DetailTab.TIMELINE) {
            closeHighlightVideo()
        }
        _detail.update { it.copy(activeTab = tab) }
    }

    fun toggleHighlightVideo(event: MatchEvent) {
        if (event.videoId.isBlank()) return
        if (_detail.value.activeHighlightEventId == event.id) {
            closeHighlightVideo()
            return
        }
        highlightJob?.cancel()
        _detail.update {
            it.copy(
                activeHighlightEventId = event.id,
                highlightVideo = null,
                isLoadingHighlight = true,
                highlightStatus = "در حال آماده‌سازی ویدئوی صحنه…"
            )
        }
        highlightJob = viewModelScope.launch {
            runCatching {
                repository.fetchVarzesh3HighlightVideo(event.videoId)
            }.onSuccess { video ->
                _detail.update {
                    it.copy(
                        highlightVideo = video,
                        isLoadingHighlight = false,
                        highlightStatus = ""
                    )
                }
            }.onFailure { err ->
                _detail.update {
                    it.copy(
                        isLoadingHighlight = false,
                        highlightStatus = "پخش این صحنه ممکن نیست: ${err.message ?: "خطا"}"
                    )
                }
            }
        }
    }

    fun closeHighlightVideo() {
        highlightJob?.cancel()
        _detail.update {
            it.copy(
                activeHighlightEventId = null,
                highlightVideo = null,
                isLoadingHighlight = false,
                highlightStatus = ""
            )
        }
    }

    private fun syncOpenMatchDetailFromLeagues(leagues: List<LeagueGroup>) {
        val currentDetail = _detail.value
        val activeMatch = currentDetail.match
        if (!currentDetail.isOpen || activeMatch == null) return

        val updatedEntry = findMatchEntryById(leagues, activeMatch.id) ?: return
        _detail.update { it.copy(match = updatedEntry.match) }

        if (updatedEntry.sport == 1 && updatedEntry.match.isLive) {
            viewModelScope.launch {
                runCatching { repository.fetchVarzesh3Events(updatedEntry.match.id) }
                    .onSuccess { list ->
                        if (_detail.value.isOpen && _detail.value.match?.id == updatedEntry.match.id) {
                            _detail.update { state ->
                                state.copy(
                                    events = list,
                                    eventsMessage = if (list.isEmpty()) "هنوز رخدادی برای این بازی ثبت نشده است." else null
                                )
                            }
                        }
                    }
            }
        }
    }

    fun closeMatchDetail() {
        detailJob?.cancel()
        highlightJob?.cancel()
        _detail.value = MatchDetailUiState(isOpen = false)
        startOrRestartScorePolling()
    }

    fun toggleFavoriteTeam(
        teamName: String,
        logoUrl: String = "",
        leagueTitle: String = "",
        sport: Int = 1
    ) {
        val cleanName = teamName.trim()
        if (cleanName.isBlank()) return
        val current = _favoriteTeams.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.name.equals(cleanName, ignoreCase = true) }
        if (existingIdx >= 0) {
            current.removeAt(existingIdx)
            showToast("«$cleanName» از تیم‌های محبوب حذف شد.")
        } else {
            current.add(
                0,
                FavoriteTeam(
                    name = cleanName,
                    logoUrl = logoUrl,
                    leagueTitle = leagueTitle,
                    sport = sport
                )
            )
            showToast("«$cleanName» به تیم‌های محبوب اضافه شد.")
        }
        _favoriteTeams.value = current
        viewModelScope.launch { settingsStore.saveFavoriteTeams(current) }
    }

    fun openTeamProfile(
        teamName: String,
        logoUrl: String = "",
        leagueTitle: String = "",
        sport: Int = 1
    ) {
        val cleanName = teamName.trim()
        if (cleanName.isBlank()) return

        val teamMatches = _scores.value.leagues.flatMap { league ->
            league.matches.filter { m ->
                m.host.name.equals(cleanName, ignoreCase = true) ||
                    m.guest.name.equals(cleanName, ignoreCase = true) ||
                    m.host.name.contains(cleanName, ignoreCase = true) ||
                    m.guest.name.contains(cleanName, ignoreCase = true)
            }.map { m ->
                MatchEntry(match = m, leagueTitle = league.title, sport = league.sport)
            }
        }

        val resolvedLogo = logoUrl.ifBlank {
            teamMatches.firstNotNullOfOrNull { entry ->
                when {
                    entry.match.host.name.equals(cleanName, ignoreCase = true) -> entry.match.host.logoUrl.takeIf { it.isNotBlank() }
                    entry.match.guest.name.equals(cleanName, ignoreCase = true) -> entry.match.guest.logoUrl.takeIf { it.isNotBlank() }
                    else -> null
                }
            }.orEmpty()
        }

        val resolvedLeague = leagueTitle.ifBlank {
            teamMatches.firstOrNull()?.leagueTitle.orEmpty()
        }

        val teamBroadcasts = _broadcast.value.mergedGames.filter { game ->
            game.title.contains(cleanName, ignoreCase = true)
        }

        val currentDetail = _detail.value
        val matchedSide = when {
            currentDetail.isOpen && currentDetail.match?.host?.name.equals(cleanName, ignoreCase = true) ->
                currentDetail.detailInfo?.hostLineup
            currentDetail.isOpen && currentDetail.match?.guest?.name.equals(cleanName, ignoreCase = true) ->
                currentDetail.detailInfo?.guestLineup
            else -> null
        }

        val relatedNews = _news.value.articles.filter { article ->
            article.title.contains(cleanName, ignoreCase = true) ||
                article.summary.contains(cleanName, ignoreCase = true)
        }.take(6)

        _activeTeamProfile.value = TeamProfileData(
            teamName = cleanName,
            logoUrl = resolvedLogo,
            leagueTitle = resolvedLeague,
            sport = teamMatches.firstOrNull()?.sport ?: sport,
            matches = teamMatches,
            broadcastGames = teamBroadcasts,
            squadPlayers = matchedSide?.let { it.starters + it.bench }.orEmpty(),
            formation = matchedSide?.formation.orEmpty(),
            coach = matchedSide?.coach.orEmpty(),
            relatedNews = relatedNews
        )
    }

    fun closeTeamProfile() {
        _activeTeamProfile.value = null
    }

    fun openPlayerProfile(
        player: LineupPlayer,
        teamName: String,
        teamLogoUrl: String = ""
    ) {
        val currentDetail = _detail.value
        val events = currentDetail.events
        val playerName = player.name.trim()

        val contributions = events.filter { ev ->
            playerName.isNotBlank() && (
                ev.strikerName.contains(playerName, ignoreCase = true) ||
                    ev.assisterName.contains(playerName, ignoreCase = true) ||
                    ev.description.contains(playerName, ignoreCase = true)
                )
        }.map { ev ->
            PlayerMatchContribution(
                minute = "${SportsRepository.toPersianDigits(ev.time)}′",
                title = ev.titleLabel,
                detail = ev.description.ifBlank { ev.decisionTitle },
                eventType = ev.eventType
            )
        }

        val goalsCount = events.count { ev ->
            ev.eventType == 1 && playerName.isNotBlank() &&
                (ev.strikerName.contains(playerName, ignoreCase = true) || ev.description.contains(playerName, ignoreCase = true))
        }
        val assistsCount = events.count { ev ->
            playerName.isNotBlank() && ev.assisterName.contains(playerName, ignoreCase = true)
        }
        val cardsCount = events.count { ev ->
            playerName.isNotBlank() && ev.description.contains(playerName, ignoreCase = true) &&
                (ev.description.contains("کارت زرد") || ev.description.contains("کارت قرمز"))
        }

        val matchLabel = currentDetail.match?.let { m ->
            "${m.host.name} ـ ${m.guest.name}"
        }.orEmpty()

        _activePlayerProfile.value = PlayerProfileData(
            player = player,
            teamName = teamName,
            teamLogoUrl = teamLogoUrl,
            leagueTitle = currentDetail.leagueTitle,
            matchLabel = matchLabel,
            contributions = contributions,
            goalsCount = goalsCount,
            assistsCount = assistsCount,
            cardsCount = cardsCount
        )
    }

    fun openPlayerProfileByName(
        rawName: String,
        teamName: String,
        teamLogoUrl: String = ""
    ) {
        val clean = rawName.trim()
        if (clean.isBlank()) return
        val currentDetail = _detail.value
        val allPlayers = listOfNotNull(
            currentDetail.detailInfo?.hostLineup,
            currentDetail.detailInfo?.guestLineup
        ).flatMap { it.starters + it.bench }
        val matched = allPlayers.firstOrNull {
            it.name.equals(clean, ignoreCase = true) || it.name.contains(clean, ignoreCase = true)
        } ?: LineupPlayer(name = clean)
        openPlayerProfile(player = matched, teamName = teamName, teamLogoUrl = teamLogoUrl)
    }

    fun closePlayerProfile() {
        _activePlayerProfile.value = null
    }
}
