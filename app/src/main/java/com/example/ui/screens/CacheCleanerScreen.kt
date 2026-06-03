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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.PerformanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    var searchQuery by remember { mutableStateOf("") }
    val selectedPackages = remember { mutableStateListOf<String>() }

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

    // Auto select apps with caches > 0 on start or update
    LaunchedEffect(allApps) {
        if (selectedPackages.isEmpty() && allApps.isNotEmpty()) {
            selectedPackages.addAll(allApps.map { it.packageName })
        }
    }

    // Calculate sum sizes of selected apps
    val sumSelectedMegabytes = remember(selectedPackages, cacheSizes) {
        var totalBytes = 0L
        selectedPackages.forEach { pkg ->
            totalBytes += cacheSizes[pkg] ?: 0L
        }
        totalBytes / (1024f * 1024f)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // High level dashboard header card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "PEMBERSIHAN MEMORI PINTAR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "Bersihkan cache sampah, shader & berkas sementara",
                                fontSize = 8.sp,
                                color = MutedSlate
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Kapasitas Potensi Pembersihan",
                        fontSize = 10.sp,
                        color = MutedSlate
                    )
                    Text(
                        text = "${String.format("%.1f", sumSelectedMegabytes)} MB",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = if (sumSelectedMegabytes > 0) NeonCyan else NeonGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Menghapus cache membersihkan file shader usang, membebaskan memori RAM/Dalvik Heap dan mencegah lag patah-patah.",
                        fontSize = 9.sp,
                        color = MutedSlate,
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Processing Console if isCleaning is active
        if (isCleaning || logs.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isCleaning) NeonCyan.copy(alpha = 0.3f) else DarkBorder.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = DarkBackground.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "KONSOL PROSES PEMBERSIHAN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                letterSpacing = 0.5.sp
                            )
                            if (isCleaning) {
                                Text(
                                    text = "${(cleanProgress * 100).toInt()}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LinearProgressIndicator(
                            progress = { cleanProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = NeonCyan,
                            trackColor = DarkBorder.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(8.dp)
                        ) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(logs.reversed()) { logLine ->
                                    Text(
                                        text = logLine,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (logLine.startsWith("✔") || logLine.startsWith("✨")) NeonGreen else if (logLine.startsWith("🚀")) NeonCyan else PureWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Select All toggle button
                Button(
                    onClick = {
                        if (selectedPackages.size == allApps.size) {
                            selectedPackages.clear()
                        } else {
                            selectedPackages.clear()
                            selectedPackages.addAll(allApps.map { it.packageName })
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceSlate,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Text(
                        text = if (selectedPackages.size == allApps.size) "BATALKAN SEMUA" else "PILIH SEMUA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Clear action trigger
                Button(
                    onClick = {
                        viewModel.cleanAppsCache(selectedPackages.toList())
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("action_clean_cache_button"),
                    enabled = selectedPackages.isNotEmpty() && !isCleaning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = DarkBackground,
                        disabledContainerColor = SurfaceSlate.copy(alpha = 0.4f),
                        disabledContentColor = MutedSlate
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MULAI PEMBERSIHAN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Package filtering search
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nama aplikasi...", fontSize = 10.sp, color = MutedSlate) },
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
                    focusedContainerColor = SurfaceSlate.copy(alpha = 0.5f),
                    unfocusedContainerColor = SurfaceSlate.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }

        // Applications list showing checkable items
        if (filteredApps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tidak ada aplikasi cocok dengan filter.", color = MutedSlate, fontSize = 11.sp)
                }
            }
        } else {
            items(filteredApps) { app ->
                val isSelected = selectedPackages.contains(app.packageName)
                val appBytes = cacheSizes[app.packageName] ?: 0L
                val sizeOnMb = String.format("%.2f", appBytes / (1024f * 1024f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) NeonCyan.copy(alpha = 0.05f) else SurfaceSlate.copy(alpha = 0.6f))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NeonCyan.copy(alpha = 0.25f) else DarkBorder.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            if (isSelected) {
                                selectedPackages.remove(app.packageName)
                            } else {
                                selectedPackages.add(app.packageName)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked == true) {
                                    if (!selectedPackages.contains(app.packageName)) selectedPackages.add(app.packageName)
                                } else {
                                    selectedPackages.remove(app.packageName)
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = NeonCyan,
                                uncheckedColor = MutedSlate,
                                checkmarkColor = DarkBackground
                            ),
                            modifier = Modifier.testTag("cache_cleanup_checkbox_${app.packageName}").size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = app.appName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = app.packageName,
                                fontSize = 8.sp,
                                color = MutedSlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (appBytes > 0) NeonCyan.copy(alpha = 0.1f) else NeonGreen.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (appBytes > 0) "$sizeOnMb MB" else "BERSIH 0.0 MB",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (appBytes > 0) NeonCyan else NeonGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
