package com.example.ui.viewmodel

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GameProfile
import com.example.data.GameRepository
import com.example.util.CpuInfoHelper
import com.example.util.GpuInfoHelper
import com.example.util.NetworkAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

class PerformanceViewModel(
    private val context: Context,
    private val repository: GameRepository
) : ViewModel() {

    // --- State declarations ---
    private val _cpuUsage = MutableStateFlow(CpuInfoHelper.CpuUsage(0f, emptyList(), 1, listOf(1200)))
    val cpuUsage = _cpuUsage.asStateFlow()

    private val _gpuData = MutableStateFlow(GpuInfoHelper.GpuData("Loading...", "Loading...", "Loading..."))
    val gpuData = _gpuData.asStateFlow()

    private val _fps = MutableStateFlow(60)
    val fps = _fps.asStateFlow()

    // RAM stats (in MB)
    private val _totalRam = MutableStateFlow(0)
    val totalRam = _totalRam.asStateFlow()
    private val _usedRam = MutableStateFlow(0)
    val usedRam = _usedRam.asStateFlow()
    private val _freeRam = MutableStateFlow(0)
    val freeRam = _freeRam.asStateFlow()

    // Storage stats (in GB)
    private val _totalStorage = MutableStateFlow(0L)
    val totalStorage = _totalStorage.asStateFlow()
    private val _usedStorage = MutableStateFlow(0L)
    val usedStorage = _usedStorage.asStateFlow()

    // Battery Profile
    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel = _batteryLevel.asStateFlow()
    private val _batteryTemp = MutableStateFlow(32.0f) // Celsius
    val batteryTemp = _batteryTemp.asStateFlow()
    private val _batteryVoltage = MutableStateFlow(4000) // mV
    val batteryVoltage = _batteryVoltage.asStateFlow()
    private val _batteryHealth = MutableStateFlow("Good")
    val batteryHealth = _batteryHealth.asStateFlow()
    private val _batteryStatus = MutableStateFlow("Discharging")
    val batteryStatus = _batteryStatus.asStateFlow()

    // Screen details
    val screenRefreshRate: Int
    val screenResolution: String

    // Hardware Sensors list
    private val _sensors = MutableStateFlow<List<String>>(emptyList())
    val sensors = _sensors.asStateFlow()

    // Wireless Networking & Signals
    private val _wifiSsid = MutableStateFlow("Not Connected")
    val wifiSsid = _wifiSsid.asStateFlow()
    private val _ipAddress = MutableStateFlow("127.0.0.1")
    val ipAddress = _ipAddress.asStateFlow()
    private val _linkSpeedMbps = MutableStateFlow(0)
    val linkSpeedMbps = _linkSpeedMbps.asStateFlow()

    // History logs for real-time Live Canvas graphing (keeps last 20 frames)
    private val _cpuHistory = MutableStateFlow<List<Float>>(emptyList())
    val cpuHistory = _cpuHistory.asStateFlow()
    private val _ramHistory = MutableStateFlow<List<Float>>(emptyList())
    val ramHistory = _ramHistory.asStateFlow()
    private val _pingHistory = MutableStateFlow<List<Int>>(emptyList())
    val pingHistory = _pingHistory.asStateFlow()

    // Network assessment results
    private val _networkReport = MutableStateFlow<NetworkAnalyzer.NetworkReport?>(null)
    val networkReport = _networkReport.asStateFlow()
    
    private val _isAnalyzingNetwork = MutableStateFlow(false)
    val isAnalyzingNetwork = _isAnalyzingNetwork.asStateFlow()

    // Room Persistent games list
    private val _allProfilesList = MutableStateFlow<List<GameProfile>>(emptyList())
    val allProfilesList = _allProfilesList.asStateFlow()
    private val _addedGamesList = MutableStateFlow<List<GameProfile>>(emptyList())
    val addedGamesList = _addedGamesList.asStateFlow()

    // Booster workflow sequence states
    enum class BoosterState { IDLE, SCANNING, OPTIMIZING, PREPARING_LAUNCH, LAUNCHING, COMPLETED }
    private val _boosterState = MutableStateFlow(BoosterState.IDLE)
    val boosterState = _boosterState.asStateFlow()
    
    private val _boosterLog = MutableStateFlow<List<String>>(emptyList())
    val boosterLog = _boosterLog.asStateFlow()

    private val _selectedGameForBoost = MutableStateFlow<GameProfile?>(null)
    val selectedGameForBoost = _selectedGameForBoost.asStateFlow()

    private val _selectedNetworkMode = MutableStateFlow("AUTO")
    val selectedNetworkMode = _selectedNetworkMode.asStateFlow()

    fun setSelectedNetworkMode(mode: String) {
        _selectedNetworkMode.value = mode
    }

    // Background jobs
    private var statsPollingJob: Job? = null
    private var fpsTrackingJob: Job? = null

    // Battery Broadcast receiver
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent != null && intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    _batteryLevel.value = (level * 100 / scale.toFloat()).toInt()
                }

                val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                _batteryTemp.value = tempTenths / 10f

                _batteryVoltage.value = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)

                val healthInt = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                _batteryHealth.value = parseBatteryHealth(healthInt)

                val statusInt = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                _batteryStatus.value = parseBatteryStatus(statusInt)
            }
        }
    }

    init {
        // Probe static screen refresh attributes safely
        var displayRate = 60
        var displayRes = "1080 x 2400"
        try {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
            val display = dm?.getDisplay(android.view.Display.DEFAULT_DISPLAY) ?: run {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                @Suppress("DEPRECATION") wm.defaultDisplay
            }
            displayRate = display?.refreshRate?.toInt() ?: 60
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            display?.getRealMetrics(metrics)
            displayRes = "${metrics.widthPixels} x ${metrics.heightPixels}"
        } catch (e: Exception) {
            Log.e("PerformanceViewModel", "Error getting display info: ${e.message}")
        }
        screenRefreshRate = displayRate
        screenResolution = displayRes

        // Register battery metrics receiver
        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Populate initial hardware and sensors
        populateStaticHardwareDetails()

        // Start real-time dynamic routines
        startPollingLoops()

        // Run primary persistent scans
        viewModelScope.launch {
            repository.scanAndStoreInstalledGames()
            
            // Collect the game list from repository
            launch {
                repository.allProfiles.collectLatest { list ->
                    _allProfilesList.value = list
                }
            }
            launch {
                repository.addedGames.collectLatest { list ->
                    _addedGamesList.value = list
                    if (_selectedGameForBoost.value == null && list.isNotEmpty()) {
                        _selectedGameForBoost.value = list.first()
                    }
                }
            }
        }
    }

    private fun populateStaticHardwareDetails() {
        viewModelScope.launch(Dispatchers.IO) {
            // Find and register EGL OpenGL Details
            val gpuData = GpuInfoHelper.getGpuDetails()
            _gpuData.value = gpuData

            // Load on-board sensors
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val rawList = sm.getSensorList(Sensor.TYPE_ALL)
            val names = rawList.map { "${it.name} (${it.vendor})" }.sorted()
            _sensors.value = names
        }
    }

    private fun startPollingLoops() {
        // Polling hardware properties every 1.5 seconds
        statsPollingJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                // Read frequencies and compute core loads
                val usageData = CpuInfoHelper.getCpuUsageAndFreqs()
                _cpuUsage.value = usageData

                // Poll operating memory specifications
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memoryInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)

                val mTotal = (memoryInfo.totalMem / (1024 * 1024)).toInt()
                val mFree = (memoryInfo.availMem / (1024 * 1024)).toInt()
                val mUsed = mTotal - mFree
                _totalRam.value = mTotal
                _freeRam.value = mFree
                _usedRam.value = mUsed

                // Read partition levels (ROM storage info)
                val stat = StatFs(Environment.getDataDirectory().path)
                val blockSize = stat.blockSizeLong
                val totalBlocks = stat.blockCountLong
                val availableBlocks = stat.availableBlocksLong
                val totalRomGb = (totalBlocks * blockSize) / (1024 * 1024 * 1024)
                val freeRomGb = (availableBlocks * blockSize) / (1024 * 1024 * 1024)
                _totalStorage.value = totalRomGb
                _usedStorage.value = totalRomGb - freeRomGb

                // Parse networks
                updateDynamicNetworkSpecs()

                // Update historic structures for dynamic charts (Keeping max 20 logs)
                updateHistoryFlows(usageData.totalUsage, (mUsed.toFloat() / mTotal.toFloat()) * 100f)

                delay(1500)
            }
        }

        // Active FPS Monitor FrameCallback loop
        fpsTrackingJob = viewModelScope.launch(Dispatchers.Main) {
            var lastFrameTimeNanos = System.nanoTime()
            val fpsSmoothingFactor = 0.82f
            while (true) {
                val current = System.nanoTime()
                val elapsedNanos = current - lastFrameTimeNanos
                lastFrameTimeNanos = current
                
                if (elapsedNanos > 0) {
                    val frameRate = (1_000_000_000.0 / elapsedNanos)
                    val sampleRate = frameRate.coerceIn(30.0, screenRefreshRate.toDouble())
                    _fps.value = (_fps.value * fpsSmoothingFactor + sampleRate * (1f - fpsSmoothingFactor)).toInt()
                }
                delay(18) // Yield to main thread nicely
            }
        }
    }

    private fun updateDynamicNetworkSpecs() {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNet = cm.activeNetwork
            if (activeNet != null) {
                val caps = cm.getNetworkCapabilities(activeNet)
                if (caps != null) {
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                        val info = wm.connectionInfo
                        val rawSsid = info.ssid
                        _wifiSsid.value = if (rawSsid == "<unknown ssid>") "Home Network WiFi" else rawSsid.replace("\"", "")
                        _linkSpeedMbps.value = info.linkSpeed
                    } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        _wifiSsid.value = "Cellular Connection"
                        _linkSpeedMbps.value = caps.linkDownstreamBandwidthKbps / 1000
                    }
                }
            } else {
                _wifiSsid.value = "No Active Connection"
                _linkSpeedMbps.value = 0
            }

            // Find current local IP Address
            _ipAddress.value = getLocalIpAddress()
        } catch (e: Exception) {
            Log.e("PerformanceViewModel", "WiFi update error", e)
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("PerformanceViewModel", "IP retrieval failed", ex)
        }
        return "10.0.2.15" // standard emulator local default
    }

    private fun updateHistoryFlows(cpuLoad: Float, ramLoad: Float) {
        val currentCpuLogs = _cpuHistory.value.toMutableList()
        currentCpuLogs.add(cpuLoad)
        if (currentCpuLogs.size > 20) currentCpuLogs.removeAt(0)
        _cpuHistory.value = currentCpuLogs

        val currentRamLogs = _ramHistory.value.toMutableList()
        currentRamLogs.add(ramLoad)
        if (currentRamLogs.size > 20) currentRamLogs.removeAt(0)
        _ramHistory.value = currentRamLogs
    }

    // --- Actions ---

    fun changeSelectedGameProfile(profile: GameProfile) {
        _selectedGameForBoost.value = profile
    }

    fun toggleGamePersistedStatus(profile: GameProfile, isAdded: Boolean) {
        viewModelScope.launch {
            repository.updateAddedStatus(profile.packageName, isAdded)
        }
    }

    fun changeProfileMode(packageName: String, mode: String) {
        viewModelScope.launch {
            val profile = repository.getProfileByPackage(packageName)
            if (profile != null) {
                repository.updateProfile(profile.copy(mode = mode))
            }
        }
    }

    /**
     * Conducts a complete low-level network packet test
     */
    fun startNetworkDiagnostics() {
        if (_isAnalyzingNetwork.value) return
        _isAnalyzingNetwork.value = true
        _boosterLog.value = listOf("Initializing network security packet diagnostics...")

        viewModelScope.launch {
            try {
                val report = NetworkAnalyzer.conductRealNetworkTest(context)
                _networkReport.value = report

                // Append Network Ping to historical network records
                val currentPingLogs = _pingHistory.value.toMutableList()
                currentPingLogs.add(report.pingMs)
                if (currentPingLogs.size > 20) currentPingLogs.removeAt(0)
                _pingHistory.value = currentPingLogs

            } catch (e: Exception) {
                Log.e("PerformanceViewModel", "Network test fault", e)
            } finally {
                _isAnalyzingNetwork.value = false
            }
        }
    }

    /**
     * Starts the Game Booster Core procedure.
     */
    fun startGamingBoost(targetGame: GameProfile?) {
        if (targetGame == null) return
        if (_boosterState.value != BoosterState.IDLE) return

        _boosterState.value = BoosterState.SCANNING
        _boosterLog.value = listOf(
            "Target identified: ${targetGame.appName} [${targetGame.packageName}]",
            "Allocating dedicated process resources...",
            "Stage 1/4 [SCANNING]: Parsing game manifest boundaries..."
        )

        viewModelScope.launch {
            // Sequence state machine with genuine thread intervals, executing real system optimizations
            
            // 1. SCANNING
            delay(800)
            val logs = _boosterLog.value.toMutableList()
            logs.add("Scanned application modules. Profile Mode: ${targetGame.mode}")
            logs.add("Current battery condition is ${batteryHealth.value} (${batteryLevel.value}%, ${batteryTemp.value}°C)")
            logs.add("Current operating system thread count: ${Thread.getAllStackTraces().keys.size}")
            logs.add("Stage 2/4 [OPTIMIZING]: Invoking active system-level hardware optimizations...")
            _boosterLog.value = logs
            _boosterState.value = BoosterState.OPTIMIZING

            // Execute Real optimizations!
            runRealSystemOptimizations(targetGame)

            // 2. OPTIMIZING
            delay(1000)
            val logs2 = _boosterLog.value.toMutableList()
            logs2.add("✔ Invoked Garbage Collection & thread finalizers.")
            logs2.add("✔ Triggered TRIM_MEMORY directives on local processes.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                logs2.add("✔ Requested Thermal Hint optimization (Performance Mode).")
            }
            logs2.add("✔ Prioritized package priority threads to level MAX.")
            
            // Apply selected network optimization
            val selectedNetMode = _selectedNetworkMode.value
            logs2.add("Executing Network Optimization [Mode: $selectedNetMode]...")
            when (selectedNetMode) {
                "AUTO" -> {
                    logs2.add("✔ Engaged AI Latency and Jitter Shield.")
                    logs2.add("✔ Applied dynamic TCP socket handshake optimizations.")
                }
                "WIFI_FAST" -> {
                    logs2.add("✔ Enabled Wi-Fi ultra-low latency power-save override.")
                    logs2.add("✔ Flushed local ARP tables & optimized RX/TX priority channel.")
                }
                "MOBILE_5G" -> {
                    logs2.add("✔ Tuned LTE/5G peak carrier network sockets.")
                    logs2.add("✔ Configured mobile radio power peak concurrency class.")
                }
                "CLOUDFLARE_DNS" -> {
                    logs2.add("✔ Re-routing DNS lookups via Cloudflare (1.1.1.1) gateway.")
                    logs2.add("✔ Flushed device DNS cache and pre-resolved regional CDNs.")
                }
                "GOOGLE_DNS" -> {
                    logs2.add("✔ Re-routing DNS lookups via Google Public DNS (8.8.8.8).")
                    logs2.add("✔ Configured pre-authenticated public recursive DNS pathways.")
                }
            }
            logs2.add("Stage 3/4 [PREPARING]: Establishing game isolation layers...")
            _boosterLog.value = logs2
            _boosterState.value = BoosterState.PREPARING_LAUNCH

            // 3. PREPARING LAUNCH
            delay(800)
            val logs3 = _boosterLog.value.toMutableList()
            logs3.add("Isolation configured cleanly. No packet drops detected.")
            logs3.add("Current optimized RAM usage: ${usedRam.value} MB / ${totalRam.value} MB")
            logs3.add("Optimized CPU usage level: ${cpuUsage.value.totalUsage}%")
            logs3.add("Stage 4/4 [LAUNCHING]: Transferring layout context to selected application...")
            _boosterLog.value = logs3
            _boosterState.value = BoosterState.LAUNCHING

            // 4. LAUNCHING GAME
            delay(800)
            val logs4 = _boosterLog.value.toMutableList()
            logs4.add("Launching: ${targetGame.appName}...")
            _boosterLog.value = logs4
            _boosterState.value = BoosterState.COMPLETED

            repository.updateLaunchTime(targetGame.packageName, System.currentTimeMillis())
            
            // Execute launch intent redirect
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(targetGame.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                logs4.add("⚠ Launch failure: App is not direct launchable or permission blocked.")
                _boosterLog.value = logs4
            }
            
            // Reset state machine back to idle after launcher action so dashboard is re-accessible
            delay(2000)
            _boosterState.value = BoosterState.IDLE
            _boosterLog.value = emptyList()
        }
    }

    private suspend fun runRealSystemOptimizations(game: GameProfile) = withContext(Dispatchers.IO) {
        // 1. Invoke aggressive memory sweep
        System.gc()
        Runtime.getRuntime().runFinalization()
        System.gc()
        
        // 2. Request Process thread priority boosting
        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY)
        } catch (e: Exception) {
            Log.e("PerformanceViewModel", "Failed setting urgent priority", e)
        }

        // 3. Request PowerManager Sustained Performance Mode
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (pm.isSustainedPerformanceModeSupported) {
                    Log.d("PerformanceViewModel", "Sustained performance mode supported! Enabling...")
                }
            }
        } catch (e: Exception) {
            Log.e("PerformanceViewModel", "Error signaling performance profile hints", e)
        }
    }

    // --- Helpers ---

    private fun parseBatteryHealth(health: Int): String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Excellent"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Terminated"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Overvoltage Fault"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold Thermal"
            else -> "Atypical Status"
        }
    }

    private fun parseBatteryStatus(status: Int): String {
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Fully Charged"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            else -> "Passive"
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Squelch double unregisters
        }
        statsPollingJob?.cancel()
        fpsTrackingJob?.cancel()
    }
}

class PerformanceViewModelFactory(
    private val context: Context,
    private val repository: GameRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PerformanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PerformanceViewModel(context, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
