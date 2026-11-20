package com.bellatrixos.wayland

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.bellatrixos.R

private data class Key(
    val label: String,
    val combo: String? = null,
    val latch: String? = null,
    val repeats: Boolean = false,
    val arrow: Int? = null,
    val icon: Int? = null,
    val images: Boolean = false,
)

/**
 * The stock Bellatrix terminal key bar, drawn over the soft keyboard. The
 * two rows, labels, modifiers, clipboard keys, photo key, repeat timing and
 * latch gestures are the ones in `keybar.owl`. Bellatrix does not ship the OWL
 * compiler, so this draws the stock bar rather than parsing a user's .owl.
 */
class KeyBarView(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val bridge get() = KeyboardBridgeHolder.bridge

    /** Set by LinuxWindowActivity once the runtime is available. */
    object KeyboardBridgeHolder {
        @Volatile
        var bridge: KeyboardBridge? = null
    }

    private val rows =
        listOf(
            listOf(
                Key("esc", combo = "esc"),
                Key("/", combo = "/"),
                Key("-", combo = "-"),
                Key("home", combo = "home"),
                Key("up", combo = "up", repeats = true, arrow = 0),
                Key("end", combo = "end"),
                Key("pgup", combo = "pgup", repeats = true),
                Key("copy", combo = "ctrl+insert", icon = R.drawable.ic_key_copy),
                Key("|", combo = "|"),
            ),
            listOf(
                Key("tab", combo = "tab"),
                Key("ctrl", latch = "ctrl"),
                Key("alt", latch = "alt"),
                Key("left", combo = "left", repeats = true, arrow = -90),
                Key("down", combo = "down", repeats = true, arrow = 180),
                Key("right", combo = "right", repeats = true, arrow = 90),
                Key("pgdn", combo = "pgdn", repeats = true),
                Key("paste", combo = "shift+insert", icon = R.drawable.ic_key_paste),
                Key("photos", icon = R.drawable.ic_key_photo, images = true),
            ),
        )

    private val dp = resources.displayMetrics.density
    private val keyHeight = (40 * dp).toInt()
    private val pad = (3 * dp).toInt()
    private val textSize = 14 * dp
    private val corner = 8 * dp

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2 * dp
        }
    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            this.textSize = this@KeyBarView.textSize
            typeface = android.graphics.Typeface.MONOSPACE
        }

    private val colors =
        Colors(
            background = 0xFF1A1B26.toInt(),
            surface = 0xFF282A3A.toInt(),
            foreground = 0xFFC0CAF5.toInt(),
            accent = 0xFF7AA2F7.toInt(),
            border = 0xFF3B4261.toInt(),
        )

    private data class Colors(
        val background: Int,
        val surface: Int,
        val foreground: Int,
        val accent: Int,
        val border: Int,
    )

    private val icons =
        mapOf(
            R.drawable.ic_key_copy to ContextCompat.getDrawable(context, R.drawable.ic_key_copy),
            R.drawable.ic_key_paste to ContextCompat.getDrawable(context, R.drawable.ic_key_paste),
            R.drawable.ic_key_photo to ContextCompat.getDrawable(context, R.drawable.ic_key_photo),
        )

    private val chevron: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_key_chevron)
    private val rect = RectF()
    private var downKey: Key? = null
    private var downRow = -1
    private var downCol = -1
    private var longPressed = false

    private val handler = Handler(Looper.getMainLooper())
    private val repeat =
        object : Runnable {
            override fun run() {
                val key = downKey ?: return
                if (key.repeats && !key.images && key.combo != null) bridge?.press(key.combo)
                handler.postDelayed(this, 50L)
            }
        }
    private val longPress =
        Runnable {
            longPressed = true
            downKey?.latch?.let { bridge?.latch(it, "locked") }
        }

    override fun onMeasure(
        widthSpec: Int,
        heightSpec: Int,
    ) {
        val w = MeasureSpec.getSize(widthSpec)
        setMeasuredDimension(w, keyHeight * rows.size + pad * 2)
    }

    override fun onDraw(canvas: Canvas) {
        backgroundPaint.color = colors.background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        for ((r, row) in rows.withIndex()) {
            for ((c, key) in row.withIndex()) {
                val bounds = bounds(r, c)
                val held = key.latch?.let { bridge?.latches?.get(it)?.latch }
                val pressed = downKey === key
                val locked = held == Latch.Locked
                val once = held == Latch.Once
                keyPaint.color = if (pressed || locked) colors.accent else colors.surface
                canvas.drawRoundRect(bounds, corner, corner, keyPaint)
                if (once) {
                    borderPaint.color = colors.accent
                    canvas.drawRoundRect(bounds, corner, corner, borderPaint)
                }
                val fg = if (pressed || locked) colors.background else colors.foreground
                val cx = bounds.centerX()
                val cy = bounds.centerY()
                val icon = key.icon?.let(icons::get)
                if (key.arrow != null && chevron != null) {
                    drawRotated(canvas, chevron, cx, cy, key.arrow, fg)
                } else if (icon != null) {
                    drawIcon(canvas, icon, cx, cy, fg)
                } else {
                    textPaint.color = fg
                    canvas.drawText(key.label, cx, cy - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
                }
            }
        }
    }

    private fun bounds(
        row: Int,
        col: Int,
    ): RectF {
        val cols = rows[row].size
        val cell = width.toFloat() / cols
        val x0 = col * cell + pad.toFloat()
        val y0 = pad.toFloat() + row * keyHeight.toFloat() + pad.toFloat()
        return RectF(x0, y0, (col + 1) * cell - pad.toFloat(), y0 + keyHeight.toFloat() - pad.toFloat() * 2f)
    }

    private fun drawIcon(
        canvas: Canvas,
        drawable: Drawable,
        cx: Float,
        cy: Float,
        color: Int,
    ) {
        val size = (20 * dp).toInt()
        drawable.setBounds((cx - size / 2).toInt(), (cy - size / 2).toInt(), (cx + size / 2).toInt(), (cy + size / 2).toInt())
        drawable.setTint(color)
        drawable.draw(canvas)
    }

    private fun drawRotated(
        canvas: Canvas,
        drawable: Drawable,
        cx: Float,
        cy: Float,
        degrees: Int,
        color: Int,
    ) {
        val size = (20 * dp).toInt()
        canvas.save()
        canvas.rotate(degrees.toFloat(), cx, cy)
        drawIcon(canvas, drawable, cx, cy, color)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val bridge = bridge ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val col = (event.x / (width.toFloat() / rows[0].size)).toInt().coerceIn(0, rows[0].size - 1)
                val row = ((event.y - pad) / keyHeight).toInt().coerceIn(0, rows.size - 1)
                val key = rows[row][col]
                downKey = key
                downRow = row
                downCol = col
                longPressed = false
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                if (key.images || key.latch != null) {
                    handler.postDelayed(longPress, 400L)
                } else if (key.repeats) {
                    bridge.press(key.combo ?: return true)
                    handler.postDelayed(repeat, 400L)
                } else {
                    bridge.flush()
                    key.combo?.let(bridge::press)
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> return true

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val key = downKey ?: return true
                handler.removeCallbacks(repeat)
                handler.removeCallbacks(longPress)
                if (event.actionMasked == MotionEvent.ACTION_UP && !longPressed) {
                    when {
                        key.images -> bridge.pickImages()
                        key.latch != null -> bridge.latch(key.latch)
                        else -> Unit
                    }
                }
                downKey = null
                downRow = -1
                downCol = -1
                invalidate()
                return true
            }

            else -> return false
        }
    }

    fun refresh() = invalidate()

    init {
        isClickable = true
    }
}
