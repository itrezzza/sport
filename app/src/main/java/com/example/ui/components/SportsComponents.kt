package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.MatchEntry
import com.example.data.model.MatchGoals
import com.example.data.network.SportsRepository
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.sportsSpacing

@Composable
fun LivePulseIndicator(
    modifier: Modifier = Modifier,
    color: Color = LiveCrimson,
    size: Dp = 8.dp,
    label: String? = null
) {
    val reducedMotion = rememberReducedMotionEnabled()

    val haloScale: Float
    val haloAlpha: Float
    if (reducedMotion) {
        haloScale = 1.6f
        haloAlpha = 0.25f
    } else {
        val transition = rememberInfiniteTransition(label = "livePulse")
        val animatedScale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 2.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1050, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "haloScale"
        )
        val animatedAlpha by transition.animateFloat(
            initialValue = 0.50f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1050, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "haloAlpha"
        )
        haloScale = animatedScale
        haloAlpha = animatedAlpha
    }

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = label ?: "در حال پخش زنده"
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier.size(size * 1.6f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = color.copy(alpha = haloAlpha),
                    radius = (this.size.minDimension / 2f) * (haloScale / 2.1f)
                )
                drawCircle(
                    color = color,
                    radius = size.toPx() / 2f
                )
            }
        }
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TeamLogo(
    logoUrl: String,
    teamName: String,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier,
    highlightBorder: Boolean = false
) {
    val context = LocalContext.current
    val monogram = remember(teamName) {
        teamName.trim().firstOrNull()?.toString() ?: "●"
    }
    val imageRequest = remember(logoUrl, context) {
        if (logoUrl.isBlank()) null
        else ImageRequest.Builder(context)
            .data(logoUrl)
            .crossfade(220)
            .memoryCacheKey(logoUrl)
            .diskCacheKey(logoUrl)
            .build()
    }
    val borderColor = if (highlightBorder) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .border(0.9.dp, borderColor, CircleShape)
            .semantics { contentDescription = teamName },
        contentAlignment = Alignment.Center
    ) {
        if (imageRequest != null) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = teamName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding((size.value * 0.1f).dp.coerceAtLeast(2.dp))
                    .clip(CircleShape),
                loading = {
                    TeamLogoFallback(monogram = monogram, size = size)
                },
                error = {
                    TeamLogoFallback(monogram = monogram, size = size)
                }
            )
        } else {
            TeamLogoFallback(monogram = monogram, size = size)
        }
    }
}

@Composable
private fun TeamLogoFallback(
    monogram: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = monogram,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

enum class ScoreboardSize {
    COMPACT,
    SPOTLIGHT,
    HERO
}

@Composable
fun AnimatedScoreboard(
    goals: MatchGoals?,
    isLive: Boolean,
    modifier: Modifier = Modifier,
    size: ScoreboardSize = ScoreboardSize.COMPACT,
    isLoading: Boolean = false
) {
    val hostText = goals?.host?.let { SportsRepository.toPersianDigits(it) } ?: "–"
    val guestText = goals?.guest?.let { SportsRepository.toPersianDigits(it) } ?: "–"
    val scoreDisplay = "$hostText : $guestText"

    val cornerRadius = when (size) {
        ScoreboardSize.COMPACT -> 9.dp
        ScoreboardSize.SPOTLIGHT -> 10.dp
        ScoreboardSize.HERO -> 14.dp
    }
    val minWidth = when (size) {
        ScoreboardSize.COMPACT -> 58.dp
        ScoreboardSize.SPOTLIGHT -> 72.dp
        ScoreboardSize.HERO -> 108.dp
    }
    val textStyle = when (size) {
        ScoreboardSize.COMPACT -> MaterialTheme.typography.titleMedium.copy(
            fontSize = 15.sp,
            fontFeatureSettings = "tnum"
        )
        ScoreboardSize.SPOTLIGHT -> MaterialTheme.typography.titleLarge.copy(
            fontSize = 21.sp,
            fontFeatureSettings = "tnum"
        )
        ScoreboardSize.HERO -> MaterialTheme.typography.displaySmall.copy(
            fontSize = 32.sp,
            fontFeatureSettings = "tnum"
        )
    }
    val containerColor = when {
        size == ScoreboardSize.COMPACT && isLive -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = if (isLive) {
        LiveCrimson.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outline
    }
    val scoreColor = when {
        size == ScoreboardSize.COMPACT && isLive -> MaterialTheme.colorScheme.onTertiaryContainer
        isLive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(0.9.dp, borderColor),
        modifier = modifier.semantics {
            contentDescription = if (goals != null) "نتیجه $scoreDisplay" else "نتیجه نامشخص"
        }
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = minWidth)
                .padding(
                    horizontal = if (size == ScoreboardSize.HERO) 16.dp else 10.dp,
                    vertical = if (size == ScoreboardSize.HERO) 8.dp else 5.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Text(
                    text = "· : ·",
                    style = textStyle,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                RollingScoreText(
                    text = scoreDisplay,
                    style = textStyle,
                    fontWeight = FontWeight.Black,
                    color = scoreColor
                )
            }
        }
    }
}

@Composable
fun SportCategoryGlyph(
    sportId: Int?,
    tint: Color,
    size: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val strokePx = 1.4.dp.toPx()
        val r = this.size.minDimension * 0.42f
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        when (sportId) {
            1 -> {
                // Football: circle + center dot + meridian lines
                drawCircle(color = tint, radius = r, center = c, style = Stroke(strokePx))
                drawCircle(color = tint, radius = r * 0.28f, center = c)
                drawLine(tint, Offset(c.x, c.y - r), Offset(c.x, c.y - r * 0.35f), strokePx, StrokeCap.Round)
                drawLine(tint, Offset(c.x - r * 0.85f, c.y + r * 0.35f), Offset(c.x - r * 0.28f, c.y + r * 0.1f), strokePx, StrokeCap.Round)
                drawLine(tint, Offset(c.x + r * 0.85f, c.y + r * 0.35f), Offset(c.x + r * 0.28f, c.y + r * 0.1f), strokePx, StrokeCap.Round)
            }
            2 -> {
                // Volleyball: circle + sweeping arc seams
                drawCircle(color = tint, radius = r, center = c, style = Stroke(strokePx))
                drawArc(
                    color = tint,
                    startAngle = -35f,
                    sweepAngle = 115f,
                    useCenter = false,
                    topLeft = Offset(c.x - r * 0.7f, c.y - r * 0.7f),
                    size = Size(r * 1.4f, r * 1.4f),
                    style = Stroke(strokePx * 0.85f, cap = StrokeCap.Round)
                )
                drawLine(tint, Offset(c.x - r * 0.6f, c.y + r * 0.45f), Offset(c.x + r * 0.35f, c.y - r * 0.25f), strokePx * 0.85f, StrokeCap.Round)
            }
            3 -> {
                // Basketball: circle + cross + curved ribs
                drawCircle(color = tint, radius = r, center = c, style = Stroke(strokePx))
                drawLine(tint, Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokePx * 0.8f)
                drawLine(tint, Offset(c.x, c.y - r), Offset(c.x, c.y + r), strokePx * 0.8f)
            }
            4 -> {
                // Futsal: pitch rectangle + center line
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(c.x - r, c.y - r * 0.72f),
                    size = Size(r * 2f, r * 1.44f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(strokePx)
                )
                drawLine(tint, Offset(c.x, c.y - r * 0.72f), Offset(c.x, c.y + r * 0.72f), strokePx * 0.8f)
                drawCircle(color = tint, radius = r * 0.28f, center = c, style = Stroke(strokePx * 0.8f))
            }
            else -> {
                // All sports: geometric 4-quadrant stadium radar
                val cell = r * 0.72f
                val gap = r * 0.22f
                drawRoundRect(tint, Offset(c.x - cell - gap / 2f, c.y - cell - gap / 2f), Size(cell, cell), CornerRadius(1.5.dp.toPx()))
                drawRoundRect(tint, Offset(c.x + gap / 2f, c.y - cell - gap / 2f), Size(cell, cell), CornerRadius(1.5.dp.toPx()))
                drawRoundRect(tint, Offset(c.x - cell - gap / 2f, c.y + gap / 2f), Size(cell, cell), CornerRadius(1.5.dp.toPx()))
                drawRoundRect(tint.copy(alpha = 0.55f), Offset(c.x + gap / 2f, c.y + gap / 2f), Size(cell, cell), CornerRadius(1.5.dp.toPx()))
            }
        }
    }
}

@Composable
fun LeagueHeader(
    title: String,
    matchCount: Int,
    hasLive: Boolean = false,
    expanded: Boolean? = null,
    onToggleExpand: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotionEnabled()
    val chevronAngle by animateFloatAsState(
        targetValue = if (expanded != false) 0f else 180f,
        animationSpec = tween(if (reducedMotion) 0 else 200),
        label = "leagueHeaderChevron"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .then(
                if (onToggleExpand != null) {
                    Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleExpand()
                    }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        if (hasLive) LiveCrimson else MaterialTheme.colorScheme.primary
                    )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
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
            Text(
                text = SportsRepository.toPersianDigits(matchCount),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (hasLive) LiveCrimson else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (expanded != null) {
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
    }
}

@Composable
fun PremiumLiveMatchCard(
    entry: MatchEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isWatched: Boolean = false,
    onToggleWatch: (() -> Unit)? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null
) {
    val match = entry.match
    val primaryAccent = MaterialTheme.colorScheme.primary
    val topAccentColor = if (match.isLive) LiveCrimson else primaryAccent

    Surface(
        modifier = modifier
            .widthIn(min = 240.dp)
            .clip(RoundedCornerShape(14.dp))
            .bouncyClickable(enabled = !isLoading && errorMessage == null, onClick = onClick)
            .testTag("premium_match_card_${match.id}"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 0.8.dp,
            color = if (match.isLive) LiveCrimson.copy(alpha = 0.42f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.85f)
        )
    ) {
        when {
            isLoading -> {
                MatchSkeletonLoader(
                    variant = MatchSkeletonVariant.HERO_CARD,
                    modifier = Modifier.height(114.dp)
                )
            }
            !errorMessage.isNullOrBlank() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (onRetry != null) {
                        OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تلاش دوباره", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            else -> {
                val hostScoreText = match.goals?.host?.let { SportsRepository.toPersianDigits(it) } ?: "–"
                val guestScoreText = match.goals?.guest?.let { SportsRepository.toPersianDigits(it) } ?: "–"
                val hostLeads = (match.goals?.host ?: 0) > (match.goals?.guest ?: 0)
                val guestLeads = (match.goals?.guest ?: 0) > (match.goals?.host ?: 0)

                Column(
                    modifier = Modifier
                        .drawBehind {
                            // Subtle top telemetry accent line
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        topAccentColor.copy(alpha = 0.9f),
                                        topAccentColor.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                ),
                                topLeft = Offset.Zero,
                                size = Size(size.width, 2.dp.toPx())
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header telemetry bar: league + live pulse/minute + watch toggle
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (match.isLive) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    LivePulseIndicator(color = LiveCrimson, size = 5.5.dp)
                                    Text(
                                        text = match.liveTime.ifBlank { "زنده" },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = LiveCrimson
                                    )
                                }
                            } else {
                                Text(
                                    text = match.stateLabel(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (onToggleWatch != null && match.id.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onToggleWatch),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isWatched) Icons.Rounded.NotificationsActive else Icons.Outlined.NotificationsNone,
                                        contentDescription = stringResource(R.string.cd_match_notifications),
                                        tint = if (isWatched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Host row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TeamLogo(
                                logoUrl = match.host.logoUrl,
                                teamName = match.host.name,
                                size = 22.dp
                            )
                            Text(
                                text = match.host.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (hostLeads || match.isLive) FontWeight.Bold else FontWeight.Medium,
                                color = if (guestLeads && !match.isLive) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        RollingScoreText(
                            text = hostScoreText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            fontWeight = FontWeight.Black,
                            color = when {
                                match.isLive -> primaryAccent
                                hostLeads -> MaterialTheme.colorScheme.onSurface
                                guestLeads -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }

                    // Guest row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TeamLogo(
                                logoUrl = match.guest.logoUrl,
                                teamName = match.guest.name,
                                size = 22.dp
                            )
                            Text(
                                text = match.guest.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (guestLeads || match.isLive) FontWeight.Bold else FontWeight.Medium,
                                color = if (hostLeads && !match.isLive) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        RollingScoreText(
                            text = guestScoreText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            fontWeight = FontWeight.Black,
                            color = when {
                                match.isLive -> primaryAccent
                                guestLeads -> MaterialTheme.colorScheme.onSurface
                                hostLeads -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteTeamButton(
    isFavorite: Boolean,
    teamName: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotionEnabled()

    val containerColor by animateColorAsState(
        targetValue = if (isFavorite) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(if (reducedMotion) 0 else 200),
        label = "favBtnBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFavorite) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline,
        animationSpec = tween(if (reducedMotion) 0 else 200),
        label = "favBtnBorder"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggle()
        },
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .semantics {
                contentDescription = if (isFavorite) {
                    "حذف $teamName از علاقه‌مندی"
                } else {
                    "افزودن $teamName به علاقه‌مندی"
                }
            }
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            if (reducedMotion) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                AnimatedContent(
                    targetState = isFavorite,
                    transitionSpec = {
                        (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(180)))
                            .togetherWith(scaleOut(tween(120)) + fadeOut(tween(120)))
                    },
                    label = "favIconToggle"
                ) { fav ->
                    Icon(
                        imageVector = if (fav) Icons.Rounded.Star else Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = if (fav) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

enum class MatchSkeletonVariant {
    LIST_ROWS,
    LEAGUE_BOARD,
    HERO_CARD
}

@Composable
fun rememberBroadcastShimmerBrush(): Brush {
    val reducedMotion = rememberReducedMotionEnabled()
    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
    val highlightColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
    if (reducedMotion) {
        return remember(baseColor) {
            Brush.linearGradient(listOf(baseColor, baseColor))
        }
    }
    val transition = rememberInfiniteTransition(label = "broadcastUnifiedShimmer")
    val progress by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(progress * 520f - 180f, 0f),
        end = Offset(progress * 520f + 180f, 200f)
    )
}

@Composable
fun MatchSkeletonLoader(
    variant: MatchSkeletonVariant = MatchSkeletonVariant.LIST_ROWS,
    modifier: Modifier = Modifier
) {
    val shimmerBrush = rememberBroadcastShimmerBrush()

    when (variant) {
        MatchSkeletonVariant.HERO_CARD -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(shimmerBrush)
                    .testTag("match_skeleton_loader")
            )
        }

        MatchSkeletonVariant.LIST_ROWS -> {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("match_skeleton_loader"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column {
                    repeat(4) { idx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(shimmerBrush)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.35f)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(shimmerBrush)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.78f)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(shimmerBrush)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(shimmerBrush)
                            )
                        }
                        if (idx < 3) {
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

        MatchSkeletonVariant.LEAGUE_BOARD -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("match_skeleton_loader"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(2) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(126.dp)
                                    .height(11.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(shimmerBrush)
                            )
                        }
                        repeat(3) {
                            HorizontalDivider(
                                thickness = 0.6.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(34.dp)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(shimmerBrush)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.62f)
                                            .height(11.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(shimmerBrush)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.54f)
                                            .height(11.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(shimmerBrush)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(shimmerBrush)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(shimmerBrush)
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
