package com.example.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@SuppressLint("ViewConstructor")
class FloatingBubbleView(
    context: Context,
    var config: BubbleConfig = BubbleConfig(),
    var state: BubbleAssistantState = BubbleAssistantState.IDLE,
    private val listener: Listener
) : View(context) {

    interface Listener {
        fun onBubbleTap()
        fun onDrag(deltaX: Float, deltaY: Float)
        fun onDragEnd(rawX: Float, rawY: Float)
        fun onMenuOpenRequested()
        fun onMenuClosed()
        fun onVoiceAction()
        fun onChatAction()
        fun onCameraAction()
        fun onCloseAction()
    }

    private val density = resources.displayMetrics.density

    // Paint objects (pre-allocated for 60fps performance without GC allocation)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    private val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boltGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val menuBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val menuBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 10f * density
        isFakeBoldText = true
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Layout & Geometry
    private val highlightRect = RectF()
    private val boltPath = Path()
    private val menuButtonRect = RectF()

    // Scales & Animation states
    private var baseRadiusPx = (config.sizeDp * density) / 2f
    private var scaleMultiplier = 1.0f
    private var isTouching = false
    private var isDragging = false
    private var isAnimationsActive = true

    // Idle dimming: 1.0f when active, dims to 0.60f after 3s idle
    var idleAlphaFactor = 1.0f
        set(value) {
            field = value.coerceIn(0.2f, 1.0f)
            invalidate()
        }

    // Drag light trail coordinates
    private data class TrailPoint(val x: Float, val y: Float, val time: Long)
    private val trailPoints = mutableListOf<TrailPoint>()

    // Quick radial menu
    var isMenuOpen = false
        private set
    private var menuProgress = 0f
    private var menuAnimator: ValueAnimator? = null

    // Touch event tracking
    private var touchDownTime = 0L
    private var touchDownRawX = 0f
    private var touchDownRawY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f
    private val touchSlop = 8f * density
    private var isLongPressScheduled = false

    private val longPressRunnable = Runnable {
        if (!isDragging && isTouching && !isMenuOpen) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            openRadialMenu()
        }
    }

    init {
        try {
            setLayerType(LAYER_TYPE_NONE, null)
        } catch (e: Throwable) {
            // Ignored
        }
        updateDimensions()
    }

    fun updateDimensions() {
        baseRadiusPx = (config.sizeDp * density) / 2f
        requestLayout()
        invalidate()
    }

    fun pauseAnimations() {
        isAnimationsActive = false
        removeCallbacks(longPressRunnable)
    }

    fun resumeAnimations() {
        isAnimationsActive = true
        invalidate()
    }

    fun openRadialMenu() {
        if (isMenuOpen) return
        isMenuOpen = true
        listener.onMenuOpenRequested()

        menuAnimator?.cancel()
        menuAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320
            interpolator = OvershootInterpolator(1.3f)
            addUpdateListener {
                menuProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun closeRadialMenu() {
        if (!isMenuOpen) return
        menuAnimator?.cancel()
        menuAnimator = ValueAnimator.ofFloat(menuProgress, 0f).apply {
            duration = 200
            addUpdateListener {
                menuProgress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isMenuOpen = false
                    listener.onMenuClosed()
                }
            })
            start()
        }
    }

    private fun animateScaleTo(target: Float, bounce: Boolean = false) {
        val anim = ValueAnimator.ofFloat(scaleMultiplier, target).apply {
            duration = if (bounce) 350 else 180
            if (bounce) interpolator = OvershootInterpolator(1.8f)
            addUpdateListener {
                scaleMultiplier = it.animatedValue as Float
                invalidate()
            }
        }
        anim.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val now = SystemClock.uptimeMillis()

        val theme = config.theme
        val userOpacity = config.opacity * idleAlphaFactor

        // 1. Idle Breathing scale calculation
        val idleCycle = (now % 2500) / 2500f
        val idleBreathing = 1.0f + 0.06f * sin(idleCycle * 2.0 * PI).toFloat()
        val currentScale = if (isTouching || isDragging) scaleMultiplier else scaleMultiplier * idleBreathing
        val currentRadius = baseRadiusPx * currentScale

        // 2. Dragging Light Trail (Glowing trail points)
        if (isDragging && trailPoints.isNotEmpty()) {
            val cutoff = now - 400
            trailPoints.removeAll { it.time < cutoff }
            for (p in trailPoints) {
                val age = (now - p.time) / 400f
                val alpha = ((1f - age) * 0.45f * userOpacity).coerceIn(0f, 1f)
                trailPaint.color = theme.glowColor
                trailPaint.alpha = (alpha * 255).toInt()
                canvas.drawCircle(p.x, p.y, (baseRadiusPx * 0.4f * (1f - age * 0.5f)), trailPaint)
            }
        }

        // 3. Ripple Animation: 2 expanding fading rings every 3s
        if (!isTouching && !isDragging && !isMenuOpen && state == BubbleAssistantState.IDLE) {
            val rippleDuration = 3000f
            val phase1 = (now % rippleDuration.toLong()) / rippleDuration
            val phase2 = ((now + 1500) % rippleDuration.toLong()) / rippleDuration

            drawRippleRing(canvas, cx, cy, currentRadius, phase1, theme.glowColor, userOpacity)
            drawRippleRing(canvas, cx, cy, currentRadius, phase2, theme.glowColor, userOpacity)
        }

        // 4. Neon Glow: soft outer shadow (12-16dp blur)
        if (config.glowEnabled) {
            val glowBlurRadius = 16f * density * currentScale
            val pulse = if (isDragging) 1.25f else 0.75f + 0.25f * sin(idleCycle * 2.0 * PI).toFloat()
            val glowAlpha = (0.50f * pulse * userOpacity).coerceIn(0f, 1f)

            val glowShader = RadialGradient(
                cx, cy, currentRadius + glowBlurRadius,
                intArrayOf(
                    adjustAlpha(theme.glowColor, glowAlpha),
                    adjustAlpha(theme.glowColor, glowAlpha * 0.4f),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0.5f, 0.75f, 1.0f),
                Shader.TileMode.CLAMP
            )
            glowPaint.shader = glowShader
            canvas.drawCircle(cx, cy, currentRadius + glowBlurRadius, glowPaint)
        }

        // 5. Glass Bubble Base: Translucent radial gradient (cyan to purple, 25-45% alpha)
        val innerAlpha = (0.32f * userOpacity).coerceIn(0f, 1f)
        val outerAlpha = (0.45f * userOpacity).coerceIn(0f, 1f)
        val glassShader = RadialGradient(
            cx - currentRadius * 0.25f, cy - currentRadius * 0.25f, currentRadius * 1.3f,
            intArrayOf(
                adjustAlpha(theme.primaryColor, innerAlpha),
                adjustAlpha(theme.secondaryColor, outerAlpha)
            ),
            floatArrayOf(0.0f, 1.0f),
            Shader.TileMode.CLAMP
        )
        bubblePaint.shader = glassShader
        canvas.drawCircle(cx, cy, currentRadius, bubblePaint)

        // 6. 1.5dp Gradient Border (iridescent glowing rim)
        val borderColors = intArrayOf(
            adjustAlpha(theme.primaryColor, 0.85f * userOpacity),
            adjustAlpha(theme.secondaryColor, 0.90f * userOpacity),
            adjustAlpha(theme.glowColor, 0.75f * userOpacity),
            adjustAlpha(theme.primaryColor, 0.85f * userOpacity)
        )
        val borderShader = SweepGradient(cx, cy, borderColors, null)
        borderPaint.shader = borderShader
        canvas.drawCircle(cx, cy, currentRadius - (borderPaint.strokeWidth / 2f), borderPaint)

        // 7. Inner Highlight at top-left for glossy sphere look
        highlightRect.set(
            cx - currentRadius * 0.70f,
            cy - currentRadius * 0.75f,
            cx + currentRadius * 0.05f,
            cy - currentRadius * 0.15f
        )
        canvas.save()
        canvas.rotate(-28f, highlightRect.centerX(), highlightRect.centerY())
        val highlightShader = RadialGradient(
            highlightRect.centerX(), highlightRect.top,
            highlightRect.width() * 0.75f,
            intArrayOf(
                adjustAlpha(Color.WHITE, 0.60f * userOpacity),
                adjustAlpha(Color.WHITE, 0.15f * userOpacity),
                Color.TRANSPARENT
            ),
            floatArrayOf(0.0f, 0.5f, 1.0f),
            Shader.TileMode.CLAMP
        )
        highlightPaint.shader = highlightShader
        canvas.drawOval(highlightRect, highlightPaint)
        canvas.restore()

        // 8. State-specific center graphics
        when (state) {
            BubbleAssistantState.IDLE -> {
                drawBoltIcon(canvas, cx, cy, currentRadius, theme, userOpacity)
            }
            BubbleAssistantState.LISTENING -> {
                drawAudioWaveRing(canvas, cx, cy, currentRadius, theme, now, userOpacity)
            }
            BubbleAssistantState.THINKING -> {
                drawRotatingGradientArc(canvas, cx, cy, currentRadius, theme, now, userOpacity)
            }
            BubbleAssistantState.SPEAKING -> {
                drawSpeakingPulsingGlow(canvas, cx, cy, currentRadius, theme, now, userOpacity)
            }
        }

        // 9. Radial Quick Menu (if open or animating)
        if (menuProgress > 0.01f) {
            drawRadialQuickMenu(canvas, cx, cy, currentRadius, theme, userOpacity)
        }

        // Keep 60fps loop running while active
        if (isAnimationsActive) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawRippleRing(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        progress: Float,
        color: Int,
        userOpacity: Float
    ) {
        val rippleRadius = radius + (progress * 26f * density)
        val alpha = ((1f - progress) * 0.40f * userOpacity).coerceIn(0f, 1f)
        ripplePaint.color = color
        ripplePaint.alpha = (alpha * 255).toInt()
        canvas.drawCircle(cx, cy, rippleRadius, ripplePaint)
    }

    private fun drawBoltIcon(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        theme: BubbleThemeColor,
        userOpacity: Float
    ) {
        val iconSize = radius * 0.95f
        boltPath.reset()

        // High precision lightning bolt path centered
        val left = cx - iconSize * 0.35f
        val right = cx + iconSize * 0.38f
        val top = cy - iconSize * 0.52f
        val bottom = cy + iconSize * 0.55f

        boltPath.moveTo(cx + iconSize * 0.08f, top)
        boltPath.lineTo(left, cy + iconSize * 0.04f)
        boltPath.lineTo(cx - iconSize * 0.02f, cy + iconSize * 0.04f)
        boltPath.lineTo(cx - iconSize * 0.12f, bottom)
        boltPath.lineTo(right, cy - iconSize * 0.04f)
        boltPath.lineTo(cx + iconSize * 0.02f, cy - iconSize * 0.04f)
        boltPath.close()

        // Subtle inner glow / backdrop
        boltGlowPaint.color = theme.glowColor
        boltGlowPaint.alpha = (0.35f * userOpacity * 255).toInt()
        canvas.save()
        canvas.scale(1.08f, 1.08f, cx, cy)
        canvas.drawPath(boltPath, boltGlowPaint)
        canvas.restore()

        // Gradient filled: Pure White to Cyan/Primary
        val boltShader = LinearGradient(
            cx, top, cx, bottom,
            intArrayOf(
                adjustAlpha(Color.WHITE, 0.98f * userOpacity),
                adjustAlpha(theme.primaryColor, 0.95f * userOpacity)
            ),
            floatArrayOf(0.1f, 0.95f),
            Shader.TileMode.CLAMP
        )
        boltPaint.shader = boltShader
        canvas.drawPath(boltPath, boltPaint)
    }

    private fun drawAudioWaveRing(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        theme: BubbleThemeColor,
        now: Long,
        userOpacity: Float
    ) {
        val barCount = 18
        val ringRadius = radius * 0.55f
        wavePaint.strokeWidth = 2.4f * density
        wavePaint.color = theme.primaryColor
        wavePaint.alpha = (0.95f * userOpacity * 255).toInt()

        for (i in 0 until barCount) {
            val angle = (i * 2.0 * PI / barCount)
            val wavePhase = (now / 180.0) + (i * 0.7)
            val waveHeight = (4f + 7f * abs(sin(wavePhase)).toFloat()) * density

            val startX = cx + (ringRadius * cos(angle)).toFloat()
            val startY = cy + (ringRadius * sin(angle)).toFloat()
            val endX = cx + ((ringRadius + waveHeight) * cos(angle)).toFloat()
            val endY = cy + ((ringRadius + waveHeight) * sin(angle)).toFloat()

            canvas.drawLine(startX, startY, endX, endY, wavePaint)
        }

        // Inner glowing core dot
        val corePulse = 0.85f + 0.15f * sin(now / 150.0).toFloat()
        iconPaint.color = Color.WHITE
        iconPaint.alpha = (0.90f * userOpacity * 255).toInt()
        canvas.drawCircle(cx, cy, 5f * density * corePulse, iconPaint)
    }

    private fun drawRotatingGradientArc(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        theme: BubbleThemeColor,
        now: Long,
        userOpacity: Float
    ) {
        val rotationAngle = (now / 3.0f) % 360f
        val arcRadius = radius * 0.65f
        val arcRect = RectF(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius)

        arcPaint.strokeWidth = 3f * density
        arcPaint.color = theme.primaryColor
        arcPaint.alpha = (0.90f * userOpacity * 255).toInt()

        canvas.save()
        canvas.rotate(rotationAngle, cx, cy)
        canvas.drawArc(arcRect, 0f, 130f, false, arcPaint)
        canvas.restore()

        // Subtle inner counter-spinning secondary arc
        canvas.save()
        canvas.rotate(-rotationAngle * 1.4f, cx, cy)
        arcPaint.strokeWidth = 1.8f * density
        arcPaint.color = theme.secondaryColor
        canvas.drawArc(RectF(cx - arcRadius * 0.6f, cy - arcRadius * 0.6f, cx + arcRadius * 0.6f, cy + arcRadius * 0.6f), 0f, 90f, false, arcPaint)
        canvas.restore()
    }

    private fun drawSpeakingPulsingGlow(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        theme: BubbleThemeColor,
        now: Long,
        userOpacity: Float
    ) {
        // Fast acoustic breathing pulse synced to TTS cadence (~3.5 Hz)
        val speechPulse = 0.5f + 0.5f * abs(sin(now / 140.0)).toFloat()
        val speechGlowRadius = radius * (0.8f + 0.35f * speechPulse)

        glowPaint.shader = RadialGradient(
            cx, cy, speechGlowRadius,
            intArrayOf(
                adjustAlpha(theme.glowColor, 0.70f * speechPulse * userOpacity),
                Color.TRANSPARENT
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, speechGlowRadius, glowPaint)

        // Center bolt
        drawBoltIcon(canvas, cx, cy, radius, theme, userOpacity)
    }

    // Radial Quick Menu: Voice, Chat, Camera, Close
    private fun drawRadialQuickMenu(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        theme: BubbleThemeColor,
        userOpacity: Float
    ) {
        val distance = (68f * density) * menuProgress
        val buttonRadius = 20f * density

        val items = listOf(
            RadialItem("Voice", "🎙️", -PI / 2, theme.primaryColor), // Top
            RadialItem("Chat", "💬", 0.0, theme.secondaryColor),     // Right
            RadialItem("Camera", "📷", PI / 2, Color.parseColor("#EC4899")), // Bottom
            RadialItem("Close", "✕", PI, Color.parseColor("#EF4444"))        // Left
        )

        for ((index, item) in items.withIndex()) {
            val staggerDelay = index * 0.08f
            val itemProgress = ((menuProgress - staggerDelay) / (1f - staggerDelay)).coerceIn(0f, 1f)
            if (itemProgress <= 0.01f) continue

            val currentDist = distance * itemProgress
            val bx = cx + (currentDist * cos(item.angle)).toFloat()
            val by = cy + (currentDist * sin(item.angle)).toFloat()
            val itemAlpha = (itemProgress * userOpacity).coerceIn(0f, 1f)

            // Glass button background
            menuBgPaint.color = Color.parseColor("#141A29")
            menuBgPaint.alpha = (0.88f * itemAlpha * 255).toInt()
            canvas.drawCircle(bx, by, buttonRadius * itemProgress, menuBgPaint)

            // Glowing border
            menuBorderPaint.color = item.accentColor
            menuBorderPaint.alpha = (0.85f * itemAlpha * 255).toInt()
            canvas.drawCircle(bx, by, buttonRadius * itemProgress, menuBorderPaint)

            // Icon text
            val iconTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 15f * density * itemProgress
                textAlign = Paint.Align.CENTER
                color = Color.WHITE
            }
            val textOffset = (iconTextPaint.descent() + iconTextPaint.ascent()) / 2f
            canvas.drawText(item.icon, bx, by - textOffset, iconTextPaint)

            // Mini pill label
            textPaint.alpha = (itemAlpha * 255).toInt()
            textPaint.textSize = 9f * density * itemProgress
            val labelY = by + buttonRadius + (11f * density)
            canvas.drawText(item.label, bx, labelY, textPaint)
        }
    }

    private data class RadialItem(
        val label: String,
        val icon: String,
        val angle: Double,
        val accentColor: Int
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cx = width / 2f
        val cy = height / 2f
        val density = resources.displayMetrics.density

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownTime = SystemClock.uptimeMillis()
                touchDownRawX = event.rawX
                touchDownRawY = event.rawY
                lastRawX = event.rawX
                lastRawY = event.rawY

                isTouching = true
                isDragging = false
                trailPoints.clear()

                // Check if user touched a radial menu item
                if (isMenuOpen && menuProgress > 0.8f) {
                    val touchedItem = findTouchedMenuItem(event.x, event.y, cx, cy)
                    if (touchedItem != null) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        executeMenuItem(touchedItem)
                        closeRadialMenu()
                        return true
                    } else {
                        // Tapped outside: close menu
                        closeRadialMenu()
                        return true
                    }
                }

                // Haptic feedback tick on touch down
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                // Scale down to 0.9 on touch
                animateScaleTo(0.9f)

                // Schedule long press
                isLongPressScheduled = true
                postDelayed(longPressRunnable, 480)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - lastRawX
                val deltaY = event.rawY - lastRawY
                val totalDist = sqrt(
                    (event.rawX - touchDownRawX) * (event.rawX - touchDownRawX) +
                    (event.rawY - touchDownRawY) * (event.rawY - touchDownRawY)
                )

                if (totalDist > touchSlop && !isMenuOpen) {
                    if (!isDragging) {
                        isDragging = true
                        removeCallbacks(longPressRunnable)
                        isLongPressScheduled = false
                        animateScaleTo(1.1f)
                    }

                    // Record drag light trail point
                    trailPoints.add(TrailPoint(event.x, event.y, SystemClock.uptimeMillis()))
                    if (trailPoints.size > 8) trailPoints.removeAt(0)

                    listener.onDrag(deltaX, deltaY)
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPressRunnable)
                isLongPressScheduled = false
                isTouching = false

                // Spring bounce back to 1.0
                animateScaleTo(1.0f, bounce = true)

                if (isDragging) {
                    isDragging = false
                    trailPoints.clear()
                    listener.onDragEnd(event.rawX, event.rawY)
                } else if (!isMenuOpen) {
                    // Tap event
                    val duration = SystemClock.uptimeMillis() - touchDownTime
                    if (duration < 450) {
                        listener.onBubbleTap()
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findTouchedMenuItem(touchX: Float, touchY: Float, cx: Float, cy: Float): String? {
        val distance = 68f * density
        val touchTolerance = 28f * density

        val items = listOf(
            Pair("Voice", -PI / 2),
            Pair("Chat", 0.0),
            Pair("Camera", PI / 2),
            Pair("Close", PI)
        )

        for ((name, angle) in items) {
            val bx = cx + (distance * cos(angle)).toFloat()
            val by = cy + (distance * sin(angle)).toFloat()
            val dist = sqrt((touchX - bx) * (touchX - bx) + (touchY - by) * (touchY - by))
            if (dist <= touchTolerance) {
                return name
            }
        }
        return null
    }

    private fun executeMenuItem(name: String) {
        when (name) {
            "Voice" -> listener.onVoiceAction()
            "Chat" -> listener.onChatAction()
            "Camera" -> listener.onCameraAction()
            "Close" -> listener.onCloseAction()
        }
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
