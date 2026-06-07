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
                    "AUTO" to "Pelindung Otomatis Latensi",
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
            Text("Kunci Bingkai Kecepatan Target (FPS)", fontSize = 10.sp, color = MutedSlate)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(60, 90, 120).forEach { fps ->
                    val isFSelected = profile.customFpsTarget == fps
                    Card(
                        onClick = { onFpsTargetChange(fps) },
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 1.dp,
                                color = if (isFSelected) NeonGreen else DarkBorder.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFSelected) NeonGreen.copy(alpha = 0.08f) else DarkBackground.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$fps FPS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFSelected) NeonGreen else MutedSlate
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Performance spec listing
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Kunci Bingkai Gambar Maks", fontSize = 10.sp, color = MutedSlate)
                Text("${profile.customFpsTarget} FPS (KUNCI)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
            }
            Divider(color = DarkBorder.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pembersih Memori Sisi Latar", fontSize = 10.sp, color = MutedSlate)
                Text("Otomatis Aktif", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
            }
            Divider(color = DarkBorder.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
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
