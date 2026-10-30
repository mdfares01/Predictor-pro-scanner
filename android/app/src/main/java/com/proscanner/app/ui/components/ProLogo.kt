package com.proscanner.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proscanner.app.ui.theme.NeonCyan
import com.proscanner.app.ui.theme.NeonGreen
import com.proscanner.app.ui.theme.PitchBlack

@Composable
fun ProLogo(
    size: Dp = 120.dp,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pro_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scaleModifier = if (animated) Modifier.size(size * pulseScale) else Modifier.size(size)

    Box(
        modifier = scaleModifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer concentric rings
        Canvas(modifier = Modifier.size(size)) {
            val centerOffset = center
            val radius = size.toPx() / 2f

            // Outer ring with glow gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonGreen.copy(alpha = 0.35f), Color.Transparent),
                    center = centerOffset,
                    radius = radius
                ),
                radius = radius
            )

            // Tech ticks/arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(NeonGreen, NeonCyan, NeonGreen)
                ),
                startAngle = if (animated) rotationAngle else 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Inner circle border
            drawCircle(
                color = NeonGreen.copy(alpha = 0.5f),
                radius = radius * 0.78f,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Inner glowing core
        Box(
            modifier = Modifier
                .size(size * 0.68f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF0F261E), PitchBlack)
                    )
                )
                .border(2.dp, NeonGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PRO",
                color = NeonGreen,
                fontSize = (size.value * 0.22f).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.5.sp
            )
        }
    }
}
