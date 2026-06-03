package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.GameRepository
import com.example.ui.screens.BoosterOverlay
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeepInfoScreen
import com.example.ui.screens.GamingBoosterScreen
import com.example.ui.screens.NetworkScreen
import com.example.ui.screens.CacheCleanerScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.PerformanceViewModel
import com.example.ui.viewmodel.PerformanceViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Initialize database and repository inside composition / context scope safely
                val context = LocalContext.current.applicationContext
                val database = remember { AppDatabase.getDatabase(context) }
                val repository = remember { GameRepository(context, database.gameProfileDao()) }
                
                val perfViewModel: PerformanceViewModel = viewModel(
                    factory = PerformanceViewModelFactory(context, repository)
                )

                MainAppLayout(perfViewModel)
            }
        }
    }
}

enum class NavigationTab(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("SPEK DINAMIS", Icons.Default.Home, "tab_dashboard"),
    SYSTEM("INFO DETAIL", Icons.Default.Info, "tab_system"),
    NETWORK("JARINGAN", Icons.Default.Settings, "tab_network"),
    BOOSTER("MESIN GAME", Icons.Default.PlayArrow, "tab_booster"),
    CACHE("BERSIH CACHE", Icons.Default.Delete, "tab_cache")
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainAppLayout(viewModel: PerformanceViewModel) {
    var selectedTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }
    val boosterState by viewModel.boosterState.collectAsState()
    val boosterLog by viewModel.boosterLog.collectAsState()
    val selectedGame by viewModel.selectedGameForBoost.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        topBar = {
            Column {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.safeDrawing))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AG CPU TOOLS",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = PureWhite,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "MESIN AKTIF",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                    }
                }
                Divider(color = DarkBorder.copy(alpha = 0.3f))
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceSlate,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { 
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { 
                            Text(
                                text = tab.title,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkBackground,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan,
                            unselectedIconColor = MutedSlate,
                            unselectedTextColor = MutedSlate
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            // Transitions between pages
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn() + scaleIn(initialScale = 0.98f) togetherWith fadeOut()
                },
                label = "TabTransition"
            ) { tab ->
                when (tab) {
                    NavigationTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                    NavigationTab.SYSTEM -> DeepInfoScreen(viewModel = viewModel)
                    NavigationTab.NETWORK -> NetworkScreen(viewModel = viewModel)
                    NavigationTab.BOOSTER -> GamingBoosterScreen(viewModel = viewModel)
                    NavigationTab.CACHE -> CacheCleanerScreen(viewModel = viewModel)
                }
            }

            // Real-time Optimization full-screen overlay (Interposed sequentially on boost start)
            if (boosterState != PerformanceViewModel.BoosterState.IDLE) {
                BoosterOverlay(
                    boosterState = boosterState,
                    logEntries = boosterLog,
                    selectedGameName = selectedGame?.appName ?: "Game Terpilih"
                )
            }
        }
    }
}
