package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameProfile
import com.example.ui.theme.*
import com.example.ui.viewmodel.PerformanceViewModel
import com.example.util.VoiceProfile

@Composable
fun SidebarAndFloatingWindows(
    viewModel: PerformanceViewModel,
    allProfiles: List<GameProfile>
) {
    val context = LocalContext.current
    val sidebarEnabled by viewModel.sidebarEnabled.collectAsState()
    var isSidebarOpen by remember { mutableStateOf(false) }
    var isCrosshairActive by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Optional FPS Precision Crosshair Reticle
        if (isCrosshairActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                // Crosshair Center Dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NeonGreen)
                        .border(1.dp, Color.Black, CircleShape)
                )
                // Crosshair Ring
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(1.5.dp, NeonGreen.copy(alpha = 0.8f), CircleShape)
                )
            }
        }

        // Sidebar Trigger & Drawer if enabled in settings
        if (sidebarEnabled) {
            // Screen Left Edge Swipe Detection Zone
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(28.dp)
                    .align(Alignment.CenterStart)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount.x > 8) {
                                    isSidebarOpen = true
                                }
                            }
                        )
                    }
                    .background(Color.Transparent)
            )

            // Sleek Game Turbo handle at left edge
            Box(
                modifier = Modifier
                    .padding(top = 110.dp)
                    .size(width = 16.dp, height = 76.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.85f), NeonCyan)
                        )
                    )
                    .clickable { isSidebarOpen = true }
                    .border(
                        1.dp,
                        NeonCyan.copy(alpha = 0.5f),
                        RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
                    )
                    .testTag("game_turbo_sidebar_handle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Buka Game Turbo HUD",
                    tint = DarkBackground,
                    modifier = Modifier.size(12.dp)
                )
            }

            // Game Turbo HUD Drawer Modal
            if (isSidebarOpen) {
                // Dimmed Backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { isSidebarOpen = false }
                )

                // Sidebar Drawer Panel
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 340.dp)
                        .fillMaxWidth(0.85f)
                        .align(Alignment.CenterStart)
                        .background(DarkBackground)
                        .border(
                            1.dp,
                            DarkBorder,
                            RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 18.dp)
                ) {
                    GameTurboDrawerContent(
                        viewModel = viewModel,
                        allProfiles = allProfiles,
                        isCrosshairActive = isCrosshairActive,
                        onToggleCrosshair = { isCrosshairActive = !isCrosshairActive },
                        onClose = { isSidebarOpen = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun GameTurboDrawerContent(
    viewModel: PerformanceViewModel,
    allProfiles: List<GameProfile>,
    isCrosshairActive: Boolean,
    onToggleCrosshair: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val fps by viewModel.fps.collectAsState()
    val pingHistory by viewModel.pingHistory.collectAsState()
    val currentPing = pingHistory.lastOrNull() ?: 12
    val batteryTemp by viewModel.batteryTemp.collectAsState()
    val wifiTurboSelected by viewModel.wifiTurboSelected.collectAsState()

    // Voice Engine & Noise Reduction States
    val isVoiceActive by viewModel.isVoiceEngineActive.collectAsState()
    val isLiveMonitoring by viewModel.isLiveMonitoring.collectAsState()
    val isNoiseSuppression by viewModel.isNoiseSuppressionEnabled.collectAsState()
    val noiseGateLevel by viewModel.noiseGateThresholdLevel.collectAsState()
    val activeVoiceProfile by viewModel.activeVoiceProfile.collectAsState()
    val micDb by viewModel.micDecibel.collectAsState()

    // FPS Stabilizer states
    val fpsStabilizerActive by viewModel.fpsStabilizerActive.collectAsState()
    val targetFps by viewModel.targetFps.collectAsState()
    val stutterCount by viewModel.stutterCount.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Header: Game Turbo Pro HUD ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "GAME TURBO HUD",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = PureWhite,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "10000% Max Boost Engine",
                            fontSize = 9.sp,
                            color = NeonGreen
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = MutedSlate
                    )
                }
            }
        }

        // --- Live Game Telemetry Pill (FPS, Ping, Temp) ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceSlate)
                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // FPS
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$fps", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                    Text(text = "FPS GAME", fontSize = 8.sp, color = MutedSlate, fontWeight = FontWeight.SemiBold)
                }
                VerticalDivider(modifier = Modifier.height(28.dp), color = DarkBorder)
                // Ping
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${currentPing}ms", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    Text(text = "LATENSI", fontSize = 8.sp, color = MutedSlate, fontWeight = FontWeight.SemiBold)
                }
                VerticalDivider(modifier = Modifier.height(28.dp), color = DarkBorder)
                // Temp
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${batteryTemp.toInt()}°C", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    Text(text = "SUHU HP", fontSize = 8.sp, color = MutedSlate, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // --- 1-TAP 10000% SUPER TURBO BOOST BUTTON ---
        item {
            Button(
                onClick = {
                    viewModel.boost10000PercentMax()
                    Toast.makeText(
                        context,
                        "🚀 TURBO 10000% DIAKTIFKAN! RAM dibersihkan, Jaringan dikunci, FPS distabilkan!",
                        Toast.LENGTH_LONG
                    ).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("sidebar_turbo_boost_btn"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonYellow)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "1-TAP 10000% SUPER BOOST",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // --- PEREDAM KEBISINGAN ON-MIC GAME ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isNoiseSuppression) NeonCyan.copy(alpha = 0.5f) else DarkBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎙️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "PEREDAM KEBISINGAN MIC",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNoiseSuppression) NeonCyan else PureWhite
                                )
                                Text(
                                    text = "Hilangkan suara kipas, angin & nafas",
                                    fontSize = 8.5.sp,
                                    color = MutedSlate
                                )
                            }
                        }
                        Switch(
                            checked = isNoiseSuppression,
                            onCheckedChange = { viewModel.setNoiseSuppression(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonCyan,
                                uncheckedThumbColor = MutedSlate,
                                uncheckedTrackColor = DarkBackground
                            )
                        )
                    }

                    if (isNoiseSuppression) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Level Kekuatan Peredam: $noiseGateLevel%",
                            fontSize = 8.5.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = noiseGateLevel.toFloat(),
                            onValueChange = { viewModel.setNoiseGateThreshold(it.toInt()) },
                            valueRange = 20f..95f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = DarkBorder
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Ringan", fontSize = 7.5.sp, color = MutedSlate)
                            Text(text = "Sedang (Ideal)", fontSize = 7.5.sp, color = NeonCyan)
                            Text(text = "Maksimal (Anti-Kipas)", fontSize = 7.5.sp, color = NeonGreen)
                        }
                    }
                }
            }
        }

        // --- PENGUBAH SUARA GAME (VOICE CHANGER) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isVoiceActive) NeonGreen.copy(alpha = 0.5f) else DarkBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎭", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "PENGUBAH SUARA GAME",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isVoiceActive) NeonGreen else PureWhite
                                )
                                Text(
                                    text = "Ubah suara on-mic saat mabar",
                                    fontSize = 8.5.sp,
                                    color = MutedSlate
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.toggleVoiceEngine(liveMonitor = !isLiveMonitoring)
                                Toast.makeText(
                                    context,
                                    if (!isVoiceActive) "Mic & Voice Changer aktif! Gunakan headset untuk mendengar efek." else "Voice Changer dimatikan.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVoiceActive) NeonGreen else DarkBackground
                            )
                        ) {
                            Text(
                                text = if (isVoiceActive) "AKTIF" else "TES MIC",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isVoiceActive) Color.Black else PureWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "PILIH EFEK SUARA:",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedSlate,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Preset Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(VoiceProfile.values()) { profile ->
                            val isSelected = activeVoiceProfile == profile
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkBackground)
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonCyan else DarkBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.setVoiceProfile(profile)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = profile.icon, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = profile.title.take(12),
                                        fontSize = 8.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) NeonCyan else PureWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- STABILISASI FPS & ELIMINASI PATAH-PATAH ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (fpsStabilizerActive) NeonYellow.copy(alpha = 0.4f) else DarkBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎯", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "STABILISASI FPS GAME",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (fpsStabilizerActive) NeonYellow else PureWhite
                                )
                                Text(
                                    text = "Anti patah-patah & kunci frame pacing",
                                    fontSize = 8.5.sp,
                                    color = MutedSlate
                                )
                            }
                        }
                        Switch(
                            checked = fpsStabilizerActive,
                            onCheckedChange = { viewModel.toggleFpsStabilizer() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = NeonYellow,
                                uncheckedThumbColor = MutedSlate,
                                uncheckedTrackColor = DarkBackground
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(60, 90, 120).forEach { target ->
                            val isSel = targetFps == target
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) NeonYellow.copy(alpha = 0.2f) else DarkBackground)
                                    .border(1.dp, if (isSel) NeonYellow else DarkBorder, RoundedCornerShape(6.dp))
                                    .clickable { viewModel.setTargetFps(target) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$target FPS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) NeonYellow else MutedSlate
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- KUNCI JARINGAN 100% LANCAR (PAKSA STABIL) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (wifiTurboSelected) NeonGreen.copy(alpha = 0.5f) else DarkBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "PAKSA JARINGAN 100% STABIL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (wifiTurboSelected) NeonGreen else PureWhite
                            )
                            Text(
                                text = "Kunci Wi-Fi low latency & anti RTO",
                                fontSize = 8.5.sp,
                                color = MutedSlate
                            )
                        }
                    }
                    Switch(
                        checked = wifiTurboSelected,
                        onCheckedChange = { viewModel.toggleWifiTurboBoost() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonGreen,
                            uncheckedThumbColor = MutedSlate,
                            uncheckedTrackColor = DarkBackground
                        )
                    )
                }
            }
        }

        // --- FITUR CROSSHAIR AIM (BIDIKAN TENGAH LAYAR) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isCrosshairActive) NeonGreen.copy(alpha = 0.5f) else DarkBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎯", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "CROSSHAIR AIM (BIDIKAN)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCrosshairActive) NeonGreen else PureWhite
                            )
                            Text(
                                text = "Titik bidik presisi untuk game tembak",
                                fontSize = 8.5.sp,
                                color = MutedSlate
                            )
                        }
                    }
                    Switch(
                        checked = isCrosshairActive,
                        onCheckedChange = { onToggleCrosshair() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonGreen,
                            uncheckedThumbColor = MutedSlate,
                            uncheckedTrackColor = DarkBackground
                        )
                    )
                }
            }
        }

        // --- DAFTAR GAME CEPAT ---
        if (allProfiles.isNotEmpty()) {
            item {
                Text(
                    text = "LUNCURKAN GAME CEPAT:",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedSlate,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allProfiles) { game ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceSlate)
                                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    onClose()
                                    viewModel.startGamingBoost(game)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "▶ ${game.appName.take(12)}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonYellow
                            )
                        }
                    }
                }
            }
        }
    }
}
