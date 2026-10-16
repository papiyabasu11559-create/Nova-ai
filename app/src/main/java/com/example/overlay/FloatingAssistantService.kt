package com.example.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.NovaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FloatingAssistantService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START_OVERLAY"
        const val ACTION_STOP = "ACTION_STOP_OVERLAY"
        const val EXTRA_ACTION = "EXTRA_ACTION"
        const val ACTION_VOICE = "VOICE"
        const val ACTION_CHAT = "CHAT"
        const val ACTION_CAMERA = "CAMERA"

        private const val NOTIFICATION_ID = 4040
        private const val CHANNEL_ID = "nova_overlay_channel"

        var isRunning = false
            private set

        fun stopOverlay(context: Context) {
            val intent = Intent(context, FloatingAssistantService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var bubbleView: FloatingBubbleView? = null
    private var params: WindowManager.LayoutParams? = null

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var screenWidth = 0
    private var screenHeight = 0
    private var density = 1.0f
    private var normalWindowSizePx = 0
    private var isHalfDocked = false
    private var isAtLeftEdge = false

    // Screen off/on broadcast receiver for performance & battery saving
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    bubbleView?.pauseAnimations()
                    cancelIdleTimer()
                }
                Intent.ACTION_SCREEN_ON -> {
                    bubbleView?.resumeAnimations()
                    resetIdleTimer()
                }
            }
        }
    }

    // 3-second idle timer: dim to 60% and half-hide
    private val idleHalfHideRunnable = Runnable {
        performHalfHideAndDim()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        showFloatingBubble()
        isRunning = true

        observeAssistantStateAndConfig()

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NOVA AI Floating Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active status of the floating assistant bubble"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingLaunch = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingAssistantService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NOVA AI Floating Assistant")
            .setContentText("Tap to open or drag HUD bubble")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingLaunch)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Bubble", pendingStop)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingBubble() {
        if (bubbleView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = resources.displayMetrics
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        density = metrics.density

        val initialConfig = BubblePreferencesManager.currentConfig.value
        normalWindowSizePx = ((initialConfig.sizeDp + 48) * density).toInt()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        // Background blur behind the bubble on Android 12+ (FLAG_BLUR_BEHIND / blurBehindRadius)
        var blurSupported = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (windowManager?.isCrossWindowBlurEnabled == true) {
                    flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                    blurSupported = true
                }
            } catch (e: Throwable) {
                // Fallback to plain translucency if blur not supported or on emulators
            }
        }

        params = WindowManager.LayoutParams(
            normalWindowSizePx,
            normalWindowSizePx,
            layoutType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - normalWindowSizePx - (12 * density).toInt()
            y = (screenHeight * 0.35f).toInt()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurSupported) {
                try {
                    blurBehindRadius = (16 * density).toInt()
                } catch (e: Throwable) {
                    // Ignored
                }
            }
        }

        isAtLeftEdge = false

        bubbleView = FloatingBubbleView(
            context = this,
            config = initialConfig,
            state = BubblePreferencesManager.currentState.value,
            listener = object : FloatingBubbleView.Listener {
                override fun onBubbleTap() {
                    if (isHalfDocked) {
                        undockBubble()
                    } else {
                        launchAppAction(ACTION_CHAT)
                    }
                }

                override fun onDrag(deltaX: Float, deltaY: Float) {
                    cancelIdleTimer()
                    if (isHalfDocked) {
                        undockBubble(animate = false)
                    }
                    bubbleView?.idleAlphaFactor = 1.0f

                    params?.let { p ->
                        p.x = (p.x + deltaX.toInt()).coerceIn(-normalWindowSizePx / 2, screenWidth - normalWindowSizePx / 2)
                        p.y = (p.y + deltaY.toInt()).coerceIn(0, screenHeight - normalWindowSizePx)
                        windowManager?.updateViewLayout(bubbleView, p)
                    }
                }

                override fun onDragEnd(rawX: Float, rawY: Float) {
                    snapToNearestEdge()
                }

                override fun onMenuOpenRequested() {
                    cancelIdleTimer()
                    expandWindowForRadialMenu()
                }

                override fun onMenuClosed() {
                    collapseWindowAfterRadialMenu()
                    resetIdleTimer()
                }

                override fun onVoiceAction() {
                    launchAppAction(ACTION_VOICE)
                }

                override fun onChatAction() {
                    launchAppAction(ACTION_CHAT)
                }

                override fun onCameraAction() {
                    launchAppAction(ACTION_CAMERA)
                }

                override fun onCloseAction() {
                    stopSelf()
                }
            }
        )

        try {
            windowManager?.addView(bubbleView, params)
            resetIdleTimer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observeAssistantStateAndConfig() {
        val app = application as? NovaApplication ?: return

        // 1. Observe Audio & AI states to reflect: LISTENING, THINKING, SPEAKING, IDLE
        serviceScope.launch {
            combine(
                app.voiceSubsystem.isListening,
                app.voiceSubsystem.isSpeaking,
                app.apiClient.isGenerating
            ) { isListening, isSpeaking, isGenerating ->
                when {
                    isListening -> BubbleAssistantState.LISTENING
                    isGenerating -> BubbleAssistantState.THINKING
                    isSpeaking -> BubbleAssistantState.SPEAKING
                    else -> BubbleAssistantState.IDLE
                }
            }.collect { newState ->
                BubblePreferencesManager.currentState.value = newState
                bubbleView?.state = newState
                bubbleView?.invalidate()
            }
        }

        // 2. Observe Live Bubble Customization Settings (size, opacity, glow, theme)
        serviceScope.launch {
            BubblePreferencesManager.currentConfig.collect { newConfig ->
                bubbleView?.config = newConfig
                val newSizePx = ((newConfig.sizeDp + 48) * density).toInt()

                if (newSizePx != normalWindowSizePx && bubbleView?.isMenuOpen != true) {
                    normalWindowSizePx = newSizePx
                    params?.let { p ->
                        p.width = newSizePx
                        p.height = newSizePx
                        windowManager?.updateViewLayout(bubbleView, p)
                    }
                }
                bubbleView?.updateDimensions()
            }
        }
    }

    private fun snapToNearestEdge() {
        val p = params ?: return
        val currentCenterX = p.x + p.width / 2
        val snapLeft = currentCenterX < screenWidth / 2
        isAtLeftEdge = snapLeft

        val targetX = if (snapLeft) 0 else screenWidth - p.width

        val animator = ValueAnimator.ofInt(p.x, targetX).apply {
            duration = 320
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener {
                params?.x = it.animatedValue as Int
                windowManager?.updateViewLayout(bubbleView, params)
            }
        }
        animator.start()
        resetIdleTimer()
    }

    private fun resetIdleTimer() {
        cancelIdleTimer()
        mainHandler.postDelayed(idleHalfHideRunnable, 3000)
    }

    private fun cancelIdleTimer() {
        mainHandler.removeCallbacks(idleHalfHideRunnable)
    }

    private fun performHalfHideAndDim() {
        if (bubbleView?.isMenuOpen == true) return
        val p = params ?: return
        isHalfDocked = true

        // 1. Animate alpha to 60%
        val alphaAnimator = ValueAnimator.ofFloat(bubbleView?.idleAlphaFactor ?: 1.0f, 0.60f).apply {
            duration = 300
            addUpdateListener {
                bubbleView?.idleAlphaFactor = it.animatedValue as Float
            }
        }
        alphaAnimator.start()

        // 2. Animate half-hide into edge
        val currentX = p.x
        val targetX = if (isAtLeftEdge) {
            -p.width / 2 + (12 * density).toInt()
        } else {
            screenWidth - p.width / 2 - (12 * density).toInt()
        }

        val slideAnimator = ValueAnimator.ofInt(currentX, targetX).apply {
            duration = 300
            addUpdateListener {
                params?.x = it.animatedValue as Int
                windowManager?.updateViewLayout(bubbleView, params)
            }
        }
        slideAnimator.start()
    }

    private fun undockBubble(animate: Boolean = true) {
        isHalfDocked = false
        val p = params ?: return

        bubbleView?.idleAlphaFactor = 1.0f

        val targetX = if (isAtLeftEdge) 0 else screenWidth - p.width
        if (animate) {
            val animator = ValueAnimator.ofInt(p.x, targetX).apply {
                duration = 220
                interpolator = OvershootInterpolator(1.3f)
                addUpdateListener {
                    params?.x = it.animatedValue as Int
                    windowManager?.updateViewLayout(bubbleView, params)
                }
            }
            animator.start()
        } else {
            p.x = targetX
            windowManager?.updateViewLayout(bubbleView, p)
        }
        resetIdleTimer()
    }

    private fun expandWindowForRadialMenu() {
        val p = params ?: return
        val bubbleCenterX = p.x + p.width / 2
        val bubbleCenterY = p.y + p.height / 2

        val menuWindowSize = (230 * density).toInt()
        p.width = menuWindowSize
        p.height = menuWindowSize
        p.x = (bubbleCenterX - menuWindowSize / 2).coerceIn(0, screenWidth - menuWindowSize)
        p.y = (bubbleCenterY - menuWindowSize / 2).coerceIn(0, screenHeight - menuWindowSize)

        windowManager?.updateViewLayout(bubbleView, p)
    }

    private fun collapseWindowAfterRadialMenu() {
        val p = params ?: return
        val bubbleCenterX = p.x + p.width / 2
        val bubbleCenterY = p.y + p.height / 2

        p.width = normalWindowSizePx
        p.height = normalWindowSizePx
        p.x = (bubbleCenterX - normalWindowSizePx / 2).coerceIn(0, screenWidth - normalWindowSizePx)
        p.y = (bubbleCenterY - normalWindowSizePx / 2).coerceIn(0, screenHeight - normalWindowSizePx)

        windowManager?.updateViewLayout(bubbleView, p)
        snapToNearestEdge()
    }

    private fun launchAppAction(actionType: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_ACTION, actionType)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        cancelIdleTimer()
        serviceJob.cancel()

        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Ignored
        }

        bubbleView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bubbleView = null
        }
    }
}
