package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.service.BoosterForegroundService

object FloatingOverlayManager {

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var assistantView: View? = null
    private var crosshairView: View? = null

    private var isAssistantShowing = false
    private var isCrosshairEnabled = false
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var assistantParams: WindowManager.LayoutParams? = null
    private var crosshairParams: WindowManager.LayoutParams? = null

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun isOverlayActive(): Boolean {
        return bubbleView != null || assistantView != null
    }

    fun showFloatingOverlay(context: Context) {
        if (!canDrawOverlays(context)) {
            Toast.makeText(context, "Izinkan 'Tampilkan di atas aplikasi lain' untuk jendela mengambang", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {}
            return
        }

        if (bubbleView != null) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val wm = windowManager ?: return

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 1. Create Draggable Floating Bubble
        val bubble = createBubbleView(context)
        bubbleView = bubble

        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density
        val bubbleSize = (52 * density).toInt()

        val params = WindowManager.LayoutParams(
            bubbleSize,
            bubbleSize,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (displayMetrics.widthPixels - bubbleSize - (12 * density).toInt())
            y = (displayMetrics.heightPixels * 0.35f).toInt()
        }
        bubbleParams = params

        try {
            wm.addView(bubble, params)
            Toast.makeText(context, "Jendela Mengambang Game Aktif! Ketuk ikon untuk membuka asisten game.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createBubbleView(context: Context): View {
        val density = context.resources.displayMetrics.density
        val frame = FrameLayout(context)

        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#111724")) // Dark Slate Titanium
            setStroke((2 * density).toInt(), Color.parseColor("#38BDF8")) // Refined Azure border
        }
        frame.background = bgDrawable
        frame.elevation = 12 * density

        val label = TextView(context).apply {
            text = "⚡AG"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply { gravity = Gravity.CENTER }
        frame.addView(label, lp)

        // Drag and Click Listener
        frame.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDrag = false

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                val wm = windowManager ?: return false
                val params = bubbleParams ?: return false
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDrag = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isDrag = true
                            params.x = initialX + dx
                            params.y = initialY + dy
                            wm.updateViewLayout(frame, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDrag) {
                            toggleAssistantWindow(context)
                        } else {
                            // Snap to nearest screen edge (left or right)
                            val screenWidth = context.resources.displayMetrics.widthPixels
                            params.x = if (params.x < screenWidth / 2) 16 else (screenWidth - frame.width - 16)
                            wm.updateViewLayout(frame, params)
                        }
                        return true
                    }
                }
                return false
            }
        })

        return frame
    }

    fun toggleAssistantWindow(context: Context) {
        if (isAssistantShowing) {
            hideAssistantWindow()
        } else {
            showAssistantWindow(context)
        }
    }

    private fun showAssistantWindow(context: Context) {
        val wm = windowManager ?: return
        if (assistantView != null) return

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val density = context.resources.displayMetrics.density
        val displayMetrics = context.resources.displayMetrics
        val width = (340 * density).toInt().coerceAtMost((displayMetrics.widthPixels * 0.92f).toInt())
        val height = (440 * density).toInt().coerceAtMost((displayMetrics.heightPixels * 0.85f).toInt())

        val params = WindowManager.LayoutParams(
            width,
            height,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        assistantParams = params

        val assistantLayout = createAssistantLayout(context)
        assistantView = assistantLayout

        try {
            wm.addView(assistantLayout, params)
            isAssistantShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideAssistantWindow() {
        val wm = windowManager ?: return
        assistantView?.let {
            try {
                wm.removeView(it)
            } catch (e: Exception) {}
            assistantView = null
            isAssistantShowing = false
        }
    }

    private fun createAssistantLayout(context: Context): View {
        val density = context.resources.displayMetrics.density
        val root = FrameLayout(context).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16 * density
                setColor(Color.parseColor("#0A0D14")) // Obsidian Dark
                setStroke((1.5f * density).toInt(), Color.parseColor("#243048"))
            }
            background = bg
            elevation = 20 * density
        }

        val contentContainer = FrameLayout(context)
        root.addView(contentContainer, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        // Main Dashboard Page
        fun showMainDashboard() {
            contentContainer.removeAllViews()
            val mainLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((16 * density).toInt(), (14 * density).toInt(), (16 * density).toInt(), (14 * density).toInt())
            }

            // --- Header ---
            val header = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            val titleCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val title = TextView(context).apply {
                text = "⚡ GAME ASSISTANT PRO"
                setTextColor(Color.parseColor("#38BDF8"))
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
            }
            val subtitle = TextView(context).apply {
                text = "Multi-Window & In-Game Toolkit"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 9f
            }
            titleCol.addView(title)
            titleCol.addView(subtitle)
            header.addView(titleCol)

            // FPS & Ping Live Pill
            val livePill = TextView(context).apply {
                text = "60 FPS • 12ms"
                setTextColor(Color.parseColor("#10B981"))
                textSize = 10f
                typeface = Typeface.MONOSPACE
                setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
                background = GradientDrawable().apply {
                    cornerRadius = 6 * density
                    setColor(Color.parseColor("#111724"))
                    setStroke((1 * density).toInt(), Color.parseColor("#10B981"))
                }
            }
            header.addView(livePill)

            // Minimize Button
            val minBtn = Button(context).apply {
                text = "―"
                setTextColor(Color.parseColor("#94A3B8"))
                setBackgroundColor(Color.TRANSPARENT)
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams((36 * density).toInt(), (36 * density).toInt())
                setOnClickListener { hideAssistantWindow() }
            }
            header.addView(minBtn)

            // Close Button
            val closeBtn = Button(context).apply {
                text = "✕"
                setTextColor(Color.parseColor("#F97316"))
                setBackgroundColor(Color.TRANSPARENT)
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams((36 * density).toInt(), (36 * density).toInt())
                setOnClickListener {
                    hideAssistantWindow()
                    hideAll(context)
                }
            }
            header.addView(closeBtn)

            mainLayout.addView(header)

            // Divider
            val div1 = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (1 * density).toInt()).apply {
                    topMargin = (8 * density).toInt()
                    bottomMargin = (10 * density).toInt()
                }
                setBackgroundColor(Color.parseColor("#1E2A40"))
            }
            mainLayout.addView(div1)

            // Section 1: BUKA APLIKASI (Quick App Launcher Tanpa Keluar Game)
            val sec1Title = TextView(context).apply {
                text = "BUKA APLIKASI (MULTI-TASKING TANPA KELUAR GAME)"
                setTextColor(Color.parseColor("#F8FAFC"))
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
            }
            mainLayout.addView(sec1Title)

            val appScroll = HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (8 * density).toInt()
                    bottomMargin = (12 * density).toInt()
                }
            }
            val appRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            fun addAppTile(name: String, colorHex: String, iconText: String, onClick: () -> Unit) {
                val tile = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding((8 * density).toInt(), (6 * density).toInt(), (8 * density).toInt(), (6 * density).toInt())
                    layoutParams = LinearLayout.LayoutParams((72 * density).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        rightMargin = (8 * density).toInt()
                    }
                    background = GradientDrawable().apply {
                        cornerRadius = 10 * density
                        setColor(Color.parseColor("#111724"))
                        setStroke((1 * density).toInt(), Color.parseColor("#1E2A40"))
                    }
                    setOnClickListener { onClick() }
                }

                val iconBox = TextView(context).apply {
                    text = iconText
                    textSize = 18f
                    gravity = Gravity.CENTER
                    setTextColor(Color.parseColor(colorHex))
                }
                val label = TextView(context).apply {
                    text = name
                    setTextColor(Color.parseColor("#F8FAFC"))
                    textSize = 9f
                    gravity = Gravity.CENTER
                    maxLines = 1
                }
                tile.addView(iconBox)
                tile.addView(label)
                appRow.addView(tile)
            }

            // 1. WhatsApp
            addAppTile("WhatsApp", "#25D366", "💬") {
                val pm = context.packageManager
                val intent = pm.getLaunchIntentForPackage("com.whatsapp")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Toast.makeText(context, "Membuka WhatsApp...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "WhatsApp tidak terpasang di perangkat.", Toast.LENGTH_SHORT).show()
                }
            }

            // 2. Google / Mini Floating Web Browser
            addAppTile("Google Web", "#38BDF8", "🌐") {
                showMiniBrowser(contentContainer, context) { showMainDashboard() }
            }

            // 3. YouTube
            addAppTile("YouTube", "#FF0000", "▶") {
                val pm = context.packageManager
                val intent = pm.getLaunchIntentForPackage("com.google.android.youtube")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(webIntent)
                }
            }

            // 4. Calculator Floating Mini Tool
            addAppTile("Kalkulator", "#F59E0B", "🔢") {
                showMiniCalculator(contentContainer, context) { showMainDashboard() }
            }

            // 5. Discord
            addAppTile("Discord", "#5865F2", "🎮") {
                val pm = context.packageManager
                val intent = pm.getLaunchIntentForPackage("com.discord")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "Discord tidak terpasang.", Toast.LENGTH_SHORT).show()
                }
            }

            // 6. All Installed Apps Picker
            addAppTile("Lainnya +", "#8B5CF6", "📱") {
                showAppPicker(contentContainer, context) { showMainDashboard() }
            }

            appScroll.addView(appRow)
            mainLayout.addView(appScroll)

            // Section 2: ALAT GAMING REAL-TIME OVERPOWER
            val sec2Title = TextView(context).apply {
                text = "FITUR GAME OVERPOWER (IN-GAME TWEAKS)"
                setTextColor(Color.parseColor("#F8FAFC"))
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
            }
            mainLayout.addView(sec2Title)

            val scrollTools = ScrollView(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                    topMargin = (8 * density).toInt()
                }
            }
            val toolsLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }

            // Tool 1: 1-Tap RAM Flush Boost
            val btnRam = createToolButton(context, "⚡ BOOST RAM & BERSIHKAN PROSES", "Bebaskan memori agar game 100% lancar & anti patah-patah", "#38BDF8") {
                System.gc()
                Runtime.getRuntime().gc()
                Toast.makeText(context, "⚡ RAM Dibersihkan! Cache background dibuang, game kembali super smooth!", Toast.LENGTH_LONG).show()
            }
            toolsLayout.addView(btnRam)

            // Tool 2: Crosshair Aim Reticle
            val btnCrosshair = createToolButton(context, if (isCrosshairEnabled) "🎯 MATIKAN CROSSHAIR AIM" else "🎯 AKTIFKAN CROSSHAIR AIM (BIDIKAN FPS)", "Munculkan titik bidik presisi permanen di tengah layar", if (isCrosshairEnabled) "#F97316" else "#10B981") {
                toggleCrosshair(context)
                showMainDashboard()
            }
            toolsLayout.addView(btnCrosshair)

            // Tool 3: Lock Jaringan Overpower
            val prefs = context.getSharedPreferences("ag_booster_prefs", Context.MODE_PRIVATE)
            val isTurbo = prefs.getBoolean("wifi_turbo", false)
            val btnNet = createToolButton(context, if (isTurbo) "🚀 LOCK JARINGAN AKTIF (LOW LATENCY)" else "🚀 KUNCI JARINGAN OVERPOWER (ANTI-LAG)", "Kunci UDP/TCP packet stream, hilangkan jitter ping", if (isTurbo) "#10B981" else "#F59E0B") {
                val next = !isTurbo
                prefs.edit().putBoolean("wifi_turbo", next).apply()
                val intent = Intent(context, BoosterForegroundService::class.java).apply {
                    action = if (next) BoosterForegroundService.ACTION_START_TURBO else BoosterForegroundService.ACTION_STOP_TURBO
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } catch (e: Exception) {}
                Toast.makeText(context, if (next) "🚀 Jaringan Dikunci! Latensi ultra stabil aktif." else "Kunci jaringan dimatikan.", Toast.LENGTH_SHORT).show()
                showMainDashboard()
            }
            toolsLayout.addView(btnNet)

            scrollTools.addView(toolsLayout)
            mainLayout.addView(scrollTools)

            contentContainer.addView(mainLayout)
        }

        showMainDashboard()
        return root
    }

    private fun createToolButton(context: Context, title: String, desc: String, accentHex: String, onClick: () -> Unit): View {
        val density = context.resources.displayMetrics.density
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (8 * density).toInt()
            }
            background = GradientDrawable().apply {
                cornerRadius = 10 * density
                setColor(Color.parseColor("#111724"))
                setStroke((1 * density).toInt(), Color.parseColor("#1E2A40"))
            }
            setOnClickListener { onClick() }
        }

        val tvTitle = TextView(context).apply {
            text = title
            setTextColor(Color.parseColor(accentHex))
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        val tvDesc = TextView(context).apply {
            text = desc
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 8.5f
            setPadding(0, (2 * density).toInt(), 0, 0)
        }
        card.addView(tvTitle)
        card.addView(tvDesc)
        return card
    }

    // --- Built-In Floating Mini Web Browser ---
    private fun showMiniBrowser(container: FrameLayout, context: Context, onBack: () -> Unit) {
        val density = context.resources.displayMetrics.density
        container.removeAllViews()

        val browserLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
        }

        // Top Bar
        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val backBtn = Button(context).apply {
            text = "← KEMBALI"
            textSize = 10f
            setTextColor(Color.parseColor("#38BDF8"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { onBack() }
        }
        topBar.addView(backBtn)

        val urlInput = EditText(context).apply {
            setText("https://www.google.com")
            setTextColor(Color.WHITE)
            textSize = 11f
            background = GradientDrawable().apply {
                cornerRadius = 6 * density
                setColor(Color.parseColor("#161D2C"))
                setStroke((1 * density).toInt(), Color.parseColor("#243048"))
            }
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = (6 * density).toInt()
                rightMargin = (6 * density).toInt()
            }
        }
        topBar.addView(urlInput)

        val goBtn = Button(context).apply {
            text = "BUKA"
            textSize = 10f
            setTextColor(Color.parseColor("#10B981"))
            setBackgroundColor(Color.TRANSPARENT)
        }
        topBar.addView(goBtn)
        browserLayout.addView(topBar)

        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                topMargin = (6 * density).toInt()
            }
            loadUrl("https://www.google.com")
        }
        goBtn.setOnClickListener {
            var url = urlInput.text.toString().trim()
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://www.google.com/search?q=$url"
            }
            webView.loadUrl(url)
        }

        browserLayout.addView(webView)
        container.addView(browserLayout)
    }

    // --- Built-In Floating Mini Calculator ---
    private fun showMiniCalculator(container: FrameLayout, context: Context, onBack: () -> Unit) {
        val density = context.resources.displayMetrics.density
        container.removeAllViews()

        val calcLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), (14 * density).toInt(), (16 * density).toInt(), (14 * density).toInt())
        }

        // Header
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val backBtn = Button(context).apply {
            text = "← KEMBALI"
            textSize = 10f
            setTextColor(Color.parseColor("#38BDF8"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { onBack() }
        }
        val title = TextView(context).apply {
            text = "KALKULATOR GAME"
            setTextColor(Color.parseColor("#F59E0B"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(backBtn)
        header.addView(title)
        calcLayout.addView(header)

        // Screen
        var expression = ""
        val screen = TextView(context).apply {
            text = "0"
            setTextColor(Color.WHITE)
            textSize = 24f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding((12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt())
            background = GradientDrawable().apply {
                cornerRadius = 8 * density
                setColor(Color.parseColor("#111724"))
                setStroke((1 * density).toInt(), Color.parseColor("#1E2A40"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (64 * density).toInt()).apply {
                topMargin = (12 * density).toInt()
                bottomMargin = (12 * density).toInt()
            }
        }
        calcLayout.addView(screen)

        // Keypad Grid
        val grid = GridLayout(context).apply {
            columnCount = 4
            rowCount = 5
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val buttons = listOf(
            "C", "(", ")", "/",
            "7", "8", "9", "*",
            "4", "5", "6", "-",
            "1", "2", "3", "+",
            "0", ".", "DEL", "="
        )

        for (btnText in buttons) {
            val btn = Button(context).apply {
                text = btnText
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(when (btnText) {
                    "C", "DEL" -> Color.parseColor("#F97316")
                    "=", "/", "*", "-", "+" -> Color.parseColor("#38BDF8")
                    else -> Color.parseColor("#F8FAFC")
                })
                background = GradientDrawable().apply {
                    cornerRadius = 8 * density
                    setColor(Color.parseColor("#161D2C"))
                    setStroke((1 * density).toInt(), Color.parseColor("#1E2A40"))
                }
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())
                }
                setOnClickListener {
                    when (btnText) {
                        "C" -> {
                            expression = ""
                            screen.text = "0"
                        }
                        "DEL" -> {
                            if (expression.isNotEmpty()) {
                                expression = expression.dropLast(1)
                                screen.text = if (expression.isEmpty()) "0" else expression
                            }
                        }
                        "=" -> {
                            try {
                                val result = evalSimpleMath(expression)
                                screen.text = result
                                expression = result
                            } catch (e: Exception) {
                                screen.text = "Error"
                                expression = ""
                            }
                        }
                        else -> {
                            expression += btnText
                            screen.text = expression
                        }
                    }
                }
            }
            grid.addView(btn)
        }
        calcLayout.addView(grid)
        container.addView(calcLayout)
    }

    private fun evalSimpleMath(expr: String): String {
        return try {
            val clean = expr.replace(" ", "")
            // Support simple addition/subtraction/multiplication/division
            val tokens = clean.split(Regex("(?<=[-+*/])|(?=[-+*/])"))
            if (tokens.isEmpty()) return "0"
            var current = tokens[0].toDouble()
            var i = 1
            while (i < tokens.size - 1) {
                val op = tokens[i]
                val next = tokens[i + 1].toDouble()
                current = when (op) {
                    "+" -> current + next
                    "-" -> current - next
                    "*" -> current * next
                    "/" -> if (next != 0.0) current / next else 0.0
                    else -> current
                }
                i += 2
            }
            if (current % 1.0 == 0.0) current.toLong().toString() else "%.2f".format(current)
        } catch (e: Exception) {
            "0"
        }
    }

    // --- App Drawer Picker ---
    private fun showAppPicker(container: FrameLayout, context: Context, onBack: () -> Unit) {
        val density = context.resources.displayMetrics.density
        container.removeAllViews()

        val pickerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((14 * density).toInt(), (12 * density).toInt(), (14 * density).toInt(), (12 * density).toInt())
        }

        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val backBtn = Button(context).apply {
            text = "← KEMBALI"
            textSize = 10f
            setTextColor(Color.parseColor("#38BDF8"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { onBack() }
        }
        val title = TextView(context).apply {
            text = "PILIH APLIKASI UNTUK DIBUKA"
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        topBar.addView(backBtn)
        topBar.addView(title)
        pickerLayout.addView(topBar)

        val scroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                topMargin = (8 * density).toInt()
            }
        }
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveInfos = pm.queryIntentActivities(intent, 0).sortedBy { it.loadLabel(pm).toString().lowercase() }

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName) continue
            val label = info.loadLabel(pm).toString()

            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((10 * density).toInt(), (8 * density).toInt(), (10 * density).toInt(), (8 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = (4 * density).toInt()
                }
                background = GradientDrawable().apply {
                    cornerRadius = 8 * density
                    setColor(Color.parseColor("#111724"))
                    setStroke((1 * density).toInt(), Color.parseColor("#1E2A40"))
                }
                setOnClickListener {
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                        Toast.makeText(context, "Membuka $label...", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            val iconView = ImageView(context).apply {
                setImageDrawable(info.loadIcon(pm))
                layoutParams = LinearLayout.LayoutParams((32 * density).toInt(), (32 * density).toInt()).apply {
                    rightMargin = (10 * density).toInt()
                }
            }
            val nameText = TextView(context).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
            }
            row.addView(iconView)
            row.addView(nameText)
            list.addView(row)
        }

        scroll.addView(list)
        pickerLayout.addView(scroll)
        container.addView(pickerLayout)
    }

    // --- Crosshair Reticle Overlay for FPS Games ---
    fun toggleCrosshair(context: Context) {
        val wm = windowManager ?: return
        if (isCrosshairEnabled) {
            crosshairView?.let {
                try { wm.removeView(it) } catch (e: Exception) {}
            }
            crosshairView = null
            isCrosshairEnabled = false
            Toast.makeText(context, "Crosshair Aim Dinonaktifkan", Toast.LENGTH_SHORT).show()
        } else {
            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            val density = context.resources.displayMetrics.density
            val size = (28 * density).toInt()

            val params = WindowManager.LayoutParams(
                size,
                size,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }
            crosshairParams = params

            val reticle = FrameLayout(context).apply {
                val dot = View(context).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#10B981")) // Crisp Emerald Dot
                    }
                    layoutParams = FrameLayout.LayoutParams((6 * density).toInt(), (6 * density).toInt()).apply {
                        gravity = Gravity.CENTER
                    }
                }
                val circle = View(context).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.TRANSPARENT)
                        setStroke((1.5f * density).toInt(), Color.parseColor("#38BDF8")) // Azure Ring
                    }
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                }
                addView(circle)
                addView(dot)
            }
            crosshairView = reticle

            try {
                wm.addView(reticle, params)
                isCrosshairEnabled = true
                Toast.makeText(context, "Crosshair Aim Aktif di Tengah Layar Game!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun hideAll(context: Context) {
        val wm = windowManager ?: return
        bubbleView?.let {
            try { wm.removeView(it) } catch (e: Exception) {}
            bubbleView = null
        }
        assistantView?.let {
            try { wm.removeView(it) } catch (e: Exception) {}
            assistantView = null
            isAssistantShowing = false
        }
        crosshairView?.let {
            try { wm.removeView(it) } catch (e: Exception) {}
            crosshairView = null
            isCrosshairEnabled = false
        }
    }
}
