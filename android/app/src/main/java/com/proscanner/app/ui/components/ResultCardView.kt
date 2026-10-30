package com.proscanner.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.proscanner.app.data.model.ScanResult
import com.proscanner.app.data.model.SignalType
import com.proscanner.app.ui.theme.CardBackground
import com.proscanner.app.ui.theme.CardElevated
import com.proscanner.app.ui.theme.NeonGreen
import com.proscanner.app.ui.theme.SignalDown
import com.proscanner.app.ui.theme.SignalNeutral
import com.proscanner.app.ui.theme.SignalUp
import com.proscanner.app.ui.theme.TextMuted
import com.proscanner.app.ui.theme.TextPrimary
import com.proscanner.app.ui.theme.TextSecondary

@Composable
fun ResultCardView(
    result: ScanResult,
    modifier: Modifier = Modifier
) {
    val signalColor = when (result.signal) {
        SignalType.UP -> SignalUp
        SignalType.DOWN -> SignalDown
        SignalType.NO_TRADE -> SignalNeutral
    }

    val signalTitle = when (result.signal) {
        SignalType.UP -> stringResource(R.string.signal_up)
        SignalType.DOWN -> stringResource(R.string.signal_down)
        SignalType.NO_TRADE -> stringResource(R.string.signal_no_trade)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, signalColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Status header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.signal_ready),
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = stringResource(R.string.analysis_completed),
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Big Signal Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardElevated)
                .border(1.dp, signalColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Signal Icon Badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(signalColor.copy(alpha = 0.15f))
                        .border(1.5.dp, signalColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when (result.signal) {
                        SignalType.UP -> Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "UP",
                            tint = signalColor,
                            modifier = Modifier.size(26.dp)
                        )
                        SignalType.DOWN -> Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "DOWN",
                            tint = signalColor,
                            modifier = Modifier.size(26.dp)
                        )
                        SignalType.NO_TRADE -> Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "NO TRADE",
                            tint = signalColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = signalTitle,
                        color = signalColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = stringResource(R.string.confidence_label, result.confidence),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { result.confidence / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = signalColor,
                        trackColor = Color(0xFF1E2830)
                    )
                }
            }
        }

        // Pair & Timeframe
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${result.pair}  |  ${result.timeframe}",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }

        // Visible Evidence / Analysis Summary
        val displayEvidence = if (result.evidence.isNotEmpty()) result.evidence else result.summary
        if (displayEvidence.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.evidence_title),
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            displayEvidence.forEach { point ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(text = "• ", color = NeonGreen, fontSize = 12.sp)
                    Text(text = point, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        // Reasoning
        val displayReasoning = if (result.reasoning.isNotBlank()) result.reasoning else result.keyReason
        if (displayReasoning.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.key_reason, displayReasoning),
                color = TextPrimary,
                fontSize = 12.sp
            )
        }

        // Uncertainty & Missing Data
        if (result.uncertainty.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.uncertainty_title),
                    color = Color(0xFF64B5F6),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            result.uncertainty.forEach { item ->
                Row(modifier = Modifier.padding(vertical = 1.dp)) {
                    Text(text = "• ", color = Color(0xFF64B5F6), fontSize = 11.sp)
                    Text(text = item, color = TextSecondary, fontSize = 11.sp)
                }
            }
        }

        // Risks
        if (result.risks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFFB800),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.risks_title),
                    color = Color(0xFFFFB800),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            result.risks.forEach { risk ->
                Row(modifier = Modifier.padding(vertical = 1.dp)) {
                    Text(text = "• ", color = Color(0xFFFFB800), fontSize = 11.sp)
                    Text(text = risk, color = TextSecondary, fontSize = 11.sp)
                }
            }
        }

        // Disclaimer
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = result.disclaimer,
            color = TextMuted,
            fontSize = 10.sp,
            lineHeight = 13.sp
        )
    }
}
