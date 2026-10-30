package com.proscanner.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.proscanner.app.ui.theme.NeonGreen

@Composable
fun ScanningRadar(
    size: Dp = 140.dp
) {
    val transition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val centerOffset = center
            val radius = size.toPx() / 2f

            // Radar circular grid rings
            drawCircle(
                color = NeonGreen.copy(alpha = 0.2f),
                radius = radius,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = NeonGreen.copy(alpha = 0.25f),
                radius = radius * 0.65f,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = NeonGreen.copy(alpha = 0.35f),
                radius = radius * 0.3f,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Crosshair lines
            drawLine(
                color = NeonGreen.copy(alpha = 0.25f),
                start = Offset(centerOffset.x, 0f),
                end = Offset(centerOffset.x, size.toPx()),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = NeonGreen.copy(alpha = 0.25f),
                start = Offset(0f, centerOffset.y),
                end = Offset(size.toPx(), centerOffset.y),
                strokeWidth = 1.dp.toPx()
            )

            // Rotating sweep gradient beam
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.7f to Color.Transparent,
                    1.0f to NeonGreen.copy(alpha = 0.45f),
                    center = centerOffset
                ),
                startAngle = sweepAngle - 90f,
                sweepAngle = 90f,
                useCenter = true
            )

            // Glowing center blip
            drawCircle(
                color = NeonGreen,
                radius = 4.dp.toPx(),
                center = centerOffset
            )
        }
    }
}
