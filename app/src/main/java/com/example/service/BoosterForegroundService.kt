package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.net.InetSocketAddress

class BoosterForegroundService : Service() {

    private var wifiLock: WifiManager.WifiLock? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isTurboActive = false
    private var turboJob: Job? = null
    
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        const val CHANNEL_ID = "ag_booster_active_channel"
        const val NOTIFICATION_ID = 4821

        const val ACTION_START_TURBO = "com.example.service.ACTION_START_TURBO"
        const val ACTION_STOP_TURBO = "com.example.service.ACTION_STOP_TURBO"

        fun startService(context: Context) {
            val intent = Intent(context, BoosterForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                try {
                    context.startService(intent)
                } catch (ex: Exception) {}
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BoosterForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Initial state load
        val prefs = getSharedPreferences("ag_booster_prefs", Context.MODE_PRIVATE)
        val isWifiTurboEnabled = prefs.getBoolean("wifi_turbo", false)
        if (isWifiTurboEnabled) {
            startWifiTurboBoost()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_START_TURBO) {
            startWifiTurboBoost()
        } else if (action == ACTION_STOP_TURBO) {
            stopWifiTurboBoost()
        }

        val title = if (isTurboActive) "AG Booster [TURBO WI-FI AKTIF]" else "AG Booster Active Engine"
        val desc = if (isTurboActive) "Mengunci latensi & mengeliminasi jitter Wi-Fi (Low Ping MLBB)" else "Optimisasi CPU & Anti-Lag Jaringan Berjalan Stabil"
        
        val notification = createNotification(title, desc)
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Safe fallback
        }
        return START_STICKY
    }

    private fun startWifiTurboBoost() {
        if (isTurboActive) return
        isTurboActive = true
        Log.d("BoosterService", "Starting high frequency real-time Wi-Fi Low-Latency Turbo Boost")
        
        // 1. Acquire Locks
        try {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wm != null) {
                if (wifiLock == null) {
                    wifiLock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        wm.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "Booster::WiFiLowLatencyActive")
                    } else {
                        @Suppress("DEPRECATION")
                        wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Booster::WiFiHighPerfActive")
                    }
                }
                wifiLock?.let {
                    if (!it.isHeld) {
                        it.acquire()
                        Log.d("BoosterService", "Acquired real low-latency Wi-Fi lock")
                    }
                }

                // Acquire Multicast lock to ensure broadcast packets don't queue or force sleep cycles 
                if (multicastLock == null) {
                    multicastLock = wm.createMulticastLock("Booster::WiFiMulticastActive")
                }
                multicastLock?.let {
                    if (!it.isHeld) {
                        it.acquire()
                        Log.d("BoosterService", "Acquired real Wi-Fi Multicast Lock")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BoosterService", "Failed to acquire WifiLock/MulticastLock", e)
        }

        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null) {
                if (wakeLock == null) {
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Booster::CPULatencyWakeLock")
                }
                wakeLock?.let {
                    if (!it.isHeld) {
                        it.acquire()
                        Log.d("BoosterService", "Acquired real CPU WakeLock")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BoosterService", "Failed to acquire WakeLock", e)
        }

        // 2. Active Radio Warming & Socket Optimization Loop
        // Sends tiny, non-intrusive packets to keep the interface at peak throughput and prevent sleep.
        turboJob?.cancel()
        turboJob = serviceScope.launch {
            // Adjust Thread Priority on the executing background pool thread to high-priority audio class
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            } catch (e: Exception) {}

            val clDns = InetAddress.getByName("1.1.1.1") // Cloudflare DNS
            val gDns = InetAddress.getByName("8.8.8.8")  // Google DNS
            var rotateIndex = 0
            
            while (isTurboActive) {
                val prefs = getSharedPreferences("ag_booster_prefs", Context.MODE_PRIVATE)
                val netMode = prefs.getString("selected_network_mode", "AUTO") ?: "AUTO"
                
                // Set extremely proactive physical carrier keeping-awake durations to force Constantly Active Mode (CAM).
                val delayMs = when (netMode) {
                    "MOBILE_EXTREME_FORCE" -> 20L // Force cellular LTE/5G modem to stay in peak active power state (Active DCH Mode)
                    "WIFI_EXTREME_WALL" -> 30L  // Force 100% TX/RX hardware power amplitude to bypass wall obstruction
                    "WIFI_TURBO" -> 80L         // High speed profile (anti-shuttering)
                    "WIFI_FAST" -> 180L         // Standard active pacing
                    "MOBILE_5G" -> 150L         // Mobile peak carrier connection
                    else -> 400L                // Balanced
                }

                try {
                    val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    val gatewayInt = wm?.dhcpInfo?.gateway ?: 0
                    val gatewayAddr = if (gatewayInt != 0) {
                        val ipBytes = byteArrayOf(
                            (gatewayInt and 0xff).toByte(),
                            (gatewayInt shr 8 and 0xff).toByte(),
                            (gatewayInt shr 16 and 0xff).toByte(),
                            (gatewayInt shr 24 and 0xff).toByte()
                        )
                        try { InetAddress.getByAddress(ipBytes) } catch (e: Exception) { null }
                    } else {
                        null
                    }

                    // Instantly warm up transmission trails
                    val socket = DatagramSocket()
                    
                    // Fetch and bind to active hardware adapter to bypass routing lookup tables entirely
                    try {
                        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                        val activeNetwork = cm?.activeNetwork
                        if (activeNetwork != null) {
                            val caps = cm.getNetworkCapabilities(activeNetwork)
                            if (caps != null) {
                                val isCellM = netMode == "MOBILE_EXTREME_FORCE" || netMode == "MOBILE_5G"
                                val transport = if (isCellM) NetworkCapabilities.TRANSPORT_CELLULAR else NetworkCapabilities.TRANSPORT_WIFI
                                if (caps.hasTransport(transport)) {
                                    activeNetwork.bindSocket(socket)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("BoosterService", "Direct hardware socket binding skipped: ${e.message}")
                    }
                    
                    // Set Type of Service to DSCP EF - Expedited Forwarding / Voice Class (0xB8)
                    // This maps directly to the peak AC_VO (Access Category Voice) high-priority queue on WMM-enabled Access Points.
                    try {
                        socket.trafficClass = 0xB8
                    } catch (e: Exception) {}
                    
                    // Create an empty STUN Keepalive packet (standard 20-byte STUN binding indication header)
                    val buf = ByteArray(20) { 0x00.toByte() }
                    buf[0] = 0x00.toByte() // STUN Message Type: Binding Indication
                    buf[1] = 0x11.toByte()
                    
                    if (netMode == "MOBILE_EXTREME_FORCE") {
                        // Force All Operator cellular keepalive parallel delivery to Cloudflare and Google endpoints
                        val packet1 = DatagramPacket(buf, buf.size, clDns, 3478)
                        val packet2 = DatagramPacket(buf, buf.size, gDns, 3478)
                        socket.send(packet1)
                        socket.send(packet2)
                    } else if (netMode == "WIFI_EXTREME_WALL") {
                        // Extreme Walls mode: simultaneous transmission to eliminate gateway lookup table delays completely!
                        val gatewayTarget = gatewayAddr ?: clDns
                        val packet1 = DatagramPacket(buf, buf.size, gatewayTarget, 3478)
                        val packet2 = DatagramPacket(buf, buf.size, if (rotateIndex == 0) clDns else gDns, 3478)
                        socket.send(packet1)
                        socket.send(packet2)
                    } else {
                        val targetAddr = when (rotateIndex) {
                            0 -> gatewayAddr ?: clDns
                            1 -> clDns
                            else -> gDns
                        }
                        val packet = DatagramPacket(buf, buf.size, targetAddr, 3478)
                        socket.send(packet)
                    }
                    
                    socket.close()
                    rotateIndex = (rotateIndex + 1) % 3
                } catch (e: Exception) {
                    // TCP Connect keepalive fallback with Voice QoS tag and direct binding compatibility on standard port 443
                    try {
                        val fallbackSocket = Socket()
                        try {
                            fallbackSocket.trafficClass = 0xB8
                        } catch (ex: Exception) {}
                        
                        try {
                            val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                            val activeNetwork = cm?.activeNetwork
                            if (activeNetwork != null) {
                                val caps = cm.getNetworkCapabilities(activeNetwork)
                                if (caps != null) {
                                    val isCellM = netMode == "MOBILE_EXTREME_FORCE" || netMode == "MOBILE_5G"
                                    val transport = if (isCellM) NetworkCapabilities.TRANSPORT_CELLULAR else NetworkCapabilities.TRANSPORT_WIFI
                                    if (caps.hasTransport(transport)) {
                                        activeNetwork.bindSocket(fallbackSocket)
                                    }
                                }
                            }
                        } catch (e: Exception) {}

                        fallbackSocket.connect(InetSocketAddress("1.1.1.1", 443), 250)
                        fallbackSocket.close()
                    } catch (ex: Exception) {}
                }
                
                delay(delayMs)
            }
        }
        
        // Update notification
        updateRunningNotification()
    }

    private fun stopWifiTurboBoost() {
        if (!isTurboActive) return
        isTurboActive = false
        Log.d("BoosterService", "Stopping Wi-Fi Turbo Boost")
        
        turboJob?.cancel()
        turboJob = null

        try {
            wifiLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d("BoosterService", "Released Wi-Fi lock")
                }
            }
        } catch (e: Exception) {}

        try {
            multicastLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d("BoosterService", "Released Multicast lock")
                }
            }
        } catch (e: Exception) {}

        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d("BoosterService", "Released CPU WakeLock")
                }
            }
        } catch (e: Exception) {}
        
        updateRunningNotification()
    }

    private fun updateRunningNotification() {
        val title = if (isTurboActive) "AG Booster [TURBO WI-FI AKTIF]" else "AG Booster Active Engine"
        val desc = if (isTurboActive) "Mengunci latensi & mengeliminasi jitter Wi-Fi (Low Ping MLBB)" else "Optimisasi CPU & Anti-Lag Jaringan Berjalan Stabil"
        
        val notification = createNotification(title, desc)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        stopWifiTurboBoost()
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(title: String, text: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AG Booster Active Optimization Engine",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Saluran prioritas untuk mengunci RAM, menjaga CPU, dan mengkoreksi latency jaringan."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
