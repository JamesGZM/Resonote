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
import androidx.core.graphics.drawable.DrawableCompat
import com.resonote.core.designsystem.icon.iconResource
import com.resonote.core.model.DesktopLyricsDefaults
import com.resonote.core.model.DesktopLyricsPosition
import com.resonote.core.model.LyricsPreferences
import com.resonote.core.model.PlaybackMode
import kotlin.math.abs
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
    private val view = DesktopLyricsControllerView(context, ::handleControl)
    private var params: WindowManager.LayoutParams? = null
    private var preferences = LyricsPreferences()
    private var dragStartX = 0
    private var dragStartY = 0

    val isVisible: Boolean get() = params != null

    fun show(preferences: LyricsPreferences): Boolean {
        this.preferences = preferences
        if (isVisible) return true
        val contentWidth = contentWidth(preferences)
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
        val oldWidth = layoutParams.width
        val newWidth = contentWidth(preferences)
        layoutParams.flags = flags()
        layoutParams.alpha = windowAlpha()
        layoutParams.width = newWidth
        layoutParams.height = expandedHeight(preferences)
        if (oldWidth != newWidth) {
            layoutParams.x += (oldWidth - newWidth) / 2
        }
        runCatching { windowManager.updateViewLayout(view, layoutParams) }.onFailure { hide() }
        view.post(::clampToScreen)
    }

    fun render(content: DesktopLyricsContent) {
        view.content = content
    }

    fun renderMessage(message: String) {
        view.content = DesktopLyricsContent(message, 0f)
    }

    fun updatePlayback(isPlaying: Boolean, mode: PlaybackMode) {
        view.isPlaying = isPlaying
        view.playbackMode = mode
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

    private fun flags(): Int = desktopLyricsWindowFlags(preferences.desktopLyricsLocked)

    private fun windowAlpha(): Float = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        desktopLyricsWindowAlpha(
            locked = preferences.desktopLyricsLocked,
            opacityPercent = preferences.desktopLyricsSurfaceOpacity,
            maximumObscuringOpacity = context.getSystemService(InputManager::class.java)
                .maximumObscuringOpacityForTouch,
        )
    } else {
        preferences.desktopLyricsSurfaceOpacity.coerceIn(0, 100) / 100f
    }

    private fun defaultPosition(): DesktopLyricsPosition {
        val (width, height) = displaySize()
        return DesktopLyricsPosition((width - contentWidth(preferences)) / 2, (height * 0.14f).toInt())
    }

    private fun contentWidth(value: LyricsPreferences): Int {
        val available = displaySize().first - dp(SCREEN_EDGE_GAP_DP * 2)
        return (available * value.desktopLyricsWidthPercent.coerceIn(40, 100) / 100f).toInt()
            .coerceIn(minOf(dp(MIN_CONTROLLER_WIDTH_DP), available), available)
    }

    private fun collapsedHeight(value: LyricsPreferences): Int =
        (value.desktopLyricsFontSizeSp * context.resources.displayMetrics.scaledDensity + dp(28))
            .toInt()
            .coerceAtLeast(dp(64))
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
        const val MIN_CONTROLLER_WIDTH_DP = 220
        const val SCREEN_EDGE_GAP_DP = 4
    }
}

internal enum class DesktopLyricsControl { Lock, Close, Settings, Previous, PlayPause, Next, Mode }

internal fun desktopLyricsWindowFlags(locked: Boolean): Int = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
    if (locked) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0

internal fun desktopLyricsWindowAlpha(locked: Boolean, opacityPercent: Int, maximumObscuringOpacity: Float): Float {
    val requestedOpacity = opacityPercent.coerceIn(0, 100) / 100f
    return if (locked) {
        minOf(requestedOpacity, maximumObscuringOpacity.coerceIn(0f, 1f))
    } else {
        requestedOpacity
    }
}

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

private class DesktopLyricsControllerView(context: Context, private val onControl: (DesktopLyricsControl) -> Unit) :
    View(context) {
    private val density = resources.displayMetrics.density
    private val scaledDensity = density * resources.configuration.fontScale
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val handler = Handler(Looper.getMainLooper())
    private val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD }
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
    private var downRawX = 0f
    private var downRawY = 0f
    private var dragging = false
    private var movedBeyondTouchSlop = false
    private var controlsWereVisibleOnDown = false
    private var pressedControl: DesktopLyricsControl? = null
    private var previousPrimary: String? = null
    private var previousPrimaryHighlightTextOffset = 0f
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
            invalidate()
        }
    var content: DesktopLyricsContent = DesktopLyricsContent("", 0f)
        set(value) {
            if (field.primary.isNotBlank() && field.primary != value.primary) {
                previousPrimary = field.primary
                previousPrimaryHighlightTextOffset = field.primaryHighlightTextOffset
                lineTransitionAnimator.cancel()
                lineTransitionProgress = 0f
                lineTransitionAnimator.start()
            }
            field = value
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

    fun release() {
        lineTransitionAnimator.cancel()
        resetControlsVisibility()
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

    private fun drawLyrics(canvas: Canvas) {
        val lyricsTop = lyricsInset().toFloat()
        val lyricsHeight = height - lyricsInset() * 2f
        val outgoingAlpha = (1f - lineTransitionProgress / 0.72f).coerceIn(0f, 1f)
        val incomingAlpha = ((lineTransitionProgress - 0.18f) / 0.82f).coerceIn(0f, 1f)
        previousPrimary?.let { previous ->
            drawLyricsBlock(
                canvas = canvas,
                primary = previous,
                highlightTextOffset = previousPrimaryHighlightTextOffset,
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
        lyricsTop: Float,
        lyricsHeight: Float,
        offsetY: Float,
        alpha: Float,
    ) {
        if (alpha <= 0f || primary.isBlank()) return
        textPaint.textSize = preferences.desktopLyricsFontSizeSp.coerceIn(16, 40) * scaledDensity
        val rows = wrapText(primary, textPaint, width - dp(LYRICS_HORIZONTAL_PADDING_DP * 2))
        val row = rows.getOrNull(
            desktopLyricsSegmentIndex(rows.map(LyricsTextRow::sourceStart), highlightTextOffset),
        ) ?: return
        val startX = (width - row.width) / 2f
        val textHeight = textPaint.descent() - textPaint.ascent()
        val baseline = lyricsTop + (lyricsHeight - textHeight) / 2f + offsetY - textPaint.ascent()
        val background = preferences.desktopLyricsBackgroundColorArgb
        drawStyledText(
            canvas = canvas,
            text = row.text,
            x = startX,
            baseline = baseline,
            fillColor = background.withAlpha((150 * alpha).toInt()),
        )
        drawKaraokeHighlight(
            canvas = canvas,
            text = row.text,
            startX = startX,
            baseline = baseline,
            highlightTextOffset = highlightTextOffset - row.sourceStart,
            alpha = alpha,
        )
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
        val accent = preferences.desktopLyricsForegroundColorArgb.withAlpha((255 * alpha).toInt())
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
        textPaint.clearShadowLayer()
        canvas.drawText(text, startX, baseline, textPaint)
        textPaint.shader = null
        textPaint.clearShadowLayer()
    }

    private fun drawStyledText(canvas: Canvas, text: String, x: Float, baseline: Float, fillColor: Int) {
        textPaint.shader = null
        textPaint.clearShadowLayer()
        val outlineWidth = preferences.desktopLyricsOutlineWidthDp.coerceIn(0f, 4f)
        if (outlineWidth > 0f) {
            textPaint.style = Paint.Style.STROKE
            textPaint.strokeWidth = density * outlineWidth
            textPaint.strokeJoin = Paint.Join.ROUND
            textPaint.color = preferences.desktopLyricsOutlineColorArgb.withAlpha(Color.alpha(fillColor))
            canvas.drawText(text, x, baseline, textPaint)
        }
        textPaint.style = Paint.Style.FILL
        textPaint.color = fillColor
        val blur = preferences.desktopLyricsShadowBlurRadiusDp.coerceIn(0f, 12f)
        if (blur > 0f) {
            textPaint.setShadowLayer(
                blur * density,
                preferences.desktopLyricsShadowOffsetXDp.coerceIn(-8f, 8f) * density,
                preferences.desktopLyricsShadowOffsetYDp.coerceIn(-8f, 8f) * density,
                preferences.desktopLyricsShadowColorArgb.withAlpha(Color.alpha(fillColor)),
            )
        }
        canvas.drawText(text, x, baseline, textPaint)
        textPaint.clearShadowLayer()
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
        surfacePaint.style = Paint.Style.FILL
        surfacePaint.color = DesktopLyricsDefaults.FOREGROUND_COLOR_ARGB
        canvas.drawCircle(
            centerX,
            centerY,
            dp(if (isPrimary) 19 else 15).toFloat(),
            surfacePaint,
        )
        iconPaint.color = Color.WHITE
        drawIcon(canvas, control, centerX, centerY)
    }

    private fun lyricsInset(): Int = dp(LYRICS_CONTROLS_INSET_DP)

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
    }
}

private data class LyricsTextRow(val text: String, val sourceStart: Int, val width: Float)

internal fun desktopLyricsSegmentIndex(sourceStarts: List<Int>, highlightTextOffset: Float): Int =
    sourceStarts.indexOfLast { it <= highlightTextOffset }.coerceAtLeast(0)

private fun Int.withAlpha(alpha: Int): Int = Color.argb(
    alpha.coerceIn(0, 255),
    Color.red(this),
    Color.green(this),
    Color.blue(this),
)

internal fun desktopLyricsWindowY(anchorY: Int, controlsInsetPx: Int): Int = anchorY - controlsInsetPx

internal fun desktopLyricsAnchorY(windowY: Int, controlsInsetPx: Int): Int = windowY + controlsInsetPx
