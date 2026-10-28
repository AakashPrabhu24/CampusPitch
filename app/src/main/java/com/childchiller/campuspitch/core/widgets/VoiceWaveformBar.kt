package com.childchiller.campuspitch.core.widgets

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.childchiller.campuspitch.core.theme.CyanAccent
import com.childchiller.campuspitch.core.theme.EmeraldLight
import com.childchiller.campuspitch.core.theme.EmeraldPrimary

/**
 * Animated voice waveform visualizer responding to mic activity.
 * Standout design touch featuring modern neon glow gradients.
 */
@Composable
fun VoiceWaveformBar(
    isListening: Boolean,
    audioLevel: Float = 0.5f,
    modifier: Modifier = Modifier,
    barCount: Int = 12,
    maxHeight: Dp = 56.dp,
    minHeight: Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p1"
    )

    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p2"
    )

    val pulse3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p3"
    )

    val gradient = Brush.verticalGradient(
        colors = listOf(CyanAccent, EmeraldLight, EmeraldPrimary)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val factor = when (i % 3) {
                0 -> pulse1
                1 -> pulse2
                else -> pulse3
            }
            val activeHeight = if (isListening) {
                val dynamic = (minHeight + (maxHeight - minHeight) * factor * (audioLevel.coerceIn(0.2f, 1f)))
                dynamic
            } else {
                minHeight
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(activeHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(gradient)
            )
        }
    }
}
