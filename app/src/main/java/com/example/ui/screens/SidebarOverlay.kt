package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.GameProfile
import com.example.ui.theme.*
import com.example.ui.viewmodel.PerformanceViewModel
import kotlin.math.roundToInt

@Composable
fun SidebarAndFloatingWindows(
    viewModel: PerformanceViewModel,
    allProfiles: List<GameProfile>
) {
    val context = LocalContext.current
    val sidebarEnabled by viewModel.sidebarEnabled.collectAsState()
    val floatingWindows by viewModel.floatingWindows.collectAsState()

    var isSidebarOpen by remember { mutableStateOf(false) }
    var showAddAppDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Render Floating Windows on top of everything
        FloatingWindowsContainer(
            floatingWindows = floatingWindows,
            viewModel = viewModel
        )

        // Sidebar Trigger & drawer if enabled in settings
        if (sidebarEnabled) {
            // Drag handle at top-left edge
            Box(
                modifier = Modifier
                    .padding(top = 96.dp)
                    .size(width = 14.dp, height = 72.dp)
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.9f), CyberPink.copy(alpha = 0.9f))
                        )
                    )
                    .border(
                        1.dp,
                        PureWhite.copy(alpha = 0.4f),
                        RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                    )
                    .testTag("sidebar_handle")
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { isSidebarOpen = true },
                            onDrag = { change, dragAmount ->
                                if (dragAmount.x > 8) {
                                    isSidebarOpen = true
                                }
                            }
                        )
                    }
                    .clickable { isSidebarOpen = true }
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Swipe Sidebar",
                    tint = PureWhite,
                    modifier = Modifier.size(10.dp)
                )
            }

            // Dark Backdrop when sidebar is active
            if (isSidebarOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5F))
                        .clickable { isSidebarOpen = false }
                )
            }

            // Animated Sidebar Panel Sliding from Left
            AnimatedVisibility(
                visible = isSidebarOpen,
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                modifier = Modifier.fillMaxHeight()
            ) {
                SidebarContentPanel(
                    viewModel = viewModel,
                    allProfiles = allProfiles,
                    onClose = { isSidebarOpen = false },
                    onOpenAddApp = { showAddAppDialog = true }
                )
            }
        }
    }

    if (showAddAppDialog) {
        AddFloatingAppDialog(
            allProfiles = allProfiles,
            onDismiss = { showAddAppDialog = false },
            onAddUtility = { title, appType ->
                viewModel.addFloatingWindow(title, appType)
                showAddAppDialog = false
                Toast.makeText(context, "$title ditambahkan ke layar mengambang", Toast.LENGTH_SHORT).show()
            },
            onAddCustomApp = { profile ->
                viewModel.addFloatingWindow(profile.appName, "custom_app", profile.packageName)
                showAddAppDialog = false
                Toast.makeText(context, "${profile.appName} ditambahkan ke layar mengambang", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun SidebarContentPanel(
    viewModel: PerformanceViewModel,
    allProfiles: List<GameProfile>,
    onClose: () -> Unit,
    onOpenAddApp: () -> Unit
) {
    val activeMode by viewModel.globalGameMode.collectAsState()
    val lockFps by viewModel.lockFpsSelected.collectAsState()
    val lockNetwork by viewModel.lockNetworkSelected.collectAsState()
    val hdrMode by viewModel.hdrModeSelected.collectAsState()
    val highRes by viewModel.highResSelected.collectAsState()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(290.dp)
            .background(SurfaceSlate)
            .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(0.dp))
            .padding(16.dp)
    ) {
        // Sidebar Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CONSOLE LAUNCHER",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup",
                    tint = MutedSlate
                )
            }
        }

        Divider(color = DarkBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Mode Sistem Direct Change
            item {
                Text(
                    text = "MODE SISTEM AKTIF",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val modes = listOf(
                        Triple("PERFORMANCE", "Mode Performa Maks", NeonOrange),
                        Triple("BALANCED", "Mode Seimbang", NeonCyan),
                        Triple("BATTERY_SAVER", "Mode Hemat Baterai", NeonGreen)
                    )

                    modes.forEach { (modeCode, title, color) ->
                        val isSelected = activeMode == modeCode
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) color.copy(alpha = 0.15f) else DarkBackground)
                                .border(
                                    1.dp,
                                    if (isSelected) color else DarkBorder.copy(alpha = 0.2F),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    viewModel.setGlobalGameMode(modeCode)
                                    Toast.makeText(context, "$title diaktifkan!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = if (modeCode == "PERFORMANCE") "⚡ PERFORMA UTAMA" 
                                              else if (modeCode == "BALANCED") "⚖ SEIMBANG" 
                                              else "🍃 HEMAT DAYA",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) color else PureWhite
                                    )
                                    Text(
                                        text = if (modeCode == "PERFORMANCE") "Tingkatkan CPU/GPU maksimal" 
                                              else if (modeCode == "BALANCED") "Konsumsi baterai stabil optimal" 
                                              else "Batasi FPS & stabilkan suhu",
                                        fontSize = 10.sp,
                                        color = MutedSlate
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Hardware locks
            item {
                Text(
                    text = "PENGORGANISASI GAME (100% WORKS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Lock FPS
                    ToggleRow(
                        title = "Kunci FPS Tinggi",
                        desc = "Mengunci frame-rate di level maksimal",
                        checked = lockFps,
                        color = NeonGreen,
                        onCheckedChange = { viewModel.toggleLockFps() }
                    )

                    // Lock Network
                    ToggleRow(
                        title = "Kunci Koneksi Jaringan",
                        desc = "Fokuskan bandwidth ke game, anti jumping",
                        checked = lockNetwork,
                        color = NeonCyan,
                        onCheckedChange = { viewModel.toggleLockNetwork() }
                    )

                    // HDR Mode
                    ToggleRow(
                        title = "Visual Mode HDR",
                        desc = "Saturasi dinamis visual layar game",
                        checked = hdrMode,
                        color = CyberPink,
                        onCheckedChange = { viewModel.toggleHdrMode() }
                    )

                    // High Resolution
                    ToggleRow(
                        title = "Resolusi Tinggi (4K)",
                        desc = "Lumpuhkan frame drop, optimisasi piksel",
                        checked = highRes,
                        color = NeonYellow,
                        onCheckedChange = { viewModel.toggleHighRes() }
                    )
                }
            }

            // Launcher Quick App Windows
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ALAT MENGAMBANG (PIP)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "+ Buka",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberPink,
                        modifier = Modifier
                            .clickable { onOpenAddApp() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onOpenAddApp,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                    shape = RoundedCornerShape(8.dp),
                    border = borderStroke(NeonCyan.copy(alpha = 0.3F))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Aplikasi Mengambang", fontSize = 11.sp, color = PureWhite)
                }
            }
        }

        // Sidebar Footer
        Divider(color = DarkBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = "AG CONSOLE ENGINE v3.4 [PRO]",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = MutedSlate,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    color: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBackground)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
            Text(desc, fontSize = 9.sp, color = MutedSlate)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = color,
                checkedTrackColor = color.copy(alpha = 0.3F),
                uncheckedThumbColor = MutedSlate,
                uncheckedTrackColor = DarkBorder.copy(alpha = 0.3F)
            ),
            modifier = Modifier.scale(0.8f)
        )
    }
}

// Helper Extension to scale elements scale
private fun Modifier.scale(scale: Float) = this.graphicsLayer(scaleX = scale, scaleY = scale)

private fun borderStroke(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)

@Composable
fun AddFloatingAppDialog(
    allProfiles: List<GameProfile>,
    onDismiss: () -> Unit,
    onAddUtility: (String, String) -> Unit,
    onAddCustomApp: (GameProfile) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("KEMBALI", color = NeonCyan)
            }
        },
        title = {
            Text(
                "PILIH APLIKASI MENGAMBANG",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Alat Utama Sistem bawaan:", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UtilityLauncherChip(
                        label = "Browser",
                        icon = Icons.Default.Share,
                        color = NeonCyan,
                        onClick = { onAddUtility("Browser Internet", "browser") },
                        modifier = Modifier.weight(1f)
                    )
                    UtilityLauncherChip(
                        label = "Catatan",
                        icon = Icons.Default.Edit,
                        color = NeonYellow,
                        onClick = { onAddUtility("Catatan Taktis", "notes") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UtilityLauncherChip(
                        label = "Network Info",
                        icon = Icons.Default.Refresh,
                        color = CyberPink,
                        onClick = { onAddUtility("Monitor Ping", "ping") },
                        modifier = Modifier.weight(1f)
                    )
                    UtilityLauncherChip(
                        label = "Kalkulator",
                        icon = Icons.Default.Menu,
                        color = NeonGreen,
                        onClick = { onAddUtility("Kalkulator Game", "calculator") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Atau dari Aplikasi Terpasang anda:", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.height(180.dp)) {
                    if (allProfiles.isEmpty()) {
                        Text("Tidak ada aplikasi lain terdeteksi.", fontSize = 10.sp, color = MutedSlate)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(allProfiles) { profile ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkBackground)
                                        .clickable { onAddCustomApp(profile) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(profile.appName, fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.SemiBold)
                                        Text(profile.packageName, fontSize = 9.sp, color = MutedSlate, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
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
fun UtilityLauncherChip(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBackground)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, color = PureWhite, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun FloatingWindowsContainer(
    floatingWindows: List<PerformanceViewModel.FloatingWindow>,
    viewModel: PerformanceViewModel
) {
    floatingWindows.forEach { window ->
        key(window.id) {
            FloatingWindowComponent(
                window = window,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun FloatingWindowComponent(
    window: PerformanceViewModel.FloatingWindow,
    viewModel: PerformanceViewModel
) {
    val density = LocalDensity.current.density
    val context = LocalContext.current

    // Touch positions variables
    var localX by remember { mutableStateOf(window.x) }
    var localY by remember { mutableStateOf(window.y) }
    var localWidth by remember { mutableStateOf(window.width) }
    var localHeight by remember { mutableStateOf(window.height) }

    Box(
        modifier = Modifier
            .offset { IntOffset(localX.roundToInt(), localY.roundToInt()) }
            .size(width = localWidth.dp, height = localHeight.dp)
            .shadow(12.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceSlate)
            .border(1.5.dp, NeonCyan, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Draggable strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(DarkBackground)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            localX = (localX + dragAmount.x).coerceIn(0f, 1200f)
                            localY = (localY + dragAmount.y).coerceIn(0f, 2000f)
                            viewModel.updateFloatingWindowPosition(window.id, localX, localY)
                        }
                    }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = window.title.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Minimize icon
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "${window.title} diminimalkan ke background thread", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimize",
                            tint = MutedSlate,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    // Close icon
                    IconButton(
                        onClick = { viewModel.removeFloatingWindow(window.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = CyberPink,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Window Contents
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(DarkBackground)
            ) {
                when (window.appType) {
                    "browser" -> FloatingBrowser(context)
                    "notes" -> FloatingNotepad()
                    "ping" -> FloatingPingAnalyzer(viewModel)
                    "calculator" -> FloatingCalculator()
                    "custom_app" -> CustomAppMockSandbox(window, context)
                    else -> Text("Error loading workspace", color = CyberPink, modifier = Modifier.align(Alignment.Center))
                }
            }
        }

        // Draggable Resizer Icon Overlay on bottom-right corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(20.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        localWidth = (localWidth + dragAmount.x / density).coerceIn(180f, 600f)
                        localHeight = (localHeight + dragAmount.y / density).coerceIn(140f, 600f)
                        viewModel.updateFloatingWindowSize(window.id, localWidth, localHeight)
                    }
                }
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, NeonCyan.copy(alpha = 0.5f))
                    ),
                    RoundedCornerShape(bottomEnd = 12.dp)
                ),
            contentAlignment = Alignment.BottomEnd
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Resize Window",
                tint = PureWhite,
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer(rotationZ = 45f)
            )
        }
    }
}

@Composable
fun FloatingBrowser(context: Context) {
    var urlText by remember { mutableStateOf("https://www.google.com") }
    var searchTriggeredUrl by remember { mutableStateOf("https://www.google.com") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceSlate)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = urlText,
                onValueChange = { urlText = it },
                textStyle = LocalTextStyle.current.copy(color = PureWhite, fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    searchTriggeredUrl = if (!urlText.startsWith("http://") && !urlText.startsWith("https://")) {
                        "https://$urlText"
                    } else {
                        urlText
                    }
                }),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkBackground)
                    .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                    .padding(6.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = {
                    searchTriggeredUrl = if (!urlText.startsWith("http://") && !urlText.startsWith("https://")) {
                        "https://$urlText"
                    } else {
                        urlText
                    }
                },
                modifier = Modifier
                    .size(26.dp)
                    .background(NeonCyan, RoundedCornerShape(4.dp))
            ) {
                Icon(Icons.Default.Search, contentDescription = "Launch", tint = SurfaceSlate, modifier = Modifier.size(14.dp))
            }
        }

        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    loadUrl(searchTriggeredUrl)
                }
            },
            update = { webView ->
                webView.loadUrl(searchTriggeredUrl)
            }
        )
    }
}

@Composable
fun FloatingNotepad() {
    var notesText by remember { mutableStateOf("=== CATATAN GAMING ===\n- Atur target FPS: 120 FPS\n- Target push rank Mlbb malam ini\n- Bersihkan cache sebelum bermain") }

    Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        BasicTextField(
            value = notesText,
            onValueChange = { notesText = it },
            textStyle = LocalTextStyle.current.copy(
                color = NeonCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(DarkBackground)
                .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        )
    }
}

@Composable
fun FloatingPingAnalyzer(viewModel: PerformanceViewModel) {
    val activeFps by viewModel.fps.collectAsState()
    val localCpuUsage by viewModel.cpuUsage.collectAsState()
    val currentMode by viewModel.globalGameMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("DASHBOARD LIVE TELEMETRI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(DarkBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.3F), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FPS LIVE", fontSize = 8.sp, color = MutedSlate)
                    Text("$activeFps fps", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(DarkBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, NeonCyan.copy(alpha = 0.3F), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PENGGUNAAN CPU", fontSize = 8.sp, color = MutedSlate)
                    Text("${localCpuUsage.totalUsage.toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBackground, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Column {
                Text("INTEGRASI SISTEM", fontSize = 8.sp, color = MutedSlate)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mode Aktif: $currentMode",
                    fontSize = 11.sp,
                    color = PureWhite,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Suhu CPU Perkiraan: 38.5°C",
                    fontSize = 10.sp,
                    color = NeonYellow
                )
                Text(
                    text = "Status: Stabil Anti-Lag",
                    fontSize = 10.sp,
                    color = NeonGreen
                )
            }
        }
    }
}

@Composable
fun FloatingCalculator() {
    var display by remember { mutableStateOf("0") }
    var currentOp by remember { mutableStateOf("") }
    var storedVal by remember { mutableStateOf(0.0) }
    var resetOnNext by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Monitor Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(DarkBackground, RoundedCornerShape(6.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = display,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                color = NeonCyan,
                fontWeight = FontWeight.Bold
            )
        }

        // Layout Keys
        val rows = listOf(
            listOf("7", "8", "9", "/"),
            listOf("4", "5", "6", "*"),
            listOf("1", "2", "3", "-"),
            listOf("C", "0", "=", "+")
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { char ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(SurfaceSlate, RoundedCornerShape(6.dp))
                                .border(1.dp, NeonCyan.copy(alpha = 0.1F), RoundedCornerShape(6.dp))
                                .clickable {
                                    when {
                                        char == "C" -> {
                                            display = "0"
                                            storedVal = 0.0
                                            currentOp = ""
                                        }
                                        char in listOf("+", "-", "*", "/") -> {
                                            storedVal = display.toDoubleOrNull() ?: 0.0
                                            currentOp = char
                                            resetOnNext = true
                                        }
                                        char == "=" -> {
                                            if (currentOp.isNotEmpty()) {
                                                val nowVal = display.toDoubleOrNull() ?: 0.0
                                                val res = when (currentOp) {
                                                    "+" -> storedVal + nowVal
                                                    "-" -> storedVal - nowVal
                                                    "*" -> storedVal * nowVal
                                                    "/" -> if (nowVal != 0.0) storedVal / nowVal else 0.0
                                                    else -> nowVal
                                                }
                                                display = if (res % 1.0 == 0.0) res.toInt().toString() else res.toString()
                                                currentOp = ""
                                            }
                                        }
                                        else -> { // Numbers
                                            if (display == "0" || resetOnNext) {
                                                display = char
                                                resetOnNext = false
                                            } else {
                                                display += char
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                char,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (char in listOf("+", "-", "*", "/", "=")) NeonYellow else PureWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomAppMockSandbox(
    window: PerformanceViewModel.FloatingWindow,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(NeonGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(window.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                Text(window.packageName ?: "com.app.isolated", fontSize = 8.sp, color = MutedSlate, maxLines = 1)
            }
        }

        Divider(color = DarkBorder.copy(alpha = 0.3F))

        Text(
            text = "SANDBOX AKSELEROR (ANTI LAG): ON",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(DarkBackground, RoundedCornerShape(4.dp))
                    .padding(6.dp)
            ) {
                Column {
                    Text("ISOLATION CORE", fontSize = 7.sp, color = MutedSlate)
                    Text("Core #3, #4", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(DarkBackground, RoundedCornerShape(4.dp))
                    .padding(6.dp)
            ) {
                Column {
                    Text("GPU MEM ALLOC", fontSize = 7.sp, color = MutedSlate)
                    Text("Locked Max", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonYellow)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                try {
                    val intent = context.packageManager.getLaunchIntentForPackage(window.packageName ?: "")
                    if (intent != null) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "Aplikasi gagal diluncurkan secara langsung", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Terjadi gangguan meluncurkan manifest", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            shape = RoundedCornerShape(6.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SurfaceSlate, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("BUKA SEKARANG (NATIVE)", fontSize = 11.sp, color = SurfaceSlate, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(NeonCyan)
    )
}
