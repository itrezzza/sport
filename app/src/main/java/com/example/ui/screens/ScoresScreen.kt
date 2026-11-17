package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.LeagueGroup
import com.example.data.model.MatchEntry
import com.example.data.model.MatchSummary
import com.example.data.model.ScoreFilter
import com.example.data.model.SportCategory
import com.example.data.network.SportsRepository
import com.example.ui.components.EmptyCrestLottie
import com.example.ui.components.InstagramStoriesBar
import com.example.ui.components.LeagueHeader
import com.example.ui.components.LivePulseIndicator
import com.example.ui.components.LiveScoresPullRefreshIndicator
import com.example.ui.components.MatchSkeletonLoader
import com.example.ui.components.MatchSkeletonVariant
import com.example.ui.components.PremiumLiveMatchCard
import com.example.ui.components.RollingScoreText
import com.example.ui.components.SportCategoryGlyph
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.TeamLogo
import com.example.ui.components.bouncyClickable
import com.example.ui.components.rememberReducedMotionEnabled
import com.example.ui.theme.LiveCrimson
import com.example.ui.viewmodel.ScoresUiState
import kotlinx.coroutines.launch

private enum class ScoreScheduleFilter(val labelFa: String) {
    ALL("همه وضعیت‌ها"),
    UPCOMING("پیش‌رو"),
    FINISHED("پایان‌یافته")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoresScreen(
    scoresState: ScoresUiState,
    watchedMatchIds: Set<String>,
    onFilterChange: (ScoreFilter) -> Unit,
    onSportChange: (SportCategory) -> Unit,
    onSearchChange: (String) -> Unit,
    onRefreshClick: () -> Unit,
    onMatchClick: (MatchSummary, String, Int) -> Unit,
    onToggleWatchMatch: (MatchSummary, String) -> Unit,
    onOpenStoryGroup: (Int) -> Unit = {},
    onRefreshStories: () -> Unit = {},
    onManageStoryAccounts: () -> Unit = {},
    onLoginInstagram: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchOpen by rememberSaveable { mutableStateOf(scoresState.searchQuery.isNotBlank()) }
    var scheduleFilter by rememberSaveable { mutableStateOf(ScoreScheduleFilter.ALL) }
    val reducedMotion = rememberReducedMotionEnabled()
    val query = scoresState.searchQuery.trim()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val liveTickerMatches = remember(scoresState.leagues, scoresState.sportCategory) {
        scoresState.leagues
            .filter { scoresState.sportCategory.sportId == null || it.sport == scoresState.sportCategory.sportId }
            .flatMap { league ->
                league.matches
                    .filter { it.isLive }
                    .map { m -> MatchEntry(match = m, leagueTitle = league.title, sport = league.sport) }
            }
            .take(10)
    }

    val filteredLeagues = remember(
        scoresState.leagues,
        scoresState.sportCategory,
        scoresState.filter,
        scheduleFilter,
        query
    ) {
        scoresState.leagues.mapNotNull { league ->
            val sportMatches = scoresState.sportCategory.sportId == null ||
                league.sport == scoresState.sportCategory.sportId
            if (!sportMatches) return@mapNotNull null

            val visibleMatches = league.matches.filter { m ->
                val liveOk = scoresState.filter == ScoreFilter.ALL || m.isLive
                val scheduleOk = if (scoresState.filter == ScoreFilter.LIVE) {
                    true
                } else {
                    when (scheduleFilter) {
                        ScoreScheduleFilter.ALL -> true
                        ScoreScheduleFilter.UPCOMING -> !m.isLive && m.goals == null
                        ScoreScheduleFilter.FINISHED -> !m.isLive && m.goals != null
                    }
                }
                val queryOk = query.isBlank() ||
                    league.title.contains(query, ignoreCase = true) ||
                    m.host.name.contains(query, ignoreCase = true) ||
                    m.guest.name.contains(query, ignoreCase = true)
                liveOk && scheduleOk && queryOk
            }
            if (visibleMatches.isEmpty()) null else league.copy(matches = visibleMatches)
        }
    }

    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = scoresState.isManualScoreSyncing,
        onRefresh = onRefreshClick,
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
            .testTag("scores_pull_to_refresh")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Sleek flush sports category underline strip (no boxy pills)
            item(key = "scores_sport_tabs") {
                ScoresSportCategoryBar(
                    selectedCategory = scoresState.sportCategory,
                    onSelectCategory = onSportChange
                )
            }

            // 2. Compact live/all + schedule + telemetry + search control strip
            item(key = "scores_toolbar") {
                ScoresControlStrip(
                    scoresState = scoresState,
                    scheduleFilter = scheduleFilter,
                    searchOpen = searchOpen,
                    hasStories = scoresState.storyGroups.isNotEmpty() || scoresState.needsInstagramLogin,
                    onFilterChange = onFilterChange,
                    onScheduleFilterChange = { scheduleFilter = it },
                    onToggleSearch = {
                        if (searchOpen) {
                            onSearchChange("")
                            searchOpen = false
                        } else {
                            searchOpen = true
                        }
                    },
                    onSearchChange = onSearchChange,
                    onRefreshClick = onRefreshClick,
                    onJumpToStories = {
                        scope.launch {
                            val items = listState.layoutInfo.totalItemsCount
                            if (items > 1) {
                                listState.animateScrollToItem((items - 2).coerceAtLeast(0))
                            }
                        }
                    }
                )
            }

            // 3. Featured live matches horizontal broadcast spotlight
            if (liveTickerMatches.isNotEmpty() && query.isBlank()) {
                item(key = "live_ticker_strip") {
                    FeaturedLiveMatchesSection(
                        liveMatches = liveTickerMatches,
                        watchedMatchIds = watchedMatchIds,
                        onMatchClick = onMatchClick,
                        onToggleWatchMatch = onToggleWatchMatch
                    )
                }
            }

            // 4. High-density edge-to-edge expandable league scoreboards
            if (scoresState.isLoading && scoresState.leagues.isEmpty()) {
                item(key = "scores_loading") {
                    MatchSkeletonLoader(variant = MatchSkeletonVariant.LEAGUE_BOARD)
                }
            } else if (filteredLeagues.isEmpty()) {
                item(key = "scores_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            EmptyCrestLottie(size = 64.dp)
                            Text(
                                text = if (scoresState.filter == ScoreFilter.LIVE) {
                                    "در این لحظه بازی زنده‌ای فعال نیست. «همهٔ بازی‌ها» را بررسی کن."
                                } else {
                                    "مسابقه‌ای با این فیلتر پیدا نشد."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    count = filteredLeagues.size,
                    key = { "${filteredLeagues[it].sport}_${filteredLeagues[it].title}" }
                ) { idx ->
                    val league = filteredLeagues[idx]
                    StaggeredEntrance(index = idx) {
                        EdgeToEdgeLeagueScoreboard(
                            league = league,
                            watchedMatchIds = watchedMatchIds,
                            onMatchClick = { match ->
                                onMatchClick(match, league.title, league.sport)
                            },
                            onToggleWatchMatch = { match ->
                                onToggleWatchMatch(match, league.title)
                            }
                        )
                    }
                }
            }

            // 5. Instagram stories bar preserved cleanly beneath the scoreboard feed
            item(key = "instagram_stories_top_tray") {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    HorizontalDivider(
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    InstagramStoriesBar(
                        storyGroups = scoresState.storyGroups,
                        seenStoryItemIds = scoresState.seenStoryItemIds,
                        needsLogin = scoresState.needsInstagramLogin,
                        connectedUsername = scoresState.instagramSession.username,
                        isLoading = scoresState.isLoadingStories,
                        errorMessage = scoresState.storiesError,
                        onOpenGroup = onOpenStoryGroup,
                        onRefresh = onRefreshStories,
                        onManageAccounts = onManageStoryAccounts,
                        onLoginClick = onLoginInstagram
                    )
                }
            }

            item(key = "scores_bottom_pad") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ScoresSportCategoryBar(
    selectedCategory: SportCategory,
    onSelectCategory: (SportCategory) -> Unit
) {
    val reducedMotion = rememberReducedMotionEnabled()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SportCategory.entries.forEach { category ->
                val selected = selectedCategory == category
                val tintColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(if (reducedMotion) 0 else 180),
                    label = "sportTabColor"
                )
                val barWidth by animateDpAsState(
                    targetValue = if (selected) 26.dp else 0.dp,
                    animationSpec = if (reducedMotion) tween(0) else spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "sportUnderline"
                )

                Box(
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .bouncyClickable(pressedScale = 0.95f) {
                            onSelectCategory(category)
                        }
                        .padding(horizontal = 10.dp)
                        .testTag("sport_chip_${category.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SportCategoryGlyph(
                            sportId = category.sportId,
                            tint = tintColor,
                            size = 13.dp
                        )
                        Text(
                            text = category.labelFa,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = tintColor,
                            maxLines = 1
                        )
                    }

                    if (barWidth > 0.dp) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .width(barWidth)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            thickness = 0.6.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun ScoresControlStrip(
    scoresState: ScoresUiState,
    scheduleFilter: ScoreScheduleFilter,
    searchOpen: Boolean,
    hasStories: Boolean,
    onFilterChange: (ScoreFilter) -> Unit,
    onScheduleFilterChange: (ScoreScheduleFilter) -> Unit,
    onToggleSearch: () -> Unit,
    onSearchChange: (String) -> Unit,
    onRefreshClick: () -> Unit,
    onJumpToStories: () -> Unit
) {
    val reducedMotion = rememberReducedMotionEnabled()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Segmented Live vs All switch
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreFilter.entries.forEach { filterOption ->
                    val selected = scoresState.filter == filterOption
                    val pillBg by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        animationSpec = tween(if (reducedMotion) 0 else 180),
                        label = "scoreFilterPill"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(if (reducedMotion) 0 else 180),
                        label = "scoreFilterTxt"
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(pillBg)
                            .bouncyClickable(pressedScale = 0.95f) {
                                onFilterChange(filterOption)
                            }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                            .testTag("score_filter_${filterOption.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (filterOption == ScoreFilter.LIVE) {
                            LivePulseIndicator(
                                color = if (selected) LiveCrimson else LiveCrimson.copy(alpha = 0.85f),
                                size = 5.dp
                            )
                        }
                        Text(
                            text = filterOption.labelFa,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (hasStories) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onJumpToStories)
                            .testTag("scores_stories_shortcut")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "استوری‌ها",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "استوری",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (scoresState.lastUpdatedTime.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f))
                            .clickable(onClick = onRefreshClick)
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("scores_live_polling_badge"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (scoresState.liveCount > 0) {
                            LivePulseIndicator(size = 5.dp)
                        }
                        Text(
                            text = "${SportsRepository.toPersianDigits(scoresState.pollIntervalSeconds)}″",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = scoresState.lastUpdatedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("scores_toggle_search")
                ) {
                    Icon(
                        imageVector = if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = if (searchOpen) "بستن جستجو" else "جستجو",
                        tint = if (searchOpen) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        // Sleek text-tab strip for All/Upcoming/Finished subfilters (no heavy boxy chips)
        if (scoresState.filter == ScoreFilter.ALL) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreScheduleFilter.entries.forEach { option ->
                    val selected = scheduleFilter == option
                    val color by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(if (reducedMotion) 0 else 160),
                        label = "scheduleSubColor"
                    )
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onScheduleFilterChange(option) }
                            .padding(vertical = 3.dp, horizontal = 2.dp)
                            .testTag("score_schedule_${option.name.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = option.labelFa,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = color,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .width(if (selected) 24.dp else 0.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = searchOpen || scoresState.searchQuery.isNotEmpty(),
            enter = expandVertically(tween(if (reducedMotion) 0 else 200)) + fadeIn(tween(if (reducedMotion) 0 else 180)),
            exit = shrinkVertically(tween(if (reducedMotion) 0 else 160)) + fadeOut(tween(if (reducedMotion) 0 else 130))
        ) {
            OutlinedTextField(
                value = scoresState.searchQuery,
                onValueChange = onSearchChange,
                singleLine = true,
                placeholder = { Text("جستجوی تیم یا لیگ…", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                trailingIcon = if (scoresState.searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "پاک کردن", modifier = Modifier.size(16.dp))
                        }
                    }
                } else null,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scores_search_input")
            )
        }
    }
}

@Composable
private fun FeaturedLiveMatchesSection(
    liveMatches: List<MatchEntry>,
    watchedMatchIds: Set<String>,
    onMatchClick: (MatchSummary, String, Int) -> Unit,
    onToggleWatchMatch: (MatchSummary, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LivePulseIndicator(size = 6.5.dp)
                Text(
                    text = "پخش ویژهٔ زنده",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "${SportsRepository.toPersianDigits(liveMatches.size)} مسابقه",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(liveMatches, key = { "ticker_${it.match.id}" }) { entry ->
                PremiumLiveMatchCard(
                    entry = entry,
                    isWatched = entry.match.id in watchedMatchIds,
                    onToggleWatch = if (entry.match.id.isNotBlank()) {
                        { onToggleWatchMatch(entry.match, entry.leagueTitle) }
                    } else null,
                    onClick = { onMatchClick(entry.match, entry.leagueTitle, entry.sport) },
                    modifier = Modifier.width(248.dp)
                )
            }
        }
    }
}

@Composable
private fun EdgeToEdgeLeagueScoreboard(
    league: LeagueGroup,
    watchedMatchIds: Set<String>,
    onMatchClick: (MatchSummary) -> Unit,
    onToggleWatchMatch: (MatchSummary) -> Unit
) {
    var expanded by rememberSaveable(league.title) { mutableStateOf(true) }
    val reducedMotion = rememberReducedMotionEnabled()
    val hasLiveInLeague = league.matches.any { it.isLive }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalDivider(
            thickness = 0.6.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        )
        LeagueHeader(
            title = league.title,
            matchCount = league.matches.size,
            hasLive = hasLiveInLeague,
            expanded = expanded,
            onToggleExpand = { expanded = !expanded }
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(if (reducedMotion) 0 else 200)) + fadeIn(tween(if (reducedMotion) 0 else 180)),
            exit = shrinkVertically(tween(if (reducedMotion) 0 else 160)) + fadeOut(tween(if (reducedMotion) 0 else 130))
        ) {
            Column {
                league.matches.forEach { match ->
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
                        modifier = Modifier.padding(start = 14.dp)
                    )
                    CompactTelemetryMatchRow(
                        match = match,
                        isWatched = match.id in watchedMatchIds,
                        onClick = { onMatchClick(match) },
                        onToggleWatch = { onToggleWatchMatch(match) }
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 0.6.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun CompactTelemetryMatchRow(
    match: MatchSummary,
    isWatched: Boolean,
    onClick: () -> Unit,
    onToggleWatch: () -> Unit
) {
    val hostGoals = match.goals?.host
    val guestGoals = match.goals?.guest
    val hostWins = !match.isLive && hostGoals != null && guestGoals != null && hostGoals > guestGoals
    val guestWins = !match.isLive && hostGoals != null && guestGoals != null && guestGoals > hostGoals

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(onClick = onClick)
            .testTag("score_card_${match.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Edge indicator rail: Live Crimson when live, Electric Lime when watched
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                .background(
                    when {
                        match.isLive -> LiveCrimson
                        isWatched -> MaterialTheme.colorScheme.primary
                        else -> Color.Transparent
                    }
                )
        )

        // Status / Minute telemetry column
        Column(
            modifier = Modifier
                .width(50.dp)
                .padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (match.isLive) {
                LivePulseIndicator(size = 5.dp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = match.liveTime.ifBlank { "زنده" },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = LiveCrimson,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            } else if (match.goals != null) {
                Text(
                    text = "پایان",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = match.time.ifBlank { "–" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Dual stacked team rows: plenty of width even on 360dp screens
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TeamLogo(
                    logoUrl = match.host.logoUrl,
                    teamName = match.host.name,
                    size = 20.dp,
                    highlightBorder = hostWins
                )
                Text(
                    text = match.host.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    fontWeight = when {
                        hostWins || match.isLive -> FontWeight.Bold
                        guestWins -> FontWeight.Normal
                        else -> FontWeight.SemiBold
                    },
                    color = if (guestWins) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TeamLogo(
                    logoUrl = match.guest.logoUrl,
                    teamName = match.guest.name,
                    size = 20.dp,
                    highlightBorder = guestWins
                )
                Text(
                    text = match.guest.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    fontWeight = when {
                        guestWins || match.isLive -> FontWeight.Bold
                        hostWins -> FontWeight.Normal
                        else -> FontWeight.SemiBold
                    },
                    color = if (hostWins) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Stacked vertical large tabular scores
        Column(
            modifier = Modifier.width(34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val hostScoreDisplay = hostGoals?.let { SportsRepository.toPersianDigits(it) } ?: "–"
            val guestScoreDisplay = guestGoals?.let { SportsRepository.toPersianDigits(it) } ?: "–"

            RollingScoreText(
                text = hostScoreDisplay,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    fontFeatureSettings = "tnum"
                ),
                fontWeight = if (hostWins || match.isLive) FontWeight.Black else FontWeight.Bold,
                color = when {
                    match.isLive -> MaterialTheme.colorScheme.primary
                    hostGoals == null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    guestWins -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            RollingScoreText(
                text = guestScoreDisplay,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    fontFeatureSettings = "tnum"
                ),
                fontWeight = if (guestWins || match.isLive) FontWeight.Black else FontWeight.Bold,
                color = when {
                    match.isLive -> MaterialTheme.colorScheme.primary
                    guestGoals == null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    hostWins -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }

        // Accessible 48dp touch-target notification bell
        if (match.id.isNotBlank()) {
            IconButton(
                onClick = onToggleWatch,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("watch_match_${match.id}")
            ) {
                Icon(
                    imageVector = if (isWatched) Icons.Rounded.NotificationsActive
                    else Icons.Outlined.NotificationsNone,
                    contentDescription = stringResource(R.string.cd_match_notifications),
                    tint = if (isWatched) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(17.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(12.dp))
        }
    }
}
