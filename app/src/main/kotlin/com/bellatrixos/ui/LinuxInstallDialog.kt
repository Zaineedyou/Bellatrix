package com.bellatrixos.ui

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.bellatrixos.R
import java.util.function.Consumer
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

/**
 * "Install Linux?" prompt shown when a guest command (Terminal, a Linux app) is started before Linux exists, instead
 * of throwing the person into Settings.
 *
 * It is an iOS-style liquid glass card made of plain views, like the rest of the launcher's windows:
 *  - Android 12+: the window itself blurs what is behind it (real backdrop blur, clipped to the card's rounded shape).
 *  - Android 13+: an AGSL shader paints the glass: a bevelled edge with a specular highlight, chromatic dispersion
 *    on the rim, and a sheen that follows the phone's tilt and the finger.
 *  - Older phones get the same layers drawn with plain canvas gradients.
 * The card springs in, the blur ramps up with it, and the buttons squish under the finger.
 */
class LinuxInstallDialog(
    context: Context,
    private val onInstall: () -> Unit,
    private val onCancel: () -> Unit = {},
) : Dialog(context, android.R.style.Theme_Translucent_NoTitleBar) {
    private val density = context.resources.displayMetrics.density
    private val glass = GlassState()
    private var card: GlassLayout? = null
    private var blurRadiusPx = 0
    private var blurOn = false
    private var closing = false
    private var blurListener: Consumer<Boolean>? = null
    private var sensors: SensorManager? = null
    private var tiltListener: SensorEventListener? = null
    private var ticker: ValueAnimator? = null
    private var windowShape: GradientDrawable? = null

    private fun dp(v: Float) = v * density

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val w = window ?: return
        w.setBackgroundDrawable(null)
        w.setLayout(min(context.resources.displayMetrics.widthPixels - dp(48f).toInt(), dp(340f).toInt()), ViewGroup.LayoutParams.WRAP_CONTENT)
        w.setGravity(Gravity.CENTER)
        w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        w.setDimAmount(0f)
        setCanceledOnTouchOutside(true)
        setOnCancelListener { onCancel() }

        val panel = buildCard()
        card = panel
        setContentView(panel)

        // The window background decides the shape of the blurred area (see "Window blurs" in the Android docs).
        val shape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(CARD_RADIUS_DP)
        }
        windowShape = shape
        w.setBackgroundDrawable(shape)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurRadiusPx = dp(38f).toInt()
            w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            val attrs = w.attributes
            attrs.blurBehindRadius = dp(10f).toInt()
            attrs.dimAmount = 0.18f
            w.attributes = attrs
            val wm = w.windowManager
            blurOn = wm.isCrossWindowBlurEnabled
            val listener = Consumer<Boolean> { enabled ->
                blurOn = enabled
                applyBlur(if (enabled) blurRadiusPx else 0)
            }
            blurListener = listener
            wm.addCrossWindowBlurEnabledListener(listener)
        } else {
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            w.setDimAmount(0.45f)
        }
        applyBlur(0)
    }

    /** The card's own tint is drawn by the shader; the window colour only needs to be readable when blur is off. */
    private fun applyBlur(radius: Int) {
        val w = window ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            w.setBackgroundBlurRadius(if (blurOn) radius else 0)
        }
        windowShape?.setColor(if (blurOn) Color.argb(46, 14, 18, 30) else Color.argb(235, 24, 28, 40))
    }

    private fun buildCard(): GlassLayout {
        val root = GlassLayout(context, glass, dp(CARD_RADIUS_DP), Color.rgb(16, 22, 40), 0.34f)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        val pad = dp(24f).toInt()
        root.setPadding(pad, dp(26f).toInt(), pad, pad)
        glass.watch(root)

        val badge = GlassText(context, glass, dp(30f), Color.WHITE, 0.16f).apply {
            text = "›_"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            isClickable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        root.addView(badge, LinearLayout.LayoutParams(dp(60f).toInt(), dp(60f).toInt()))

        val title = TextView(context).apply {
            text = context.getString(R.string.linux_prompt_title)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setShadowLayer(dp(6f), 0f, dp(1f), Color.argb(90, 0, 0, 0))
        }
        root.addView(title, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16f).toInt() })

        val message = TextView(context).apply {
            text = context.getString(R.string.linux_prompt_message)
            setTextColor(Color.argb(214, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.12f)
            setShadowLayer(dp(5f), 0f, dp(1f), Color.argb(80, 0, 0, 0))
        }
        root.addView(message, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8f).toInt() })

        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val notNow = glassButton(context.getString(R.string.linux_prompt_cancel), primary = false) { closeWith(onCancel) }
        val install = glassButton(context.getString(R.string.linux_prompt_install), primary = true) { closeWith(onInstall) }
        val gap = dp(6f).toInt()
        row.addView(notNow, LinearLayout.LayoutParams(0, dp(52f).toInt(), 1f).apply { marginEnd = gap })
        row.addView(install, LinearLayout.LayoutParams(0, dp(52f).toInt(), 1f).apply { marginStart = gap })
        root.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(22f).toInt() })
        return root
    }

    private fun glassButton(label: String, primary: Boolean, action: () -> Unit): GlassText {
        val tint = if (primary) Color.rgb(10, 132, 255) else Color.WHITE
        val strength = if (primary) 0.58f else 0.16f
        return GlassText(context, glass, dp(26f), tint, strength).apply {
            text = label
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.create("sans-serif-medium", if (primary) Typeface.BOLD else Typeface.NORMAL)
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            contentDescription = label
            setShadowLayer(dp(4f), 0f, dp(1f), Color.argb(70, 0, 0, 0))
            glass.watch(this)
            bindPress(this)
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                action()
            }
        }
    }

    /** iOS-like press: the button squishes in under the finger and springs back with a little bounce. */
    private fun bindPress(v: View) {
        fun spring(prop: DynamicAnimation.ViewProperty, to: Float, bounce: Float) =
            SpringAnimation(v, prop, to).apply {
                spring.stiffness = SpringForce.STIFFNESS_MEDIUM
                spring.dampingRatio = bounce
            }
        v.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    spring(DynamicAnimation.SCALE_X, 0.94f, SpringForce.DAMPING_RATIO_NO_BOUNCY).start()
                    spring(DynamicAnimation.SCALE_Y, 0.94f, SpringForce.DAMPING_RATIO_NO_BOUNCY).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    spring(DynamicAnimation.SCALE_X, 1f, SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY).start()
                    spring(DynamicAnimation.SCALE_Y, 1f, SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY).start()
                }
            }
            false
        }
    }

    private fun closeWith(then: () -> Unit) {
        // Run the choice first so the install starts at once; the card then melts away.
        then()
        dismiss()
    }

    override fun onStart() {
        super.onStart()
        startTilt()
        startTicker()
        animateIn()
    }

    override fun onStop() {
        stopTilt()
        ticker?.cancel()
        ticker = null
        super.onStop()
    }

    override fun dismiss() {
        val c = card
        val w = window
        if (closing || c == null || w == null || !isShowing) {
            finishDismiss()
            return
        }
        closing = true
        c.animate().cancel()
        c.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f).setDuration(170).setInterpolator(DecelerateInterpolator()).withEndAction { finishDismiss() }.start()
        ValueAnimator.ofFloat(1f, 0f).setDuration(170).apply {
            addUpdateListener { applyBlur((blurRadiusPx * it.animatedValue as Float).toInt()) }
            start()
        }
    }

    private fun finishDismiss() {
        val w = window
        val l = blurListener
        if (l != null && w != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { w.windowManager.removeCrossWindowBlurEnabledListener(l) }
        }
        blurListener = null
        if (isShowing) super.dismiss()
    }

    private fun animateIn() {
        val c = card ?: return
        c.alpha = 0f
        c.scaleX = 0.84f
        c.scaleY = 0.84f
        c.animate().alpha(1f).setDuration(160).start()
        SpringAnimation(c, DynamicAnimation.SCALE_X, 1f).apply {
            spring.stiffness = SpringForce.STIFFNESS_LOW
            spring.dampingRatio = 0.62f
            start()
        }
        SpringAnimation(c, DynamicAnimation.SCALE_Y, 1f).apply {
            spring.stiffness = SpringForce.STIFFNESS_LOW
            spring.dampingRatio = 0.62f
            start()
        }
        ValueAnimator.ofFloat(0f, 1f).setDuration(340).apply {
            interpolator = DecelerateInterpolator()
            addUpdateListener { applyBlur((blurRadiusPx * it.animatedValue as Float).toInt()) }
            start()
        }
    }

    /** One clock for every glass surface in the card: eases the light and the finger glow and repaints them. */
    private fun startTicker() {
        ticker?.cancel()
        ticker =
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1000
                repeatCount = ValueAnimator.INFINITE
                interpolator = android.view.animation.LinearInterpolator()
                addUpdateListener { glass.tick() }
                start()
            }
    }

    private fun startTilt() {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        val l =
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    // Upright and still the light comes from the top left; leaning the phone swings it.
                    val tx = (-event.values[0] / 9.81f).coerceIn(-1f, 1f)
                    val ty = ((event.values[1] / 9.81f) - 0.75f).coerceIn(-1f, 1f)
                    glass.setTilt(tx, ty)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
        sensors = sm
        tiltListener = l
        sm.registerListener(l, accel, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun stopTilt() {
        tiltListener?.let { sensors?.unregisterListener(it) }
        tiltListener = null
    }

    private companion object {
        const val CARD_RADIUS_DP = 34f
    }
}

/** What every glass surface shares: where the light is, how long it has been running, and which views to repaint. */
internal class GlassState {
    var lightX = -0.55f
    var lightY = -0.85f
    var time = 0f
    private var tiltX = 0f
    private var tiltY = 0f
    private val views = ArrayList<View>()
    private val start = System.nanoTime()

    fun watch(v: View) {
        views.add(v)
    }

    fun setTilt(x: Float, y: Float) {
        tiltX = x
        tiltY = y
    }

    fun tick() {
        time = (System.nanoTime() - start) / 1_000_000_000f
        // Ease toward the tilted light, with a slow drift so the glass shimmers even when the phone lies still.
        val goalX = -0.55f + tiltX * 1.1f + 0.12f * kotlin.math.sin(time * 0.7f)
        val goalY = -0.85f + tiltY * 0.9f + 0.10f * kotlin.math.cos(time * 0.55f)
        lightX += (goalX - lightX) * 0.12f
        lightY += (goalY - lightY) * 0.12f
        for (v in views) v.invalidate()
    }
}

/** Paints one rounded glass surface: the AGSL shader on Android 13+, canvas gradients before that. */
internal class GlassPainter(private val density: Float) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val matrix = Matrix()
    private val shader: Shader? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) createShader() else null

    private fun createShader(): Shader? = runCatching { RuntimeShader(AGSL) }.getOrNull()

    fun draw(
        c: Canvas,
        w: Float,
        h: Float,
        radius: Float,
        tint: Int,
        tintAlpha: Float,
        g: GlassState,
        touchX: Float,
        touchY: Float,
        press: Float,
    ) {
        if (w <= 0f || h <= 0f) return
        val s = shader
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && s != null) {
            drawAgsl(c, s as RuntimeShader, w, h, radius, tint, tintAlpha, g, touchX, touchY, press)
        } else {
            drawFallback(c, w, h, radius, tint, tintAlpha, g, touchX, touchY, press)
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun drawAgsl(
        c: Canvas,
        s: RuntimeShader,
        w: Float,
        h: Float,
        radius: Float,
        tint: Int,
        tintAlpha: Float,
        g: GlassState,
        touchX: Float,
        touchY: Float,
        press: Float,
    ) {
        s.setFloatUniform("size", w, h)
        s.setFloatUniform("radius", min(radius, min(w, h) / 2f))
        s.setFloatUniform("bevel", min(radius * 0.62f, 18f * density))
        s.setFloatUniform("light", g.lightX, g.lightY)
        s.setFloatUniform("touch", touchX, touchY)
        s.setFloatUniform("press", press)
        s.setFloatUniform("time", g.time)
        s.setFloatUniform("tint", Color.red(tint) / 255f, Color.green(tint) / 255f, Color.blue(tint) / 255f, tintAlpha)
        paint.shader = s
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
    }

    private fun drawFallback(
        c: Canvas,
        w: Float,
        h: Float,
        radius: Float,
        tint: Int,
        tintAlpha: Float,
        g: GlassState,
        touchX: Float,
        touchY: Float,
        press: Float,
    ) {
        val r = min(radius, min(w, h) / 2f)
        rect.set(0f, 0f, w, h)
        val a = (tintAlpha * 255f).toInt().coerceIn(0, 255)
        val base = Color.argb(a, Color.red(tint), Color.green(tint), Color.blue(tint))
        val base2 = Color.argb((a * 0.7f).toInt(), Color.red(tint), Color.green(tint), Color.blue(tint))
        paint.shader = LinearGradient(0f, 0f, 0f, h, base, base2, Shader.TileMode.CLAMP)
        paint.style = Paint.Style.FILL
        c.drawRoundRect(rect, r, r, paint)

        // Soft top sheen.
        paint.shader = LinearGradient(0f, 0f, 0f, h * 0.55f, Color.argb(70, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRoundRect(rect, r, r, paint)

        // Finger glow.
        if (press > 0.01f) {
            val gr = max(w, h) * 0.5f
            paint.shader = RadialGradient(touchX, touchY, gr, Color.argb((70 * press).toInt(), 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            c.drawRoundRect(rect, r, r, paint)
        }

        // Rim lit from the light's side: a sweep whose bright spot points at the light.
        val stroke = 1.5f * density
        val angle = Math.toDegrees(atan2(g.lightY.toDouble(), g.lightX.toDouble())).toFloat()
        val sweep =
            SweepGradient(
                w / 2f,
                h / 2f,
                intArrayOf(Color.argb(230, 255, 255, 255), Color.argb(40, 255, 255, 255), Color.argb(150, 255, 255, 255), Color.argb(40, 255, 255, 255), Color.argb(230, 255, 255, 255)),
                floatArrayOf(0f, 0.28f, 0.5f, 0.78f, 1f),
            )
        matrix.setRotate(angle, w / 2f, h / 2f)
        sweep.setLocalMatrix(matrix)
        paint.shader = sweep
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        rect.set(stroke / 2f, stroke / 2f, w - stroke / 2f, h - stroke / 2f)
        c.drawRoundRect(rect, r - stroke / 2f, r - stroke / 2f, paint)
        paint.style = Paint.Style.FILL
        paint.shader = null
    }

    private companion object {
        /**
         * Liquid glass body: a signed-distance rounded rectangle whose edge bends like a glass bevel. It lights that
         * bevel from [light] (specular plus a dimmer bounce on the far side), splits the rim into colour fringes the
         * way glass disperses light, and adds a broad sheen and a glow under the finger. Output is premultiplied.
         */
        const val AGSL = """
uniform float2 size;
uniform float radius;
uniform float bevel;
uniform float2 light;
uniform float2 touch;
uniform float press;
uniform float time;
uniform float4 tint;

float sdBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + float2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - r;
}

half4 main(float2 fc) {
    float2 hs = size * 0.5;
    float2 p = fc - hs;
    float d = sdBox(p, hs, radius);
    float cover = 1.0 - smoothstep(-1.0, 0.6, d);
    if (cover <= 0.0) { return half4(0.0); }

    float depth = -d;
    float e = 1.0;
    float2 g = float2(
        sdBox(p + float2(e, 0.0), hs, radius) - sdBox(p - float2(e, 0.0), hs, radius),
        sdBox(p + float2(0.0, e), hs, radius) - sdBox(p - float2(0.0, e), hs, radius));
    float gl = length(g);
    float2 n = gl > 0.0001 ? g / gl : float2(0.0, -1.0);
    float2 L = normalize(light);

    // Bevel profile: 1 on the very edge, 0 once we are bevel pixels inside.
    float t = clamp(1.0 - depth / bevel, 0.0, 1.0);
    float curve = t * t;
    float facing = dot(n, L);

    float spec = curve * pow(clamp(facing, 0.0, 1.0), 1.5);
    float bounce = curve * pow(clamp(-facing, 0.0, 1.0), 2.0) * 0.32;
    float hair = (1.0 - smoothstep(0.0, 1.7, depth)) * (0.30 + 0.70 * clamp(facing, 0.0, 1.0));

    // Dispersion: red leans toward the light, blue away from it, green sits between.
    float disp = curve * 0.22;
    float3 fringe = float3(
        disp * clamp(facing, 0.0, 1.0),
        disp * 0.45 * (1.0 - abs(facing)),
        disp * clamp(-facing, 0.0, 1.0));

    // Body: the tint, a touch deeper toward the bottom, with a soft inner glow at the edge.
    float vert = p.y / hs.y;
    float body = tint.a * (0.92 + 0.10 * vert);

    // Broad sheen that slides with the light, and a slow shimmer.
    float side = dot(p / hs, L);
    float sheen = 0.075 * smoothstep(0.05, 1.0, side) + 0.015 * sin(time * 1.3 + side * 3.0);

    // Glow under the finger.
    float sigma = min(size.x, size.y) * 0.55;
    float2 dt = fc - touch;
    float glow = press * 0.24 * exp(-dot(dt, dt) / (2.0 * sigma * sigma));

    float lit = spec * 0.95 + bounce + hair * 0.55 + curve * 0.07 + sheen + glow;

    float3 rgb = tint.rgb * body + float3(lit) + fringe;
    float a = clamp(body + lit + max(fringe.r, max(fringe.g, fringe.b)), 0.0, 1.0);
    rgb = min(rgb, float3(a));
    return half4(half3(rgb * cover), half(a * cover));
}
"""
    }
}

/** The card: a vertical layout that paints glass behind its children and follows the finger without taking touches. */
internal class GlassLayout(
    context: Context,
    private val glass: GlassState,
    private val radius: Float,
    private val tint: Int,
    private val tintAlpha: Float,
) : LinearLayout(context) {
    private val painter = GlassPainter(context.resources.displayMetrics.density)
    private var touchX = 0f
    private var touchY = 0f
    private var press = 0f
    private var pressGoal = 0f

    init {
        setWillNotDraw(false)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                touchX = ev.x
                touchY = ev.y
                pressGoal = 1f
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> pressGoal = 0f
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDraw(canvas: Canvas) {
        press += (pressGoal - press) * 0.18f
        painter.draw(canvas, width.toFloat(), height.toFloat(), radius, tint, tintAlpha, glass, touchX, touchY, press)
        super.onDraw(canvas)
    }
}

/** A text view on a glass pill or disc; the finger glow follows the touch inside it. */
internal class GlassText(
    context: Context,
    private val glass: GlassState,
    private val radius: Float,
    private val tint: Int,
    private val tintAlpha: Float,
) : TextView(context) {
    private val painter = GlassPainter(context.resources.displayMetrics.density)
    private var touchX = 0f
    private var touchY = 0f
    private var press = 0f
    private var pressGoal = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                touchX = event.x
                touchY = event.y
                pressGoal = 1f
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> pressGoal = 0f
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        press += (pressGoal - press) * 0.22f
        painter.draw(canvas, width.toFloat(), height.toFloat(), radius, tint, tintAlpha, glass, touchX, touchY, press)
        super.onDraw(canvas)
    }
}
