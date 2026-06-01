package com.example.ui.screens

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.PerformanceViewModel
import com.example.util.CpuInfoHelper
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import java.io.BufferedReader
import java.io.InputStreamReader

enum class DeepInfoCategory(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    OS_CORE("OS & KERNEL", Icons.Default.Info),
    SOC_CPU("SOC & GPU", Icons.Default.Settings),
    HARDWARE("HARDWARE & RAM", Icons.Default.Build),
    SENSORS("DETAILED SENSORS", Icons.Default.List)
}

@Composable
fun DeepInfoScreen(
    viewModel: PerformanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeCategory by remember { mutableStateOf(DeepInfoCategory.OS_CORE) }
    
    val cpuData by viewModel.cpuUsage.collectAsState()
    val gpuInfo by viewModel.gpuData.collectAsState()
    val rTotal by viewModel.totalRam.collectAsState()
    val rUsed by viewModel.usedRam.collectAsState()
    val rFree by viewModel.freeRam.collectAsState()
    val sTotal by viewModel.totalStorage.collectAsState()
    val sUsed by viewModel.usedStorage.collectAsState()
    
    // Dynamic uptime string
    var uptimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val uptimeMs = SystemClock.elapsedRealtime()
            val hours = (uptimeMs / (1000 * 60 * 60)) % 24
            val minutes = (uptimeMs / (1000 * 60)) % 60
            val seconds = (uptimeMs / 1000) % 60
            val days = uptimeMs / (1000 * 60 * 60 * 24)
            uptimeString = "${days}d ${hours}h ${minutes}m ${seconds}s"
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Horizontal Tabs Selection Block
        ScrollableTabRow(
            selectedTabIndex = activeCategory.ordinal,
            containerColor = SurfaceSlate,
            contentColor = NeonCyan,
            edgePadding = 12.dp,
            divider = { Divider(color = DarkBorder.copy(alpha = 0.2f)) },
            indicator = { tabPositions ->
                if (tabPositions.isNotEmpty()) {
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[activeCategory.ordinal])
                            .height(3.dp)
                            .background(
                                brush = Brush.horizontalGradient(listOf(NeonCyan, NeonYellow)),
                                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                            )
                    )
                }
            }
        ) {
            DeepInfoCategory.values().forEach { category ->
                val isSelected = activeCategory == category
                Tab(
                    selected = isSelected,
                    onClick = { activeCategory = category },
                    text = {
                        Text(
                            text = category.title,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) NeonCyan else MutedSlate,
                            letterSpacing = 0.5.sp
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = if (isSelected) NeonCyan else MutedSlate,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        // Selected Specs Dashboard Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            item {
                Text(
                    text = "DEEP CORE INFORMATION DATABASE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedSlate,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (activeCategory) {
                        DeepInfoCategory.OS_CORE -> "Operating System & low-level kernel properties extracted from core device runtimes."
                        DeepInfoCategory.SOC_CPU -> "Silicon architecture, compiler instruction layouts, and real-time core frequency loads."
                        DeepInfoCategory.HARDWARE -> "Physical manufacturing specifications, hardware serial identifiers, and active memory heaps."
                        DeepInfoCategory.SENSORS -> "Comprehensive list of physical raw on-board hardware sensors and tracking parameters."
                    },
                    fontSize = 10.sp,
                    color = MutedSlate
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            when (activeCategory) {
                DeepInfoCategory.OS_CORE -> {
                    val kernelDetails = getKernelVersion()
                    val selinuxMode = getSELinuxState()
                    val properties = listOf(
                        SpecRow("Android Release", Build.VERSION.RELEASE),
                        SpecRow("API SDK Target", Build.VERSION.SDK_INT.toString()),
                        SpecRow("Security Patch Level", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A"),
                        SpecRow("Build ID Token", Build.ID),
                        SpecRow("Kernel Version Info", kernelDetails, isPreformatted = true),
                        SpecRow("SELinux Core Status", selinuxMode),
                        SpecRow("Uptime Tracker", uptimeString),
                        SpecRow("Hardware Fingerprint", Build.FINGERPRINT, isPreformatted = true),
                        SpecRow("Bootloader Kernel", Build.BOOTLOADER),
                        SpecRow("Baseband Codename", Build.getRadioVersion() ?: "Unknown"),
                        SpecRow("Java Runtime VM", System.getProperty("java.vm.version") ?: "Android Runtime (ART)"),
                        SpecRow("Development Host", Build.HOST)
                    )
                    items(properties) { spec -> SpecDataCard(spec) }
                }

                DeepInfoCategory.SOC_CPU -> {
                    val abisList = Build.SUPPORTED_ABIS.joinToString(", ")
                    val properties = listOf(
                        SpecRow("Processor Hardware", Build.HARDWARE),
                        SpecRow("Board Product ID", Build.BOARD),
                        SpecRow("Instruction Support", abisList, isPreformatted = true),
                        SpecRow("Core Count Identified", cpuData.activeCores.toString()),
                        SpecRow("Architecture Code", CpuInfoHelper.getCpuArchitecture()),
                        SpecRow("GPU Chipset Vendor", gpuInfo.vendor),
                        SpecRow("GPU Graphic Renderer", gpuInfo.renderer),
                        SpecRow("OpenGL Support Level", gpuInfo.version, isPreformatted = true)
                    )
                    items(properties) { spec -> SpecDataCard(spec) }

                    item {
                        Text(
                            text = "CPU FREQUENCY CLUSTER BREAKDOWN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonYellow,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }

                    items(cpuData.coreFrequenciesMhz.size) { index ->
                        val freq = cpuData.coreFrequenciesMhz.getOrNull(index) ?: 0
                        val load = cpuData.coreUsages.getOrNull(index) ?: 0.0f
                        CoreFreqBarItem(coreIndex = index, freqMhz = freq, loadPercent = load)
                    }
                }

                DeepInfoCategory.HARDWARE -> {
                    val maxHeapMb = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt()
                    val properties = listOf(
                        SpecRow("Device Brand", Build.BRAND.uppercase()),
                        SpecRow("Manufacturer OEM", Build.MANUFACTURER),
                        SpecRow("Display Model Name", Build.MODEL),
                        SpecRow("Product Identifier", Build.PRODUCT),
                        SpecRow("On-Board Hardware Code", Build.DEVICE),
                        SpecRow("Total Physical RAM", "$rTotal MB"),
                        SpecRow("Available Free RAM", "$rFree MB"),
                        SpecRow("Used Operating RAM", "$rUsed MB"),
                        SpecRow("VM Max Memory Heap Limit", "$maxHeapMb MB"),
                        SpecRow("Total Built-in Storage", "$sTotal GB"),
                        SpecRow("Allocated User Storage", "$sUsed GB")
                    )
                    items(properties) { spec -> SpecDataCard(spec) }
                }

                DeepInfoCategory.SENSORS -> {
                    val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
                    val fullList = sensorManager.getSensorList(Sensor.TYPE_ALL).sortedBy { it.name }
                    
                    if (fullList.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No hardware sensor profiles discovered on this emulator target.", color = MutedSlate, fontSize = 11.sp)
                            }
                        }
                    } else {
                        items(fullList) { sensor ->
                            SensorSpecCard(sensor = sensor)
                        }
                    }
                }
            }
        }
    }
}

data class SpecRow(
    val label: String,
    val value: String,
    val isPreformatted: Boolean = false
)

@Composable
fun SpecDataCard(spec: SpecRow) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = spec.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MutedSlate,
                modifier = Modifier.weight(1.2f)
            )
            Text(
                text = spec.value,
                fontSize = 10.sp,
                fontFamily = if (spec.isPreformatted) FontFamily.Monospace else FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                color = PureWhite,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.weight(2f)
            )
        }
    }
}

@Composable
fun CoreFreqBarItem(coreIndex: Int, freqMhz: Int, loadPercent: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Core #$coreIndex Load",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
                Text(
                    text = "${freqMhz}MHz   ${String.format("%.1f", loadPercent)}%",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DarkBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = (loadPercent / 100f).coerceIn(0.01f, 1.0f))
                        .background(
                            brush = Brush.horizontalGradient(listOf(NeonCyan, NeonYellow))
                        )
                )
            }
        }
    }
}

@Composable
fun SensorSpecCard(sensor: Sensor) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (expanded) NeonCyan.copy(alpha = 0.3f) else DarkBorder.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(2f)) {
                    Text(
                        text = sensor.name.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Vendor: ${sensor.vendor}  |  Version: ${sensor.version}",
                        fontSize = 8.sp,
                        color = MutedSlate
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (expanded) NeonCyan else MutedSlate,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = DarkBorder.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SensorSmallProp(label = "MAX RANGE", value = String.format("%.2f", sensor.maximumRange))
                    SensorSmallProp(label = "RESOLUTION", value = String.format("%.4f", sensor.resolution))
                    SensorSmallProp(label = "POWER BOUNDS", value = String.format("%.3f mA", sensor.power))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SensorSmallProp(label = "MIN DELAY", value = "${sensor.minDelay} μs")
                    SensorSmallProp(label = "REPORT MODE", value = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> getReportingModeName(sensor.reportingMode)
                        else -> "Standard"
                    })
                    SensorSmallProp(label = "WAKEUP TYPE", value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) sensor.isWakeUpSensor.toString().uppercase() else "N/A")
                }
            }
        }
    }
}

@Composable
fun SensorSmallProp(label: String, value: String) {
    Column {
        Text(label, fontSize = 7.sp, color = MutedSlate, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 9.sp, color = PureWhite, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
    }
}

private fun getReportingModeName(mode: Int): String {
    return when (mode) {
        0 -> "CONTINUOUS"
        1 -> "ON-CHANGE"
        2 -> "ONE-SHOT"
        3 -> "SPECIAL"
        else -> "UNKNOWN"
    }
}

private fun getKernelVersion(): String {
    return try {
        System.getProperty("os.version") ?: "Unknown Linux Kernel"
    } catch (e: Exception) {
        "Unknown Kernel"
    }
}

private fun getSELinuxState(): String {
    return try {
        val process = Runtime.getRuntime().exec("gentenforce")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = reader.readLine()
        reader.close()
        output?.trim() ?: "Enforcing"
    } catch (e: Exception) {
        // Fallback checks using system file state
        try {
            val file = java.io.File("/sys/fs/selinux/enforce")
            if (file.exists()) {
                val reader = java.io.FileReader(file)
                val char = reader.read()
                reader.close()
                if (char.toChar() == '1') "Enforcing" else "Permissive"
            } else {
                "Enforcing"
            }
        } catch (ex: Exception) {
            "Enforcing"
        }
    }
}
