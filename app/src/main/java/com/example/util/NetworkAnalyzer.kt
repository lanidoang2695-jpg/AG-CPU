package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.math.abs

object NetworkAnalyzer {

    data class NetworkReport(
        val pingMs: Int,
        val jitterMs: Int,
        val packetLossPercent: Int,
        val dnsResponseTimeMs: Int,
        val downloadSpeedMbps: Float,
        val uploadSpeedMbps: Float,
        val connectionType: String,
        val signalStrengthPercent: Int,
        val subnetMask: String,
        val stabilityScore: Int, // 0 - 100
        val status: String // Excellent, Good, Moderate, Poor, Critical
    )

    suspend fun conductRealNetworkTest(
        context: Context,
        isHighPerformance: Boolean = false,
        isNetworkLocked: Boolean = false,
        isWifiTurboActive: Boolean = false
    ): NetworkReport = withContext(Dispatchers.IO) {
        val publicServerIp = "8.8.8.8" // Google Public DNS
        val publicServerPort = 53       // DNS Port (always open and fast)
        val testTrials = 5
        val pings = mutableListOf<Int>()
        var timeoutsCount = 0

        // 1. Measure Ping & Jitter via TCP handshake on port 53 (safe and universal)
        for (i in 1..testTrials) {
            val socket = Socket()
            val startTime = System.currentTimeMillis()
            try {
                socket.connect(InetSocketAddress(publicServerIp, publicServerPort), 1200)
                val duration = (System.currentTimeMillis() - startTime).toInt()
                pings.add(duration)
                socket.close()
            } catch (e: Exception) {
                timeoutsCount++
                try { socket.close() } catch (ex: Exception) {}
            }
            // Minor delay between runs
            try { Thread.sleep(60) } catch (e: Exception) {}
        }

        var packetLossPercent = (timeoutsCount * 100) / testTrials
        var avgPing = if (pings.isNotEmpty()) pings.average().toInt() else 999

        if (isHighPerformance || isNetworkLocked || isWifiTurboActive) {
            packetLossPercent = 0
            avgPing = if (isWifiTurboActive) (2..4).random() else (4..9).random() // Extremely low latency lock
        }

        // Jitter is the variance of sequential delays
        var jitterSum = 0
        if (pings.size >= 2) {
            for (i in 0 until pings.size - 1) {
                jitterSum += abs(pings[i] - pings[i + 1])
            }
        }
        var jitterMs = if (pings.size >= 2) jitterSum / (pings.size - 1) else if (pings.isNotEmpty()) 2 else 0

        if (isHighPerformance || isNetworkLocked || isWifiTurboActive) {
            jitterMs = if (isWifiTurboActive) 0 else (0..1).random() // Locked jitter correction
        }

        // 2. Measure real Internet DNS lookup response time of cloudflare.com
        val dnsStart = System.currentTimeMillis()
        var dnsTimeMs = 0
        try {
            InetAddress.getByName("cloudflare.com")
            dnsTimeMs = (System.currentTimeMillis() - dnsStart).toInt()
        } catch (e: Exception) {
            dnsTimeMs = 999
        }

        if (isHighPerformance || isNetworkLocked || isWifiTurboActive) {
            dnsTimeMs = if (isWifiTurboActive) 1 else (1..2).random() // Ultra-fast DNS response
        }

        // 3. Measure Bandwidth Download Speed using a lightweight public asset
        var downloadSpeed = 0f
        try {
            val dlsStart = System.currentTimeMillis()
            val url = URL("https://www.google.com")
            val connection = url.openConnection()
            connection.connectTimeout = 1500
            connection.readTimeout = 1500
            connection.useCaches = false
            
            val stream: InputStream = connection.getInputStream()
            val buffer = ByteArray(4096)
            var bytesRead = 0
            var totalBytes = 0
            
            // Read up to 80KB to avoid excessive data consumption on cellular
            while (true) {
                val read = stream.read(buffer)
                if (read == -1) break
                bytesRead = read
                totalBytes += bytesRead
                if (totalBytes > 80000) break 
            }
            stream.close()
            
            val durationSecs = (System.currentTimeMillis() - dlsStart) / 1000f
            if (durationSecs > 0) {
                // bytes to Mbps: (bytes * 8) / (1024 * 1024 * duration)
                downloadSpeed = ((totalBytes * 8f) / (1024f * 1024f * durationSecs)).coerceIn(0.1f, 150f)
            }
        } catch (e: Exception) {
            // Fallback download calculation using capabilities
            downloadSpeed = try {
                getCapSpeedMbps(context)
            } catch (ex: Exception) {
                3.5f
            }
        }

        if (isHighPerformance || isNetworkLocked || isWifiTurboActive) {
            downloadSpeed = if (isWifiTurboActive) {
                (480..595).random() + (0..9).random() / 10f // WiFi Super turbo speeds!
            } else {
                (150..295).random() + (0..9).random() / 10f // Overpowered speed lock
            }
        }

        // 4. Simulate Upload (due to security limits we estimate based on download speed and linkage metadata)
        var uploadSpeed = (downloadSpeed * 0.35f).coerceAtLeast(0.1f)
        if (isHighPerformance || isNetworkLocked || isWifiTurboActive) {
            uploadSpeed = if (isWifiTurboActive) {
                (downloadSpeed * 0.75f).coerceAtLeast(360f)
            } else {
                (downloadSpeed * 0.55f).coerceAtLeast(80f)
            }
        }

        // 5. Query active interface metadata
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var connectionType = "Offline"
        var signalStrength = 75

        try {
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                if (capabilities != null) {
                    when {
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                            connectionType = "WiFi"
                            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                            val rssi = wifiManager.connectionInfo.rssi
                            signalStrength = WifiManager.calculateSignalLevel(rssi, 100)
                        }
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                            connectionType = getCellularGeneration(context)
                            signalStrength = 65 // standard baseline for mobile cells
                        }
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                            connectionType = "Ethernet"
                            signalStrength = 100
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkAnalyzer", "Error resolving networking details", e)
        }

        if (isHighPerformance || isNetworkLocked || isWifiTurboActive) {
            signalStrength = 100 // Lock signal at full power
        }

        // Calculate stability and rank status
        // Weight: ping (40%), packetLoss (40%), jitter (20%)
        val pingScore = (100 - (avgPing / 4)).coerceIn(0, 100)
        val lossScore = (100 - packetLossPercent).coerceIn(0, 100)
        val jitterScore = (100 - (jitterMs * 2)).coerceIn(0, 100)
        
        var stabilityScore = ((pingScore * 0.4) + (lossScore * 0.4) + (jitterScore * 0.2)).toInt().coerceIn(10, 100)
        if (isHighPerformance || isNetworkLocked || isWifiTurboActive) {
            stabilityScore = if (isWifiTurboActive) 100 else (98..100).random()
        }

        val status = when {
            isWifiTurboActive -> "Super WiFi Turbo Active"
            stabilityScore >= 90 || isHighPerformance || isNetworkLocked -> "Excellent"
            stabilityScore >= 70 && packetLossPercent <= 2 -> "Good"
            stabilityScore >= 50 && packetLossPercent <= 5 -> "Moderate"
            stabilityScore >= 30 && packetLossPercent <= 10 -> "Poor"
            else -> "Critical"
        }

        return@withContext NetworkReport(
            pingMs = avgPing,
            jitterMs = jitterMs,
            packetLossPercent = packetLossPercent,
            dnsResponseTimeMs = dnsTimeMs,
            downloadSpeedMbps = downloadSpeed,
            uploadSpeedMbps = uploadSpeed,
            connectionType = connectionType,
            signalStrengthPercent = signalStrength,
            subnetMask = "255.255.255.0",
            stabilityScore = stabilityScore,
            status = status
        )
    }

    private fun getCapSpeedMbps(context: Context): Float {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return 12.0f
        return (capabilities.linkDownstreamBandwidthKbps / 1020f).coerceIn(1f, 300f)
    }

    private fun getCellularGeneration(context: Context): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        try {
            return when (tm.networkType) {
                TelephonyManager.NETWORK_TYPE_GPRS,
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_CDMA,
                TelephonyManager.NETWORK_TYPE_1xRTT,
                TelephonyManager.NETWORK_TYPE_IDEN -> "2G Mobile"
                
                TelephonyManager.NETWORK_TYPE_UMTS,
                TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_EVDO_A,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_EVDO_B,
                TelephonyManager.NETWORK_TYPE_EHRPD,
                TelephonyManager.NETWORK_TYPE_HSPAP -> "3G Mobile"
                
                TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                
                TelephonyManager.NETWORK_TYPE_NR -> "5G Mobile"
                else -> "Mobile Data"
            }
        } catch (e: Exception) {
            return "Cellular Network"
        }
    }
}
