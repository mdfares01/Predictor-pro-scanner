package com.proscanner.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proscanner.app.R
import com.proscanner.app.ui.theme.CardBackground
import com.proscanner.app.ui.theme.NeonGreen
import com.proscanner.app.ui.theme.PitchBlack
import com.proscanner.app.ui.theme.TextPrimary
import com.proscanner.app.ui.theme.TextSecondary

@Composable
fun FloatingBubbleOverlayContent(
    isScanning: Boolean,
    onBubbleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable { onBubbleClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        ProLogo(size = 56.dp, animated = isScanning)
    }
}

@Composable
fun CollapsedPillBar(
    onOpenClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(CardBackground)
            .border(1.5.dp, NeonGreen.copy(alpha = 0.8f), RoundedCornerShape(32.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProLogo(size = 32.dp, animated = false)
            Spacer(modifier = Modifier.width(10.dp))
            Row(
                modifier = Modifier.clickable { onOpenClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pro_scanner),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "• ${stringResource(R.string.tap_to_open)}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = TextSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onSettingsClick() }
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = TextSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onCloseClick() }
            )
        }
    }
}
