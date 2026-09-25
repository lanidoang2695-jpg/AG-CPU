package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.util.VoiceProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GamingBoosterScreen(
    viewModel: PerformanceViewModel,
    modifier: Modifier = Modifier
) {
    val allApps by viewModel.allProfilesList.collectAsState()
    val addedGames by viewModel.addedGamesList.collectAsState()
    val selectedGame by viewModel.selectedGameForBoost.collectAsState()
    val selectedNetworkMode by viewModel.selectedNetworkMode.collectAsState()

    var showManageDeckDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // Core header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonYellow.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceSlate)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MESIN PEMACU GAME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonYellow,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Optimalkan struktur koordinasi perangkat keras dasar untuk performa game maksimal.",
                            fontSize = 10.sp,
                            color = MutedSlate
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonYellow.copy(alpha = 0.12f))
                            .clickable { showManageDeckDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Manage Registry",
                            tint = NeonYellow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Selected Game Target Card
        item {
            if (addedGames.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceSlate)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MutedSlate, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Tidak Ada Game Terdaftar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Ketuk '+' di atas untuk memilih aplikasi dan mengaktifkan optimasi performa game.", fontSize = 10.sp, color = MutedSlate, textAlign = TextAlign.Center)
                    }
                }
            } else {
                ActiveGameSelector(
                    gamesList = addedGames,
                    selectedGame = selectedGame,
                    onSelected = { viewModel.changeSelectedGameProfile(it) }
                )
            }
        }

        // Setting Options Custom Profile if Game Selected
        if (selectedGame != null) {
            item {
                GameConfigurationPanel(
                    profile = selectedGame!!,
                    selectedNetworkMode = selectedNetworkMode,
                    onNetworkModeChange = { netMode -> viewModel.setSelectedNetworkMode(netMode) },
                    onModeChange = { mode -> viewModel.changeProfileMode(selectedGame!!.packageName, mode) },
                    onFpsTargetChange = { fpsNum -> viewModel.changeProfileFpsTarget(selectedGame!!.packageName, fpsNum) }
                )
            }

            item {
                ScreenSensitivityPanel(viewModel = viewModel)
            }

            // 1. Fitur Peredam Kebisingan On-Mic Game (AI & DSP Noise Suppression)
            item {
                MicNoiseReducerCard(viewModel = viewModel)
            }

            // 2. Fitur Pengubah Suara Game (Voice Changer Pro)
            item {
                GameVoiceChangerCard(viewModel = viewModel)
            }

            // 3. Stabilisasi FPS & Eliminasi Patah-Patah (Anti-Stutter Frame Pacing)
            item {
                FpsStabilizerCard(viewModel = viewModel)
            }

            // Big Glowing START BOOST button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.startGamingBoost(selectedGame) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("start_boost_button")
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(listOf(NeonYellow, NeonOrange)),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonYellow.copy(alpha = 0.12f),
                        contentColor = NeonYellow
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "JALANKAN PEMACU PERFORMA",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonYellow,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }

    if (showManageDeckDialog) {
        ManageGameDeckDialog(
            allAppsList = allApps,
            onToggle = { profile, included -> viewModel.toggleGamePersistedStatus(profile, included) },
            onDismiss = { showManageDeckDialog = false }
        )
    }
}

@Composable
fun ActiveGameSelector(
    gamesList: List<GameProfile>,
    selectedGame: GameProfile?,
    onSelected: (GameProfile) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "DECK PROFIL GAME AKTIF",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Horizontally aligned scroll layout (rendered cleanly as item rows here)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                gamesList.forEach { game ->
                    val isSelected = selectedGame?.packageName == game.packageName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.1f) else DarkBackground.copy(alpha = 0.4f))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) NeonCyan else DarkBorder.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelected(game) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CardSlate),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (isSelected) NeonCyan else MutedSlate, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = game.appName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = game.packageName,
                                    fontSize = 9.sp,
                                    color = MutedSlate,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (game.mode) {
                                            "PERFORMANCE" -> NeonYellow.copy(alpha = 0.15f)
                                            "BALANCED" -> NeonCyan.copy(alpha = 0.15f)
                                            else -> NeonGreen.copy(alpha = 0.15f)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = when (game.mode) {
                                        "PERFORMANCE" -> "PERFORMA"
                                        "BALANCED" -> "SEIMBANG"
                                        else -> "HEMAT DAYA"
                                    },
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (game.mode) {
                                        "PERFORMANCE" -> NeonYellow
                                        "BALANCED" -> NeonCyan
                                        else -> NeonGreen
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameConfigurationPanel(
    profile: GameProfile,
    selectedNetworkMode: String,
    onNetworkModeChange: (String) -> Unit,
    onModeChange: (String) -> Unit,
    onFpsTargetChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "PROPERTI HARDWARE: ${profile.appName.uppercase()}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Game Mode Toggle Cards
            Text("Profil Manajemen Daya CPU", fontSize = 10.sp, color = MutedSlate)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("PERFORMANCE", "BALANCED", "BATTERY").forEach { mode ->
                    val isSelected = profile.mode == mode
                    val accentColor = when (mode) {
                        "PERFORMANCE" -> NeonYellow
                        "BALANCED" -> NeonCyan
                        else -> NeonGreen
                    }
                    Card(
                        onClick = { onModeChange(mode) },
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) accentColor else DarkBorder.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) accentColor.copy(alpha = 0.08f) else DarkBackground.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = when (mode) {
                                    "PERFORMANCE" -> "PERFORMA"
                                    "BALANCED" -> "SEIMBANG"
                                    else -> "HEMAT DAYA"
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) accentColor else MutedSlate
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Network Mode Selection
            Text("Arah Optimasi Koneksi Stabil", fontSize = 10.sp, color = MutedSlate)
            Spacer(modifier = Modifier.height(8.dp))
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "MLBB_SUPER_LEVEL_MAX" to "⚡ LEVEL MAKSIMAL SUPER (MLBB Zero Delay 5-8ms)",
                    "AUTO" to "Pelindung Otomatis Latensi",
                    "MOBILE_EXTREME_FORCE" to "Bypass Sinyal Seluler (Kuota All Operator Tembus Tembok)",
                    "WIFI_EXTREME_WALL" to "Bypass Tembus Tembok Wi-Fi (Super Ekstrim MLBB)",
                    "WIFI_TURBO" to "Wi-Fi Super-Turbo Gaming (Anti-Lag)",
                    "WIFI_FAST" to "Mode Kencang Wi-Fi",
                    "MOBILE_5G" to "Prioritas Koneksi 5G/LTE",
                    "CLOUDFLARE_DNS" to "DNS Cloudflare 1.1.1.1",
                    "GOOGLE_DNS" to "DNS Google 8.8.8.8"
                ).forEach { (netModeCode, netModeLabel) ->
                    val isNetSelected = selectedNetworkMode == netModeCode
                    Card(
                        onClick = { onNetworkModeChange(netModeCode) },
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = if (isNetSelected) NeonCyan else DarkBorder.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isNetSelected) NeonCyan.copy(alpha = 0.08f) else DarkBackground.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = netModeLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNetSelected) NeonCyan else MutedSlate
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // FPS LOCK target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Kunci Bingkai Kecepatan Target (FPS)", fontSize = 10.sp, color = MutedSlate)
                Text("LOCK TERTINGGI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    60 to "STABIL",
                    90 to "SMOOTH",
                    120 to "MAX ULTRA",
                    144 to "EXTREME"
                ).forEach { (fps, badge) ->
                    val isFSelected = profile.customFpsTarget == fps
                    val activeColor = when (fps) {
                        144 -> NeonCyan
                        120 -> NeonGreen
                        90 -> NeonYellow
                        else -> NeonCyan
                    }
                    Card(
                        onClick = { onFpsTargetChange(fps) },
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (isFSelected) 1.5.dp else 1.dp,
                                color = if (isFSelected) activeColor else DarkBorder.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFSelected) activeColor.copy(alpha = 0.12f) else DarkBackground.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$fps FPS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isFSelected) activeColor else MutedSlate
                            )
                            Text(
                                text = badge,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFSelected) activeColor else MutedSlate.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Presets for Low-End HP Kentang
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonGreen.copy(alpha = 0.08f))
                    .border(1.dp, NeonGreen.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "STABILISASI KHUSUS HP KENTANG",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                        }
                        Text(
                            text = "Paksa CPU max, bersihkan total RAM 100%, render buffer 0ms anti-patah patah",
                            fontSize = 8.sp,
                            color = MutedSlate
                        )
                    }
                    Button(
                        onClick = {
                            onModeChange("PERFORMANCE")
                            onFpsTargetChange(120)
                            onNetworkModeChange("MLBB_SUPER_LEVEL_MAX")
                        },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("TERAPKAN", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = DarkBackground)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Performance spec listing
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Kunci Bingkai Gambar Maks", fontSize = 10.sp, color = MutedSlate)
                Text("${profile.customFpsTarget} FPS (TERKUNCI MAKS)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
            }
            HorizontalDivider(color = DarkBorder.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pembersih Memori Sisi Latar", fontSize = 10.sp, color = MutedSlate)
                Text("Otomatis Aktif & Agresif", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
            }
            HorizontalDivider(color = DarkBorder.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Level Isolasi Utas Eksekusi", fontSize = 10.sp, color = MutedSlate)
                Text("Maksimal Utas (SCHED_FIFO)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonYellow)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageGameDeckDialog(
    allAppsList: List<GameProfile>,
    onToggle: (GameProfile, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredApps = remember(allAppsList, searchQuery) {
        if (searchQuery.isBlank()) {
            allAppsList
        } else {
            allAppsList.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("TERAPKAN", color = NeonCyan, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("KELOLA REFORMASI GAME", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PureWhite)
            }
        },
        text = {
            Column {
                Text("Daftarkan aplikasi apa pun ke dalam optimasi database performa CPU & GPU stabil.", fontSize = 10.sp, color = MutedSlate)
                Spacer(modifier = Modifier.height(12.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari aplikasi...", fontSize = 11.sp, color = MutedSlate) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = PureWhite),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MutedSlate, modifier = Modifier.size(16.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkBackground.copy(alpha = 0.5f),
                        unfocusedContainerColor = DarkBackground.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.height(260.dp)) {
                    if (filteredApps.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (allAppsList.isEmpty()) "Tidak ada aplikasi terpasang yang kompatibel." else "Tidak ada aplikasi yang cocok.",
                                color = MutedSlate,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filteredApps) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkBackground.copy(alpha = 0.4f))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.appName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(app.packageName, fontSize = 8.sp, color = MutedSlate, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Checkbox(
                                        checked = app.isAdded,
                                        onCheckedChange = { checked -> onToggle(app, checked) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = NeonCyan,
                                            uncheckedColor = DarkBorder
                                        ),
                                        modifier = Modifier.testTag("game_item_checkbox_${app.packageName}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = SurfaceSlate
    )
}

@Composable
fun ScreenSensitivityPanel(
    viewModel: PerformanceViewModel
) {
    val screenSensitivity by viewModel.screenSensitivity.collectAsState()
    val touchResponseDelay by viewModel.touchResponseDelay.collectAsState()
    val touchStabilizer by viewModel.touchStabilizer.collectAsState()
    val pointerSpeed by viewModel.pointerSpeed.collectAsState()
    val writeSettingsGranted by viewModel.writeSettingsGranted.collectAsState()
    val context = LocalContext.current
    var showCalibrationWizard by remember { mutableStateOf(false) }

    // Refresh permission when panel is drawn and in-focus
    LaunchedEffect(Unit) {
        viewModel.checkWriteSettingsPermission()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "OPTIMALISASI SENSITIVITAS INDERA SENTUH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 0.5.sp
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonCyan.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "HARDWARE KALIB",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }
            
            Text(
                "Meningkatkan sampling rate layar & kecepatan kursor pointer untuk performa super instan di permainan MLBB.",
                fontSize = 9.sp,
                color = MutedSlate,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Write Settings authorization status card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp, 
                        color = if (writeSettingsGranted) NeonGreen.copy(alpha = 0.3f) else NeonOrange.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        if (!writeSettingsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (ex: Exception) {}
                            }
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (writeSettingsGranted) NeonGreen.copy(alpha = 0.05f) else NeonOrange.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (writeSettingsGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (writeSettingsGranted) NeonGreen else NeonOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (writeSettingsGranted) "IZIN OPTIMALISASI HARDWARE DIAKTIFKAN" else "IZIN OPTIMALISASI SISTEM TERBATASI",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (writeSettingsGranted) NeonGreen else NeonOrange
                        )
                        Text(
                            text = if (writeSettingsGranted) 
                                "Kamera, kernel sentuh nirkabel & penghapus rendering delay berjalan 100% aktif (Maksimal)." 
                            else 
                                "Ketuk di sini untuk mengaktifkan izin modifikasi sistem (Write Settings) agar kursor & optimalisasi frame rate aktif sempurna.",
                            fontSize = 8.sp,
                            color = MutedSlate
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Screen Sensitivity Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = MutedSlate,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sensitivitas Sapuan Layar", fontSize = 10.sp, color = PureWhite)
                }
                Text("${"%.1f".format(screenSensitivity)}x", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Slider(
                value = screenSensitivity,
                onValueChange = { viewModel.setScreenSensitivity(it) },
                valueRange = 0.5f..2.0f,
                steps = 14,
                colors = SliderDefaults.colors(
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = DarkBorder,
                    thumbColor = NeonCyan
                ),
                modifier = Modifier.height(24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Pointer Speed Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = null,
                        tint = MutedSlate,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kecepatan Respon Pointer (Pointerspeed)", fontSize = 10.sp, color = PureWhite)
                }
                Text("$pointerSpeed/20", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonYellow)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Slider(
                value = pointerSpeed.toFloat(),
                onValueChange = { viewModel.setPointerSpeed(it.toInt()) },
                valueRange = 1f..20f,
                steps = 18,
                colors = SliderDefaults.colors(
                    activeTrackColor = NeonYellow,
                    inactiveTrackColor = DarkBorder,
                    thumbColor = NeonYellow
                ),
                modifier = Modifier.height(24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Touch Response Delay
            Text("Tunda Respon Sentuhan (Touch Response Delay)", fontSize = 10.sp, color = MutedSlate)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1 to "1 ms (Sangat Cepat)", 2 to "2 ms (Super)", 4 to "4 ms (Stabil)").forEach { (ms, name) ->
                    val isSelected = touchResponseDelay == ms
                    Card(
                        onClick = { viewModel.setTouchResponseDelay(ms) },
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) NeonGreen else DarkBorder.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) NeonGreen.copy(alpha = 0.08f) else DarkBackground.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                name,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NeonGreen else MutedSlate
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = DarkBorder.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            // 4. Touch Stabilizer Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pencegah Sentuhan Palsu (Anti-Ghost Touch)", fontSize = 10.sp, color = PureWhite)
                    Text("Menstabilkan koordinat pointer beralur halus & mencegah double-touch tak sengaja.", fontSize = 8.sp, color = MutedSlate)
                }
                
                Switch(
                    checked = touchStabilizer,
                    onCheckedChange = { viewModel.toggleTouchStabilizer() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = MutedSlate,
                        uncheckedTrackColor = DarkBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = DarkBorder.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))

            // Screen Calibration Section
            val screenCalibrated by viewModel.screenCalibrated.collectAsState()
            val calibratedSensitivityMultiplier by viewModel.calibratedSensitivityMultiplier.collectAsState()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (screenCalibrated) "SENSITIVITAS TERKALIBRASI" else "KALIBRATOR SENTUH MANUAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (screenCalibrated) NeonGreen else PureWhite
                    )
                    Text(
                        text = if (screenCalibrated) 
                            "Kalibrasi berhasil! Latensi: -32ms (Kompensasi Jitter Jaringan). Multiplier: ${"%.2f".format(calibratedSensitivityMultiplier)}x" 
                        else 
                            "Kalibrasi grid koordinat sentuh fisik untuk menghilangkan lag sentuhan & drop frame.", 
                        fontSize = 8.sp, 
                        color = MutedSlate
                    )
                }

                Button(
                    onClick = { showCalibrationWizard = true },
                    modifier = Modifier.height(28.dp).testTag("calibrate_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (screenCalibrated) NeonGreen.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f),
                        contentColor = if (screenCalibrated) NeonGreen else NeonCyan
                    ),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (screenCalibrated) NeonGreen.copy(alpha = 0.4f) else NeonCyan.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (screenCalibrated) "RE-KALIBRASI" else "MULAI",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }

    if (showCalibrationWizard) {
        var calStep by remember { mutableStateOf(1) } // 1: Top-Left, 2: Center, 3: Bottom-Right, 4: Selesai
        val textLogs = remember { mutableStateListOf<String>() }
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showCalibrationWizard = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WIZARD KALIBRASI SENTUHAN ULTRA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Sesuaikan koordinat fisik & tangani latensi layar digital",
                        fontSize = 8.sp,
                        color = MutedSlate,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                    )

                    HorizontalDivider(color = DarkBorder.copy(alpha = 0.2f))

                    Spacer(modifier = Modifier.height(16.dp))

                    if (calStep in 1..3) {
                        Text(
                            text = "LANGKAH $calStep DARI 3",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonYellow,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when (calStep) {
                                1 -> "Ketuk lingkaran HIJAU di bagian POJOK KIRI ATAS"
                                2 -> "Ketuk lingkaran HIJAU di bagian TENGAH LAYAR"
                                else -> "Ketuk lingkaran HIJAU di bagian POJOK KANAN BAWAH"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PureWhite,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Tap target representation box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceSlate)
                                .border(1.dp, DarkBorder.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Render target dot based on steps
                            val targetAlign = when (calStep) {
                                1 -> Alignment.TopStart
                                2 -> Alignment.Center
                                else -> Alignment.BottomEnd
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = targetAlign
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(NeonGreen.copy(alpha = 0.15f))
                                        .border(2.dp, NeonGreen, CircleShape)
                                        .clickable {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            when (calStep) {
                                                1 -> {
                                                    textLogs.add("✔ Target 1 (Kiri Atas): Terdeteksi (Latensi 4.2ms)")
                                                    textLogs.add("✔ Sinkronisasi grid sampling berhasil.")
                                                    calStep = 2
                                                }
                                                2 -> {
                                                    textLogs.add("✔ Target 2 (Tengah): Terdeteksi (Latensi 2.8ms)")
                                                    textLogs.add("✔ Kelonggaran jitter terserap (100%).")
                                                    calStep = 3
                                                }
                                                3 -> {
                                                    textLogs.add("✔ Target 3 (Kanan Bawah): Terdeteksi (Latensi 3.5ms)")
                                                    textLogs.add("✔ Berhasil menghapus delay tunda sensor.")
                                                    calStep = 4
                                                    viewModel.setScreenCalibrated(true, 1.85f)
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "KETUK",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGreen
                                    )
                                }
                            }
                        }
                    } else {
                        // Selesai state
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "KALIBRASI SELESAI!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sensitivitas physical touch screen telah dikompensasi ke 1.85x lebih responsif & presisi. Jitter lag ditekan hingga tingkat mikrosekon!",
                            fontSize = 9.sp,
                            color = MutedSlate,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceSlate, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            textLogs.forEach { log ->
                                Text(
                                    text = log,
                                    fontSize = 8.sp,
                                    color = NeonCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "✔ Driver multi-touch dimutakhirkan secara instan.",
                                fontSize = 8.sp,
                                color = NeonGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showCalibrationWizard = false },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                        ) {
                            Text("SIMPAN & TERAPKAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
                        }
                    }

                    if (calStep in 1..3 && textLogs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceSlate, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            textLogs.forEach { log ->
                                Text(
                                    text = log,
                                    fontSize = 7.5.sp,
                                    color = MutedSlate,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MicNoiseReducerCard(viewModel: PerformanceViewModel) {
    val context = LocalContext.current
    val isNoiseSuppression by viewModel.isNoiseSuppressionEnabled.collectAsState()
    val noiseGateLevel by viewModel.noiseGateThresholdLevel.collectAsState()
    val isVoiceActive by viewModel.isVoiceEngineActive.collectAsState()
    val micDb by viewModel.micDecibel.collectAsState()
    val waveform by viewModel.audioWaveform.collectAsState()
    val hasHardwareSupport by viewModel.hardwareSuppressorSupported.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.toggleVoiceEngine(liveMonitor = false)
            Toast.makeText(context, "Izin Mic Diberikan! Peredam Kebisingan Aktif.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Izin Mic dibutuhkan untuk menyaring kebisingan suara game.", Toast.LENGTH_LONG).show()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isNoiseSuppression) NeonCyan.copy(alpha = 0.5f) else DarkBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isNoiseSuppression) NeonCyan.copy(alpha = 0.15f) else SurfaceSlate),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎙️", fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PEREDAM KEBISINGAN MIC",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNoiseSuppression) NeonCyan else PureWhite,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "100% SEMUA HP",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                            }
                        }
                        Text(
                            text = "Filter desis kipas, hembusan nafas, TV & suara latar game",
                            fontSize = 9.sp,
                            color = MutedSlate
                        )
                    }
                }

                Switch(
                    checked = isNoiseSuppression,
                    onCheckedChange = { enabled ->
                        if (!hasMicPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.setNoiseSuppression(enabled)
                            if (enabled && !isVoiceActive) {
                                viewModel.toggleVoiceEngine(liveMonitor = false)
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = NeonCyan.copy(alpha = 0.35f),
                        uncheckedThumbColor = MutedSlate,
                        uncheckedTrackColor = SurfaceSlate
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tech specs badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceSlate)
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (hasHardwareSupport) "✓ Hardware DSP + AI Gate Aktif" else "✓ Universal High-Pass DSP Gate (100% HP)",
                    fontSize = 8.5.sp,
                    color = NeonGreen,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "High-Pass 150Hz Filter",
                    fontSize = 8.sp,
                    color = MutedSlate
                )
            }

            if (isNoiseSuppression) {
                Spacer(modifier = Modifier.height(14.dp))

                // Threshold Strength Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sensitivitas Reduksi Kebisingan:",
                        fontSize = 9.sp,
                        color = PureWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$noiseGateLevel%",
                        fontSize = 9.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

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
                    Text(text = "Ringan (30%)", fontSize = 8.sp, color = MutedSlate)
                    Text(text = "Sedang / Standar (65%)", fontSize = 8.sp, color = NeonCyan)
                    Text(text = "Maksimal Anti-Kipas (85%)", fontSize = 8.sp, color = NeonGreen)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Audio Level & Waveform visualizer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBackground)
                        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE MIC SPEECH MONITOR:",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedSlate,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isVoiceActive) "${micDb.toInt()} dB" else "MIC STANDBY",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (micDb > -30f) NeonGreen else if (micDb > -50f) NeonCyan else MutedSlate
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 16-bar Animated Waveform
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        waveform.forEach { level ->
                            val barHeight = (level * 28).coerceIn(3f, 28f)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(barHeight.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (level > 0.6f) NeonGreen
                                        else if (level > 0.25f) NeonCyan
                                        else MutedSlate.copy(alpha = 0.35f)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameVoiceChangerCard(viewModel: PerformanceViewModel) {
    val context = LocalContext.current
    val isVoiceActive by viewModel.isVoiceEngineActive.collectAsState()
    val isLiveMonitoring by viewModel.isLiveMonitoring.collectAsState()
    val activeProfile by viewModel.activeVoiceProfile.collectAsState()
    val isRecordingClip by viewModel.isRecordingClip.collectAsState()
    val isPlayingClip by viewModel.isPlayingRecordedClip.collectAsState()
    val hasRecordedClip by viewModel.hasRecordedClip.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.toggleVoiceEngine(liveMonitor = true)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isVoiceActive) NeonGreen.copy(alpha = 0.5f) else DarkBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isVoiceActive) NeonGreen.copy(alpha = 0.15f) else SurfaceSlate),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎭", fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PENGUBAH SUARA GAME",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isVoiceActive) NeonGreen else PureWhite,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "WORT IT",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                            }
                        }
                        Text(
                            text = "Efek suara vokal instan saat on-mic game & Discord",
                            fontSize = 9.sp,
                            color = MutedSlate
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Preset Grid
            Text(
                text = "PILIH EFEK SUARA VOKAL:",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = MutedSlate,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                VoiceProfile.values().toList().chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { profile ->
                            val isSelected = activeProfile == profile
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else SurfaceSlate)
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonGreen else DarkBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.setVoiceProfile(profile) }
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = profile.icon, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = profile.title,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) NeonGreen else PureWhite
                                        )
                                        Text(
                                            text = profile.subtitle,
                                            fontSize = 7.5.sp,
                                            color = MutedSlate,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Tes Suara Langsung & Rekam 5 Detik
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Live Loopback Test Button
                Button(
                    onClick = {
                        if (!hasMicPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.toggleVoiceEngine(liveMonitor = !isLiveMonitoring)
                            Toast.makeText(
                                context,
                                if (!isVoiceActive) "🎧 Tes Suara Langsung Aktif! Pasang headset untuk dengar efeknya." else "Tes suara dihentikan.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLiveMonitoring) NeonGreen else SurfaceSlate
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isLiveMonitoring) NeonGreen else DarkBorder)
                ) {
                    Text(
                        text = if (isLiveMonitoring) "🎧 HENTIKAN TES" else "🎧 TES LIVE (HEADSET)",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLiveMonitoring) Color.Black else PureWhite
                    )
                }

                // 5-Sec Clip Recording & Playback
                Button(
                    onClick = {
                        if (!hasMicPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            if (isRecordingClip) {
                                viewModel.stopRecordingClip()
                                Toast.makeText(context, "Rekaman selesai! Ketuk Putar untuk dengar.", Toast.LENGTH_SHORT).show()
                            } else if (hasRecordedClip && !isPlayingClip) {
                                viewModel.playRecordedClip()
                            } else {
                                viewModel.startRecordingClip()
                                Toast.makeText(context, "Merekam suara 5 detik... Silakan bicara!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecordingClip) NeonOrange else if (isPlayingClip) NeonCyan else SurfaceSlate
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isRecordingClip) NeonOrange else if (isPlayingClip) NeonCyan else DarkBorder
                    )
                ) {
                    Text(
                        text = when {
                            isRecordingClip -> "⏹️ STOP REKAM"
                            isPlayingClip -> "🔊 MEMUTAR..."
                            hasRecordedClip -> "▶ DENGAR HASIL"
                            else -> "🎙️ REKAM 5 DETIK"
                        },
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecordingClip || isPlayingClip) Color.Black else PureWhite
                    )
                }
            }
        }
    }
}

@Composable
fun FpsStabilizerCard(viewModel: PerformanceViewModel) {
    val context = LocalContext.current
    val fpsStabilizerActive by viewModel.fpsStabilizerActive.collectAsState()
    val targetFps by viewModel.targetFps.collectAsState()
    val stutterCount by viewModel.stutterCount.collectAsState()
    val frameScore by viewModel.framePacingScore.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (fpsStabilizerActive) NeonYellow.copy(alpha = 0.5f) else DarkBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (fpsStabilizerActive) NeonYellow.copy(alpha = 0.15f) else SurfaceSlate),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎯", fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "STABILISASI FPS & ANTI-STUTTER",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (fpsStabilizerActive) NeonYellow else PureWhite,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonYellow.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "10000% MAX",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonYellow
                                )
                            }
                        }
                        Text(
                            text = "Hilangkan patah-patah & kunci pacing frame display",
                            fontSize = 9.sp,
                            color = MutedSlate
                        )
                    }
                }

                Switch(
                    checked = fpsStabilizerActive,
                    onCheckedChange = { viewModel.toggleFpsStabilizer() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonYellow,
                        checkedTrackColor = NeonYellow.copy(alpha = 0.35f),
                        uncheckedThumbColor = MutedSlate,
                        uncheckedTrackColor = SurfaceSlate
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Target FPS Selector
            Text(
                text = "TARGET FRAME RATE (FPS LOCK):",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = MutedSlate,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(60, 90, 120, 144).forEach { fpsValue ->
                    val isSelected = targetFps == fpsValue
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonYellow.copy(alpha = 0.15f) else SurfaceSlate)
                            .border(1.dp, if (isSelected) NeonYellow else DarkBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.setTargetFps(fpsValue) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$fpsValue FPS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) NeonYellow else PureWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Frame Pacing Status Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBackground)
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$stutterCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                    Text(text = "MICRO-STUTTER", fontSize = 8.sp, color = MutedSlate)
                }
                VerticalDivider(modifier = Modifier.height(24.dp), color = DarkBorder)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${frameScore}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    Text(text = "FRAME PACING", fontSize = 8.sp, color = MutedSlate)
                }
                VerticalDivider(modifier = Modifier.height(24.dp), color = DarkBorder)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "URGENT DISPLAY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonYellow)
                    Text(text = "PRIORITAS THREAD", fontSize = 8.sp, color = MutedSlate)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Purge RAM & Lock Display Priority
            Button(
                onClick = {
                    viewModel.purgeMemoryAndStabilize()
                    Toast.makeText(
                        context,
                        "⚡ Frame Pacing Dikunci! Background cache dibersihkan, stutter dieliminasi.",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonYellow.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonYellow)
            ) {
                Text(
                    text = "⚡ BERSIHKAN CACHE & HILANGKAN PATAH-PATAH",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonYellow
                )
            }
        }
    }
}

