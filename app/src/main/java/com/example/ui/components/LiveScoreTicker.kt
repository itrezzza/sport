package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Scoreboard
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.LeagueGroup
import com.example.data.model.MatchEntry
import com.example.data.model.MatchSummary
import com.example.data.network.SportsRepository
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.VazirmatnFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScoresPullRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val dragProgress = state.distanceFraction.coerceIn(0f, 1.35f)
    val visible = isRefreshing || dragProgress > 0.02f
    if (!visible) return

    val spinTransition = rememberInfiniteTransition(label = "livePullRefreshSpin")
    val activeSpinAngle by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "activeSpinAngle"
    )
    val pullAngle by animateFloatAsState(
        targetValue = (dragProgress * 320f).coerceAtMost(420f),
        animationSpec = tween(durationMillis = 80),
        label = "pullDragAngle"
    )
    val targetAlpha by animateFloatAsState(
        targetValue = if (isRefreshing) 1f else (dragProgress * 1.15f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 120),
        label = "pullAlpha"
    )
    val targetScale by animateFloatAsState(
        targetValue = if (isRefreshing) 1f else (0.72f + 0.28f * dragProgress.coerceIn(0f, 1f)),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pullScale"
    )
    val targetOffsetPx by animateFloatAsState(
        targetValue = with(density) {
            if (isRefreshing) 14.dp.toPx()
            else (dragProgress * 46.dp.toPx()).coerceIn(0f, 64.dp.toPx())
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pullTranslationY"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                translationY = targetOffsetPx
                scaleX = targetScale
                scaleY = targetScale
                alpha = targetAlpha
            }
            .testTag("live_scores_pull_indicator"),
        shape = RoundedCornerShape(99.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (isRefreshing) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sync,
                    contentDescription = "همگام‌سازی نتایج زنده",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer {
                            rotationZ = if (isRefreshing) activeSpinAngle else pullAngle
                        }
                        .testTag("live_scores_pull_refresh_icon")
                )
            }
            Text(
                text = when {
                    isRefreshing -> "در حال همگام‌سازی…"
                    dragProgress >= 1f -> "رها کن برای همگام‌سازی"
                    else -> "بکش برای همگام‌سازی"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun LiveScoreTicker(
    leagues: List<LeagueGroup>,
    isLoading: Boolean,
    lastUpdatedTime: String,
    onMatchClick: (MatchSummary, String, Int) -> Unit,
    onOpenScoresTab: () -> Unit,
    onRefreshScores: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val liveEntries = remember(leagues) {
        leagues
            .flatMap { league ->
                league.matches
                    .filter { it.isLive }
                    .map { match -> MatchEntry(match = match, leagueTitle = league.title, sport = league.sport) }
            }
            .sortedBy { if (it.sport == 1) 0 else it.sport }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_score_ticker"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                LivePulseDot(
                    color = if (liveEntries.isNotEmpty()) LiveCrimson else MaterialTheme.colorScheme.primary,
                    size = 6.dp
                )
                Text(
                    text = "نتایج لحظه‌ای",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (liveEntries.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = LiveCrimson.copy(alpha = 0.14f)
                    ) {
                        RollingScoreText(
                            text = "${SportsRepository.toPersianDigits(liveEntries.size)} زنده",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = LiveCrimson,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                if (lastUpdatedTime.isNotBlank() && liveEntries.isNotEmpty()) {
                    Text(
                        text = lastUpdatedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val syncTransition = rememberInfiniteTransition(label = "tickerSyncIcon")
            val syncRotation by syncTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 850, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "tickerSyncRotation"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(34.dp)
                        .clip(CircleShape)
                        .bouncyClickable(
                            enabled = !isLoading,
                            pressedScale = 0.92f,
                            onClick = onRefreshScores
                        )
                        .testTag("live_score_ticker_sync_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = "همگام‌سازی نتایج زنده",
                            tint = if (isLoading) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .size(17.dp)
                                .graphicsLayer {
                                    rotationZ = if (isLoading) syncRotation else 0f
                                }
                                .testTag("live_score_ticker_sync_icon")
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(99.dp))
                        .bouncyClickable(pressedScale = 0.94f, onClick = onOpenScoresTab)
                        .testTag("ticker_view_all_scores")
                ) {
                    Row(
                        modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "همهٔ نتایج",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "مشاهدهٔ همهٔ نتایج زنده",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        when {
            liveEntries.isNotEmpty() -> {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_score_ticker_row"),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = liveEntries,
                        key = { it.match.id.ifBlank { "${it.leagueTitle}_${it.match.host.name}_${it.match.guest.name}" } }
                    ) { entry ->
                        LiveScoreTickerCard(
                            entry = entry,
                            onClick = { onMatchClick(entry.match, entry.leagueTitle, entry.sport) }
                        )
                    }
                }
            }

            isLoading -> {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = false
                ) {
                    items(3) {
                        LiveScoreTickerSkeleton()
                    }
                }
            }

            else -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .bouncyClickable(pressedScale = 0.98f, onClick = onOpenScoresTab)
                        .testTag("live_score_ticker_empty"),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Scoreboard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "در حال حاضر مسابقه‌ای در جریان نیست",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "برنامهٔ امروز",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveScoreTickerCard(
    entry: MatchEntry,
    onClick: () -> Unit
) {
    val match = entry.match
    val hostGoals = match.goals?.host
    val guestGoals = match.goals?.guest
    val hostLeading = hostGoals != null && guestGoals != null && hostGoals > guestGoals
    val guestLeading = hostGoals != null && guestGoals != null && guestGoals > hostGoals
    val timeLabel = match.liveTime.ifBlank { match.statusTitle.ifBlank { "زنده" } }

    Surface(
        modifier = Modifier
            .width(214.dp)
            .clip(RoundedCornerShape(18.dp))
            .bouncyClickable(pressedScale = 0.96f, onClick = onClick)
            .testTag("ticker_match_${match.id}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, LiveCrimson.copy(alpha = 0.34f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            LiveCrimson.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LivePulseDot(color = LiveCrimson, size = 5.dp)
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = LiveCrimson,
                            maxLines = 1
                        )
                    }
                    if (entry.leagueTitle.isNotBlank()) {
                        Text(
                            text = entry.leagueTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }

                TickerTeamRow(
                    teamName = match.host.name,
                    logoUrl = match.host.logoUrl,
                    score = hostGoals,
                    isLeading = hostLeading
                )
                TickerTeamRow(
                    teamName = match.guest.name,
                    logoUrl = match.guest.logoUrl,
                    score = guestGoals,
                    isLeading = guestLeading
                )
            }
        }
    }
}

@Composable
private fun TickerTeamRow(
    teamName: String,
    logoUrl: String,
    score: Int?,
    isLeading: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            TeamBadge(
                logoUrl = logoUrl,
                teamName = teamName,
                size = 20.dp
            )
            Text(
                text = teamName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isLeading) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isLeading) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isLeading) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
            }
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                RollingScoreText(
                    text = score?.let { SportsRepository.toPersianDigits(it) } ?: "۰",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = VazirmatnFontFamily),
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isLeading) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun LiveScoreTickerSkeleton() {
    val transition = rememberInfiniteTransition(label = "tickerSkeleton")
    val offset by transition.animateFloat(
        initialValue = -180f,
        targetValue = 420f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tickerSkeletonOffset"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Box(
        modifier = Modifier
            .width(214.dp)
            .height(92.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(offset, 0f),
                    end = Offset(offset + 180f, 120f)
                )
            )
    )
}
