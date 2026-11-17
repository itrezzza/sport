package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.BroadcastFilter
import com.example.data.model.BroadcastSource
import com.example.data.model.FavoriteTeam
import com.example.data.model.MatchSummary
import com.example.data.model.MergedBroadcastGame
import com.example.data.model.NewsArticle
import com.example.data.model.TeamInfo
import com.example.data.network.SportsRepository
import com.example.ui.components.BroadcastPlayerController
import com.example.ui.components.EmptyCrestLottie
import com.example.ui.components.FeaturedMatchSlider
import com.example.ui.components.LiveBeaconLottie
import com.example.ui.components.LivePulseIndicator
import com.example.ui.components.LiveScoresPullRefreshIndicator
import com.example.ui.components.LiveStreamingPlayer
import com.example.ui.components.MatchSkeletonLoader
import com.example.ui.components.MatchSkeletonVariant
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.TeamLogo
import com.example.ui.components.bouncyClickable
import com.example.ui.theme.LiveCrimson
import com.example.ui.viewmodel.BroadcastUiState
import com.example.ui.viewmodel.ScoresUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(
    broadcastState: BroadcastUiState,
    scoresState: ScoresUiState = ScoresUiState(),
    featuredNews: List<NewsArticle> = emptyList(),
    isLoadingNews: Boolean = false,
    favoriteTeams: List<FavoriteTeam> = emptyList(),
    onFilterChange: (BroadcastFilter) -> Unit,
    onMergedGameClick: (MergedBroadcastGame) -> Unit,
    onMatchClick: (MatchSummary, String, Int) -> Unit = { _, _, _ -> },
    onTeamClick: (TeamInfo, String, Int) -> Unit = { _, _, _ -> },
    onLeagueShortcutClick: (String) -> Unit = {},
    onViewAllScoresClick: () -> Unit = {},
    onRefreshScores: () -> Unit = {},
    onNewsClick: (NewsArticle) -> Unit = {},
    onViewAllNewsClick: () -> Unit = {},
    onSelectQuality: (Int) -> Unit,
    onToggleFullscreen: (Boolean) -> Unit,
    onClosePlayer: () -> Unit,
    onPlayerError: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onRefreshPrograms: () -> Unit,
    modifier: Modifier = Modifier,
    playerController: BroadcastPlayerController? = null,
    isChatOpen: Boolean = false,
    onToggleChat: (() -> Unit)? = null,
    chatOverlayContent: (@Composable BoxScope.() -> Unit)? = null
) {
    var searchOpen by rememberSaveable { mutableStateOf(broadcastState.searchQuery.isNotBlank()) }
    val query = broadcastState.searchQuery.trim()
    val filteredGames = remember(broadcastState.mergedGames, broadcastState.filter, query) {
        broadcastState.mergedGames.filter { game ->
            val sourceOk = when (broadcastState.filter) {
                BroadcastFilter.ALL -> true
                BroadcastFilter.ANTEN -> game.sources.containsKey(BroadcastSource.ANTEN)
                BroadcastFilter.APARAT -> game.sources.containsKey(BroadcastSource.APARAT)
            }
            val queryOk = query.isBlank() || game.title.contains(query, ignoreCase = true)
            sourceOk && queryOk
        }
    }
    val liveGames = remember(filteredGames) { filteredGames.filter { it.isLive } }
    val scheduledGames = remember(filteredGames) { filteredGames.filter { !it.isLive } }
    val leagueShortcuts = remember(broadcastState.mergedGames, scoresState.leagues) {
        (broadcastState.mergedGames.map { it.leagueTitle.trim() } +
            scoresState.leagues.map { it.title.trim() })
            .filter { it.isNotBlank() }
            .distinct()
            .take(8)
    }

    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = broadcastState.isLoadingPrograms,
        onRefresh = onRefreshPrograms,
        state = pullRefreshState,
        indicator = {
            LiveScoresPullRefreshIndicator(
                state = pullRefreshState,
                isRefreshing = broadcastState.isLoadingPrograms,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("broadcast_pull_to_refresh")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 840.dp && maxHeight >= 500.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "todays_poster_banner_slider") {
                    FeaturedMatchSlider(
                        games = broadcastState.mergedGames,
                        featuredNews = featuredNews,
                        isLoading = broadcastState.isLoadingPrograms,
                        onGameClick = onMergedGameClick,
                        onNewsClick = onNewsClick
                    )
                }

                if (isWide) {
                    item(key = "wide_workspace") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1.28f),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                LiveStreamingPlayer(
                                    activeProgramId = broadcastState.activeProgramId,
                                    programTitle = broadcastState.activeProgramTitle,
                                    sourceLabel = broadcastState.activeSourceLabel,
                                    playerStatus = broadcastState.playerStatus,
                                    streamUrl = broadcastState.streamUrl,
                                    qualities = broadcastState.qualities,
                                    activeQualityIndex = broadcastState.activeQualityIndex,
                                    isFullscreen = broadcastState.isFullscreen,
                                    onSelectQuality = onSelectQuality,
                                    onToggleFullscreen = onToggleFullscreen,
                                    onClosePlayer = onClosePlayer,
                                    onPlayerError = onPlayerError,
                                    controller = playerController,
                                    isChatOpen = isChatOpen,
                                    onToggleChat = onToggleChat,
                                    chatOverlayContent = chatOverlayContent
                                )
                                BroadcastFeedHeader(
                                    broadcastState = broadcastState,
                                    itemCount = filteredGames.size,
                                    searchOpen = searchOpen,
                                    onToggleSearch = {
                                        if (searchOpen) {
                                            onSearchChange("")
                                            searchOpen = false
                                        } else {
                                            searchOpen = true
                                        }
                                    },
                                    onFilterChange = onFilterChange,
                                    onSearchChange = onSearchChange,
                                    onRefreshPrograms = onRefreshPrograms
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (filteredGames.isEmpty()) {
                                    EmptyProgramsBox("بازی قابل پخشی فعلاً پیدا نشد.")
                                } else {
                                    BroadcastProgramsBoard(
                                        games = filteredGames,
                                        activeProgramId = broadcastState.activeProgramId,
                                        onMergedGameClick = onMergedGameClick
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item(key = "player_stage") {
                        LiveStreamingPlayer(
                            activeProgramId = broadcastState.activeProgramId,
                            programTitle = broadcastState.activeProgramTitle,
                            sourceLabel = broadcastState.activeSourceLabel,
                            playerStatus = broadcastState.playerStatus,
                            streamUrl = broadcastState.streamUrl,
                            qualities = broadcastState.qualities,
                            activeQualityIndex = broadcastState.activeQualityIndex,
                            isFullscreen = broadcastState.isFullscreen,
                            onSelectQuality = onSelectQuality,
                            onToggleFullscreen = onToggleFullscreen,
                            onClosePlayer = onClosePlayer,
                            onPlayerError = onPlayerError,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            controller = playerController,
                            isChatOpen = isChatOpen,
                            onToggleChat = onToggleChat,
                            chatOverlayContent = chatOverlayContent
                        )
                    }

                    item(key = "program_header") {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            BroadcastFeedHeader(
                                broadcastState = broadcastState,
                                itemCount = filteredGames.size,
                                searchOpen = searchOpen,
                                onToggleSearch = {
                                    if (searchOpen) {
                                        onSearchChange("")
                                        searchOpen = false
                                    } else {
                                        searchOpen = true
                                    }
                                },
                                onFilterChange = onFilterChange,
                                onSearchChange = onSearchChange,
                                onRefreshPrograms = onRefreshPrograms
                            )
                        }
                    }

                    if (broadcastState.isLoadingPrograms && filteredGames.isEmpty()) {
                        item(key = "loading_programs") {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                MatchSkeletonLoader(variant = MatchSkeletonVariant.LIST_ROWS)
                            }
                        }
                    } else if (filteredGames.isEmpty()) {
                        item(key = "empty_merged") {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                EmptyProgramsBox("بازی قابل پخشی با این فیلتر پیدا نشد.")
                            }
                        }
                    } else {
                        if (liveGames.isNotEmpty()) {
                            item(key = "live_section_board") {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    BroadcastSectionHeader(
                                        title = "در حال پخش زنده",
                                        count = liveGames.size,
                                        isLive = true
                                    )
                                    StaggeredEntrance(index = 0) {
                                        BroadcastProgramsBoard(
                                            games = liveGames,
                                            activeProgramId = broadcastState.activeProgramId,
                                            onMergedGameClick = onMergedGameClick
                                        )
                                    }
                                }
                            }
                        }

                        if (scheduledGames.isNotEmpty()) {
                            item(key = "scheduled_section_board") {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    BroadcastSectionHeader(
                                        title = "کنداکتور پخش امروز",
                                        count = scheduledGames.size,
                                        isLive = false
                                    )
                                    StaggeredEntrance(index = 1) {
                                        BroadcastProgramsBoard(
                                            games = scheduledGames,
                                            activeProgramId = broadcastState.activeProgramId,
                                            onMergedGameClick = onMergedGameClick
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (favoriteTeams.isNotEmpty() || leagueShortcuts.isNotEmpty()) {
                    item(key = "home_shortcuts_rail") {
                        HomeShortcutsStrip(
                            favoriteTeams = favoriteTeams,
                            leagueTitles = leagueShortcuts,
                            onTeamClick = onTeamClick,
                            onLeagueClick = onLeagueShortcutClick
                        )
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun BroadcastSectionHeader(
    title: String,
    count: Int,
    isLive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (isLive) {
                LivePulseIndicator(color = LiveCrimson, size = 7.dp)
            } else {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (isLive) LiveCrimson else MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = "${SportsRepository.toPersianDigits(count)} برنامه",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BroadcastFeedHeader(
    broadcastState: BroadcastUiState,
    itemCount: Int,
    searchOpen: Boolean,
    onToggleSearch: () -> Unit,
    onFilterChange: (BroadcastFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onRefreshPrograms: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BroadcastFilter.entries.forEach { option ->
                    val selected = broadcastState.filter == option
                    val chipBg by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                        animationSpec = tween(200),
                        label = "broadcastFilterBg"
                    )
                    val chipText by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(200),
                        label = "broadcastFilterText"
                    )
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .bouncyClickable(pressedScale = 0.94f) {
                                onFilterChange(option)
                            }
                            .testTag("broadcast_filter_${option.name.lowercase()}"),
                        shape = RoundedCornerShape(10.dp),
                        color = chipBg,
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text(
                            text = option.labelFa,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = chipText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 14.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onRefreshPrograms)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "به‌روزرسانی پخش‌ها",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = SportsRepository.toPersianDigits(itemCount),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("broadcast_toggle_search")
                ) {
                    Icon(
                        imageVector = if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = if (searchOpen) "بستن جستجو" else "جستجو",
                        tint = if (searchOpen) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = searchOpen || broadcastState.searchQuery.isNotEmpty(),
            enter = expandVertically(tween(220)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
        ) {
            OutlinedTextField(
                value = broadcastState.searchQuery,
                onValueChange = onSearchChange,
                singleLine = true,
                placeholder = { Text("جستجوی مسابقه یا تیم…", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                trailingIcon = if (broadcastState.searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "پاک کردن", modifier = Modifier.size(16.dp))
                        }
                    }
                } else null,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("broadcast_search_input")
            )
        }
    }
}

@Composable
private fun BroadcastProgramsBoard(
    games: List<MergedBroadcastGame>,
    activeProgramId: String?,
    onMergedGameClick: (MergedBroadcastGame) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column {
            games.forEachIndexed { idx, game ->
                val isActive = activeProgramId != null &&
                    game.sources.values.any { it.id == activeProgramId }
                BroadcastProgramRow(
                    game = game,
                    isActive = isActive,
                    onClick = { onMergedGameClick(game) }
                )
                if (idx < games.lastIndex) {
                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BroadcastProgramRow(
    game: MergedBroadcastGame,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val thumbUrl = game.posterUrl.ifBlank { game.newsPhotoUrl }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .testTag("merged_game_${game.key}")
    ) {
        if (isActive || game.isLive) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(if (isActive) MaterialTheme.colorScheme.primary else LiveCrimson)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                thumbUrl.isNotBlank() -> {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbUrl)
                            .crossfade(250)
                            .build(),
                        contentDescription = game.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(0.8.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                        error = {
                            GameVisualFallbackBadge(game = game)
                        }
                    )
                }
                game.hostLogoUrl.isNotBlank() || game.guestLogoUrl.isNotBlank() -> {
                    Box(
                        modifier = Modifier.size(width = 52.dp, height = 38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TeamLogo(
                            logoUrl = game.hostLogoUrl,
                            teamName = game.title,
                            size = 28.dp,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                        TeamLogo(
                            logoUrl = game.guestLogoUrl,
                            teamName = game.title,
                            size = 28.dp,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset(x = (-2).dp)
                        )
                    }
                }
                else -> {
                    GameVisualFallbackBadge(game = game)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                if (game.leagueTitle.isNotBlank()) {
                    Text(
                        text = game.leagueTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (game.isLive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LiveCrimson
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                LiveBeaconLottie(size = 10.dp)
                                Text(
                                    text = "زنده",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    if (game.time.isNotBlank()) {
                        Text(
                            text = game.time,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    game.sources.keys.forEach { src ->
                        val isAnten = src == BroadcastSource.ANTEN
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isAnten) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Text(
                                text = src.badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isAnten) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                },
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    0.8.dp,
                    if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isActive) Icons.Rounded.Equalizer else Icons.Rounded.PlayArrow,
                        contentDescription = if (isActive) "در حال پخش" else "تماشا",
                        tint = if (isActive) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GameVisualFallbackBadge(game: MergedBroadcastGame) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (game.isLive) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(0.8.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (game.isLive) {
            LiveBeaconLottie(size = 20.dp)
        } else {
            Icon(
                imageVector = Icons.Rounded.LiveTv,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun HomeShortcutsStrip(
    favoriteTeams: List<FavoriteTeam>,
    leagueTitles: List<String>,
    onTeamClick: (TeamInfo, String, Int) -> Unit,
    onLeagueClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("home_shortcuts_strip"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (favoriteTeams.isNotEmpty()) {
            Text(
                text = "باشگاه‌های برگزیده شما",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                favoriteTeams.forEach { team ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .bouncyClickable {
                                onTeamClick(
                                    TeamInfo(name = team.name, logoUrl = team.logoUrl),
                                    team.leagueTitle,
                                    team.sport
                                )
                            }
                            .testTag("home_fav_team_${team.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            TeamLogo(
                                logoUrl = team.logoUrl,
                                teamName = team.name,
                                size = 24.dp,
                                highlightBorder = true
                            )
                            Text(
                                text = team.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        if (leagueTitles.isNotEmpty()) {
            Text(
                text = "دسترسی سریع لیگ‌ها",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                leagueTitles.forEach { leagueTitle ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .bouncyClickable { onLeagueClick(leagueTitle) }
                            .testTag("home_league_shortcut_$leagueTitle")
                    ) {
                        Text(
                            text = leagueTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProgramsBox(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EmptyCrestLottie(size = 64.dp)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
