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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.PerformanceViewModel

@Composable
fun CacheCleanerScreen(
    viewModel: PerformanceViewModel,
    modifier: Modifier = Modifier
) {
    val allApps by viewModel.allProfilesList.collectAsState()
    val isCleaning by viewModel.isCleaningCache.collectAsState()
    val cleanProgress by viewModel.cleanProgress.collectAsState()
    val logs by viewModel.cacheCleanerLogs.collectAsState()
    val cacheSizes by viewModel.appCacheSizes.collectAsState()
    val sTotal by viewModel.totalStorage.collectAsState()
    val sUsed by viewModel.usedStorage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val selectedPackages = remember { mutableStateListOf<String>() }
    var lastClearedAmountMb by remember { mutableStateOf<Float?>(null) }

    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) {
            allApps
        } else {
            allApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(allApps) {
        if (selectedPackages.isEmpty() && allApps.isNotEmpty()) {
            selectedPackages.addAll(allApps.map { it.packageName })
        }
    }

    val sumSelectedMegabytes = remember(selectedPackages, cacheSizes) {
        var totalBytes = 0L
        selectedPackages.forEach { pkg ->
            totalBytes += cacheSizes[pkg] ?: 0L
        }
        totalBytes / (1024f * 1024f)
    }

    val freeStorage = (sTotal - sUsed).coerceAtLeast(0)
    val usagePercent = if (sTotal > 0) (sUsed.toFloat() / sTotal) else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // --- 1. PHYSICAL STORAGE USAGE CARD (TERPAKAi vs TERSEDIA & DIPERLUAS) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "KAPASITAS PENYIMPANAN SISTEM (ROM)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Kondisi ruang memori penyimpanan internal Android",
                                fontSize = 9.sp,
                                color = MutedSlate
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceSlate)
                                .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "TOTAL: $sTotal GB",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Real-time Storage Details: Tersedia vs Terpakai
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Tersedia (Ruang Luang)",
                                fontSize = 10.sp,
                                color = MutedSlate
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$freeStorage",
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = " GB",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedSlate,
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Terpakai",
                                fontSize = 10.sp,
                                color = MutedSlate
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$sUsed",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = " / $sTotal GB",
                                    fontSize = 12.sp,
                                    color = MutedSlate,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { usagePercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (usagePercent > 0.85f) NeonOrange else NeonCyan,
                        trackColor = SurfaceSlate
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Expansion Potential & Cleared Status
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceSlate)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Potensi Ruang Yang Diperluas:",
                                    fontSize = 10.sp,
                                    color = MutedSlate
                                )
                                Text(
                                    text = "+${String.format("%.1f", sumSelectedMegabytes)} MB",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sumSelectedMegabytes > 0) NeonGreen else MutedSlate,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            val cleared = lastClearedAmountMb
                            if (cleared != null && cleared > 0.0f && !isCleaning) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "BERHASIL DIPERLUAS: +${String.format("%.1f", cleared)} MB",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2. CLEAN ACTION BUTTON & PROGRESS ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedPackages.size} Aplikasi Dipilih",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )

                        TextButton(
                            onClick = {
                                if (selectedPackages.size == allApps.size) {
                                    selectedPackages.clear()
                                } else {
                                    selectedPackages.clear()
                                    selectedPackages.addAll(allApps.map { it.packageName })
                                }
                            }
                        ) {
                            Text(
                                text = if (selectedPackages.size == allApps.size) "Batal Pilih" else "Pilih Semua",
                                fontSize = 11.sp,
                                color = NeonCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (selectedPackages.isNotEmpty() && !isCleaning) {
                                lastClearedAmountMb = sumSelectedMegabytes
                                viewModel.cleanAppsCache(selectedPackages.toList())
                            }
                        },
                        enabled = selectedPackages.isNotEmpty() && !isCleaning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("button_clean_cache"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            disabledContainerColor = SurfaceSlate
                        )
                    ) {
                        if (isCleaning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = DarkBackground,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "MEMBERSIHKAN CACHE (${(cleanProgress * 100).toInt()}%)...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBackground
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = DarkBackground,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BERSIHKAN CACHE & PERLUAS PENYIMPANAN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBackground
                            )
                        }
                    }

                    if (isCleaning) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { cleanProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = NeonGreen,
                            trackColor = SurfaceSlate
                        )
                    }
                }
            }
        }

        // --- 3. SEARCH & APP LIST ---
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari aplikasi untuk dibersihkan...", fontSize = 12.sp, color = MutedSlate) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MutedSlate,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardSlate,
                    unfocusedContainerColor = CardSlate,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                ),
                singleLine = true
            )
        }

        items(filteredApps, key = { it.packageName }) { app ->
            val isSelected = selectedPackages.contains(app.packageName)
            val sizeBytes = cacheSizes[app.packageName] ?: 0L
            val sizeMb = sizeBytes / (1024f * 1024f)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        if (isSelected) {
                            selectedPackages.remove(app.packageName)
                        } else {
                            selectedPackages.add(app.packageName)
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) NeonCyan.copy(alpha = 0.4f) else DarkBorder
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) NeonCyan else SurfaceSlate)
                            .border(1.dp, if (isSelected) NeonCyan else DarkBorder, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = DarkBackground,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.appName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = app.packageName,
                            fontSize = 9.sp,
                            color = MutedSlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (sizeMb > 0) "${String.format("%.1f", sizeMb)} MB" else "Bersih",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sizeMb > 0) NeonGreen else MutedSlate,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Cache sampah",
                            fontSize = 8.sp,
                            color = MutedSlate
                        )
                    }
                }
            }
        }
    }
}
