package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.LineupPlayer
import com.example.data.model.LineupSide
import com.example.data.model.MatchEvent
import com.example.data.model.MatchSummary
import com.example.data.model.TeamInfo
import com.example.data.network.SportsRepository
import com.example.ui.components.AnimatedScoreboard
import com.example.ui.components.EmptyCrestLottie
import com.example.ui.components.FavoriteTeamButton
import com.example.ui.components.FootballPitchVisualization
import com.example.ui.components.GoalEventAnimation
import com.example.ui.components.InlineHighlightPlayer
import com.example.ui.components.LivePulseIndicator
import com.example.ui.components.MatchStatisticsPanel
import com.example.ui.components.ScoreboardSize
import com.example.ui.components.TeamLogo
import com.example.ui.components.bouncyClickable
import com.example.ui.components.rememberReducedMotionEnabled
import com.example.ui.theme.AmberCard
import com.example.ui.theme.BroadcastCyan
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GoldMotm
import com.example.ui.theme.LiveCrimson
import com.example.ui.viewmodel.DetailTab
import com.example.ui.viewmodel.MatchDetailUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchDetailModal(
    state: MatchDetailUiState,
    isWatched: Boolean,
    onToggleWatch: () -> Unit,
    onSelectTab: (DetailTab) -> Unit,
    onToggleHighlight: (MatchEvent) -> Unit,
    onCloseHighlight: () -> Unit,
    onClose: () -> Unit,
    onTeamClick: (TeamInfo, String, Int) -> Unit = { _, _, _ -> },
    onPlayerClick: (LineupPlayer, String, String) -> Unit = { _, _, _ -> },
    onPlayerNameClick: (String, String, String) -> Unit = { _, _, _ -> },
    favoriteTeamNames: Set<String> = emptySet(),
    onToggleFavoriteTeam: ((TeamInfo, String, Int) -> Unit)? = null
) {
    val match = state.match ?: return

    BackHandler { onClose() }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } },
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .testTag("match_detail_list"),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            // Floodlight Stadium Scoreboard Header
            item(key = "detail_hero") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    0.5f to MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    1f to MaterialTheme.colorScheme.background
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("close_detail_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.cd_close),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = state.leagueTitle.ifBlank { "مرکز مسابقه" },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (isWatched) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isWatched) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                IconButton(
                                    onClick = onToggleWatch,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("detail_watch_button")
                                ) {
                                    Icon(
                                        imageVector = if (isWatched) Icons.Rounded.NotificationsActive
                                        else Icons.Outlined.NotificationsNone,
                                        contentDescription = stringResource(R.string.cd_match_notifications),
                                        tint = if (isWatched) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Host club block (clickable -> Team Profile)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                onTeamClick(match.host, state.leagueTitle, state.sport)
                                            }
                                            .padding(vertical = 4.dp, horizontal = 4.dp)
                                            .testTag("detail_host_team"),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        TeamLogo(
                                            logoUrl = match.host.logoUrl,
                                            teamName = match.host.name,
                                            size = 58.dp,
                                            highlightBorder = true
                                        )
                                        Text(
                                            text = match.host.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2
                                        )
                                        Text(
                                            text = "پروفایل تیم ›",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (onToggleFavoriteTeam != null) {
                                            FavoriteTeamButton(
                                                isFavorite = match.host.name.lowercase() in favoriteTeamNames,
                                                teamName = match.host.name,
                                                onToggle = {
                                                    onToggleFavoriteTeam(match.host, state.leagueTitle, state.sport)
                                                },
                                                modifier = Modifier.testTag("detail_host_favorite_button")
                                            )
                                        }
                                    }

                                    // Central Broadcast Scoreboard
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AnimatedScoreboard(
                                            goals = match.goals,
                                            isLive = match.isLive,
                                            size = ScoreboardSize.HERO
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (match.isLive) MaterialTheme.colorScheme.tertiaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                if (match.isLive) {
                                                    LivePulseIndicator(size = 6.dp)
                                                }
                                                Text(
                                                    text = match.stateLabel(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (match.isLive) MaterialTheme.colorScheme.onTertiaryContainer
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    // Guest club block (clickable -> Team Profile)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                onTeamClick(match.guest, state.leagueTitle, state.sport)
                                            }
                                            .padding(vertical = 4.dp, horizontal = 4.dp)
                                            .testTag("detail_guest_team"),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        TeamLogo(
                                            logoUrl = match.guest.logoUrl,
                                            teamName = match.guest.name,
                                            size = 58.dp,
                                            highlightBorder = true
                                        )
                                        Text(
                                            text = match.guest.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2
                                        )
                                        Text(
                                            text = "پروفایل تیم ›",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        if (onToggleFavoriteTeam != null) {
                                            FavoriteTeamButton(
                                                isFavorite = match.guest.name.lowercase() in favoriteTeamNames,
                                                teamName = match.guest.name,
                                                onToggle = {
                                                    onToggleFavoriteTeam(match.guest, state.leagueTitle, state.sport)
                                                },
                                                modifier = Modifier.testTag("detail_guest_favorite_button")
                                            )
                                        }
                                    }
                                }

                                val metaChips = buildList {
                                    if (match.date.isNotBlank()) add(match.date)
                                    if (match.time.isNotBlank()) add("شروع ${match.time}")
                                    state.detailInfo?.let { info ->
                                        if (info.stadium.isNotBlank()) add("ورزشگاه: ${info.stadium}")
                                        if (info.referee.isNotBlank()) add("داور: ${info.referee}")
                                        if (info.week.isNotBlank()) add("هفتهٔ ${SportsRepository.toPersianDigits(info.week)}")
                                    }
                                }

                                if (metaChips.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        metaChips.forEach { label ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Segmented Tabs
            item(key = "detail_tabs") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DetailTab.entries.forEach { tab ->
                            val selected = state.activeTab == tab
                            val pillColor by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                animationSpec = tween(220),
                                label = "detailTabPill"
                            )
                            val labelColor by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(220),
                                label = "detailTabText"
                            )
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .bouncyClickable(pressedScale = 0.96f) {
                                        onSelectTab(tab)
                                    }
                                    .testTag("detail_tab_${tab.name.lowercase()}"),
                                shape = RoundedCornerShape(10.dp),
                                color = pillColor
                            ) {
                                Text(
                                    text = tab.labelFa,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = labelColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Tab Body
            item(key = "detail_body") {
                AnimatedContent(
                    targetState = state.activeTab,
                    transitionSpec = {
                        fadeIn(tween(220)).togetherWith(fadeOut(tween(160)))
                    },
                    label = "detailTabSwitch",
                    modifier = Modifier.padding(16.dp)
                ) { activeTab ->
                    when (activeTab) {
                        DetailTab.TIMELINE -> TimelineTabContent(
                            state = state,
                            onToggleHighlight = onToggleHighlight,
                            onCloseHighlight = onCloseHighlight,
                            onPlayerNameClick = onPlayerNameClick
                        )
                        DetailTab.STATS -> StatsTabContent(state = state)
                        DetailTab.LINEUP -> LineupTabContent(
                            state = state,
                            onPlayerClick = onPlayerClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineTabContent(
    state: MatchDetailUiState,
    onToggleHighlight: (MatchEvent) -> Unit,
    onCloseHighlight: () -> Unit,
    onPlayerNameClick: (String, String, String) -> Unit
) {
    MatchTimeline(
        events = state.events,
        match = state.match,
        isLoading = state.isLoadingEvents,
        emptyOrErrorMessage = state.eventsMessage,
        activeHighlightEventId = state.activeHighlightEventId,
        highlightMp4Url = state.highlightVideo?.mp4Url,
        highlightTitle = state.highlightVideo?.title,
        highlightStatus = state.highlightStatus,
        onToggleHighlight = onToggleHighlight,
        onCloseHighlight = onCloseHighlight,
        onPlayerNameClick = onPlayerNameClick
    )
}

@Composable
fun MatchTimeline(
    events: List<MatchEvent>,
    match: MatchSummary?,
    isLoading: Boolean = false,
    emptyOrErrorMessage: String? = null,
    activeHighlightEventId: String? = null,
    highlightMp4Url: String? = null,
    highlightTitle: String? = null,
    highlightStatus: String = "",
    onToggleHighlight: (MatchEvent) -> Unit = {},
    onCloseHighlight: () -> Unit = {},
    onPlayerNameClick: (String, String, String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    if (events.isEmpty()) {
        DetailEmptyMessage(emptyOrErrorMessage ?: "هنوز رخدادی برای این بازی ثبت نشده است.")
        return
    }

    val groupedByPeriod = remember(events) {
        linkedMapOf<String, MutableList<MatchEvent>>().apply {
            events.forEach { ev ->
                getOrPut(ev.periodLabel) { mutableListOf() }.add(ev)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("match_timeline"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        groupedByPeriod.forEach { (period, periodEvents) ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = period,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column {
                    periodEvents.forEachIndexed { idx, ev ->
                        val isVideoOpen = activeHighlightEventId == ev.id
                        val isAway = ev.side == 1
                        val teamName = if (isAway) match?.guest?.name ?: "میهمان"
                        else match?.host?.name ?: "میزبان"
                        val teamLogo = if (isAway) match?.guest?.logoUrl.orEmpty()
                        else match?.host?.logoUrl.orEmpty()

                        TimelineEventRow(
                            event = ev,
                            teamSideLabel = teamName,
                            isVideoOpen = isVideoOpen,
                            mp4Url = if (isVideoOpen) highlightMp4Url else null,
                            videoTitle = if (isVideoOpen) (highlightTitle ?: ev.titleLabel) else "",
                            videoStatus = if (isVideoOpen) highlightStatus else "",
                            onToggleHighlight = { onToggleHighlight(ev) },
                            onCloseHighlight = onCloseHighlight,
                            onPlayerClick = { playerName ->
                                onPlayerNameClick(playerName, teamName, teamLogo)
                            }
                        )
                        if (idx < periodEvents.lastIndex) {
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineEventRow(
    event: MatchEvent,
    teamSideLabel: String,
    isVideoOpen: Boolean,
    mp4Url: String?,
    videoTitle: String,
    videoStatus: String,
    onToggleHighlight: () -> Unit,
    onCloseHighlight: () -> Unit,
    onPlayerClick: (String) -> Unit
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val badgeColor = when {
        event.eventType == 1 -> ElectricLime
        event.eventType == 4 -> BroadcastCyan
        event.eventType == 5 -> Color(0xFFA78BFA)
        event.description.contains("کارت قرمز") -> LiveCrimson
        event.description.contains("کارت زرد") -> AmberCard
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (reducedMotion) Modifier
                else Modifier.animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = "${SportsRepository.toPersianDigits(event.time)}′",
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (event.eventType == 1) {
                        GoalEventAnimation(size = 20.dp, iterations = 2)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                    }
                    Text(
                        text = event.titleLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = event.description.ifBlank { event.decisionTitle }.ifBlank { "جزئیات ثبت نشده است." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = teamSideLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (event.strikerName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onPlayerClick(event.strikerName) }
                        ) {
                            Text(
                                text = "⚽ ${event.strikerName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (event.assisterName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onPlayerClick(event.assisterName) }
                        ) {
                            Text(
                                text = "پاس گل: ${event.assisterName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (event.hostScore != null && event.guestScore != null) {
                        Text(
                            text = "· ${SportsRepository.toPersianDigits(event.hostScore)} : ${SportsRepository.toPersianDigits(event.guestScore)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (event.videoId.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .bouncyClickable(onClick = onToggleHighlight)
                            .testTag("highlight_trigger_${event.videoId}"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isVideoOpen) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isVideoOpen) Icons.Rounded.Videocam else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = if (isVideoOpen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (event.eventType == 1) "تماشای ویدئوی گل" else "تماشای صحنه",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isVideoOpen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        if (isVideoOpen) {
            Spacer(modifier = Modifier.height(10.dp))
            InlineHighlightPlayer(
                title = videoTitle,
                mp4Url = mp4Url,
                statusText = videoStatus,
                onClose = onCloseHighlight
            )
        }
    }
}

@Composable
private fun StatsTabContent(state: MatchDetailUiState) {
    MatchStatisticsPanel(
        stats = state.detailInfo?.stats.orEmpty(),
        hostName = state.match?.host?.name ?: "میزبان",
        guestName = state.match?.guest?.name ?: "میهمان",
        statsTitle = state.detailInfo?.statsTitle ?: "آمار تفکیکی",
        isLoading = state.isLoadingDetails,
        errorMessage = if (!state.isLoadingDetails && state.detailInfo?.stats.isNullOrEmpty()) {
            state.detailsMessage ?: "آمار این مسابقه در منبع فعلی موجود نیست."
        } else null
    )
}

@Composable
private fun LineupTabContent(
    state: MatchDetailUiState,
    onPlayerClick: (LineupPlayer, String, String) -> Unit
) {
    if (state.isLoadingDetails) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    val hostLineup = state.detailInfo?.hostLineup
    val guestLineup = state.detailInfo?.guestLineup
    if (hostLineup == null && guestLineup == null) {
        DetailEmptyMessage(state.detailsMessage ?: "ترکیب تیم‌ها هنوز منتشر نشده است.")
        return
    }

    var selectedSide by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                listOf(
                    0 to (state.match?.host?.name ?: "میزبان"),
                    1 to (state.match?.guest?.name ?: "میهمان")
                ).forEach { (idx, teamName) ->
                    val active = selectedSide == idx
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { selectedSide = idx },
                        shape = RoundedCornerShape(9.dp),
                        color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                    ) {
                        Text(
                            text = teamName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }

        val activeSide = if (selectedSide == 0) hostLineup else guestLineup
        val activeTeamName = if (selectedSide == 0) state.match?.host?.name ?: "میزبان"
        else state.match?.guest?.name ?: "میهمان"
        val activeTeamLogo = if (selectedSide == 0) state.match?.host?.logoUrl.orEmpty()
        else state.match?.guest?.logoUrl.orEmpty()

        if (activeSide == null) {
            DetailEmptyMessage("ترکیب $activeTeamName در دسترس نیست.")
        } else {
            if (activeSide.coach.isNotBlank()) {
                Text(
                    text = "سرمربی: ${activeSide.coach}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            FootballPitchVisualization(
                teamName = activeTeamName,
                side = activeSide,
                onPlayerClick = { player ->
                    onPlayerClick(player, activeTeamName, activeTeamLogo)
                }
            )

            LineupRosterBoard(
                side = activeSide,
                onPlayerClick = { player ->
                    onPlayerClick(player, activeTeamName, activeTeamLogo)
                }
            )
        }
    }
}

@Composable
private fun LineupRosterBoard(
    side: LineupSide,
    onPlayerClick: (LineupPlayer) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (side.starters.isNotEmpty()) {
                Text(
                    text = "بازیکنان اصلی (برای جزئیات روی بازیکن بزن)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                side.starters.forEachIndexed { idx, p ->
                    PlayerLineupRow(
                        player = p,
                        showTopDivider = idx > 0,
                        onClick = { onPlayerClick(p) }
                    )
                }
            }

            if (side.bench.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ذخیره‌ها",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                side.bench.forEachIndexed { idx, p ->
                    PlayerLineupRow(
                        player = p,
                        showTopDivider = idx > 0,
                        onClick = { onPlayerClick(p) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerLineupRow(
    player: LineupPlayer,
    showTopDivider: Boolean,
    onClick: () -> Unit
) {
    Column {
        if (showTopDivider) {
            HorizontalDivider(
                thickness = 0.8.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp, horizontal = 4.dp)
                .testTag("lineup_player_${player.name}"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (player.shirtNumber.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = SportsRepository.toPersianDigits(player.shirtNumber),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (player.isCaptain) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "کاپیتان",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                if (player.isManOfTheMatch) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "بهترین بازیکن",
                        tint = GoldMotm,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (player.score.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = SportsRepository.toPersianDigits(player.score),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailEmptyMessage(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EmptyCrestLottie(size = 56.dp)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
