package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.R
import com.example.data.model.LineupSide
import com.example.data.model.MatchEvent
import com.example.data.model.StatEntry
import com.example.data.network.SportsRepository
import com.example.ui.components.InlineHighlightPlayer
import com.example.ui.components.LivePulseDot
import com.example.ui.components.RollingScoreText
import com.example.ui.components.TeamBadge
import com.example.ui.components.bouncyClickable
import com.example.ui.theme.AmberCard
import com.example.ui.theme.GoldMotm
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.PitchLime
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
    onClose: () -> Unit
) {
    val match = state.match ?: return
    val context = LocalContext.current

    BackHandler { onClose() }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            // Seamless Stadium Scoreboard Hero + Floating Translucent Top Controls
            item(key = "detail_hero") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                                    0.55f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Translucent Floating Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                tonalElevation = 2.dp
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
                                shape = RoundedCornerShape(99.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                                tonalElevation = 2.dp
                            ) {
                                Text(
                                    text = state.leagueTitle.ifBlank { "مرکز مسابقه" },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
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
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                                },
                                tonalElevation = 2.dp
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

                        // Central Stadium Scoreboard Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TeamBadge(
                                            logoUrl = match.host.logoUrl,
                                            teamName = match.host.name,
                                            size = 60.dp
                                        )
                                        Text(
                                            text = match.host.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                        ) {
                                            RollingScoreText(
                                                text = "${match.goals?.host?.let { SportsRepository.toPersianDigits(it) } ?: "–"} : ${match.goals?.guest?.let { SportsRepository.toPersianDigits(it) } ?: "–"}",
                                                style = MaterialTheme.typography.headlineLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(99.dp),
                                            color = if (match.isLive) MaterialTheme.colorScheme.tertiaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                if (match.isLive) {
                                                    LivePulseDot(color = LiveCrimson, size = 6.dp)
                                                }
                                                Text(
                                                    text = match.stateLabel(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (match.isLive) MaterialTheme.colorScheme.onTertiaryContainer
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TeamBadge(
                                            logoUrl = match.guest.logoUrl,
                                            teamName = match.guest.name,
                                            size = 60.dp
                                        )
                                        Text(
                                            text = match.guest.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2
                                        )
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
                                                shape = RoundedCornerShape(99.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                val matchPageUrl = SportsRepository.safeVarzesh3MatchUrl(match.link)
                                    .ifBlank { state.detailInfo?.pageUrl.orEmpty() }
                                val watchUrl = match.liveStreamLink

                                if (matchPageUrl.isNotBlank() || watchUrl.isNotBlank()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (watchUrl.isNotBlank()) {
                                            Button(
                                                onClick = {
                                                    runCatching {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, watchUrl.toUri()))
                                                    }
                                                },
                                                shape = RoundedCornerShape(99.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            ) {
                                                Text("تماشای پخش", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                            }
                                        }
                                        if (matchPageUrl.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    runCatching {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, matchPageUrl.toUri()))
                                                    }
                                                },
                                                shape = RoundedCornerShape(99.dp),
                                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Text("صفحهٔ کامل بازی", style = MaterialTheme.typography.labelMedium)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pill-Segmented Tabs
            item(key = "detail_tabs") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
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
                                targetValue = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                animationSpec = tween(220),
                                label = "detailTabPill"
                            )
                            val labelColor by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(220),
                                label = "detailTabText"
                            )
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .bouncyClickable(pressedScale = 0.96f) {
                                        onSelectTab(tab)
                                    }
                                    .testTag("detail_tab_${tab.name.lowercase()}"),
                                shape = RoundedCornerShape(12.dp),
                                color = pillColor,
                                shadowElevation = if (selected) 2.dp else 0.dp
                            ) {
                                Text(
                                    text = tab.labelFa,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
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
                            onCloseHighlight = onCloseHighlight
                        )
                        DetailTab.STATS -> StatsTabContent(state = state)
                        DetailTab.LINEUP -> LineupTabContent(state = state)
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
    onCloseHighlight: () -> Unit
) {
    if (state.isLoadingEvents) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    if (state.events.isEmpty()) {
        DetailEmptyMessage(state.eventsMessage ?: "هنوز رخدادی برای این بازی ثبت نشده است.")
        return
    }

    val groupedByPeriod = linkedMapOf<String, MutableList<MatchEvent>>()
    state.events.forEach { ev ->
        groupedByPeriod.getOrPut(ev.periodLabel) { mutableListOf() }.add(ev)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        groupedByPeriod.forEach { (period, periodEvents) ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = period,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }

            periodEvents.forEach { ev ->
                val isVideoOpen = state.activeHighlightEventId == ev.id
                TimelineEventRow(
                    event = ev,
                    hostName = state.match?.host?.name ?: "میزبان",
                    guestName = state.match?.guest?.name ?: "میهمان",
                    isVideoOpen = isVideoOpen,
                    mp4Url = if (isVideoOpen) state.highlightVideo?.mp4Url else null,
                    videoTitle = if (isVideoOpen) state.highlightVideo?.title ?: ev.titleLabel else "",
                    videoStatus = if (isVideoOpen) state.highlightStatus else "",
                    onToggleHighlight = { onToggleHighlight(ev) },
                    onCloseHighlight = onCloseHighlight
                )
            }
        }
    }
}

@Composable
private fun TimelineEventRow(
    event: MatchEvent,
    hostName: String,
    guestName: String,
    isVideoOpen: Boolean,
    mp4Url: String?,
    videoTitle: String,
    videoStatus: String,
    onToggleHighlight: () -> Unit,
    onCloseHighlight: () -> Unit
) {
    val isAway = event.side == 1
    val teamSideLabel = if (isAway) guestName else hostName
    val badgeColor = when {
        event.eventType == 1 -> PitchLime
        event.eventType == 4 -> MaterialTheme.colorScheme.primary
        event.eventType == 5 -> Color(0xFF9F7AEA)
        event.description.contains("کارت قرمز") -> LiveCrimson
        event.description.contains("کارت زرد") -> AmberCard
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = if (isVideoOpen) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "${SportsRepository.toPersianDigits(event.time)}′",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Text(
                            text = event.titleLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = event.description.ifBlank { event.decisionTitle }.ifBlank { "جزئیات ثبت نشده است." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = teamSideLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (event.assisterName.isNotBlank()) {
                            Text(
                                text = "· پاس گل: ${event.assisterName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (event.hostScore != null && event.guestScore != null) {
                            Text(
                                text = "· نتیجه: ${SportsRepository.toPersianDigits(event.hostScore)} : ${SportsRepository.toPersianDigits(event.guestScore)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (event.videoId.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .bouncyClickable(onClick = onToggleHighlight)
                                .testTag("highlight_trigger_${event.videoId}"),
                            shape = RoundedCornerShape(99.dp),
                            color = if (isVideoOpen) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isVideoOpen) Icons.Rounded.Videocam else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isVideoOpen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (event.eventType == 1) "تماشای ویدئوی گل" else "تماشای صحنه",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isVideoOpen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
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
}

@Composable
private fun StatsTabContent(state: MatchDetailUiState) {
    if (state.isLoadingDetails) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    val stats = state.detailInfo?.stats.orEmpty()
    if (stats.isEmpty()) {
        DetailEmptyMessage(state.detailsMessage ?: "آمار این مسابقه در منبع فعلی موجود نیست.")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.match?.host?.name ?: "میزبان",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = state.detailInfo?.statsTitle ?: "مجموع بازی",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = state.match?.guest?.name ?: "میهمان",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column {
                stats.forEachIndexed { idx, stat ->
                    StatComparisonRow(stat = stat)
                    if (idx < stats.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatComparisonRow(stat: StatEntry) {
    val targetFraction = (stat.hostPercent / 100f).coerceIn(0.05f, 0.95f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 650),
        label = "statFraction"
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = SportsRepository.toPersianDigits(stat.hostValue),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stat.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = SportsRepository.toPersianDigits(stat.guestValue),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(animatedFraction)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight((1f - animatedFraction).coerceAtLeast(0.01f))
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f))
            )
        }
    }
}

@Composable
private fun LineupTabContent(state: MatchDetailUiState) {
    if (state.isLoadingDetails) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
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
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
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
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedSide = idx },
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (active) 2.dp else 0.dp
                    ) {
                        Text(
                            text = teamName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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

        if (activeSide == null) {
            DetailEmptyMessage("ترکیب $activeTeamName در دسترس نیست.")
        } else {
            LineupTeamCard(teamName = activeTeamName, side = activeSide)
        }
    }
}

@Composable
private fun LineupTeamCard(
    teamName: String,
    side: LineupSide
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Pitch formation banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0E2C1E), Color(0xFF14422D))
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineColor = Color.White.copy(alpha = 0.22f)
                    val stroke = Stroke(width = 1.5.dp.toPx())
                    drawRect(
                        color = lineColor,
                        topLeft = Offset(8.dp.toPx(), 8.dp.toPx()),
                        size = Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()),
                        style = stroke
                    )
                    drawLine(
                        color = lineColor,
                        start = Offset(size.width / 2f, 8.dp.toPx()),
                        end = Offset(size.width / 2f, size.height - 8.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 22.dp.toPx(),
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = stroke
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = teamName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (side.coach.isNotBlank()) {
                            Text(
                                text = "سرمربی: ${side.coach}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.82f)
                            )
                        }
                    }
                    if (side.formation.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PitchLime
                        ) {
                            Text(
                                text = side.formation,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0E1E0F),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            if (side.starters.isNotEmpty()) {
                Text(
                    text = "ترکیب اصلی",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                side.starters.forEachIndexed { idx, p ->
                    PlayerLineupRow(
                        name = p.name,
                        shirtNumber = p.shirtNumber,
                        isCaptain = p.isCaptain,
                        isMotm = p.isManOfTheMatch,
                        score = p.score,
                        showTopDivider = idx > 0
                    )
                }
            }

            if (side.bench.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "نیمکت",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                side.bench.forEachIndexed { idx, p ->
                    PlayerLineupRow(
                        name = p.name,
                        shirtNumber = p.shirtNumber,
                        isCaptain = p.isCaptain,
                        isMotm = p.isManOfTheMatch,
                        score = p.score,
                        showTopDivider = idx > 0
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerLineupRow(
    name: String,
    shirtNumber: String,
    isCaptain: Boolean,
    isMotm: Boolean,
    score: String,
    showTopDivider: Boolean
) {
    Column {
        if (showTopDivider) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isCaptain) {
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
                if (isMotm) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "بهترین بازیکن",
                        tint = GoldMotm,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (score.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = SportsRepository.toPersianDigits(score),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (shirtNumber.isNotBlank()) {
                    Text(
                        text = "#${SportsRepository.toPersianDigits(shirtNumber)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailEmptyMessage(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp)
        )
    }
}
