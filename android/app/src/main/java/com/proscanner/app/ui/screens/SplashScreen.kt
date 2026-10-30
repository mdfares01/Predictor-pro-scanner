package com.proscanner.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proscanner.app.R
import com.proscanner.app.ui.components.ProLogo
import com.proscanner.app.ui.theme.NeonCyan
import com.proscanner.app.ui.theme.NeonGreen
import com.proscanner.app.ui.theme.PitchBlack
import com.proscanner.app.ui.theme.TextMuted
import com.proscanner.app.ui.theme.TextPrimary
import com.proscanner.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var progressTarget by remember { mutableStateOf(0.1f) }
    val progress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(1800, easing = LinearEasing),
        label = "splash_progress"
    )

    LaunchedEffect(Unit) {
        progressTarget = 1.0f
        delay(2200)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Glowing PRO Logo
            ProLogo(size = 150.dp, animated = true)

            Spacer(modifier = Modifier.height(48.dp))

            // App Name with highlighted "PRO"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Floating ",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "PRO ",
                    color = NeonGreen,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Scanner",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.tagline),
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.sub_tagline),
                color = TextMuted,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = NeonGreen,
                trackColor = Color(0xFF14201A)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.initializing),
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}
