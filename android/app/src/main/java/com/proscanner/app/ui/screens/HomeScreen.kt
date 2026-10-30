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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import com.proscanner.app.ui.components.ProLogo
import com.proscanner.app.ui.theme.CardBackground
import com.proscanner.app.ui.theme.CardElevated
import com.proscanner.app.ui.theme.DarkBackground
import com.proscanner.app.ui.theme.NeonGreen
import com.proscanner.app.ui.theme.PitchBlack
import com.proscanner.app.ui.theme.SignalDown
import com.proscanner.app.ui.theme.SignalNeutral
import com.proscanner.app.ui.theme.SignalUp
import com.proscanner.app.ui.theme.TextMuted
import com.proscanner.app.ui.theme.TextPrimary
import com.proscanner.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    isOverlayGranted: Boolean,
    isCaptureGranted: Boolean,
    isBackendOnline: Boolean,
    recentScans: List<ScanResult>,
    onStartScannerClick: () -> Unit,
    onRequestOverlayClick: () -> Unit,
    onRequestCaptureClick: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateHistory: () -> Unit
) {
    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            HomeBottomNav(
                currentRoute = "home",
                onNavigateHome = {},
                onNavigateHistory = onNavigateHistory,
                onNavigateSettings = onNavigateSettings
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                // Header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProLogo(size = 52.dp, animated = false)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Floating ",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "PRO ",
                            color = NeonGreen,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Scanner",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                    Text(
                        text = stringResource(R.string.tagline),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Status Card: Gemini AI Secure Backend
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, Color(0xFF1E2830), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0C241B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "✦", color = NeonGreen, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.gemini_ai),
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = stringResource(R.string.secure_backend),
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isBackendOnline) Color(0xFF0E3023) else Color(0xFF301518)
                                )
                                .border(
                                    1.dp,
                                    if (isBackendOnline) NeonGreen.copy(alpha = 0.5f) else SignalDown.copy(alpha = 0.5f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isBackendOnline) "✦ ${stringResource(R.string.connected)}" else stringResource(R.string.offline),
                                color = if (isBackendOnline) NeonGreen else SignalDown,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Permissions Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, Color(0xFF1E2830), RoundedCornerShape(16.dp))
                ) {
                    // Floating Button Row
                    PermissionRowItem(
                        icon = Icons.Default.Layers,
                        title = stringResource(R.string.floating_button),
                        subtitle = stringResource(R.string.floating_button_desc),
                        isGranted = isOverlayGranted,
                        grantedText = stringResource(R.string.active),
                        notGrantedText = stringResource(R.string.not_active),
                        onClick = onRequestOverlayClick
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF172026))
                    )

                    // Screen Capture Row
                    PermissionRowItem(
                        icon = Icons.Default.Screenshot,
                        title = stringResource(R.string.screen_capture),
                        subtitle = stringResource(R.string.screen_capture_desc),
                        isGranted = isCaptureGranted,
                        grantedText = stringResource(R.string.granted),
                        notGrantedText = stringResource(R.string.not_granted),
                        onClick = onRequestCaptureClick
                    )
                }
            }

            // START SCANNER BUTTON
            item {
                Button(
                    onClick = onStartScannerClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = PitchBlack
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = PitchBlack,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.start_scanner),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PitchBlack,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Recent Analysis Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recent_analysis),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = stringResource(R.string.view_all),
                        color = NeonGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onNavigateHistory() }
                    )
                }
            }

            // Recent Analysis List
            items(recentScans.take(3)) { scan ->
                RecentScanItem(scan = scan)
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun PermissionRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    grantedText: String,
    notGrantedText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF14201A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isGranted) Color(0xFF0F261E) else Color(0xFF281C1C)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isGranted) grantedText else notGrantedText,
                    color = if (isGranted) NeonGreen else SignalDown,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun RecentScanItem(scan: ScanResult) {
    val signalColor = when (scan.signal) {
        SignalType.UP -> SignalUp
        SignalType.DOWN -> SignalDown
        SignalType.NO_TRADE -> SignalNeutral
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = "Today, " + timeFormat.format(Date(scan.timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(1.dp, Color(0xFF172026), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pair avatar circle
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16232D)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = scan.pair.take(2),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${scan.pair}  ${scan.timeframe}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = formattedTime,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(signalColor.copy(alpha = 0.15f))
                    .border(1.dp, signalColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = scan.signal.name,
                    color = signalColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun HomeBottomNav(
    currentRoute: String,
    onNavigateHome: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    NavigationBar(
        containerColor = PitchBlack,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = onNavigateHome,
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text(stringResource(R.string.nav_home), fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonGreen,
                selectedTextColor = NeonGreen,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted,
                indicatorColor = Color(0xFF0F261E)
            )
        )
        NavigationBarItem(
            selected = currentRoute == "history",
            onClick = onNavigateHistory,
            icon = { Icon(Icons.Default.History, contentDescription = "History") },
            label = { Text(stringResource(R.string.nav_history), fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonGreen,
                selectedTextColor = NeonGreen,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted,
                indicatorColor = Color(0xFF0F261E)
            )
        )
        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = onNavigateSettings,
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text(stringResource(R.string.nav_settings), fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonGreen,
                selectedTextColor = NeonGreen,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted,
                indicatorColor = Color(0xFF0F261E)
            )
        )
    }
}
