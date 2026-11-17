package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay

/**
 * Checks Android accessibility animator duration scale to respect user reduced-motion preferences.
 */
@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Throwable) {
            false
        }
    }
}

/**
 * Provides tactile spring press scale + ripple + light haptic feedback.
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.965f,
    enableHaptics: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val reducedMotion = rememberReducedMotionEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reducedMotion) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncyScale"
    )

    this
        .then(
            if (reducedMotion) Modifier
            else Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        )
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true),
            enabled = enabled,
            onClick = {
                if (enableHaptics) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            }
        )
}

/**
 * Staggered reveal animation for list items and cards.
 */
@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    maxStaggerItems: Int = 8,
    content: @Composable () -> Unit
) {
    val reducedMotion = rememberReducedMotionEnabled()
    if (reducedMotion || index > maxStaggerItems) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val clampedIndex = index.coerceIn(0, maxStaggerItems)
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        delay(clampedIndex * 32L)
        alpha.animateTo(1f, animationSpec = tween(240))
    }
    LaunchedEffect(Unit) {
        delay(clampedIndex * 32L)
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            this.translationY = offsetY.value
        }
    ) {
        content()
    }
}

/**
 * Smooth rolling counter / score digit transition.
 */
@Composable
fun RollingScoreText(
    text: String,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    modifier: Modifier = Modifier
) {
    if (rememberReducedMotionEnabled()) {
        Text(
            text = text,
            style = style,
            color = color,
            fontWeight = fontWeight,
            modifier = modifier
        )
        return
    }

    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically(tween(220)) { -it / 2 } + fadeIn(tween(180)))
                .togetherWith(slideOutVertically(tween(180)) { it / 2 } + fadeOut(tween(150)))
        },
        label = "rollingScore",
        modifier = modifier
    ) { target ->
        Text(
            text = target,
            style = style,
            color = color,
            fontWeight = fontWeight
        )
    }
}

@Composable
fun SmoothFadeVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reducedMotion = rememberReducedMotionEnabled()
    AnimatedVisibility(
        visible = visible,
        enter = if (reducedMotion) EnterTransition.None else fadeIn(tween(220)) + slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) { it / 6 },
        exit = if (reducedMotion) ExitTransition.None else fadeOut(tween(160)) + slideOutVertically(tween(160)) { it / 6 },
        modifier = modifier
    ) {
        content()
    }
}
