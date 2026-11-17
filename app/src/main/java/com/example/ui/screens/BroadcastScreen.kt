package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.data.model.MatchSummary
import com.example.data.model.MergedBroadcastGame
import com.example.data.model.NewsArticle
import com.example.data.network.SportsRepository
import com.example.ui.components.BroadcastPlayerController
import com.example.ui.components.BroadcastPlayerStage
import com.example.ui.components.FeaturedMatchSlider
import com.example.ui.components.HomeSportsNewsSection
import com.example.ui.components.LivePulseDot
import com.example.ui.components.LiveScoreTicker
import com.example.ui.components.LiveScoresPullRefreshIndicator
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.TeamBadge
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
    onFilterChange: (BroadcastFilter) -> Unit,
    onMergedGameClick: (MergedBroadcastGame) -> Unit,
    onMatchClick: (MatchSummary, String, Int) -> Unit = { _, _, _ -> },
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
    playerController: BroadcastPlayerController? = null
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

    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = scoresState.isManualScoreSyncing,
        onRefresh = onRefreshScores,
        state = pullRefreshState,
        indicator = {
            LiveScoresPullRefreshIndicator(
                state = pullRefreshState,
                isRefreshing = scoresState.isManualScoreSyncing,
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
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "live_score_ticker") {
                    LiveScoreTicker(
                        leagues = scoresState.leagues,
                        isLoading = scoresState.isLoading,
                        lastUpdatedTime = scoresState.lastUpdatedTime,
                        onMatchClick = onMatchClick,
                        onOpenScoresTab = onViewAllScoresClick,
                        onRefreshScores = onRefreshScores
                    )
                }

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
                            modifier = Modifier.weight(1.3f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            BroadcastPlayerStage(
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
                                controller = playerController
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (filteredGames.isEmpty()) {
                                EmptyProgramsBox("بازی قابل پخشی فعلاً پیدا نشد.")
                            } else {
                                filteredGames.forEach { game ->
                                    BroadcastGameCard(
                                        game = game,
                                        isActive = broadcastState.activeProgramId != null &&
                                            game.sources.values.any { it.id == broadcastState.activeProgramId },
                                        onClick = { onMergedGameClick(game) }
                                    )
                                }
                            }
                        }
                    }
                }
                item(key = "wide_home_sports_news") {
                    HomeSportsNewsSection(
                        articles = featuredNews,
                        isLoading = isLoadingNews,
                        isWide = true,
                        onArticleClick = onNewsClick,
                        onViewAllClick = onViewAllNewsClick,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            } else {
                item(key = "player_stage") {
                    BroadcastPlayerStage(
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
                        controller = playerController
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
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
                        item(key = "live_section_label") {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LivePulseDot(color = LiveCrimson, size = 6.dp)
                                Text(
                                    text = "در حال پخش زنده",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = LiveCrimson
                                )
                            }
                        }
                        items(liveGames.size, key = { "live_${liveGames[it].key}" }) { idx ->
                            val game = liveGames[idx]
                            val isActive = broadcastState.activeProgramId != null &&
                                game.sources.values.any { it.id == broadcastState.activeProgramId }
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                StaggeredEntrance(index = idx) {
                                    BroadcastGameCard(
                                        game = game,
                                        isActive = isActive,
                                        onClick = { onMergedGameClick(game) }
                                    )
                                }
                            }
                        }
                    }

                    if (scheduledGames.isNotEmpty()) {
                        if (liveGames.isNotEmpty()) {
                            item(key = "scheduled_section_label") {
                                Text(
                                    text = "برنامهٔ مسابقات امروز",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                                )
                            }
                        }
                        items(scheduledGames.size, key = { "sched_${scheduledGames[it].key}" }) { idx ->
                            val game = scheduledGames[idx]
                            val isActive = broadcastState.activeProgramId != null &&
                                game.sources.values.any { it.id == broadcastState.activeProgramId }
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                StaggeredEntrance(index = idx + liveGames.size) {
                                    BroadcastGameCard(
                                        game = game,
                                        isActive = isActive,
                                        onClick = { onMergedGameClick(game) }
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "compact_home_sports_news") {
                    HomeSportsNewsSection(
                        articles = featuredNews,
                        isLoading = isLoadingNews,
                        isWide = false,
                        onArticleClick = onNewsClick,
                        onViewAllClick = onViewAllNewsClick,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
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
                            .clip(RoundedCornerShape(99.dp))
                            .bouncyClickable(pressedScale = 0.94f) {
                                onFilterChange(option)
                            }
                            .testTag("broadcast_filter_${option.name.lowercase()}"),
                        shape = RoundedCornerShape(99.dp),
                        color = chipBg
                    ) {
                        Text(
                            text = option.labelFa,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
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
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .clickable(onClick = onRefreshPrograms)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("broadcast_search_input")
            )
        }
    }
}

@Composable
private fun BroadcastGameCard(
    game: MergedBroadcastGame,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val thumbUrl = game.posterUrl.ifBlank { game.newsPhotoUrl }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .bouncyClickable(onClick = onClick)
            .testTag("merged_game_${game.key}"),
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (isActive) 3.dp else 1.dp,
        border = if (isActive) {
            BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
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
                            .size(54.dp)
                            .clip(RoundedCornerShape(15.dp)),
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
                        TeamBadge(
                            logoUrl = game.hostLogoUrl,
                            teamName = game.title,
                            size = 28.dp,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                        TeamBadge(
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
                            Text(
                                text = "زنده",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
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
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
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
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isActive) Icons.Rounded.Equalizer else Icons.Rounded.PlayArrow,
                        contentDescription = if (isActive) "در حال پخش" else "تماشا",
                        tint = if (isActive) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
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
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (game.isLive) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {
        if (game.isLive) {
            LivePulseDot(color = LiveCrimson, size = 8.dp)
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
private fun EmptyProgramsBox(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(28.dp)
        )
    }
}
