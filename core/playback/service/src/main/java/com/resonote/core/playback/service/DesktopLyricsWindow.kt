package com.resonote.core.playback.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.hardware.input.InputManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import com.resonote.core.designsystem.icon.iconResource
import com.resonote.core.model.DesktopLyricsDisplayMode
import com.resonote.core.model.DesktopLyricsPosition
import com.resonote.core.model.LyricsFontSize
import com.resonote.core.model.LyricsPreferences
import com.resonote.core.model.PlaybackMode
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

internal class DesktopLyricsWindow(
    private val context: Context,
    private val onPositionChanged: (DesktopLyricsPosition) -> Unit,
    private val onTogglePlayPause: () -> Unit,
    private val onPrevious: () -> Unit,
    private val onNext: () -> Unit,
    private val onCycleMode: () -> Unit,
    private val onLockedChanged: (Boolean) -> Unit,
    private val onOpenSettings: () -> Unit,
    private val onClose: () -> Unit,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density
    private val view = DesktopLyricsControllerView(context, ::handleControl, ::updateDesiredWidth)
    private var params: WindowManager.LayoutParams? = null
    private var preferences = LyricsPreferences()
    private var contentWidth = initialWidth()
    private var dragStartX = 0
    private var dragStartY = 0

    val isVisible: Boolean get() = params != null

    fun show(preferences: LyricsPreferences): Boolean {
        this.preferences = preferences
        if (isVisible) return true
        val position = preferences.desktopLyricsPosition ?: defaultPosition()
        val layoutParams = WindowManager.LayoutParams(
            contentWidth,
            expandedHeight(preferences),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags(),
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = position.x
            y = windowY(position.y)
            alpha = windowAlpha()
        }
        return runCatching {
            view.preferences = preferences
            view.dragStartListener = ::startMove
            view.dragListener = ::moveBy
            view.dragEndListener = ::finishMove
            windowManager.addView(view, layoutParams)
            params = layoutParams
            view.post(::clampToScreen)
            true
        }.getOrElse {
            params = null
            false
        }
    }

    fun hide() {
        if (params != null) runCatching { windowManager.removeView(view) }
        params = null
        view.release()
    }

    fun applyPreferences(preferences: LyricsPreferences) {
        this.preferences = preferences
        view.preferences = preferences
        val layoutParams = params ?: return
        layoutParams.flags = flags()
        layoutParams.alpha = windowAlpha()
        layoutParams.width = contentWidth
        layoutParams.height = expandedHeight(preferences)
        preferences.desktopLyricsPosition?.let {
            layoutParams.x = it.x
            layoutParams.y = windowY(it.y)
        }
        runCatching { windowManager.updateViewLayout(view, layoutParams) }.onFailure { hide() }
        view.post(::clampToScreen)
    }

    fun render(content: DesktopLyricsContent) {
        view.content = content
    }

    fun renderMessage(message: String) {
        view.content = DesktopLyricsContent(message, 0f, null, null, message)
    }

    fun updatePlayback(isPlaying: Boolean, mode: PlaybackMode) {
        view.isPlaying = isPlaying
        view.playbackMode = mode
    }

    fun updatePalette(palette: DesktopLyricsPalette, animate: Boolean) {
        view.updatePalette(palette, animate)
    }

    fun resetPosition(): DesktopLyricsPosition {
        val position = defaultPosition()
        params?.let {
            it.x = position.x
            it.y = windowY(position.y)
            runCatching { windowManager.updateViewLayout(view, it) }
        }
        return position
    }

    fun clampToScreen() {
        val layoutParams = params ?: return
        val (width, height) = displaySize()
        val maxX = (width - layoutParams.width).coerceAtLeast(0)
        val maxY = (height - layoutParams.height).coerceAtLeast(0)
        val clampedX = layoutParams.x.coerceIn(0, maxX)
        val clampedY = layoutParams.y.coerceIn(0, maxY)
        if (clampedX == layoutParams.x && clampedY == layoutParams.y) {
            return
        }
        layoutParams.x = clampedX
        layoutParams.y = clampedY
        runCatching { windowManager.updateViewLayout(view, layoutParams) }
        onPositionChanged(DesktopLyricsPosition(clampedX, anchorY(clampedY)))
    }

    private fun handleControl(control: DesktopLyricsControl) {
        if (!isDesktopLyricsControlAvailable(control, preferences.desktopLyricsLocked)) return
        when (control) {
            DesktopLyricsControl.Lock -> {
                val locked = !preferences.desktopLyricsLocked
                applyPreferences(preferences.copy(desktopLyricsLocked = locked))
                onLockedChanged(locked)
            }
            DesktopLyricsControl.Settings -> onOpenSettings()
            DesktopLyricsControl.Previous -> onPrevious()
            DesktopLyricsControl.PlayPause -> onTogglePlayPause()
            DesktopLyricsControl.Next -> onNext()
            DesktopLyricsControl.Mode -> onCycleMode()
            DesktopLyricsControl.Close -> onClose()
        }
    }

    private fun startMove() {
        params?.let {
            dragStartX = it.x
            dragStartY = it.y
        }
    }

    private fun moveBy(deltaX: Int, deltaY: Int) {
        val layoutParams = params ?: return
        layoutParams.x = dragStartX + deltaX
        layoutParams.y = dragStartY + deltaY
        runCatching { windowManager.updateViewLayout(view, layoutParams) }
    }

    private fun finishMove() {
        clampToScreen()
        params?.let {
            onPositionChanged(DesktopLyricsPosition(it.x, anchorY(it.y)))
        }
    }

    private fun updateDesiredWidth(desiredWidth: Int) {
        val displayWidth = displaySize().first
        val maximumWidth = minOf(
            dp(MAX_CONTROLLER_WIDTH_DP),
            displayWidth - dp(SCREEN_EDGE_GAP_DP * 2),
        ).coerceAtLeast(dp(MIN_CONTROLLER_WIDTH_DP))
        val targetWidth = desiredWidth.coerceIn(
            dp(MIN_CONTROLLER_WIDTH_DP),
            maximumWidth,
        )
        if (targetWidth == contentWidth) return
        val oldWidth = contentWidth
        contentWidth = targetWidth
        val layoutParams = params ?: return
        val leftGap = layoutParams.x
        val rightGap = displayWidth - layoutParams.x - oldWidth
        layoutParams.x = when {
            leftGap <= dp(EDGE_ANCHOR_TOLERANCE_DP) -> dp(SCREEN_EDGE_GAP_DP)
            rightGap <= dp(EDGE_ANCHOR_TOLERANCE_DP) -> displayWidth - targetWidth - dp(SCREEN_EDGE_GAP_DP)
            else -> layoutParams.x + (oldWidth - targetWidth) / 2
        }
        layoutParams.width = targetWidth
        runCatching { windowManager.updateViewLayout(view, layoutParams) }
        clampToScreen()
        onPositionChanged(DesktopLyricsPosition(layoutParams.x, anchorY(layoutParams.y)))
    }

    private fun flags(): Int = desktopLyricsWindowFlags(preferences.desktopLyricsLocked)

    private fun windowAlpha(): Float = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        desktopLyricsWindowAlpha(
            locked = preferences.desktopLyricsLocked,
            maximumObscuringOpacity = context.getSystemService(InputManager::class.java)
                .maximumObscuringOpacityForTouch,
        )
    } else {
        1f
    }

    private fun defaultPosition(): DesktopLyricsPosition {
        val (width, height) = displaySize()
        return DesktopLyricsPosition((width - contentWidth) / 2, (height * 0.14f).toInt())
    }

    private fun initialWidth(): Int = (displaySize().first - dp(SCREEN_EDGE_GAP_DP * 2))
        .coerceAtMost(dp(280))
    private fun collapsedHeight(value: LyricsPreferences): Int = if (
        value.desktopLyricsDisplayMode == DesktopLyricsDisplayMode.TwoLines
    ) {
        dp(88)
    } else {
        dp(64)
    }
    private fun expandedHeight(value: LyricsPreferences): Int = collapsedHeight(value) + dp(CONTROLS_INSET_DP * 2)
    private fun windowY(anchorY: Int): Int = desktopLyricsWindowY(
        anchorY,
        dp(CONTROLS_INSET_DP),
    )
    private fun anchorY(windowY: Int): Int = desktopLyricsAnchorY(
        windowY,
        dp(CONTROLS_INSET_DP),
    )

    private fun displaySize(): Pair<Int, Int> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        windowManager.currentWindowMetrics.bounds.let { it.width() to it.height() }
    } else {
        @Suppress("DEPRECATION")
        DisplayMetrics().also(windowManager.defaultDisplay::getRealMetrics).let { it.widthPixels to it.heightPixels }
    }

    private fun dp(value: Int) = (value * density).toInt()

    private companion object {
        const val CONTROLS_INSET_DP = 38
        const val EDGE_ANCHOR_TOLERANCE_DP = 24
        const val MAX_CONTROLLER_WIDTH_DP = 260
        const val MIN_CONTROLLER_WIDTH_DP = 220
        const val SCREEN_EDGE_GAP_DP = 4
    }
}

internal enum class DesktopLyricsControl { Lock, Close, Settings, Previous, PlayPause, Next, Mode }

internal fun desktopLyricsWindowFlags(locked: Boolean): Int = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
    if (locked) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0

internal fun desktopLyricsWindowAlpha(locked: Boolean, maximumObscuringOpacity: Float): Float =
    if (locked) maximumObscuringOpacity.coerceIn(0f, 1f) else 1f

internal enum class DesktopLyricsTapOutcome { ShowControls, HideControls, InvokeControl, KeepControls }

internal fun desktopLyricsTapOutcome(
    controlsWereVisible: Boolean,
    pressedControl: Boolean,
    releasedOnPressedControl: Boolean,
    isLocked: Boolean,
): DesktopLyricsTapOutcome = when {
    !controlsWereVisible -> DesktopLyricsTapOutcome.ShowControls
    pressedControl && releasedOnPressedControl -> DesktopLyricsTapOutcome.InvokeControl
    pressedControl -> DesktopLyricsTapOutcome.KeepControls
    isLocked -> DesktopLyricsTapOutcome.ShowControls
    else -> DesktopLyricsTapOutcome.HideControls
}

internal fun isDesktopLyricsControlAvailable(control: DesktopLyricsControl, isLocked: Boolean): Boolean =
    !isLocked || control == DesktopLyricsControl.Lock

private class DesktopLyricsControllerView(
    context: Context,
    private val onControl: (DesktopLyricsControl) -> Unit,
    private val onDesiredWidthChanged: (Int) -> Unit,
) : View(context) {
    private val density = resources.displayMetrics.density
    private val scaledDensity = density * resources.configuration.fontScale
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val handler = Handler(Looper.getMainLooper())
    private val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD }
    private val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(2).toFloat()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val playbackModeIcons = PlaybackMode.entries.associateWith { mode ->
        requireNotNull(ContextCompat.getDrawable(context, mode.iconResource())).mutate()
    }
    private val buttonRects = DesktopLyricsControl.entries.associateWith { RectF() }
    private val lineTransitionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 260L
        interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
        addUpdateListener {
            lineTransitionProgress = it.animatedValue as Float
            invalidate()
        }
    }
    private val controlsVisibilityAnimator = ValueAnimator().apply {
        duration = 160L
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            controlsAlpha = it.animatedValue as Float
            invalidate()
        }
        addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (controlsCollapsePending && controlsAlpha <= 0.01f) finishControlsCollapse()
                }
            },
        )
    }
    private val paletteAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 280L
        interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
    }
    private var downRawX = 0f
    private var downRawY = 0f
    private var dragging = false
    private var movedBeyondTouchSlop = false
    private var controlsWereVisibleOnDown = false
    private var pressedControl: DesktopLyricsControl? = null
    private var previousPrimary: String? = null
    private var previousPrimaryHighlightTextOffset = 0f
    private var previousSecondary: String? = null
    private var lineTransitionProgress = 1f
    private var controlsAlpha = 0f
    private var controlsCollapsePending = false

    var preferences: LyricsPreferences = LyricsPreferences()
        set(value) {
            field = value
            if (value.desktopLyricsLocked) {
                resetControlsVisibility()
            } else {
                scheduleControlsCollapse()
            }
            onDesiredWidthChanged(desiredWidth())
            invalidate()
        }
    var palette: DesktopLyricsPalette = DesktopLyricsPalette(
        surfaceArgb = 0xFFF8EBEB.toInt(),
        accentArgb = 0xFFAE2A4B.toInt(),
        onSurfaceArgb = 0xFF201A1B.toInt(),
        onAccentArgb = Color.WHITE,
        transparentContentArgb = 0xFF201A1B.toInt(),
        transparentAccentArgb = 0xFFAE2A4B.toInt(),
    )
        private set
    var content: DesktopLyricsContent = DesktopLyricsContent("", 0f, null, null, "")
        set(value) {
            if (field.primary.isNotBlank() && field.primary != value.primary) {
                previousPrimary = field.primary
                previousPrimaryHighlightTextOffset = field.primaryHighlightTextOffset
                previousSecondary = field.supplemental ?: field.next
                lineTransitionAnimator.cancel()
                lineTransitionProgress = 0f
                lineTransitionAnimator.start()
            }
            val widthChanged = field.layoutReference != value.layoutReference
            field = value
            if (widthChanged) onDesiredWidthChanged(desiredWidth())
            invalidate()
        }
    var isPlaying: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    var playbackMode: PlaybackMode = PlaybackMode.ListLoop
        set(value) {
            field = value
            invalidate()
        }
    var controlsVisible: Boolean = false
        private set
    var dragStartListener: (() -> Unit)? = null
    var dragListener: ((Int, Int) -> Unit)? = null
    var dragEndListener: (() -> Unit)? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        isClickable = true
        contentDescription = context.getString(R.string.core_playback_service_desktop_lyrics_notification_title)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                dragging = false
                movedBeyondTouchSlop = false
                controlsWereVisibleOnDown = controlsVisible && !controlsCollapsePending
                pressedControl = if (controlsWereVisibleOnDown) controlAt(event.x, event.y) else null
                showControls()
                dragStartListener?.invoke()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                if (!movedBeyondTouchSlop && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                    movedBeyondTouchSlop = true
                }
                if (
                    !preferences.desktopLyricsLocked &&
                    !dragging &&
                    movedBeyondTouchSlop
                ) {
                    dragging = true
                }
                if (dragging) dragListener?.invoke(dx.toInt(), dy.toInt())
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (dragging) {
                    dragEndListener?.invoke()
                    scheduleControlsCollapse()
                } else if (!movedBeyondTouchSlop) {
                    performClick()
                    val releasedControl = controlAt(event.x, event.y)
                    when (
                        desktopLyricsTapOutcome(
                            controlsWereVisible = controlsWereVisibleOnDown,
                            pressedControl = pressedControl != null,
                            releasedOnPressedControl = pressedControl == releasedControl,
                            isLocked = preferences.desktopLyricsLocked,
                        )
                    ) {
                        DesktopLyricsTapOutcome.ShowControls -> showControls()
                        DesktopLyricsTapOutcome.HideControls -> hideControls()
                        DesktopLyricsTapOutcome.InvokeControl -> {
                            pressedControl?.let(onControl)
                            scheduleControlsCollapse()
                        }
                        DesktopLyricsTapOutcome.KeepControls -> scheduleControlsCollapse()
                    }
                } else {
                    scheduleControlsCollapse()
                }
                pressedControl = null
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (dragging) dragEndListener?.invoke()
                pressedControl = null
                scheduleControlsCollapse()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawSurface(canvas)
        drawLyrics(canvas)
        if (controlsVisible && controlsAlpha > 0f) {
            val checkpoint = canvas.saveLayerAlpha(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                (controlsAlpha * 255).toInt().coerceIn(0, 255),
            )
            drawControls(canvas)
            canvas.restoreToCount(checkpoint)
        }
    }

    fun updatePalette(value: DesktopLyricsPalette, animate: Boolean) {
        if (value == palette) return
        paletteAnimator.cancel()
        if (!animate) {
            palette = value
            invalidate()
            return
        }
        val from = palette
        paletteAnimator.removeAllUpdateListeners()
        paletteAnimator.addUpdateListener {
            palette = interpolateDesktopLyricsPalette(from, value, it.animatedFraction)
            invalidate()
        }
        paletteAnimator.start()
    }

    fun release() {
        lineTransitionAnimator.cancel()
        paletteAnimator.cancel()
        resetControlsVisibility()
    }

    private fun desiredWidth(): Int {
        textPaint.textSize = baseTextSizeSp() * scaledDensity
        return ceil(textPaint.measureText(content.layoutReference)).toInt() + dp(LYRICS_HORIZONTAL_PADDING_DP * 2)
    }

    fun collapseControls() {
        handler.removeCallbacksAndMessages(null)
        if (!controlsVisible) return
        hideControls()
    }

    private fun showControls() {
        controlsCollapsePending = false
        controlsVisibilityAnimator.cancel()
        if (!controlsVisible) {
            controlsVisible = true
            controlsAlpha = 0f
            requestLayout()
        }
        animateControlsAlphaTo(1f)
        scheduleControlsCollapse()
    }

    private fun controlAt(x: Float, y: Float): DesktopLyricsControl? = buttonRects.entries.firstOrNull {
        x in it.value.left..it.value.right && y in it.value.top..it.value.bottom
    }?.key

    private fun scheduleControlsCollapse() {
        handler.removeCallbacksAndMessages(null)
        if (!controlsVisible) return
        handler.postDelayed(
            {
                hideControls()
            },
            preferences.desktopLyricsControlsTimeout.seconds * 1_000L,
        )
    }

    private fun hideControls() {
        if (!controlsVisible || controlsCollapsePending) return
        controlsCollapsePending = true
        animateControlsAlphaTo(0f)
    }

    private fun animateControlsAlphaTo(target: Float) {
        controlsVisibilityAnimator.cancel()
        if (abs(controlsAlpha - target) <= 0.01f) {
            controlsAlpha = target
            if (target == 0f && controlsCollapsePending) finishControlsCollapse()
            invalidate()
            return
        }
        controlsVisibilityAnimator.setFloatValues(controlsAlpha, target)
        controlsVisibilityAnimator.start()
    }

    private fun finishControlsCollapse() {
        if (!controlsVisible) return
        controlsCollapsePending = false
        controlsVisible = false
        controlsAlpha = 0f
        requestLayout()
        invalidate()
    }

    private fun resetControlsVisibility() {
        handler.removeCallbacksAndMessages(null)
        controlsVisibilityAnimator.cancel()
        controlsCollapsePending = false
        controlsVisible = false
        controlsAlpha = 0f
        requestLayout()
        invalidate()
    }

    private fun drawSurface(canvas: Canvas) {
        val alpha = (preferences.desktopLyricsSurfaceOpacity.coerceIn(0, 100) * 2.55f).toInt()
        if (alpha == 0) return
        val lyricsTop = lyricsInset().toFloat()
        val lyricsBottom = height - lyricsInset().toFloat()
        surfacePaint.style = Paint.Style.FILL
        surfacePaint.color = palette.surfaceArgb.withAlpha(alpha)
        canvas.drawRoundRect(
            0f,
            lyricsTop,
            width.toFloat(),
            lyricsBottom,
            dp(14).toFloat(),
            dp(14).toFloat(),
            surfacePaint,
        )
        surfacePaint.style = Paint.Style.STROKE
        surfacePaint.strokeWidth = dp(1).toFloat()
        surfacePaint.color = palette.onSurfaceArgb.withAlpha((alpha * 0.42f).toInt())
        canvas.drawRoundRect(
            0f,
            lyricsTop,
            width.toFloat(),
            lyricsBottom,
            dp(14).toFloat(),
            dp(14).toFloat(),
            surfacePaint,
        )
        surfacePaint.style = Paint.Style.FILL
    }

    private fun drawLyrics(canvas: Canvas) {
        val baseSp = baseTextSizeSp()
        val lyricsTop = lyricsInset().toFloat()
        val lyricsHeight = height - lyricsInset() * 2f
        val outgoingAlpha = (1f - lineTransitionProgress / 0.72f).coerceIn(0f, 1f)
        val incomingAlpha = ((lineTransitionProgress - 0.18f) / 0.82f).coerceIn(0f, 1f)
        previousPrimary?.let { previous ->
            drawLyricsBlock(
                canvas = canvas,
                primary = previous,
                highlightTextOffset = previousPrimaryHighlightTextOffset,
                secondary = previousSecondary,
                baseSp = baseSp,
                lyricsTop = lyricsTop,
                lyricsHeight = lyricsHeight,
                offsetY = -dp(6) * lineTransitionProgress,
                alpha = outgoingAlpha,
            )
        }
        drawLyricsBlock(
            canvas = canvas,
            primary = content.primary,
            highlightTextOffset = content.primaryHighlightTextOffset,
            secondary = secondaryText(),
            baseSp = baseSp,
            lyricsTop = lyricsTop,
            lyricsHeight = lyricsHeight,
            offsetY = dp(6) * (1f - lineTransitionProgress),
            alpha = incomingAlpha,
        )
    }

    private fun drawLyricsBlock(
        canvas: Canvas,
        primary: String,
        highlightTextOffset: Float,
        secondary: String?,
        baseSp: Float,
        lyricsTop: Float,
        lyricsHeight: Float,
        offsetY: Float,
        alpha: Float,
    ) {
        if (alpha <= 0f || primary.isBlank()) return
        secondaryPaint.typeface = Typeface.DEFAULT
        val layout = fittedLyricsLayout(
            primary = primary,
            secondary = secondary,
            maximumPrimarySize = baseSp * scaledDensity,
            availableWidth = width - dp(LYRICS_HORIZONTAL_PADDING_DP * 2),
            availableHeight = lyricsHeight,
        )
        textPaint.textSize = layout.primaryTextSize
        secondaryPaint.textSize = layout.secondaryTextSize
        var baseline = lyricsTop + (lyricsHeight - layout.height) / 2f + offsetY - textPaint.ascent()
        layout.primaryRows.forEach { row ->
            val startX = (width - row.width) / 2f
            drawOutlinedText(
                canvas,
                row.text,
                startX,
                baseline,
                textPaint,
                contentColor().withAlpha((235 * alpha).toInt()),
            )
            drawKaraokeHighlight(
                canvas = canvas,
                text = row.text,
                startX = startX,
                baseline = baseline,
                highlightTextOffset = highlightTextOffset - row.sourceStart,
                alpha = alpha,
            )
            baseline += layout.primaryLineAdvance
        }
        if (layout.secondaryRows.isNotEmpty()) {
            baseline = lyricsTop + (lyricsHeight - layout.height) / 2f + offsetY +
                layout.primaryHeight + layout.blockGap - secondaryPaint.ascent()
            layout.secondaryRows.forEach { row ->
                drawOutlinedText(
                    canvas,
                    row.text,
                    (width - row.width) / 2f,
                    baseline,
                    secondaryPaint,
                    contentColor().withAlpha((170 * alpha).toInt()),
                )
                baseline += layout.secondaryLineAdvance
            }
        }
    }

    private fun drawKaraokeHighlight(
        canvas: Canvas,
        text: String,
        startX: Float,
        baseline: Float,
        highlightTextOffset: Float,
        alpha: Float,
    ) {
        if (highlightTextOffset <= 0f || alpha <= 0f) return
        val offset = highlightTextOffset.coerceAtMost(text.length.toFloat())
        val wholeCharacters = floor(offset).toInt()
        val highlighted = text.take(wholeCharacters)
        val partialCharacter = text.getOrNull(wholeCharacters)?.toString().orEmpty()
        val frontX = startX + textPaint.measureText(highlighted) +
            textPaint.measureText(partialCharacter) * (offset - wholeCharacters)
        val feather = dp(10).toFloat()
        val gradientEnd = (frontX + feather).coerceAtLeast(startX + 1f)
        val solidEnd = (frontX - feather).coerceAtLeast(startX)
        val solidPosition = ((solidEnd - startX) / (gradientEnd - startX)).coerceIn(0f, 0.98f)
        val accent = accentColor().withAlpha((255 * alpha).toInt())
        textPaint.style = Paint.Style.FILL
        textPaint.shader = LinearGradient(
            startX,
            0f,
            gradientEnd,
            0f,
            intArrayOf(accent, accent, accent.withAlpha(0)),
            floatArrayOf(0f, solidPosition, 1f),
            Shader.TileMode.CLAMP,
        )
        textPaint.setShadowLayer(
            if (isTransparentSurface()) density * 1.8f else 0f,
            0f,
            density * 0.5f,
            outlineColor(accentColor()).withAlpha(150),
        )
        canvas.drawText(text, startX, baseline, textPaint)
        textPaint.shader = null
        textPaint.clearShadowLayer()
    }

    private fun drawOutlinedText(
        canvas: Canvas,
        text: String,
        x: Float,
        baseline: Float,
        paint: Paint,
        fillColor: Int,
    ) {
        paint.shader = null
        paint.clearShadowLayer()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = density * if (isTransparentSurface()) 1.65f else 0.85f
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = outlineColor(fillColor).withAlpha(
            (if (isTransparentSurface()) 205 else 115) * Color.alpha(fillColor) / 255,
        )
        canvas.drawText(text, x, baseline, paint)
        paint.style = Paint.Style.FILL
        paint.color = fillColor
        if (isTransparentSurface()) {
            paint.setShadowLayer(
                density * 2.1f,
                0f,
                density * 0.7f,
                outlineColor(fillColor).withAlpha(180),
            )
        }
        canvas.drawText(text, x, baseline, paint)
        paint.clearShadowLayer()
    }

    private fun secondaryText(): String? = content.supplemental ?: content.next

    private fun fittedLyricsLayout(
        primary: String,
        secondary: String?,
        maximumPrimarySize: Float,
        availableWidth: Int,
        availableHeight: Float,
    ): LyricsBlockLayout {
        fun layoutAt(primarySize: Float): LyricsBlockLayout {
            textPaint.textSize = primarySize
            secondaryPaint.textSize = primarySize * SECONDARY_TEXT_SIZE_RATIO
            val primaryRows = wrapText(primary, textPaint, availableWidth)
            val secondaryRows = secondary
                ?.takeIf(String::isNotBlank)
                ?.let { wrapText(it, secondaryPaint, availableWidth) }
                .orEmpty()
            val primaryLineAdvance = textPaint.fontSpacing * LINE_ADVANCE_RATIO
            val secondaryLineAdvance = secondaryPaint.fontSpacing * LINE_ADVANCE_RATIO
            val primaryHeight = blockHeight(primaryRows.size, textPaint, primaryLineAdvance)
            val secondaryHeight = blockHeight(secondaryRows.size, secondaryPaint, secondaryLineAdvance)
            val blockGap = if (secondaryRows.isEmpty()) 0f else primarySize * BLOCK_GAP_RATIO
            return LyricsBlockLayout(
                primaryRows = primaryRows,
                secondaryRows = secondaryRows,
                primaryTextSize = primarySize,
                secondaryTextSize = secondaryPaint.textSize,
                primaryLineAdvance = primaryLineAdvance,
                secondaryLineAdvance = secondaryLineAdvance,
                primaryHeight = primaryHeight,
                blockGap = blockGap,
                height = primaryHeight + blockGap + secondaryHeight,
            )
        }

        val maximumLayout = layoutAt(maximumPrimarySize)
        if (maximumLayout.height <= availableHeight) return maximumLayout
        var lower = MIN_DYNAMIC_TEXT_SIZE_PX
        var upper = maximumPrimarySize
        repeat(DYNAMIC_TEXT_SIZE_SEARCH_STEPS) {
            val candidate = (lower + upper) / 2f
            if (layoutAt(candidate).height <= availableHeight) {
                lower = candidate
            } else {
                upper = candidate
            }
        }
        return layoutAt(lower)
    }

    private fun wrapText(text: String, paint: Paint, maximumWidth: Int): List<LyricsTextRow> {
        if (text.isEmpty()) return emptyList()
        val rows = mutableListOf<LyricsTextRow>()
        var start = 0
        while (start < text.length) {
            if (text[start] == '\n') {
                rows += LyricsTextRow("", start, 0f)
                start++
                continue
            }
            val paragraphEnd = text.indexOf('\n', start).takeIf { it >= 0 } ?: text.length
            val measuredCount = paint.breakText(
                text,
                start,
                paragraphEnd,
                true,
                maximumWidth.toFloat(),
                null,
            ).coerceAtLeast(1)
            var end = (start + measuredCount).coerceAtMost(paragraphEnd)
            if (end < paragraphEnd) {
                val wordBreak = text.lastIndexOf(' ', end - 1)
                if (wordBreak > start) end = wordBreak
            }
            val rowText = text.substring(start, end).trimEnd()
            rows += LyricsTextRow(rowText, start, paint.measureText(rowText))
            start = end
            while (start < paragraphEnd && text[start].isWhitespace()) start++
            if (start == paragraphEnd && start < text.length && text[start] == '\n') start++
        }
        return rows
    }

    private fun blockHeight(rowCount: Int, paint: Paint, lineAdvance: Float): Float = when (rowCount) {
        0 -> 0f
        else -> paint.descent() - paint.ascent() + lineAdvance * (rowCount - 1)
    }

    private fun drawControls(canvas: Canvas) {
        buttonRects.values.forEach(RectF::setEmpty)
        val edgeX = dp(CONTROL_EDGE_DP).toFloat()
        val topY = dp(CONTROL_EDGE_DP).toFloat()
        val bottomY = height - dp(CONTROL_EDGE_DP).toFloat()
        drawControl(canvas, DesktopLyricsControl.Lock, edgeX, topY)
        if (preferences.desktopLyricsLocked) return
        drawControl(canvas, DesktopLyricsControl.Close, width - edgeX, topY)
        drawControl(canvas, DesktopLyricsControl.Settings, edgeX, bottomY)
        drawControl(canvas, DesktopLyricsControl.Previous, width * 0.30f, bottomY)
        drawControl(canvas, DesktopLyricsControl.PlayPause, width / 2f, bottomY)
        drawControl(canvas, DesktopLyricsControl.Next, width * 0.70f, bottomY)
        drawControl(canvas, DesktopLyricsControl.Mode, width - edgeX, bottomY)
    }

    private fun drawControl(canvas: Canvas, control: DesktopLyricsControl, centerX: Float, centerY: Float) {
        val touchRadius = dp(CONTROL_EDGE_DP).toFloat()
        buttonRects.getValue(control).set(
            centerX - touchRadius,
            centerY - touchRadius,
            centerX + touchRadius,
            centerY + touchRadius,
        )
        val isPrimary = control == DesktopLyricsControl.PlayPause
        val containerColor = when {
            isPrimary -> accentColor()
            else -> ColorUtils.blendARGB(palette.surfaceArgb, accentColor(), 0.10f)
        }
        surfacePaint.style = Paint.Style.FILL
        surfacePaint.color = containerColor.withAlpha(if (isTransparentSurface()) 238 else 224)
        surfacePaint.setShadowLayer(density * 1.6f, 0f, density * 0.6f, Color.BLACK.withAlpha(48))
        canvas.drawCircle(
            centerX,
            centerY,
            dp(if (isPrimary) 19 else 15).toFloat(),
            surfacePaint,
        )
        surfacePaint.clearShadowLayer()
        iconPaint.color = if (isPrimary) {
            readableForeground(containerColor)
        } else {
            palette.onSurfaceArgb
        }
        iconPaint.setShadowLayer(
            if (isTransparentSurface()) density * 1.8f else 0f,
            0f,
            density * 0.6f,
            outlineColor(iconPaint.color).withAlpha(210),
        )
        drawIcon(canvas, control, centerX, centerY)
        iconPaint.clearShadowLayer()
    }

    private fun contentColor(): Int = if (preferences.desktopLyricsSurfaceOpacity < 20) {
        palette.transparentContentArgb
    } else {
        palette.onSurfaceArgb
    }

    private fun accentColor(): Int = if (preferences.desktopLyricsSurfaceOpacity < 20) {
        palette.transparentAccentArgb
    } else {
        palette.accentArgb
    }

    private fun isTransparentSurface(): Boolean = preferences.desktopLyricsSurfaceOpacity < 20

    private fun lyricsInset(): Int = dp(LYRICS_CONTROLS_INSET_DP)

    private fun baseTextSizeSp(): Float = when (preferences.desktopLyricsFontSize) {
        LyricsFontSize.Small -> 19f
        LyricsFontSize.Medium -> 23f
        LyricsFontSize.Large -> 28f
    }

    private fun outlineColor(fillColor: Int): Int = if (ColorUtils.calculateLuminance(fillColor) > 0.45) {
        Color.BLACK
    } else {
        Color.WHITE
    }

    private fun drawIcon(canvas: Canvas, control: DesktopLyricsControl, x: Float, y: Float) {
        val d = density
        iconPaint.style = Paint.Style.STROKE
        val checkpoint = canvas.save()
        canvas.scale(CONTROL_ICON_SCALE, CONTROL_ICON_SCALE, x, y)
        when (control) {
            DesktopLyricsControl.Lock -> {
                canvas.drawRoundRect(x - 7 * d, y - d, x + 7 * d, y + 9 * d, 2 * d, 2 * d, iconPaint)
                if (preferences.desktopLyricsLocked) {
                    canvas.drawArc(x - 5 * d, y - 10 * d, x + 5 * d, y, 180f, 180f, false, iconPaint)
                    canvas.drawLine(x - 5 * d, y - 5 * d, x - 5 * d, y - d, iconPaint)
                    canvas.drawLine(x + 5 * d, y - 5 * d, x + 5 * d, y - d, iconPaint)
                } else {
                    canvas.drawArc(x - 5 * d, y - 9 * d, x + 6 * d, y + 3 * d, 180f, 125f, false, iconPaint)
                }
            }
            DesktopLyricsControl.Settings -> drawSettingsIcon(canvas, x, y, d)
            DesktopLyricsControl.Previous, DesktopLyricsControl.Next -> {
                val direction = if (control == DesktopLyricsControl.Next) 1f else -1f
                canvas.drawLine(x + direction * 8 * d, y - 8 * d, x + direction * 8 * d, y + 8 * d, iconPaint)
                val path = Path().apply {
                    moveTo(x + direction * 5 * d, y)
                    lineTo(x - direction * 7 * d, y - 8 * d)
                    lineTo(x - direction * 7 * d, y + 8 * d)
                    close()
                }
                iconPaint.style = Paint.Style.FILL
                canvas.drawPath(path, iconPaint)
            }
            DesktopLyricsControl.PlayPause -> if (isPlaying) {
                iconPaint.style = Paint.Style.FILL
                canvas.drawRoundRect(x - 7 * d, y - 8 * d, x - 2 * d, y + 8 * d, d, d, iconPaint)
                canvas.drawRoundRect(x + 2 * d, y - 8 * d, x + 7 * d, y + 8 * d, d, d, iconPaint)
            } else {
                val path = Path().apply {
                    moveTo(x - 5 * d, y - 9 * d)
                    lineTo(x + 9 * d, y)
                    lineTo(x - 5 * d, y + 9 * d)
                    close()
                }
                iconPaint.style = Paint.Style.FILL
                canvas.drawPath(path, iconPaint)
            }
            DesktopLyricsControl.Mode -> {
                val icon = playbackModeIcons.getValue(playbackMode)
                val halfSize = 12 * d
                DrawableCompat.setTint(icon, iconPaint.color)
                icon.setBounds(
                    (x - halfSize).toInt(),
                    (y - halfSize).toInt(),
                    (x + halfSize).toInt(),
                    (y + halfSize).toInt(),
                )
                icon.draw(canvas)
            }
            DesktopLyricsControl.Close -> {
                canvas.drawLine(x - 7 * d, y - 7 * d, x + 7 * d, y + 7 * d, iconPaint)
                canvas.drawLine(x + 7 * d, y - 7 * d, x - 7 * d, y + 7 * d, iconPaint)
            }
        }
        canvas.restoreToCount(checkpoint)
    }

    private fun drawSettingsIcon(canvas: Canvas, x: Float, y: Float, d: Float) {
        canvas.drawLine(x - 8 * d, y - 6 * d, x + 8 * d, y - 6 * d, iconPaint)
        canvas.drawLine(x - 8 * d, y, x + 8 * d, y, iconPaint)
        canvas.drawLine(x - 8 * d, y + 6 * d, x + 8 * d, y + 6 * d, iconPaint)
        iconPaint.style = Paint.Style.FILL
        canvas.drawCircle(x - 3 * d, y - 6 * d, 2 * d, iconPaint)
        canvas.drawCircle(x + 4 * d, y, 2 * d, iconPaint)
        canvas.drawCircle(x - d, y + 6 * d, 2 * d, iconPaint)
    }

    private fun dp(value: Int) = (value * density).toInt()

    private companion object {
        const val CONTROL_EDGE_DP = 20
        const val CONTROL_ICON_SCALE = 0.68f
        const val LYRICS_CONTROLS_INSET_DP = 38
        const val LYRICS_HORIZONTAL_PADDING_DP = 14
        const val SECONDARY_TEXT_SIZE_RATIO = 0.62f
        const val LINE_ADVANCE_RATIO = 0.92f
        const val BLOCK_GAP_RATIO = 0.14f
        const val MIN_DYNAMIC_TEXT_SIZE_PX = 0.5f
        const val DYNAMIC_TEXT_SIZE_SEARCH_STEPS = 12
    }
}

private data class LyricsTextRow(val text: String, val sourceStart: Int, val width: Float)

private data class LyricsBlockLayout(
    val primaryRows: List<LyricsTextRow>,
    val secondaryRows: List<LyricsTextRow>,
    val primaryTextSize: Float,
    val secondaryTextSize: Float,
    val primaryLineAdvance: Float,
    val secondaryLineAdvance: Float,
    val primaryHeight: Float,
    val blockGap: Float,
    val height: Float,
)

private fun Int.withAlpha(alpha: Int): Int = Color.argb(
    alpha.coerceIn(0, 255),
    Color.red(this),
    Color.green(this),
    Color.blue(this),
)

private fun readableForeground(background: Int): Int = if (
    ColorUtils.calculateContrast(Color.WHITE, background) >= 4.5
) {
    Color.WHITE
} else {
    Color.BLACK
}

internal fun desktopLyricsWindowY(anchorY: Int, controlsInsetPx: Int): Int = anchorY - controlsInsetPx

internal fun desktopLyricsAnchorY(windowY: Int, controlsInsetPx: Int): Int = windowY + controlsInsetPx
