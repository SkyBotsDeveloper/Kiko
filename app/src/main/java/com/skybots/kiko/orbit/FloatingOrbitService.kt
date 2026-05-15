package com.skybots.kiko.orbit

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.skybots.kiko.MainActivity
import com.skybots.kiko.wake.WakeWordEngineState
import com.skybots.kiko.wake.WakeWordEvent
import com.skybots.kiko.wake.WakeWordRuntime
import com.skybots.kiko.wake.WakeWordService

class FloatingOrbitService : Service() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private lateinit var permissionHelper: FloatingOrbitPermissionHelper
    private var overlayView: FloatingOrbitOverlayView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var unsubscribeWake: (() -> Unit)? = null
    private var screenReceiverRegistered = false
    private var panelVisible = false
    private var currentOverlayState = OrbitOverlayState()
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context?,
            intent: Intent?,
        ) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> removeOverlay()
                Intent.ACTION_USER_PRESENT -> showOverlay(OrbitUiStateMapper.fromWakeState(
                    wakeWordState = WakeWordRuntime.currentState(),
                    wakeScoreSnapshot = WakeWordRuntime.currentScoreSnapshot(),
                    panelVisible = panelVisible,
                ))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        permissionHelper = FloatingOrbitPermissionHelper(this)
        registerScreenReceiver()
        unsubscribeWake = WakeWordRuntime.subscribe { event ->
            mainHandler.post { handleWakeEvent(event) }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_START -> showOverlay(
                OrbitUiStateMapper.fromWakeState(
                    wakeWordState = WakeWordRuntime.currentState(),
                    wakeScoreSnapshot = WakeWordRuntime.currentScoreSnapshot(),
                    panelVisible = false,
                ),
            )
            ACTION_SHOW_PANEL -> {
                panelVisible = true
                showOverlay(
                    OrbitUiStateMapper.fromWakeState(
                        wakeWordState = WakeWordRuntime.currentState(),
                        wakeScoreSnapshot = WakeWordRuntime.currentScoreSnapshot(),
                        panelVisible = true,
                    ),
                )
            }
            ACTION_SHOW_LISTENING -> {
                panelVisible = true
                showOverlay(OrbitUiStateMapper.manualListening())
            }
            ACTION_STOP -> {
                removeOverlay()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unsubscribeWake?.invoke()
        unsubscribeWake = null
        unregisterScreenReceiver()
        removeOverlay()
        super.onDestroy()
    }

    private fun handleWakeEvent(event: WakeWordEvent) {
        when (event) {
            WakeWordEvent.WakeDetected -> {
                panelVisible = true
                showOverlay(OrbitUiStateMapper.manualListening())
            }
            WakeWordEvent.Started,
            WakeWordEvent.Stopped,
            WakeWordEvent.PausedLocked,
            is WakeWordEvent.ScoreDebug,
            is WakeWordEvent.Error -> showOverlay(
                OrbitUiStateMapper.fromWakeState(
                    wakeWordState = WakeWordRuntime.currentState(),
                    wakeScoreSnapshot = WakeWordRuntime.currentScoreSnapshot(),
                    panelVisible = panelVisible,
                ),
            )
        }
    }

    private fun showOverlay(state: OrbitOverlayState) {
        if (!permissionHelper.canDrawOverlays()) {
            removeOverlay()
            stopSelf()
            return
        }
        currentOverlayState = state

        val existing = overlayView
        if (existing == null) {
            val view = FloatingOrbitOverlayView(
                context = this,
                onDrag = ::moveOverlay,
                onTapBubble = {
                    panelVisible = true
                    showOverlay(currentOverlayState.copy(mode = OrbitOverlayMode.Panel))
                },
                onMicClick = ::startManualMic,
                onSettingsClick = ::openSettings,
                onCloseClick = {
                    panelVisible = false
                    showOverlay(currentOverlayState.copy(mode = OrbitOverlayMode.Bubble))
                },
                onStopWakeClick = ::stopWakeWord,
            )
            overlayView = view
            layoutParams = createLayoutParams()
            windowManager.addView(view, layoutParams)
        }
        overlayView?.render(state)
    }

    private fun removeOverlay() {
        overlayView?.stop()
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        layoutParams = null
    }

    private fun moveOverlay(
        dx: Int,
        dy: Int,
    ) {
        val params = layoutParams ?: return
        params.x += dx
        params.y += dy
        overlayView?.let { view -> runCatching { windowManager.updateViewLayout(view, params) } }
    }

    private fun startManualMic() {
        panelVisible = true
        overlayView?.render(OrbitUiStateMapper.manualListening())
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_START_MANUAL_MIC, true)
            },
        )
    }

    private fun openSettings() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_SETTINGS, true)
            },
        )
    }

    private fun stopWakeWord() {
        startService(WakeWordService.stopIntent(this))
    }

    private fun createLayoutParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 28.dp()
            y = 140.dp()
        }

    private fun registerScreenReceiver() {
        if (screenReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(screenReceiver, filter)
        }
        screenReceiverRegistered = true
    }

    private fun unregisterScreenReceiver() {
        if (!screenReceiverRegistered) return
        runCatching { unregisterReceiver(screenReceiver) }
        screenReceiverRegistered = false
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    companion object {
        private const val ACTION_START = "com.skybots.kiko.orbit.START"
        private const val ACTION_STOP = "com.skybots.kiko.orbit.STOP"
        private const val ACTION_SHOW_PANEL = "com.skybots.kiko.orbit.SHOW_PANEL"
        private const val ACTION_SHOW_LISTENING = "com.skybots.kiko.orbit.SHOW_LISTENING"

        fun startIntent(context: Context): Intent =
            Intent(context, FloatingOrbitService::class.java).setAction(ACTION_START)

        fun stopIntent(context: Context): Intent =
            Intent(context, FloatingOrbitService::class.java).setAction(ACTION_STOP)

        fun showPanelIntent(context: Context): Intent =
            Intent(context, FloatingOrbitService::class.java).setAction(ACTION_SHOW_PANEL)

        fun showListeningIntent(context: Context): Intent =
            Intent(context, FloatingOrbitService::class.java).setAction(ACTION_SHOW_LISTENING)
    }
}

@SuppressLint("ViewConstructor")
private class FloatingOrbitOverlayView(
    context: Context,
    private val onDrag: (dx: Int, dy: Int) -> Unit,
    private val onTapBubble: () -> Unit,
    private val onMicClick: () -> Unit,
    private val onSettingsClick: () -> Unit,
    private val onCloseClick: () -> Unit,
    private val onStopWakeClick: () -> Unit,
) : FrameLayout(context) {
    private val orbit = OrbitBubbleView(context)
    private val title = TextView(context)
    private val message = TextView(context)
    private val panel = LinearLayout(context)
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var moved = false

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
        buildPanel()
        addView(orbit, LayoutParams(76.dp(), 76.dp(), Gravity.CENTER))
        addView(panel)
        setOnTouchListener { _, event -> handleTouch(event) }
    }

    fun render(state: OrbitOverlayState) {
        orbit.visualState = state.visualState
        title.text = state.title
        message.text = state.message
        panel.visibility = if (state.mode == OrbitOverlayMode.Panel) VISIBLE else GONE
        val size = if (state.mode == OrbitOverlayMode.Panel) 260.dp() else 76.dp()
        layoutParams = layoutParams?.apply {
            width = size
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        } ?: LayoutParams(size, ViewGroup.LayoutParams.WRAP_CONTENT)
        orbit.start()
    }

    fun stop() {
        orbit.stop()
    }

    private fun buildPanel() {
        panel.orientation = LinearLayout.VERTICAL
        panel.gravity = Gravity.CENTER_HORIZONTAL
        panel.visibility = GONE
        panel.background = roundedBackground(PANEL_COLOR, BORDER_COLOR)
        panel.setPadding(14.dp(), 14.dp(), 14.dp(), 12.dp())
        panel.layoutParams = LayoutParams(260.dp(), ViewGroup.LayoutParams.WRAP_CONTENT)

        title.setTextColor(Color.WHITE)
        title.textSize = 16f
        title.gravity = Gravity.CENTER
        title.setTypeface(title.typeface, android.graphics.Typeface.BOLD)
        panel.addView(title, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        message.setTextColor(MUTED_TEXT)
        message.textSize = 12f
        message.gravity = Gravity.CENTER
        val messageParams = LinearLayout.LayoutParams.MATCH_PARENT.lp()
        messageParams.topMargin = 4.dp()
        panel.addView(message, messageParams)

        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val actionsParams = LinearLayout.LayoutParams.MATCH_PARENT.lp()
        actionsParams.topMargin = 12.dp()
        panel.addView(actions, actionsParams)

        actions.addView(panelButton("Mic", onMicClick), buttonLp())
        actions.addView(panelButton("Settings", onSettingsClick), buttonLp())
        actions.addView(panelButton("Close", onCloseClick), buttonLp())

        val stop = panelButton("Stop wake", onStopWakeClick)
        val stopParams = LinearLayout.LayoutParams.MATCH_PARENT.lp()
        stopParams.topMargin = 8.dp()
        panel.addView(stop, stopParams)
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastRawX = event.rawX
                lastRawY = event.rawY
                moved = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - lastRawX).toInt()
                val dy = (event.rawY - lastRawY).toInt()
                if (kotlin.math.abs(dx) > 1 || kotlin.math.abs(dy) > 1) {
                    moved = true
                    onDrag(dx, dy)
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!moved) {
                    onTapBubble()
                }
                return true
            }
        }
        return false
    }

    private fun panelButton(
        text: String,
        onClick: () -> Unit,
    ): Button =
        Button(context).apply {
            this.text = text
            textSize = 11f
            setTextColor(Color.rgb(4, 8, 13))
            background = roundedBackground(ACCENT, ACCENT)
            setOnClickListener { onClick() }
        }

    private fun roundedBackground(
        fill: Int,
        stroke: Int,
    ): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8.dp().toFloat()
            setColor(fill)
            setStroke(1.dp(), stroke)
        }

    private fun buttonLp(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, 42.dp(), 1f).apply {
            marginEnd = 6.dp()
        }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}

private class OrbitBubbleView(context: Context) : View(context) {
    var visualState: OrbitOverlayVisualState = OrbitOverlayVisualState.Idle
        set(value) {
            field = value
            invalidate()
        }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var pulse = 0.86f
    private val animator = android.animation.ValueAnimator.ofFloat(0.82f, 1.08f).apply {
        duration = 1400L
        repeatCount = android.animation.ValueAnimator.INFINITE
        repeatMode = android.animation.ValueAnimator.REVERSE
        addUpdateListener {
            pulse = it.animatedValue as Float
            invalidate()
        }
    }

    fun start() {
        if (!animator.isStarted) animator.start()
    }

    fun stop() {
        animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val color = visualState.color()
        val radius = width.coerceAtMost(height) / 2f
        val cx = width / 2f
        val cy = height / 2f

        paint.shader = RadialGradient(
            cx,
            cy,
            radius * 0.92f,
            intArrayOf(color.withAlpha(105), color.withAlpha(22), Color.TRANSPARENT),
            floatArrayOf(0f, 0.62f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius * 0.9f * pulse, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.2f
        paint.color = color.withAlpha(170)
        canvas.drawCircle(cx, cy, radius * 0.62f, paint)
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx,
            cy,
            radius * 0.28f,
            intArrayOf(Color.WHITE, color.withAlpha(230), color.withAlpha(55)),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius * 0.24f * pulse, paint)
        paint.shader = null
    }
}

private fun OrbitOverlayVisualState.color(): Int =
    when (this) {
        OrbitOverlayVisualState.UnsafeModel -> WARNING
        OrbitOverlayVisualState.PausedLocked -> MUTED_TEXT
        OrbitOverlayVisualState.Error -> DANGER
        else -> ACCENT
    }

private fun Int.withAlpha(alpha: Int): Int = Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))

private fun Int.lp(): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(this, ViewGroup.LayoutParams.WRAP_CONTENT)

private const val ACCENT = 0xFF53F3D0.toInt()
private const val WARNING = 0xFFFFC267.toInt()
private const val DANGER = 0xFFFF6B6B.toInt()
private const val MUTED_TEXT = 0xFF9EA8B7.toInt()
private const val PANEL_COLOR = 0xEE111821.toInt()
private const val BORDER_COLOR = 0x553A4654
