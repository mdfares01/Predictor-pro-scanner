package com.proscanner.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proscanner.app.R
import com.proscanner.app.data.model.ScanResult
import com.proscanner.app.ui.theme.CardElevated
import com.proscanner.app.ui.theme.DarkBackground
import com.proscanner.app.ui.theme.NeonGreen
import com.proscanner.app.ui.theme.PitchBlack
import com.proscanner.app.ui.theme.SignalDown
import com.proscanner.app.ui.theme.TextMuted
import com.proscanner.app.ui.theme.TextPrimary
import com.proscanner.app.ui.theme.TextSecondary

@Composable
fun FloatingPanelOverlayContent(
    isScanning: Boolean,
    scanResult: ScanResult?,
    errorMessage: String?,
    onScanClick: () -> Unit,
    onStopClick: () -> Unit,
    onCollapseClick: () -> Unit,
    onCloseAppClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Interaction sources for instantaneous, tactile pressed-state animations
    val scanInteraction = remember { MutableInteractionSource() }
    val isScanPressed by scanInteraction.collectIsPressedAsState()
    val scanScale by animateFloatAsState(if (isScanPressed) 0.95f else 1f, label = "scan_scale")

    val settingsInteraction = remember { MutableInteractionSource() }
    val isSettingsPressed by settingsInteraction.collectIsPressedAsState()
    val settingsScale by animateFloatAsState(if (isSettingsPressed) 0.95f else 1f, label = "settings_scale")

    val minInteraction = remember { MutableInteractionSource() }
    val isMinPressed by minInteraction.collectIsPressedAsState()

    val closeInteraction = remember { MutableInteractionSource() }
    val isClosePressed by closeInteraction.collectIsPressedAsState()

    // Matches the WindowManager window bounds without transparent padding dead zones
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkBackground.copy(alpha = 0.98f))
            .border(1.5.dp, NeonGreen.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header bar: Dedicated visual drag handle on the left, window controls on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Header drag indicator area (handled smoothly by DraggablePanelLayout)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProLogo(size = 28.dp, animated = isScanning)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.pro_scanner),
                            color = NeonGreen,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Hold & drag to move",
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                // Window Controls: Minimize and Close with immediate visual feedback and accessible 48dp touch targets
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isMinPressed) Color(0xFF1E2830) else Color.Transparent)
                            .clickable(interactionSource = minInteraction, indication = null) {
                                onCollapseClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Minimize",
                            tint = if (isMinPressed) NeonGreen else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isClosePressed) Color(0xFF331618) else Color.Transparent)
                            .clickable(interactionSource = closeInteraction, indication = null) {
                                onCloseAppClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isClosePressed) SignalDown else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action toolbar: [Scan] and [Settings] buttons with instant pressed feedback & 48dp height
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { if (!isScanning) onScanClick() },
                    interactionSource = scanInteraction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanPressed) Color(0xFF00C77D) else NeonGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .graphicsLayer {
                            scaleX = scanScale
                            scaleY = scanScale
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = PitchBlack,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.scan),
                        color = PitchBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = { onSettingsClick() },
                    interactionSource = settingsInteraction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary,
                        containerColor = if (isSettingsPressed) Color(0xFF1E2830) else Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .graphicsLayer {
                            scaleX = settingsScale
                            scaleY = settingsScale
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.nav_settings),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Body: Scanning mode vs Result view vs Error vs Idle prompt
            if (isScanning) {
                // Scanning animation mode
                ScanningRadar(size = 130.dp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.scanning_status),
                    color = NeonGreen,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.scanning_desc),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // STOP button with pressed state animation
                val stopInteraction = remember { MutableInteractionSource() }
                val isStopPressed by stopInteraction.collectIsPressedAsState()
                val stopScale by animateFloatAsState(if (isStopPressed) 0.95f else 1f, label = "stop_scale")

                OutlinedButton(
                    onClick = { onStopClick() },
                    interactionSource = stopInteraction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SignalDown,
                        containerColor = if (isStopPressed) Color(0xFF331618) else Color.Transparent
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SignalDown)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(48.dp)
                        .graphicsLayer {
                            scaleX = stopScale
                            scaleY = stopScale
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        tint = SignalDown,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.stop),
                        color = SignalDown,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            } else if (scanResult != null) {
                // Analysis Result display
                ResultCardView(result = scanResult)
            } else if (errorMessage != null) {
                // Error card with clear, tactile Retry button
                val retryInteraction = remember { MutableInteractionSource() }
                val isRetryPressed by retryInteraction.collectIsPressedAsState()
                val retryScale by animateFloatAsState(if (isRetryPressed) 0.95f else 1f, label = "retry_scale")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardElevated)
                        .border(1.dp, SignalDown.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage,
                            color = SignalDown,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onScanClick() },
                            interactionSource = retryInteraction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRetryPressed) Color(0xFF00C77D) else NeonGreen
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(48.dp)
                                .graphicsLayer {
                                    scaleX = retryScale
                                    scaleY = retryScale
                                }
                        ) {
                            Text(
                                text = stringResource(R.string.retry),
                                color = PitchBlack,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Idle prompt
                Text(
                    text = "Tap 'Scan' to capture chart and analyze with Gemini AI.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 14.dp)
                )
            }

            // Footer branding
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "✦ ${stringResource(R.string.powered_by_gemini)}",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}
