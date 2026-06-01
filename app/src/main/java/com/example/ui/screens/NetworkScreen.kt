package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.launch

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

    // Animating circular pulse for active tests
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarPulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarPulseAlpha"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // WiFi & Cellular Interface Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceSlate)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Network Port",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = wifiSsid,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("IP: $ipAddr", fontSize = 10.sp, color = MutedSlate, fontFamily = FontFamily.Monospace)
                            Text("Speed: $linkSpeed Mbps", fontSize = 10.sp, color = NeonGreen)
                        }
                    }
                }
            }
        }

        // Radar Speed Assessment Launcher Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceSlate)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "STABILITY PACKET ANALYZER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pulse Radar Sphere
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                        if (isAnalyzing) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .border(2.dp, NeonCyan.copy(alpha = pulseAlpha), CircleShape)
                            )
                        }

                        Button(
                            onClick = { viewModel.startNetworkDiagnostics() },
                            enabled = !isAnalyzing,
                            modifier = Modifier
                                .testTag("run_speed_test_button")
                                .size(110.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAnalyzing) CardSlate else NeonCyan,
                                disabledContainerColor = CardSlate
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("TESTING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("RUN DIAGS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (isAnalyzing) "Conducting TCP latency handshaking & jitter evaluation..." 
                               else if (report != null) "Last evaluation completed successfully."
                               else "Press core trigger to begin testing network jitter, ping dropouts, and routing stability.",
                        fontSize = 10.sp,
                        color = MutedSlate,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Diagnostic Metrics Grid (Visible after at least 1 run)
        item {
            AnimatedVisibility(
                visible = report != null || isAnalyzing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val currentReport = report ?: NetworkAnalyzer.NetworkReport(
                    pingMs = 0, jitterMs = 0, packetLossPercent = 0, dnsResponseTimeMs = 0,
                    downloadSpeedMbps = 0f, uploadSpeedMbps = 0f, connectionType = "Scanning",
                    signalStrengthPercent = 0, subnetMask = "Unknown", stabilityScore = 0, status = "Scanning"
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Stability Score Card
                    StabilityScoreWidget(
                        score = currentReport.stabilityScore,
                        status = currentReport.status
                    )

                    // Grid metric values
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PacketMetricItem(
                            label = "PING ROTATION",
                            value = "${currentReport.pingMs} ms",
                            subtext = "Target: <100ms",
                            color = if (currentReport.pingMs < 80) NeonGreen else NeonYellow,
                            modifier = Modifier.weight(1f)
                        )
                        PacketMetricItem(
                            label = "JITTER SPREAD",
                            value = "${currentReport.jitterMs} ms",
                            subtext = "Buffer: <15ms",
                            color = if (currentReport.jitterMs < 10) NeonCyan else NeonOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PacketMetricItem(
                            label = "PACKET DROPOUT",
                            value = "${currentReport.packetLossPercent}%",
                            subtext = "Allowed: <2%",
                            color = if (currentReport.packetLossPercent <= 0) NeonGreen else CyberPink,
                            modifier = Modifier.weight(1f)
                        )
                        PacketMetricItem(
                            label = "DNS LOOKUP TIME",
                            value = "${currentReport.dnsResponseTimeMs} ms",
                            subtext = "Google Public DNS",
                            color = if (currentReport.dnsResponseTimeMs < 50) NeonCyan else NeonYellow,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PacketMetricItem(
                            label = "DOWNLOAD DATA",
                            value = String.format("%.2f Mbps", currentReport.downloadSpeedMbps),
                            subtext = "Real speed test",
                            color = NeonCyan,
                            modifier = Modifier.weight(1f)
                        )
                        PacketMetricItem(
                            label = "EST. UPLOAD SPEED",
                            value = String.format("%.2f Mbps", currentReport.uploadSpeedMbps),
                            subtext = "Upload baseline",
                            color = NeonYellow,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Historical Latency Tracking Plot
        if (pingHistory.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceSlate)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "HISTOGRAM LATENCY RECORDS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .background(DarkBackground.copy(alpha = 0.4f))
                        ) {
                            val w = size.width
                            val h = size.height
                            val maxPingAllowed = 250f
                            val step = w / 19f
                            val linePath = Path()

                            pingHistory.forEachIndexed { idx, value ->
                                val x = idx * step
                                val ratio = (value.toFloat() / maxPingAllowed).coerceAtMost(1f)
                                val y = h - (ratio * h)

                                if (idx == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                                drawCircle(color = NeonCyan, radius = 2.dp.toPx(), center = Offset(x, y))
                            }

                            drawPath(linePath, color = NeonCyan.copy(alpha = 0.6f), style = Stroke(width = 1.5.dp.toPx()))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StabilityScoreWidget(
    score: Int,
    status: String
) {
    val statusColor = when (status) {
        "Excellent" -> NeonGreen
        "Good" -> NeonCyan
        "Moderate" -> NeonYellow
        "Poor" -> NeonOrange
        else -> CyberPink
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("CONNECTION STABILITY SCORE", fontSize = 10.sp, color = MutedSlate, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$score / 100",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = PureWhite
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = status.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun PacketMetricItem(
    label: String,
    value: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, DarkBorder.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MutedSlate)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Black, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtext, fontSize = 8.sp, color = MutedSlate)
        }
    }
}
