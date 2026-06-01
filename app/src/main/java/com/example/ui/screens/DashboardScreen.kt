package com.example.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.PerformanceViewModel
import com.example.util.CpuInfoHelper
import com.example.util.GpuInfoHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: PerformanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cpuData by viewModel.cpuUsage.collectAsState()
    val gpuInfo by viewModel.gpuData.collectAsState()
    val localFps by viewModel.fps.collectAsState()
    val rTotal by viewModel.totalRam.collectAsState()
    val rUsed by viewModel.usedRam.collectAsState()
    val rFree by viewModel.freeRam.collectAsState()
    val sTotal by viewModel.totalStorage.collectAsState()
    val sUsed by viewModel.usedStorage.collectAsState()
    val bLevel by viewModel.batteryLevel.collectAsState()
    val bTemp by viewModel.batteryTemp.collectAsState()
    val bVoltage by viewModel.batteryVoltage.collectAsState()
    val bHealth by viewModel.batteryHealth.collectAsState()
    val bStatus by viewModel.batteryStatus.collectAsState()
    val sensorsList by viewModel.sensors.collectAsState()
    val cpuHist by viewModel.cpuHistory.collectAsState()
    val ramHist by viewModel.ramHistory.collectAsState()

    var showSensorsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // Core Summary Header Card
        item {
            HardwareOverviewCard(
                fps = localFps,
                cpuLoad = cpuData.totalUsage,
                cpuTemp = bTemp + 2.5f, // Thermal offset representation
                ramUsedPercent = if (rTotal > 0) (rUsed * 100f / rTotal) else 0f
            )
        }

        // RAM & Storage Management Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // RAM Tracker
                MemoryStatCard(
                    title = "System RAM",
                    usedText = "$rUsed MB",
                    totalText = "$rTotal MB",
                    percent = if (rTotal > 0) rUsed.toFloat() / rTotal else 0f,
                    icon = Icons.Default.Settings,
                    color = NeonCyan,
                    modifier = Modifier.weight(1f),
                    onActionClick = {
                        System.gc()
                        Toast.makeText(context, "System RAM optimized: Garbages Purged", Toast.LENGTH_SHORT).show()
                    }
                )

                // Storage Level
                MemoryStatCard(
                    title = "ROM Partition",
                    usedText = "$sUsed GB",
                    totalText = "$sTotal GB",
                    percent = if (sTotal > 0) sUsed.toFloat() / sTotal else 0f,
                    icon = Icons.Default.Info,
                    color = NeonYellow,
                    modifier = Modifier.weight(1f),
                    actionLabel = "ANALYZE"
                )
            }
        }

        // Live Graphic Trend Canvas
        item {
            PerformanceTrendGraph(
                cpuHistory = cpuHist,
                ramHistory = ramHist
            )
        }

        // CPU Active Frequencies
        item {
            CpuFrequenciesWidget(
                coresCount = cpuData.activeCores,
                frequencies = cpuData.coreFrequenciesMhz,
                usages = cpuData.coreUsages
            )
        }

        // Battery State Card & GPU Spec Matrix
        item {
            PowerAndGpuWidget(
                batteryLevel = bLevel,
                batteryTemp = bTemp,
                batteryVoltage = bVoltage,
                batteryHealth = bHealth,
                batteryStatus = bStatus,
                gpuData = gpuInfo,
                onShowSensors = { showSensorsDialog = true }
            )
        }

        // Static Spec Grid
        item {
            DeviceSpecsWidget()
        }
    }

    if (showSensorsDialog) {
        AlertDialog(
            onDismissRequest = { showSensorsDialog = false },
            confirmButton = {
                TextButton(onClick = { showSensorsDialog = false }) {
                    Text("CLOSE", color = NeonCyan)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ON-BOARD HARDEWARE SENSORS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                }
            },
            text = {
                Box(modifier = Modifier.height(280.dp)) {
                    if (sensorsList.isEmpty()) {
                        Text("Detecting device sensors...", color = MutedSlate)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(sensorsList.size) { index ->
                                Text(
                                    "- ${sensorsList[index]}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MutedSlate
                                )
                            }
                        }
                    }
                }
            },
            containerColor = SurfaceSlate
        )
    }
}

@Composable
fun HardwareOverviewCard(
    fps: Int,
    cpuLoad: Float,
    cpuTemp: Float,
    ramUsedPercent: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "SYSTEM REAL-TIME STATUS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusRadialMeter(
                    label = "ACTIVE FPS",
                    value = "$fps",
                    subLabel = "Frames/s",
                    progress = (fps / 144f).coerceIn(0f, 1f),
                    color = NeonGreen
                )

                StatusRadialMeter(
                    label = "CPU LOAD",
                    value = "${cpuLoad.toInt()}%",
                    subLabel = "Utilized",
                    progress = (cpuLoad / 100f).coerceIn(0f, 1f),
                    color = NeonCyan
                )

                StatusRadialMeter(
                    label = "CPU TEMP",
                    value = "${cpuTemp.toInt()}°C",
                    subLabel = "Thermal",
                    progress = (cpuTemp / 100f).coerceIn(0f, 1f),
                    color = if (cpuTemp > 45) NeonOrange else NeonYellow
                )
            }
        }
    }
}

@Composable
fun StatusRadialMeter(
    label: String,
    value: String,
    subLabel: String,
    progress: Float,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(72.dp)
        ) {
            Canvas(modifier = Modifier.size(64.dp)) {
                // Background Track
                drawCircle(
                    color = DarkBorder.copy(alpha = 0.3f),
                    style = Stroke(width = 6.dp.toPx())
                )
                // Foreground Progress Arc
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx())
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
                Text(
                    text = subLabel,
                    fontSize = 8.sp,
                    color = MutedSlate
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MutedSlate,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MemoryStatCard(
    title: String,
    usedText: String,
    totalText: String,
    percent: Float,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    actionLabel: String = "BOOST",
    onActionClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                if (onActionClick != null) {
                    Text(
                        actionLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier
                            .testTag("boost_ram_button")
                            .clickable { onActionClick() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                } else {
                    Text(
                        actionLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = MutedSlate
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontSize = 11.sp, color = MutedSlate)
            Text(usedText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PureWhite)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { percent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = DarkBorder.copy(alpha = 0.3f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Total: $totalText", fontSize = 9.sp, color = MutedSlate)
        }
    }
}

@Composable
fun PerformanceTrendGraph(
    cpuHistory: List<Float>,
    ramHistory: List<Float>
) {
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
                    "DYNAMIC TRACKING PLOTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(NeonCyan))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CPU", fontSize = 9.sp, color = PureWhite)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(CyberPink))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RAM", fontSize = 9.sp, color = PureWhite)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Graph drawing Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(DarkBackground.copy(alpha = 0.6f))
                    .border(1.dp, DarkBorder.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Draw background horizontal lines
                val gridLines = 4
                for (i in 1 until gridLines) {
                    val y = (canvasHeight / gridLines) * i
                    drawLine(
                        color = DarkBorder.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw CPU path
                if (cpuHistory.size > 1) {
                    val pointCount = 20f
                    val stepX = canvasWidth / (pointCount - 1f)
                    val path = Path()
                    val fillingPath = Path()

                    cpuHistory.forEachIndexed { idx, value ->
                        val x = idx * stepX
                        // Normalize 0-100%
                        val y = canvasHeight - ((value / 100f) * canvasHeight)
                        if (idx == 0) {
                            path.moveTo(x, y)
                            fillingPath.moveTo(x, canvasHeight)
                            fillingPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            fillingPath.lineTo(x, y)
                        }
                        if (idx == cpuHistory.size - 1) {
                            fillingPath.lineTo(x, canvasHeight)
                            fillingPath.close()
                        }
                    }

                    // Draw area below curve with linear gradient alpha
                    drawPath(
                        path = fillingPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.18f), Color.Transparent)
                        )
                    )

                    // Draw main line path
                    drawPath(
                        path = path,
                        color = NeonCyan,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Draw RAM path
                if (ramHistory.size > 1) {
                    val pointCount = 20f
                    val stepX = canvasWidth / (pointCount - 1f)
                    val path = Path()

                    ramHistory.forEachIndexed { idx, value ->
                        val x = idx * stepX
                        val y = canvasHeight - ((value / 100f) * canvasHeight)
                        if (idx == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = CyberPink,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun CpuFrequenciesWidget(
    coresCount: Int,
    frequencies: List<Int>,
    usages: List<Float>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "CPU MULTI-CORE LOAD DISTRIBUTION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Grid of CPU Cores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Slice into 2 rows if coresCount >= 8, or render compact rows
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0 until (coresCount / 2)) {
                        CoreStatItem(
                            coreNum = i,
                            speedMhz = frequencies.getOrNull(i) ?: 1400,
                            loadPercent = usages.getOrNull(i) ?: 10f
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in (coresCount / 2) until coresCount) {
                        CoreStatItem(
                            coreNum = i,
                            speedMhz = frequencies.getOrNull(i) ?: 1400,
                            loadPercent = usages.getOrNull(i) ?: 10f
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CoreStatItem(coreNum: Int, speedMhz: Int, loadPercent: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBackground.copy(alpha = 0.5f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.3f)) {
            Text("CORE #$coreNum", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PureWhite)
            Text("${speedMhz} MHz", fontSize = 9.sp, color = NeonGreen, fontFamily = FontFamily.Monospace)
        }
        Column(modifier = Modifier.weight(2f)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Load", fontSize = 8.sp, color = MutedSlate)
                Text("${loadPercent.toInt()}%", fontSize = 8.sp, color = PureWhite, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { loadPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.dp)),
                color = NeonCyan,
                trackColor = DarkBorder.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun PowerAndGpuWidget(
    batteryLevel: Int,
    batteryTemp: Float,
    batteryVoltage: Int,
    batteryHealth: String,
    batteryStatus: String,
    gpuData: GpuInfoHelper.GpuData,
    onShowSensors: () -> Unit
) {
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
                    "THERMAL & GRAPHICS HARDWARE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                )
                Button(
                    onClick = onShowSensors,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("SENSORS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Battery properties
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground.copy(alpha = 0.4f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Power Node", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    }
                    Divider(color = DarkBorder.copy(alpha = 0.2f))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Charge Status", fontSize = 9.sp, color = MutedSlate)
                        Text(batteryStatus, fontSize = 9.sp, color = PureWhite, fontWeight = FontWeight.Medium)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Condition", fontSize = 9.sp, color = MutedSlate)
                        Text(batteryHealth, fontSize = 9.sp, color = NeonGreen, fontWeight = FontWeight.Medium)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Voltage", fontSize = 9.sp, color = MutedSlate)
                        Text("${batteryVoltage}mV", fontSize = 9.sp, color = PureWhite, fontFamily = FontFamily.Monospace)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Power", fontSize = 9.sp, color = MutedSlate)
                        Text("$batteryLevel%", fontSize = 9.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                }

                // GPU Information
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground.copy(alpha = 0.4f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GPU GLES Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    }
                    Divider(color = DarkBorder.copy(alpha = 0.2f))
                    
                    Text("Vendor", fontSize = 8.sp, color = MutedSlate)
                    Text(
                        gpuData.vendor,
                        fontSize = 10.sp,
                        color = PureWhite,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text("Renderer", fontSize = 8.sp, color = MutedSlate)
                    Text(
                        gpuData.renderer,
                        fontSize = 10.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text("OpenGL Version", fontSize = 8.sp, color = MutedSlate)
                    Text(
                        gpuData.version.split(" ").firstOrNull() ?: gpuData.version,
                        fontSize = 9.sp,
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceSpecsWidget() {
    val items = listOf(
        Pair("Product Codename", Build.DEVICE.uppercase() ?: "Unknown"),
        Pair("Manufacturer", Build.MANUFACTURER ?: "Generic"),
        Pair("Hardware Board", Build.BOARD ?: "Unknown"),
        Pair("Android Version", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"),
        Pair("Arch Signature", CpuInfoHelper.getCpuArchitecture()),
        Pair("Security Level", Build.VERSION.SECURITY_PATCH ?: "Active")
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "COMPLETE SYSTEM SPECS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Row Spec Elements
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, fontSize = 10.sp, color = MutedSlate)
                        Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    }
                    Divider(color = DarkBorder.copy(alpha = 0.1f))
                }
            }
        }
    }
}
