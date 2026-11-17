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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class SportsSpacingTokens(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val screenHorizontal: Dp = 16.dp,
    val cardPadding: Dp = 14.dp
)

val LocalSportsSpacing = staticCompositionLocalOf { SportsSpacingTokens() }

val MaterialTheme.sportsSpacing: SportsSpacingTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalSportsSpacing.current

private val BroadcastDarkColorScheme = darkColorScheme(
    primary = ElectricLime,
    onPrimary = Color(0xFF091203),
    primaryContainer = ElectricLimeContainerDark,
    onPrimaryContainer = OnElectricLimeContainerDark,
    secondary = BroadcastCyan,
    onSecondary = Color(0xFF041E2B),
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
    outlineVariant = Color(0xFF1F2733),
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
    surfaceContainerHigh = Color(0xFFF0F4F8),
    surfaceContainerHighest = DaylightSurfaceVariant,
    outline = DaylightBorder,
    outlineVariant = Color(0xFFE4EAF2),
    error = LiveCrimsonLight,
    onError = Color.White
)

val KhateBaziShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
private fun animatedColor(targetValue: Color): Color {
    val color by animateColorAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = 320),
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

    CompositionLocalProvider(LocalSportsSpacing provides SportsSpacingTokens()) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = KhateBaziTypography,
            shapes = KhateBaziShapes,
            content = content
        )
    }
}
