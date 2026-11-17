package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppTab
import com.example.notifications.MatchNotificationManager
import com.example.ui.components.FullscreenBroadcastPlayerOverlay
import com.example.ui.components.InstagramAccountsSheet
import com.example.ui.components.InstagramLoginModal
import com.example.ui.components.InstagramStoryViewerModal
import com.example.ui.components.KeepActivityScreenOnEffect
import com.example.ui.components.KhateBaziBottomBar
import com.example.ui.components.KhateBaziTopBar
import com.example.ui.components.NotificationSettingsSheet
import com.example.ui.components.TabIconWithLiveBadge
import com.example.ui.components.WatchSourceChooserSheet
import com.example.ui.components.rememberBroadcastPlayerController
import com.example.ui.screens.BroadcastScreen
import com.example.ui.screens.MatchDetailModal
import com.example.ui.screens.NewsReaderModal
import com.example.ui.screens.NewsScreen
import com.example.ui.screens.ScoresScreen
import com.example.ui.theme.KhateBaziTheme
import com.example.ui.viewmodel.SportsViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SportsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDeepLinkIntent(intent)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            DisposableEffect(settings.isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (settings.isDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (settings.isDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                KhateBaziTheme(darkTheme = settings.isDarkTheme) {
                    KhateBaziApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val matchId = intent?.getStringExtra(MatchNotificationManager.EXTRA_MATCH_ID)
        if (!matchId.isNullOrBlank()) {
            viewModel.openMatchDetailById(matchId)
        }
    }
}

@Composable
private fun KhateBaziApp(viewModel: SportsViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val notificationSettings by viewModel.notificationSettings.collectAsStateWithLifecycle()
    val watchedMatches by viewModel.watchedMatches.collectAsStateWithLifecycle()
    val broadcast by viewModel.broadcast.collectAsStateWithLifecycle()
    val scores by viewModel.scores.collectAsStateWithLifecycle()
    val news by viewModel.news.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var pendingNotificationAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showNotificationSheet by remember { mutableStateOf(false) }
    var showInstagramAccountsSheet by remember { mutableStateOf(false) }
    var showInstagramLoginModal by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val action = pendingNotificationAction
        pendingNotificationAction = null
        if (granted) {
            action?.invoke()
        } else {
            viewModel.showToast("برای دریافت یادآور بازی، مجوز اعلان را فعال کن.")
        }
    }

    fun runWithNotificationPermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                pendingNotificationAction = action
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        action()
    }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    val playerController = rememberBroadcastPlayerController(
        streamUrl = broadcast.streamUrl,
        onPlayerError = { viewModel.onPlayerError(it) }
    )
    KeepActivityScreenOnEffect(
        enabled = broadcast.activeProgramId != null || !broadcast.streamUrl.isNullOrBlank()
    )

    if (currentTab != AppTab.BROADCAST && !detail.isOpen && !broadcast.isFullscreen && news.selectedArticle == null && scores.activeStoryGroupIndex == null) {
        BackHandler { viewModel.selectTab(AppTab.BROADCAST) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                KhateBaziTopBar(
                    liveCount = scores.liveCount,
                    watchedCount = watchedMatches.size,
                    isDarkTheme = settings.isDarkTheme,
                    isRefreshing = scores.isLoading || scores.isLoadingStories || broadcast.isLoadingPrograms || news.isLoading,
                    onRefreshClick = {
                        viewModel.refreshLiveScores()
                        viewModel.refreshInstagramStories()
                        viewModel.loadPrograms()
                        viewModel.loadNews()
                    },
                    onOpenNotificationSettings = { showNotificationSheet = true },
                    onToggleTheme = { viewModel.toggleTheme() }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    KhateBaziBottomBar(
                        currentTab = currentTab,
                        liveCount = scores.liveCount,
                        onSelectTab = { viewModel.selectTab(it) }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.weight(0.2f))
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    TabIconWithLiveBadge(
                                        tab = tab,
                                        selected = selected,
                                        liveCount = scores.liveCount,
                                        tint = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.labelFa,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .testTag("rail_tab_${tab.route}")
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            val forward = targetState.ordinal > initialState.ordinal
                            val enter = slideInHorizontally(
                                animationSpec = tween(280),
                                initialOffsetX = { fullWidth ->
                                    if (forward) -fullWidth / 5 else fullWidth / 5
                                }
                            ) + fadeIn(tween(260)) + scaleIn(
                                initialScale = 0.98f,
                                animationSpec = tween(260)
                            )
                            val exit = slideOutHorizontally(
                                animationSpec = tween(220),
                                targetOffsetX = { fullWidth ->
                                    if (forward) fullWidth / 6 else -fullWidth / 6
                                }
                            ) + fadeOut(tween(180))
                            enter.togetherWith(exit)
                        },
                        label = "screenTransition"
                    ) { tab ->
                        when (tab) {
                            AppTab.BROADCAST -> BroadcastScreen(
                                broadcastState = broadcast,
                                scoresState = scores,
                                featuredNews = news.articles,
                                isLoadingNews = news.isLoading,
                                onFilterChange = { viewModel.setBroadcastFilter(it) },
                                onMergedGameClick = { viewModel.onMergedGameClicked(it) },
                                onMatchClick = { match, league, sport ->
                                    viewModel.openMatchDetail(match, league, sport)
                                },
                                onViewAllScoresClick = { viewModel.selectTab(AppTab.SCORES) },
                                onRefreshScores = { viewModel.refreshLiveScores(isUserInitiated = true) },
                                onNewsClick = { viewModel.openNewsArticle(it) },
                                onViewAllNewsClick = { viewModel.selectTab(AppTab.NEWS) },
                                onSelectQuality = { viewModel.selectStreamQuality(it) },
                                onToggleFullscreen = { viewModel.setFullscreen(it) },
                                onClosePlayer = { viewModel.clearMedia() },
                                onPlayerError = { viewModel.onPlayerError(it) },
                                onSearchChange = { viewModel.updateBroadcastSearch(it) },
                                onRefreshPrograms = { viewModel.loadPrograms() },
                                playerController = playerController
                            )

                            AppTab.SCORES -> ScoresScreen(
                                scoresState = scores,
                                watchedMatchIds = watchedMatches.keys,
                                onFilterChange = { viewModel.setScoreFilter(it) },
                                onSportChange = { viewModel.setSportCategory(it) },
                                onSearchChange = { viewModel.updateScoreSearch(it) },
                                onRefreshClick = {
                                    viewModel.refreshLiveScores(isUserInitiated = true)
                                    viewModel.refreshInstagramStories()
                                },
                                onMatchClick = { match, league, sport ->
                                    viewModel.openMatchDetail(match, league, sport)
                                },
                                onToggleWatchMatch = { match, leagueTitle ->
                                    if (watchedMatches.containsKey(match.id)) {
                                        viewModel.toggleWatchMatch(match, leagueTitle)
                                    } else {
                                        runWithNotificationPermission {
                                            viewModel.toggleWatchMatch(match, leagueTitle)
                                        }
                                    }
                                },
                                onOpenStoryGroup = { groupIndex ->
                                    viewModel.openStoryGroup(groupIndex)
                                },
                                onRefreshStories = {
                                    viewModel.refreshInstagramStories()
                                },
                                onManageStoryAccounts = {
                                    showInstagramAccountsSheet = true
                                },
                                onLoginInstagram = {
                                    showInstagramLoginModal = true
                                }
                            )

                            AppTab.NEWS -> NewsScreen(
                                newsState = news,
                                onCategoryChange = { viewModel.setNewsCategory(it) },
                                onSearchChange = { viewModel.updateNewsSearch(it) },
                                onRefreshClick = { viewModel.loadNews() },
                                onArticleClick = { viewModel.openNewsArticle(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNotificationSheet) {
        NotificationSettingsSheet(
            notificationSettings = notificationSettings,
            watchedMatches = watchedMatches.values.toList(),
            onUpdateSettings = { viewModel.updateNotificationSettings(it) },
            onUnwatchMatch = { viewModel.unwatchMatch(it) },
            onSendTestNotification = {
                runWithNotificationPermission {
                    viewModel.sendTestNotification()
                }
            },
            onDismiss = { showNotificationSheet = false }
        )
    }

    if (showInstagramAccountsSheet) {
        InstagramAccountsSheet(
            accounts = scores.instagramUsernames,
            isAuthenticated = scores.instagramSession.isAuthenticated,
            connectedUsername = scores.instagramSession.username,
            onLoginClick = { showInstagramLoginModal = true },
            onLogoutClick = { viewModel.logoutInstagramAccount() },
            onAddAccount = { viewModel.addInstagramUsername(it) },
            onRemoveAccount = { viewModel.removeInstagramUsername(it) },
            onDismiss = { showInstagramAccountsSheet = false }
        )
    }

    if (showInstagramLoginModal) {
        InstagramLoginModal(
            onSessionCaptured = { session ->
                showInstagramLoginModal = false
                viewModel.onInstagramSessionCaptured(session)
            },
            onManualSessionSubmit = { raw ->
                val connected = viewModel.connectWithManualSessionId(raw)
                if (connected) {
                    showInstagramLoginModal = false
                }
                connected
            },
            onDismiss = { showInstagramLoginModal = false }
        )
    }

    broadcast.watchChooserGame?.let { game ->
        WatchSourceChooserSheet(
            game = game,
            onSelectSource = { source -> viewModel.selectProgramFromChooser(game, source) },
            onDismiss = { viewModel.dismissWatchChooser() }
        )
    }

    AnimatedVisibility(
        visible = news.selectedArticle != null,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            initialOffsetY = { it / 3 }
        ) + fadeIn(tween(240)),
        exit = slideOutVertically(
            animationSpec = tween(200),
            targetOffsetY = { it / 3 }
        ) + fadeOut(tween(180))
    ) {
        news.selectedArticle?.let { article ->
            NewsReaderModal(
                article = article,
                articleBody = news.articleBody,
                isLoadingBody = news.isLoadingArticleBody,
                onClose = { viewModel.closeNewsArticle() }
            )
        }
    }

    AnimatedVisibility(
        visible = detail.isOpen,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            initialOffsetY = { it / 3 }
        ) + fadeIn(tween(240)),
        exit = slideOutVertically(
            animationSpec = tween(200),
            targetOffsetY = { it / 3 }
        ) + fadeOut(tween(180))
    ) {
        if (detail.isOpen) {
            val detailMatch = detail.match
            val isDetailWatched = detailMatch?.id?.let { watchedMatches.containsKey(it) } ?: false
            MatchDetailModal(
                state = detail,
                isWatched = isDetailWatched,
                onToggleWatch = {
                    if (detailMatch != null) {
                        if (isDetailWatched) {
                            viewModel.toggleWatchMatch(detailMatch, detail.leagueTitle)
                        } else {
                            runWithNotificationPermission {
                                viewModel.toggleWatchMatch(detailMatch, detail.leagueTitle)
                            }
                        }
                    }
                },
                onSelectTab = { viewModel.selectDetailTab(it) },
                onToggleHighlight = { viewModel.toggleHighlightVideo(it) },
                onCloseHighlight = { viewModel.closeHighlightVideo() },
                onClose = { viewModel.closeMatchDetail() }
            )
        }
    }

    AnimatedVisibility(
        visible = scores.activeStoryGroupIndex != null,
        enter = scaleIn(initialScale = 0.94f, animationSpec = tween(220)) + fadeIn(tween(220)),
        exit = fadeOut(tween(180))
    ) {
        val activeGroupIdx = scores.activeStoryGroupIndex
        if (activeGroupIdx != null) {
            InstagramStoryViewerModal(
                groups = scores.storyGroups,
                activeGroupIndex = activeGroupIdx,
                activeSlideIndex = scores.activeStorySlideIndex,
                onNextSlide = { viewModel.nextStorySlide() },
                onPrevSlide = { viewModel.prevStorySlide() },
                onClose = { viewModel.closeStoryViewer() }
            )
        }
    }

        if (broadcast.isFullscreen && !broadcast.streamUrl.isNullOrBlank()) {
            FullscreenBroadcastPlayerOverlay(
                controller = playerController,
                programTitle = broadcast.activeProgramTitle,
                onExitFullscreen = { viewModel.setFullscreen(false) }
            )
        }
    }
}
