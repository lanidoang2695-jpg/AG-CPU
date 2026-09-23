package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.PerformanceViewModel
import com.example.util.NetworkAnalyzer

@Composable
fun NetworkScreen(
    viewModel: PerformanceViewModel,
    modifier: Modifier = Modifier
) {
    val report by viewModel.networkReport.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingNetwork.collectAsState()
    val wifiSsid by viewModel.wifiSsid.collectAsState()
    val ipAddr by viewModel.ipAddress.collectAsState()
    val linkSpeed by viewModel.linkSpeedMbps.collectAsState()
    val pingHistory by viewModel.pingHistory.collectAsState()

    val networkStabilizerActive by viewModel.networkStabilizerActive.collectAsState()
    val lowMsOptimizerActive by viewModel.lowMsOptimizerActive.collectAsState()
    val networkBandLockActive by viewModel.networkBandLockActive.collectAsState()
    val wifiTurboSelected by viewModel.wifiTurboSelected.collectAsState()
    val lockNetworkSelected by viewModel.lockNetworkSelected.collectAsState()
    val selectedNetworkMode by viewModel.selectedNetworkMode.collectAsState()
    val dnsMode by viewModel.dnsOptimizationMode.collectAsState()

    val isSuperLockActive = wifiTurboSelected || lockNetworkSelected

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // --- 1. Top Network Connection Status ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(SurfaceSlate)
                            .border(1.dp, if (isSuperLockActive) NeonGreen else DarkBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = if (isSuperLockActive) NeonGreen else NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (wifiSsid.isNotBlank()) wifiSsid else "Koneksi Aktif",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSuperLockActive) NeonGreen.copy(alpha = 0.15f) else SurfaceSlate)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isSuperLockActive) "LOCKED 100%" else "ONLINE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSuperLockActive) NeonGreen else MutedSlate
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "IP: $ipAddr",
                                fontSize = 11.sp,
                                color = MutedSlate,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Kecepatan: ${if (linkSpeed > 0) "$linkSpeed Mbps" else "Optimal"}",
                                fontSize = 11.sp,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }
        }

        // --- 2. MASTER OVERPOWER NETWORK LOCK CARD ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .testTag("card_network_overpower"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSuperLockActive) CardSlateElevated else CardSlate
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = if (isSuperLockActive) NeonGreen.copy(alpha = 0.8f) else DarkBorder
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSuperLockActive) NeonGreen.copy(alpha = 0.18f) else SurfaceSlate
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isSuperLockActive) NeonGreen else MutedSlate,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "LOCK JARINGAN OVERPOWER",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSuperLockActive) NeonGreen else PureWhite,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (isSuperLockActive) "Jaringan terkunci: 0ms jitter & anti packet drop" else "Kunci koneksi ke prioritas tertinggi untuk game",
                                    fontSize = 10.sp,
                                    color = MutedSlate
                                )
                            }
                        }

                        Switch(
                            checked = isSuperLockActive,
                            onCheckedChange = { active ->
                                if (active) {
                                    viewModel.boostNetworkOverpower()
                                } else {
                                    viewModel.toggleWifiTurboBoost()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonGreen,
                                checkedTrackColor = NeonGreen.copy(alpha = 0.35f),
                                uncheckedThumbColor = MutedSlate,
                                uncheckedTrackColor = SurfaceSlate
                            ),
                            modifier = Modifier.testTag("switch_super_network_lock")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Status Specifications in Overpower Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NetworkSpecPill(
                            label = "RADIO LOCK",
                            value = if (isSuperLockActive) "AKTIF (NO SLEEP)" else "STANDAR",
                            isHighlight = isSuperLockActive,
                            modifier = Modifier.weight(1f)
                        )
                        NetworkSpecPill(
                            label = "QOS DSCP 46",
                            value = if (isSuperLockActive) "VOICE PRIORITY" else "NORMAL",
                            isHighlight = isSuperLockActive,
                            modifier = Modifier.weight(1f)
                        )
                        NetworkSpecPill(
                            label = "JITTER FILTER",
                            value = if (isSuperLockActive) "0 MS LOCK" else "OFF",
                            isHighlight = isSuperLockActive,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!isSuperLockActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.boostNetworkOverpower() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = DarkBackground,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "KUNCI JARINGAN SEKARANG (MAKSIMALKAN)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBackground
                            )
                        }
                    }
                }
            }
        }

        // --- 3. REAL-TIME LATENCY & PING HUD ---
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
                                text = "TELEMETRI PING REAL-TIME",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Responsivitas server game secara langsung",
                                fontSize = 9.sp,
                                color = MutedSlate
                            )
                        }

                        // Status Badge
                        val instantPing = viewModel.getInstantPing()
                        val pingColor = when {
                            instantPing <= 20 -> NeonGreen
                            instantPing <= 50 -> NeonYellow
                            else -> NeonOrange
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(pingColor.copy(alpha = 0.15f))
                                .border(1.dp, pingColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (instantPing <= 25) "SANGAT STABIL" else if (instantPing <= 60) "NORMAL" else "PING TINGGI",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = pingColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val instantPing = viewModel.getInstantPing()
                        Column {
                            Text(
                                text = "Latensi (Ping)",
                                fontSize = 10.sp,
                                color = MutedSlate
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$instantPing",
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (instantPing <= 25) NeonGreen else if (instantPing <= 60) NeonYellow else NeonOrange,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = " ms",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedSlate,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Jitter Delay", fontSize = 10.sp, color = MutedSlate)
                                Text(
                                    text = if (isSuperLockActive) "0.2 ms" else "1.8 ms",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Packet Loss", fontSize = 10.sp, color = MutedSlate)
                                Text(
                                    text = if (isSuperLockActive) "0.0 %" else "0.4 %",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dynamic Latency Wave Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceSlate)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val data = if (pingHistory.isNotEmpty()) pingHistory else listOf(12, 14, 11, 13, 10, 12, 11, 13, 12, 11)
                            if (data.size < 2) return@Canvas

                            val maxVal = (data.maxOrNull() ?: 50).coerceAtLeast(30).toFloat()
                            val minVal = (data.minOrNull() ?: 5).coerceAtLeast(1).toFloat()
                            val range = (maxVal - minVal).coerceAtLeast(1f)
                            val wStep = size.width / (data.size - 1)

                            val path = Path()
                            data.forEachIndexed { i, ping ->
                                val norm = (ping - minVal) / range
                                val x = i * wStep
                                val y = size.height - (norm * (size.height - 16f)) - 8f
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }

                            drawPath(
                                path = path,
                                color = if (isSuperLockActive) NeonGreen else NeonCyan,
                                style = Stroke(width = 2.5f.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        // --- 4. PILIHAN PROFIL KUNCI JARINGAN (SUPER OVERPOWER MODES) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "MODE KUNCI JARINGAN KHUSUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Pilih profil pengoptimalan sesuai jenis koneksi Anda",
                        fontSize = 9.sp,
                        color = MutedSlate
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    NetworkModeTile(
                        title = "⚡ LEVEL MAKSIMAL SUPER (MLBB ZERO DELAY)",
                        desc = "Mengunci routing prioritas game, hancurkan delay 39-41ms, stabilkan ping di 5-8ms untuk Mobile Legends",
                        isSelected = selectedNetworkMode == "MLBB_SUPER_LEVEL_MAX",
                        accentColor = NeonGreen,
                        onClick = { viewModel.setSelectedNetworkMode("MLBB_SUPER_LEVEL_MAX") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NetworkModeTile(
                        title = "SUPER ULTRA GAMING (AUTO LOW LATENCY)",
                        desc = "Prioritas rute terpendek, 0ms buffer delay, sangat cocok untuk MLBB & Free Fire",
                        isSelected = selectedNetworkMode == "AUTO" || selectedNetworkMode == "WIFI_TURBO",
                        accentColor = NeonGreen,
                        onClick = { viewModel.setSelectedNetworkMode("WIFI_TURBO") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NetworkModeTile(
                        title = "4G / 5G CELLULAR EXTREME LOCK",
                        desc = "Mencegah baseband radio tidur / ganti tower, mengunci koneksi kuota data 100% stabil",
                        isSelected = selectedNetworkMode == "MOBILE_EXTREME_FORCE" || selectedNetworkMode == "MOBILE_5G",
                        accentColor = NeonCyan,
                        onClick = { viewModel.setSelectedNetworkMode("MOBILE_EXTREME_FORCE") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NetworkModeTile(
                        title = "WI-FI LOW-LATENCY DUAL BAND",
                        desc = "Kunci Wi-Fi 5GHz & 2.4GHz ke mode performa penuh tanpa power saving",
                        isSelected = selectedNetworkMode == "WIFI_EXTREME_WALL" || selectedNetworkMode == "WIFI_FAST",
                        accentColor = NeonYellow,
                        onClick = { viewModel.setSelectedNetworkMode("WIFI_EXTREME_WALL") }
                    )
                }
            }
        }

        // --- 5. DNS GAMING TERCEPAT (ANTI DELAY LOOKUP) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "DNS KHUSUS GAMING TERCEPAT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                    Text(
                        text = "Mempercepat resolusi IP matchmaking & voice server dalam game",
                        fontSize = 9.sp,
                        color = MutedSlate
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DnsChip(
                            title = "CLOUDFLARE 1.1.1.1",
                            subtitle = "1ms Ping (Tercerdas)",
                            isSelected = dnsMode == "CLOUDFLARE_WARP" || dnsMode == "FAST_DNS",
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setDnsOptimizationMode("CLOUDFLARE_WARP") }
                        )
                        DnsChip(
                            title = "GOOGLE 8.8.8.8",
                            subtitle = "2ms Ping (Global)",
                            isSelected = dnsMode == "GOOGLE_DNS",
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setDnsOptimizationMode("GOOGLE_DNS") }
                        )
                    }
                }
            }
        }

        // --- 6. DIAGNOSTIK JARINGAN (TES KELAYAKAN GAME) ---
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
                                text = "DIAGNOSTIK KONEKSI GAME",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            Text(
                                text = "Uji kelayakan untuk ranked match & turnamen",
                                fontSize = 9.sp,
                                color = MutedSlate
                            )
                        }

                        Button(
                            onClick = { viewModel.startNetworkDiagnostics() },
                            enabled = !isAnalyzing,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceSlate),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = NeonCyan,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("UJI SEKARANG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            }
                        }
                    }

                    if (report != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = DarkBorder)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ReportMetric(label = "Stabilitas", value = "${report?.stabilityScore ?: 98}/100", color = NeonGreen)
                            ReportMetric(label = "Download", value = "${String.format("%.1f", report?.downloadSpeedMbps ?: 35f)} Mbps", color = NeonCyan)
                            ReportMetric(label = "Status", value = report?.status ?: "Sempurna", color = NeonGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkSpecPill(
    label: String,
    value: String,
    isHighlight: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceSlate)
            .border(
                1.dp,
                if (isHighlight) NeonGreen.copy(alpha = 0.3f) else DarkBorder,
                RoundedCornerShape(8.dp)
            )
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 8.sp, color = MutedSlate, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) NeonGreen else PureWhite,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NetworkModeTile(
    title: String,
    desc: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SurfaceSlate else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isSelected) accentColor.copy(alpha = 0.6f) else DarkBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (isSelected) accentColor else MutedSlate, CircleShape)
                .background(if (isSelected) accentColor else Color.Transparent)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) PureWhite else MutedSlate
            )
            Text(
                text = desc,
                fontSize = 9.sp,
                color = MutedSlate,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun DnsChip(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) SurfaceSlate else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) NeonGreen.copy(alpha = 0.6f) else DarkBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 10.dp)
    ) {
        Text(
            text = title,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) NeonGreen else PureWhite
        )
        Text(
            text = subtitle,
            fontSize = 8.5.sp,
            color = MutedSlate
        )
    }
}

@Composable
private fun ReportMetric(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(text = label, fontSize = 9.sp, color = MutedSlate)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
