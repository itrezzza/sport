package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BroadcastDarkColorScheme = darkColorScheme(
    primary = EmeraldNeon,
    onPrimary = Color(0xFF042416),
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = OnEmeraldContainerDark,
    secondary = BroadcastCyan,
    onSecondary = Color(0xFF062033),
    secondaryContainer = BroadcastCyanContainerDark,
    onSecondaryContainer = BroadcastCyan,
    tertiary = LiveCrimson,
    onTertiary = Color.White,
    tertiaryContainer = LiveCrimsonContainerDark,
    onTertiaryContainer = LiveCrimsonTextDark,
    background = MidnightBg,
    onBackground = InkWhite,
    surface = MidnightSurface,
    onSurface = InkWhite,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = InkMutedDark,
    surfaceContainer = MidnightSurface,
    surfaceContainerHigh = MidnightSurfaceVariant,
    surfaceContainerHighest = MidnightSurfaceElevated,
    outline = MidnightBorder,
    outlineVariant = Color(0xFF1F232C),
    error = LiveCrimson,
    onError = Color.White
)

private val EditorialLightColorScheme = lightColorScheme(
    primary = EmeraldLight,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = OnEmeraldContainerLight,
    secondary = BroadcastCyanLight,
    onSecondary = Color.White,
    secondaryContainer = BroadcastCyanContainerLight,
    onSecondaryContainer = BroadcastCyanLight,
    tertiary = LiveCrimsonLight,
    onTertiary = Color.White,
    tertiaryContainer = LiveCrimsonContainerLight,
    onTertiaryContainer = LiveCrimsonLight,
    background = DaylightBg,
    onBackground = InkDark,
    surface = DaylightSurface,
    onSurface = InkDark,
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = InkMutedLight,
    surfaceContainer = DaylightSurface,
    surfaceContainerHigh = Color(0xFFF3F5F8),
    surfaceContainerHighest = DaylightSurfaceVariant,
    outline = DaylightBorder,
    outlineVariant = Color(0xFFE6EAF0),
    error = LiveCrimsonLight,
    onError = Color.White
)

val KhateBaziShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
private fun animatedColor(targetValue: Color): Color {
    val color by animateColorAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = 360),
        label = "themeColor"
    )
    return color
}

@Composable
private fun rememberAnimatedColorScheme(target: ColorScheme): ColorScheme {
    return target.copy(
        primary = animatedColor(target.primary),
        onPrimary = animatedColor(target.onPrimary),
        primaryContainer = animatedColor(target.primaryContainer),
        onPrimaryContainer = animatedColor(target.onPrimaryContainer),
        secondary = animatedColor(target.secondary),
        onSecondary = animatedColor(target.onSecondary),
        secondaryContainer = animatedColor(target.secondaryContainer),
        onSecondaryContainer = animatedColor(target.onSecondaryContainer),
        tertiary = animatedColor(target.tertiary),
        onTertiary = animatedColor(target.onTertiary),
        tertiaryContainer = animatedColor(target.tertiaryContainer),
        onTertiaryContainer = animatedColor(target.onTertiaryContainer),
        background = animatedColor(target.background),
        onBackground = animatedColor(target.onBackground),
        surface = animatedColor(target.surface),
        onSurface = animatedColor(target.onSurface),
        surfaceVariant = animatedColor(target.surfaceVariant),
        onSurfaceVariant = animatedColor(target.onSurfaceVariant),
        surfaceContainer = animatedColor(target.surfaceContainer),
        surfaceContainerHigh = animatedColor(target.surfaceContainerHigh),
        surfaceContainerHighest = animatedColor(target.surfaceContainerHighest),
        outline = animatedColor(target.outline),
        outlineVariant = animatedColor(target.outlineVariant),
        error = animatedColor(target.error),
        onError = animatedColor(target.onError)
    )
}

@Composable
fun KhateBaziTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val targetScheme = if (darkTheme) BroadcastDarkColorScheme else EditorialLightColorScheme
    val animatedScheme = rememberAnimatedColorScheme(targetScheme)

    MaterialTheme(
        colorScheme = animatedScheme,
        typography = KhateBaziTypography,
        shapes = KhateBaziShapes,
        content = content
    )
}
