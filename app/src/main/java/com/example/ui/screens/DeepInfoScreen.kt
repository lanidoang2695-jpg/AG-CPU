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
    HARDWARE("PERANGKAT KERAS", Icons.Default.Build),
    SENSORS("DETIL SENSOR", Icons.Default.List)
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
                    text = "DATABASE SPESIFIKASI INTI MENDALAM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedSlate,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (activeCategory) {
                        DeepInfoCategory.OS_CORE -> "Sistem Operasi & properti kernel tingkat rendah yang didapatkan dari runtime perangkat."
                        DeepInfoCategory.SOC_CPU -> "Rincian silikon arsitektur prosesor, set instruksi ABI compile, dan beban core CPU real-time."
                        DeepInfoCategory.HARDWARE -> "Informasi fisik pabrikan HP, pengidentifikasi perangkat keras, serta penggunaan memori RAM."
                        DeepInfoCategory.SENSORS -> "Daftar lengkap sensor fisik bawaan perangkat lengkap beserta data rentang dan daya sensor."
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
                        SpecRow("Rilis Versi Android", Build.VERSION.RELEASE),
                        SpecRow("Target SDK API", Build.VERSION.SDK_INT.toString()),
                        SpecRow("Tingkat Patch Keamanan", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A"),
                        SpecRow("Token ID Build", Build.ID),
                        SpecRow("Informasi Versi Kernel", kernelDetails, isPreformatted = true),
                        SpecRow("Status SELinux Core", selinuxMode),
                        SpecRow("Lama Hidup HP (Uptime)", uptimeString),
                        SpecRow("Sidik Jari Perangkat (Fingerprint)", Build.FINGERPRINT, isPreformatted = true),
                        SpecRow("Sistem Bootloader", Build.BOOTLOADER),
                        SpecRow("Kode Baseband", Build.getRadioVersion() ?: "Tidak Diketahui"),
                        SpecRow("Java VM Runtime", System.getProperty("java.vm.version") ?: "Android Runtime (ART)"),
                        SpecRow("Host Pengembang", Build.HOST)
                    )
                    items(properties) { spec -> SpecDataCard(spec) }
                }

                DeepInfoCategory.SOC_CPU -> {
                    val abisList = Build.SUPPORTED_ABIS.joinToString(", ")
                    val properties = listOf(
                        SpecRow("Hardware Chipset", Build.HARDWARE),
                        SpecRow("ID Board Perangkat", Build.BOARD),
                        SpecRow("Set Instruksi (ABIs)", abisList, isPreformatted = true),
                        SpecRow("Jumlah Core Terdeteksi", cpuData.activeCores.toString()),
                        SpecRow("Arsitektur CPU", CpuInfoHelper.getCpuArchitecture()),
                        SpecRow("Vendor GPU", gpuInfo.vendor),
                        SpecRow("Renderer Grafis GPU", gpuInfo.renderer),
                        SpecRow("Tingkat OpenGL GLES", gpuInfo.version, isPreformatted = true)
                    )
                    items(properties) { spec -> SpecDataCard(spec) }

                    item {
                        Text(
                            text = "RINCIAN KLUSTER FREKUENSI INTI",
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
                        SpecRow("Merek Perangkat", Build.BRAND.uppercase()),
                        SpecRow("Produsen Perangkat (OEM)", Build.MANUFACTURER),
                        SpecRow("Nama Model Terdaftar", Build.MODEL),
                        SpecRow("Pengenal Produk", Build.PRODUCT),
                        SpecRow("Kode Hardware Internal", Build.DEVICE),
                        SpecRow("RAM Fisik Keseluruhan", "$rTotal MB"),
                        SpecRow("RAM Bebas Tersedia", "$rFree MB"),
                        SpecRow("RAM Terpakai Sistem", "$rUsed MB"),
                        SpecRow("Batas Maks Heap Memori VM", "$maxHeapMb MB"),
                        SpecRow("Total Penyimpanan Internal", "$sTotal GB"),
                        SpecRow("Penyimpanan Terpakai", "$sUsed GB")
                    )
                    items(properties) { spec -> SpecDataCard(spec) }
                }

                DeepInfoCategory.SENSORS -> {
                    val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
                    val fullList = sensorManager.getSensorList(Sensor.TYPE_ALL).sortedBy { it.name }
                    
                    if (fullList.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                  Text("Tidak ada profil sensor perangkat keras ditemukan.", color = MutedSlate, fontSize = 11.sp)
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
                    text = "Beban Inti #$coreIndex",
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
                    SensorSmallProp(label = "RENTANG MAKS", value = String.format("%.2f", sensor.maximumRange))
                    SensorSmallProp(label = "RESOLUSI", value = String.format("%.4f", sensor.resolution))
                    SensorSmallProp(label = "BATAS DAYA", value = String.format("%.3f mA", sensor.power))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SensorSmallProp(label = "DELAY MINIMAL", value = "${sensor.minDelay} μs")
                    SensorSmallProp(label = "MODE LAPOR", value = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> getReportingModeName(sensor.reportingMode)
                        else -> "Standar"
                    })
                    SensorSmallProp(label = "TIPE BANGUN", value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) sensor.isWakeUpSensor.toString().uppercase() else "N/A")
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
