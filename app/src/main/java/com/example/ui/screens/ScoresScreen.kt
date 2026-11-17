package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LeagueGroup
import com.example.data.model.MatchEntry
import com.example.data.model.MatchSummary
import com.example.data.model.ScoreFilter
import com.example.data.model.SportCategory
import com.example.data.network.SportsRepository
import com.example.ui.components.InstagramStoriesBar
import com.example.ui.components.LivePulseDot
import com.example.ui.components.LiveScoresPullRefreshIndicator
import com.example.ui.components.RollingScoreText
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.TeamBadge
import com.example.ui.components.bouncyClickable
import com.example.ui.theme.LiveCrimson
import com.example.ui.viewmodel.ScoresUiState

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
    val query = scoresState.searchQuery.trim()

    val liveTickerMatches = remember(scoresState.leagues) {
        scoresState.leagues.flatMap { league ->
            league.matches
                .filter { it.isLive }
                .map { m -> MatchEntry(match = m, leagueTitle = league.title, sport = league.sport) }
        }.take(10)
    }

    val filteredLeagues = remember(scoresState.leagues, scoresState.sportCategory, scoresState.filter, query) {
        scoresState.leagues.mapNotNull { league ->
            val sportMatches = scoresState.sportCategory.sportId == null ||
                league.sport == scoresState.sportCategory.sportId
            if (!sportMatches) return@mapNotNull null

            val visibleMatches = league.matches.filter { m ->
                val liveOk = scoresState.filter == ScoreFilter.ALL || m.isLive
                val queryOk = query.isBlank() ||
                    league.title.contains(query, ignoreCase = true) ||
                    m.host.name.contains(query, ignoreCase = true) ||
                    m.guest.name.contains(query, ignoreCase = true)
                liveOk && queryOk
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
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item(key = "instagram_stories_top_tray") {
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

        if (liveTickerMatches.isNotEmpty() && query.isBlank()) {
            item(key = "live_ticker_strip") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LivePulseDot(color = LiveCrimson, size = 6.dp)
                        Text(
                            text = "نتایج زنده اکنون",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(liveTickerMatches, key = { "ticker_${it.match.id}" }) { entry ->
                            LiveTickerCard(
                                entry = entry,
                                onClick = { onMatchClick(entry.match, entry.leagueTitle, entry.sport) }
                            )
                        }
                    }
                }
            }
        }

        item(key = "scores_toolbar") {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            ScoreFilter.entries.forEach { filterOption ->
                                val selected = scoresState.filter == filterOption
                                val tabBg by animateColorAsState(
                                    targetValue = if (selected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    animationSpec = tween(200),
                                    label = "scoreFilterBg"
                                )
                                val tabTxt by animateColorAsState(
                                    targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    animationSpec = tween(200),
                                    label = "scoreFilterTxt"
                                )
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(99.dp))
                                        .bouncyClickable(pressedScale = 0.94f) {
                                            onFilterChange(filterOption)
                                        }
                                        .testTag("score_filter_${filterOption.name.lowercase()}"),
                                    shape = RoundedCornerShape(99.dp),
                                    color = tabBg
                                ) {
                                    Text(
                                        text = filterOption.labelFa,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = tabTxt,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (scoresState.lastUpdatedTime.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .clickable(onClick = onRefreshClick)
                                    .testTag("scores_live_polling_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    LivePulseDot(
                                        color = if (scoresState.liveCount > 0) LiveCrimson else MaterialTheme.colorScheme.primary,
                                        size = 5.dp
                                    )
                                    Text(
                                        text = "هر ${SportsRepository.toPersianDigits(scoresState.pollIntervalSeconds)} ثانیه · ${scoresState.lastUpdatedTime}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = {
                                if (searchOpen) {
                                    onSearchChange("")
                                    searchOpen = false
                                } else {
                                    searchOpen = true
                                }
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("scores_toggle_search")
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SportCategory.entries.forEach { category ->
                        val selected = scoresState.sportCategory == category
                        val bg by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            animationSpec = tween(180),
                            label = "sportChipBg"
                        )
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .bouncyClickable(pressedScale = 0.94f) {
                                    onSportChange(category)
                                }
                                .testTag("sport_chip_${category.name.lowercase()}"),
                            shape = RoundedCornerShape(99.dp),
                            color = bg
                        ) {
                            Text(
                                text = category.labelFa,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = searchOpen || scoresState.searchQuery.isNotEmpty(),
                    enter = expandVertically(tween(220)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
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
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scores_search_input")
                    )
                }
            }
        }

        if (scoresState.isLoading && scoresState.leagues.isEmpty()) {
            item(key = "scores_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (filteredLeagues.isEmpty()) {
            item(key = "scores_empty") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsSoccer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = if (scoresState.filter == ScoreFilter.LIVE) {
                                "الان بازی زنده‌ای ثبت نشده. «همهٔ بازی‌ها» را ببین."
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
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    StaggeredEntrance(index = idx) {
                        LeagueMatchBoardCard(
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
        }

            item(key = "scores_bottom_pad") {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun LiveTickerCard(
    entry: MatchEntry,
    onClick: () -> Unit
) {
    val match = entry.match
    Surface(
        modifier = Modifier
            .width(198.dp)
            .clip(RoundedCornerShape(18.dp))
            .bouncyClickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(0.8.dp, LiveCrimson.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            LiveCrimson.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.leagueTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = LiveCrimson
                ) {
                    Text(
                        text = match.liveTime.ifBlank { "زنده" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TeamBadge(logoUrl = match.host.logoUrl, teamName = match.host.name, size = 20.dp)
                    Text(
                        text = match.host.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                RollingScoreText(
                    text = match.goals?.host?.let { SportsRepository.toPersianDigits(it) } ?: "۰",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TeamBadge(logoUrl = match.guest.logoUrl, teamName = match.guest.name, size = 20.dp)
                    Text(
                        text = match.guest.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                RollingScoreText(
                    text = match.goals?.guest?.let { SportsRepository.toPersianDigits(it) } ?: "۰",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun LeagueMatchBoardCard(
    league: LeagueGroup,
    watchedMatchIds: Set<String>,
    onMatchClick: (MatchSummary) -> Unit,
    onToggleWatchMatch: (MatchSummary) -> Unit
) {
    var expanded by rememberSaveable(league.title) { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current
    val chevronAngle by animateFloatAsState(
        targetValue = if (expanded) 0f else 180f,
        animationSpec = tween(220),
        label = "leagueChevron"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        expanded = !expanded
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val hasLiveInLeague = league.matches.any { it.isLive }
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(
                                if (hasLiveInLeague) LiveCrimson
                                else MaterialTheme.colorScheme.primary
                            )
                    )
                    Text(
                        text = league.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = SportsRepository.toPersianDigits(league.matches.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "بستن لیگ" else "باز کردن لیگ",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = chevronAngle }
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(220)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
            ) {
                Column {
                    league.matches.forEach { match ->
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        ScoreMatchRow(
                            match = match,
                            isWatched = match.id in watchedMatchIds,
                            onClick = { onMatchClick(match) },
                            onToggleWatch = { onToggleWatchMatch(match) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreMatchRow(
    match: MatchSummary,
    isWatched: Boolean,
    onClick: () -> Unit,
    onToggleWatch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
            .testTag("score_card_${match.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.width(46.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (match.isLive) {
                LivePulseDot(color = LiveCrimson, size = 5.dp)
            }
            Text(
                text = when {
                    match.isLive -> match.liveTime.ifBlank { "زنده" }
                    match.goals != null -> "پایان"
                    else -> match.time.ifBlank { "–" }
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (match.isLive) FontWeight.Bold else FontWeight.Normal,
                color = if (match.isLive) LiveCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = match.host.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(6.dp))
            TeamBadge(logoUrl = match.host.logoUrl, teamName = match.host.name, size = 24.dp)
        }

        Surface(
            shape = RoundedCornerShape(9.dp),
            color = if (match.isLive) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        ) {
            RollingScoreText(
                text = "${match.goals?.host?.let { SportsRepository.toPersianDigits(it) } ?: "–"} : ${match.goals?.guest?.let { SportsRepository.toPersianDigits(it) } ?: "–"}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (match.isLive) MaterialTheme.colorScheme.onTertiaryContainer
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            TeamBadge(logoUrl = match.guest.logoUrl, teamName = match.guest.name, size = 24.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = match.guest.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

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
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
