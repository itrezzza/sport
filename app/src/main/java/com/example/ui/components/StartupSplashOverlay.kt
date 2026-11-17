package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BroadcastCyan
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.InkMutedDark
import com.example.ui.theme.InkWhite

@Composable
fun StartupSplashOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotionEnabled()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(if (reducedMotion) 0 else 180)),
        exit = fadeOut(tween(if (reducedMotion) 0 else 400)) + scaleOut(
            targetScale = if (reducedMotion) 1f else 1.04f,
            animationSpec = tween(if (reducedMotion) 0 else 400)
        ),
        modifier = modifier
    ) {
        val pulse: Float
        val ringAlpha: Float
        if (reducedMotion) {
            pulse = 1f
            ringAlpha = 0.25f
        } else {
            val transition = rememberInfiniteTransition(label = "startupSplash")
            val animatedPulse by transition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.06f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1050, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "crestPulse"
            )
            val animatedAlpha by transition.animateFloat(
                initialValue = 0.14f,
                targetValue = 0.36f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1050, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ringAlpha"
            )
            pulse = animatedPulse
            ringAlpha = animatedAlpha
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF152010),
                            Color(0xFF0E141C),
                            Color(0xFF0B0F14)
                        )
                    )
                )
                .testTag("app_startup_loading"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width * 0.5f, size.height * 0.44f)
                drawCircle(
                    color = ElectricLime.copy(alpha = ringAlpha * 0.28f),
                    radius = size.minDimension * 0.34f * pulse,
                    center = center,
                    style = Stroke(width = 1.8.dp.toPx())
                )
                drawCircle(
                    color = BroadcastCyan.copy(alpha = 0.08f),
                    radius = size.minDimension * 0.52f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LoadingPitchLottie(size = 128.dp)
                    Canvas(
                        modifier = Modifier
                            .size(66.dp)
                            .graphicsLayer {
                                scaleX = pulse
                                scaleY = pulse
                            }
                    ) {
                        val corner = size.minDimension * 0.28f
                        drawRoundRect(
                            brush = Brush.linearGradient(listOf(ElectricLime, Color(0xFF9ED420))),
                            cornerRadius = CornerRadius(corner, corner)
                        )
                        val cx = size.width * 0.5f
                        val cy = size.height * 0.5f
                        val playPath = Path().apply {
                            moveTo(cx - size.width * 0.14f, cy - size.height * 0.19f)
                            lineTo(cx + size.width * 0.21f, cy)
                            lineTo(cx - size.width * 0.14f, cy + size.height * 0.19f)
                            close()
                        }
                        drawPath(playPath, color = Color(0xFF091203))
                    }
                }

                Text(
                    text = "خط بازی",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkWhite
                )

                Text(
                    text = "پلتفرم پخش زنده، نتایج و تحلیل ورزشی",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMutedDark
                )

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    color = ElectricLime,
                    trackColor = Color.White.copy(alpha = 0.10f),
                    modifier = Modifier
                        .width(168.dp)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(99.dp))
                )
            }
        }
    }
}
