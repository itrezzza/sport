package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LineupPlayer
import com.example.data.model.LineupSide
import com.example.data.model.MatchEntry
import com.example.data.model.StatEntry
import com.example.data.network.SportsRepository
import com.example.ui.theme.BroadcastCyan
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.GoldMotm
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

@Composable
fun MatchStatsVicoChart(
    stats: List<StatEntry>,
    hostName: String,
    guestName: String,
    modifier: Modifier = Modifier
) {
    val chartedStats = remember(stats) { stats.take(6) }
    if (chartedStats.isEmpty()) return

    val hostColor = MaterialTheme.colorScheme.primary
    val guestColor = MaterialTheme.colorScheme.secondary
    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant

    val modelProducer = remember(chartedStats) {
        val hostEntries = chartedStats.mapIndexed { idx, s ->
            entryOf(idx.toFloat(), s.hostPercent.coerceIn(0f, 100f))
        }
        val guestEntries = chartedStats.mapIndexed { idx, s ->
            entryOf(idx.toFloat(), (100f - s.hostPercent).coerceIn(0f, 100f))
        }
        ChartEntryModelProducer(hostEntries, guestEntries)
    }

    val bottomFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        val label = chartedStats.getOrNull(value.toInt())?.title.orEmpty()
        if (label.length > 10) label.take(9) + "…" else label
    }
    val startFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
        "${SportsRepository.toPersianDigits(value.toInt())}٪"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("match_stats_vico_chart"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "نمودار مقایسهٔ عملکرد",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartLegendDot(color = hostColor, label = hostName.take(12))
                    ChartLegendDot(color = guestColor, label = guestName.take(12))
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Chart(
                    chart = columnChart(
                        columns = listOf(
                            lineComponent(
                                color = hostColor,
                                thickness = 10.dp,
                                shape = Shapes.roundedCornerShape(topLeftPercent = 45, topRightPercent = 45)
                            ),
                            lineComponent(
                                color = guestColor,
                                thickness = 10.dp,
                                shape = Shapes.roundedCornerShape(topLeftPercent = 45, topRightPercent = 45)
                            )
                        ),
                        spacing = 16.dp,
                        innerSpacing = 4.dp
                    ),
                    chartModelProducer = modelProducer,
                    startAxis = rememberStartAxis(
                        label = textComponent(color = axisTextColor, textSize = 10.sp),
                        guideline = lineComponent(color = gridLineColor, thickness = 0.6.dp),
                        valueFormatter = startFormatter
                    ),
                    bottomAxis = rememberBottomAxis(
                        label = textComponent(color = axisTextColor, textSize = 9.sp),
                        guideline = null,
                        valueFormatter = bottomFormatter
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                )
            }
        }
    }
}

@Composable
fun TeamFormVicoChart(
    matches: List<MatchEntry>,
    teamName: String,
    modifier: Modifier = Modifier
) {
    val completed = remember(matches, teamName) {
        matches.filter { it.match.goals != null }.take(6).reversed()
    }
    if (completed.isEmpty()) return

    val scoredColor = MaterialTheme.colorScheme.primary
    val concededColor = MaterialTheme.colorScheme.tertiary
    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant

    val modelProducer = remember(completed, teamName) {
        val scoredEntries = completed.mapIndexed { idx, entry ->
            val isHost = entry.match.host.name.contains(teamName, ignoreCase = true)
            val scored = if (isHost) entry.match.goals?.host ?: 0 else entry.match.goals?.guest ?: 0
            entryOf(idx.toFloat(), scored.toFloat())
        }
        val concededEntries = completed.mapIndexed { idx, entry ->
            val isHost = entry.match.host.name.contains(teamName, ignoreCase = true)
            val conceded = if (isHost) entry.match.goals?.guest ?: 0 else entry.match.goals?.host ?: 0
            entryOf(idx.toFloat(), conceded.toFloat())
        }
        ChartEntryModelProducer(scoredEntries, concededEntries)
    }

    val bottomFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        val item = completed.getOrNull(value.toInt()) ?: return@AxisValueFormatter ""
        val isHost = item.match.host.name.contains(teamName, ignoreCase = true)
        val opp = if (isHost) item.match.guest.name else item.match.host.name
        opp.take(7)
    }
    val startFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
        SportsRepository.toPersianDigits(value.toInt())
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("team_form_vico_chart"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "نمودار گل‌زنی در بازی‌های اخیر",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChartLegendDot(color = scoredColor, label = "گل زده")
                    ChartLegendDot(color = concededColor, label = "گل خورده")
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Chart(
                    chart = columnChart(
                        columns = listOf(
                            lineComponent(
                                color = scoredColor,
                                thickness = 11.dp,
                                shape = Shapes.roundedCornerShape(topLeftPercent = 45, topRightPercent = 45)
                            ),
                            lineComponent(
                                color = concededColor,
                                thickness = 11.dp,
                                shape = Shapes.roundedCornerShape(topLeftPercent = 45, topRightPercent = 45)
                            )
                        ),
                        spacing = 16.dp,
                        innerSpacing = 4.dp
                    ),
                    chartModelProducer = modelProducer,
                    startAxis = rememberStartAxis(
                        label = textComponent(color = axisTextColor, textSize = 10.sp),
                        guideline = lineComponent(color = gridLineColor, thickness = 0.6.dp),
                        valueFormatter = startFormatter
                    ),
                    bottomAxis = rememberBottomAxis(
                        label = textComponent(color = axisTextColor, textSize = 9.sp),
                        guideline = null,
                        valueFormatter = bottomFormatter
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp)
                )
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun MatchStatisticsPanel(
    stats: List<StatEntry>,
    hostName: String,
    guestName: String,
    statsTitle: String = "آمار تفکیکی",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        !errorMessage.isNullOrBlank() || stats.isEmpty() -> {
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EmptyCrestLottie(size = 54.dp)
                    Text(
                        text = errorMessage ?: "آمار این مسابقه در منبع فعلی موجود نیست.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        else -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("match_statistics_panel"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MatchStatsVicoChart(
                    stats = stats,
                    hostName = hostName,
                    guestName = guestName
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = hostName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = statsTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = guestName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column {
                        stats.forEachIndexed { idx, stat ->
                            MatchStatComparisonBar(stat = stat)
                            if (idx < stats.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    thickness = 0.8.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchStatComparisonBar(stat: StatEntry) {
    val reducedMotion = rememberReducedMotionEnabled()
    val targetFraction = (stat.hostPercent / 100f).coerceIn(0.05f, 0.95f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 560),
        label = "matchStatFraction"
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = SportsRepository.toPersianDigits(stat.hostValue),
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stat.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = SportsRepository.toPersianDigits(stat.guestValue),
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
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
                    .background(MaterialTheme.colorScheme.secondary)
            )
        }
    }
}

@Composable
fun TacticalPitchView(
    teamName: String,
    side: LineupSide,
    onPlayerClick: (LineupPlayer) -> Unit,
    modifier: Modifier = Modifier
) {
    FootballPitchVisualization(
        teamName = teamName,
        side = side,
        onPlayerClick = onPlayerClick,
        modifier = modifier
    )
}

@Composable
fun FootballPitchVisualization(
    teamName: String,
    side: LineupSide,
    onPlayerClick: (LineupPlayer) -> Unit,
    modifier: Modifier = Modifier
) {
    if (side.starters.isEmpty()) return

    val rows = remember(side.starters, side.formation) {
        buildFormationRows(side.starters, side.formation)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0C2217),
                        Color(0xFF103020),
                        Color(0xFF0A1D13)
                    )
                )
            )
            .testTag("tactical_pitch_view")
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val lineColor = Color.White.copy(alpha = 0.16f)
            val stroke = Stroke(width = 1.4.dp.toPx())
            val pad = 12.dp.toPx()

            // Pitch border
            drawRoundRect(
                color = lineColor,
                topLeft = Offset(pad, pad),
                size = Size(size.width - pad * 2, size.height - pad * 2),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                style = stroke
            )
            // Penalty area top (goalkeeper end)
            val boxW = size.width * 0.44f
            val boxH = size.height * 0.16f
            drawRect(
                color = lineColor,
                topLeft = Offset((size.width - boxW) / 2f, pad),
                size = Size(boxW, boxH),
                style = stroke
            )
            // Halfway line near bottom
            drawLine(
                color = lineColor,
                start = Offset(pad, size.height * 0.85f),
                end = Offset(size.width - pad, size.height * 0.85f),
                strokeWidth = 1.4.dp.toPx()
            )
            // Center circle arc
            drawCircle(
                color = lineColor,
                radius = size.minDimension * 0.14f,
                center = Offset(size.width * 0.5f, size.height * 0.85f),
                style = stroke
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = teamName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                if (side.formation.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElectricLime
                    ) {
                        Text(
                            text = side.formation,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF091203),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            rows.forEach { rowPlayers ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    rowPlayers.forEach { player ->
                        PitchPlayerNode(
                            player = player,
                            onClick = { onPlayerClick(player) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PitchPlayerNode(
    player: LineupPlayer,
    onClick: () -> Unit
) {
    val surname = remember(player.name) {
        val parts = player.name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.size > 1) parts.last() else player.name
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .widthIn(min = 48.dp, max = 72.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp, horizontal = 2.dp)
            .testTag("pitch_player_${player.name}")
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                shape = CircleShape,
                color = if (player.isManOfTheMatch) GoldMotm else Color(0xFF151B23),
                border = BorderStroke(
                    1.6.dp,
                    if (player.isCaptain) ElectricLime else BroadcastCyan.copy(alpha = 0.85f)
                ),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = player.shirtNumber.ifBlank { "•" }.let { SportsRepository.toPersianDigits(it) },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (player.isManOfTheMatch) Color(0xFF0B0F14) else Color.White
                    )
                }
            }
            if (player.score.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ElectricLime
                ) {
                    Text(
                        text = SportsRepository.toPersianDigits(player.score),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF091203),
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF0B0F14).copy(alpha = 0.78f)
        ) {
            Text(
                text = surname,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
            )
        }
    }
}

private fun buildFormationRows(
    starters: List<LineupPlayer>,
    formation: String
): List<List<LineupPlayer>> {
    if (starters.isEmpty()) return emptyList()
    val counts = Regex("\\d+")
        .findAll(formation)
        .mapNotNull { it.value.toIntOrNull()?.takeIf { n -> n in 1..6 } }
        .toList()

    if (counts.isEmpty() || counts.sum() + 1 != starters.size) {
        val gk = listOf(starters.first())
        val outfield = starters.drop(1)
        return listOf(gk) + outfield.chunked(4)
    }

    val rows = mutableListOf<List<LineupPlayer>>()
    rows.add(listOf(starters.first())) // GK
    var cursor = 1
    counts.forEach { count ->
        if (cursor < starters.size) {
            val end = (cursor + count).coerceAtMost(starters.size)
            rows.add(starters.subList(cursor, end))
            cursor = end
        }
    }
    if (cursor < starters.size) {
        rows.add(starters.subList(cursor, starters.size))
    }
    return rows
}
