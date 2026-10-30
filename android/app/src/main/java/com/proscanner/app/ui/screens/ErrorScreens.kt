package com.proscanner.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proscanner.app.R
import com.proscanner.app.ui.theme.DarkBackground
import com.proscanner.app.ui.theme.NeonGreen
import com.proscanner.app.ui.theme.PitchBlack
import com.proscanner.app.ui.theme.SignalDown
import com.proscanner.app.ui.theme.TextMuted
import com.proscanner.app.ui.theme.TextPrimary
import com.proscanner.app.ui.theme.TextSecondary

enum class ErrorType {
    PERMISSION_DENIED,
    NO_CHART_DETECTED,
    NETWORK_FAILURE,
    AI_FAILURE
}

@Composable
fun FriendlyErrorScreen(
    errorType: ErrorType,
    onPrimaryAction: () -> Unit,
    onDismiss: () -> Unit
) {
    val (icon, title, desc, actionLabel) = when (errorType) {
        ErrorType.PERMISSION_DENIED -> Quad(
            Icons.Default.Block,
            stringResource(R.string.error_permission_title),
            stringResource(R.string.error_permission_desc),
            stringResource(R.string.grant)
        )
        ErrorType.NO_CHART_DETECTED -> Quad(
            Icons.Default.ImageNotSupported,
            stringResource(R.string.error_no_chart_title),
            stringResource(R.string.error_no_chart_desc),
            stringResource(R.string.retry)
        )
        ErrorType.NETWORK_FAILURE -> Quad(
            Icons.Default.CloudOff,
            stringResource(R.string.error_network_title),
            stringResource(R.string.error_network_desc),
            stringResource(R.string.retry)
        )
        ErrorType.AI_FAILURE -> Quad(
            Icons.Default.AutoAwesome,
            stringResource(R.string.error_ai_title),
            stringResource(R.string.error_ai_desc),
            stringResource(R.string.retry)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon in glowing dark circle
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF231316))
                    .border(2.dp, SignalDown.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SignalDown,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onPrimaryAction,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp)
            ) {
                Text(
                    text = actionLabel,
                    color = PitchBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    text = stringResource(R.string.close),
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
