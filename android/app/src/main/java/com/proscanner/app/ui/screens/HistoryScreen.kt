package com.proscanner.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.proscanner.app.data.model.ScanResult
import com.proscanner.app.data.model.SignalType
import com.proscanner.app.ui.components.ResultCardView
import com.proscanner.app.ui.theme.CardBackground
import com.proscanner.app.ui.theme.DarkBackground
import com.proscanner.app.ui.theme.NeonGreen
import com.proscanner.app.ui.theme.PitchBlack
import com.proscanner.app.ui.theme.SignalDown
import com.proscanner.app.ui.theme.SignalNeutral
import com.proscanner.app.ui.theme.SignalUp
import com.proscanner.app.ui.theme.TextMuted
import com.proscanner.app.ui.theme.TextPrimary
import com.proscanner.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    scans: List<ScanResult>,
    onNavigateBack: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf<SignalType?>(null) }
    var expandedResult by remember { mutableStateOf<ScanResult?>(null) }

    val filteredList = remember(scans, selectedFilter) {
        if (selectedFilter == null) scans else scans.filter { it.signal == selectedFilter }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.recent_analysis),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkBackground
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Filter Pills: ALL, UP, DOWN, NO TRADE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        label = "ALL",
                        isSelected = selectedFilter == null,
                        color = NeonGreen,
                        onClick = { selectedFilter = null }
                    )
                    FilterChip(
                        label = "UP",
                        isSelected = selectedFilter == SignalType.UP,
                        color = SignalUp,
                        onClick = { selectedFilter = SignalType.UP }
                    )
                    FilterChip(
                        label = "DOWN",
                        isSelected = selectedFilter == SignalType.DOWN,
                        color = SignalDown,
                        onClick = { selectedFilter = SignalType.DOWN }
                    )
                    FilterChip(
                        label = "NO TRADE",
                        isSelected = selectedFilter == SignalType.NO_TRADE,
                        color = SignalNeutral,
                        onClick = { selectedFilter = SignalType.NO_TRADE }
                    )
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No analysis history found.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(filteredList) { scan ->
                    Column {
                        Box(modifier = Modifier.clickable {
                            expandedResult = if (expandedResult?.id == scan.id) null else scan
                        }) {
                            RecentScanItem(scan = scan)
                        }
                        if (expandedResult?.id == scan.id) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ResultCardView(result = scan)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) color.copy(alpha = 0.2f) else CardBackground)
            .border(
                1.dp,
                if (isSelected) color else Color(0xFF1E2830),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) color else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
