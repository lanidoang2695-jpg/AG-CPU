package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.PerformanceViewModel

@Composable
fun BoosterOverlay(
    boosterState: PerformanceViewModel.BoosterState,
    logEntries: List<String>,
    selectedGameName: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BoosterGlobe")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BoosterGlobeRotation"
    )

    // Scroll console automatically to bottom
    val listState = rememberLazyListState()
    LaunchedEffect(logEntries.size) {
        if (logEntries.isNotEmpty()) {
            listState.animateScrollToItem(logEntries.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground.copy(alpha = 0.98f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Cyberpunk Hologram sphere
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                // Outer orbital dial
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scaleRotate(rotationAngle)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(listOf(NeonYellow, NeonOrange, NeonYellow)),
                            shape = CircleShape
                        )
                )

                // Inner pulsing orb
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(NeonYellow.copy(alpha = 0.25f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = NeonYellow,
                        modifier = Modifier.size(40.dp)
                    )
                }

                CircularProgressIndicator(
                    modifier = Modifier.size(116.dp),
                    color = NeonYellow,
                    strokeWidth = 3.dp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "MENJALANKAN OVERCLOCK HARDWARE TINGKAT LANJUT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MutedSlate,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = selectedGameName.uppercase(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // State indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatusTick(
                    label = "PINDAI",
                    isActive = boosterState == PerformanceViewModel.BoosterState.SCANNING,
                    isDone = boosterState.ordinal > PerformanceViewModel.BoosterState.SCANNING.ordinal
                )
                StatusTick(
                    label = "OPTIMALKAN",
                    isActive = boosterState == PerformanceViewModel.BoosterState.OPTIMIZING,
                    isDone = boosterState.ordinal > PerformanceViewModel.BoosterState.OPTIMIZING.ordinal
                )
                StatusTick(
                    label = "PERSIAPAN",
                    isActive = boosterState == PerformanceViewModel.BoosterState.PREPARING_LAUNCH,
                    isDone = boosterState.ordinal > PerformanceViewModel.BoosterState.PREPARING_LAUNCH.ordinal
                )
                StatusTick(
                    label = "MEMULAI",
                    isActive = boosterState == PerformanceViewModel.BoosterState.LAUNCHING,
                    isDone = boosterState.ordinal > PerformanceViewModel.BoosterState.LAUNCHING.ordinal
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Live scrolling log terminal console
            Text(
                "LOG DIAGNOSTIK SISTEM",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NeonYellow,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceSlate.copy(alpha = 0.5f))
                    .border(1.dp, DarkBorder.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(logEntries) { entry ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = NeonYellow,
                                modifier = Modifier.size(14.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = entry,
                                fontSize = 10.sp,
                                color = if (entry.startsWith("✔")) NeonGreen else PureWhite,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusTick(
    label: String,
    isActive: Boolean,
    isDone: Boolean
) {
    val color = when {
        isDone -> NeonGreen
        isActive -> NeonYellow
        else -> MutedSlate.copy(alpha = 0.4f)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
    }
}

// Custom rotation helper modifier
fun Modifier.scaleRotate(degrees: Float) = this.then(
    Modifier.graphicsLayer {
        rotationZ = degrees
    }
)
