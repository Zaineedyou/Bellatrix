package com.bellatrixos.ui.nativeui

import android.app.Activity
import android.app.Application
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bellatrixos.R
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.roundToInt

/**
 * Applies look.toml to Bellatrix's existing views. It walks the real view
 * hierarchy, matches native ids and classes, and changes their layout and
 * drawables in place. No replacement activity, screen or renderer is involved.
 */
object NativeUiLook {
    private data class Watched(
        val activity: Activity?,
        val surface: String?,
        val listener: ViewTreeObserver.OnGlobalLayoutListener,
    )

    private val watched = Collections.synchronizedMap(WeakHashMap<View, Watched>())
    private var spec: NativeUiSpec.Spec = NativeUiSpec.Spec.EMPTY

    /** Called once by the Application; the one runtime owns the store and the applier. */
    fun install(
        app: Application,
        store: NativeUiStore,
    ) {
        spec = store.spec
        store.onExternalChange = { next ->
            spec = next
            refresh()
        }
        app.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?,
                ) {
                    activity.window.decorView.post { watch(activity, activity.window.decorView) }
                }

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityResumed(activity: Activity) = refresh()

                override fun onActivityPaused(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle,
                ) = Unit

                override fun onActivityDestroyed(activity: Activity) {
                    synchronized(watched) {
                        val dead = watched.filter { it.value.activity === activity }
                        for ((view, entry) in dead) {
                            runCatching { view.viewTreeObserver.removeOnGlobalLayoutListener(entry.listener) }
                            watched.remove(view)
                        }
                    }
                }
            },
        )
    }

    /** Watches a popup window's own root; the caller names its native surface. */
    fun popup(
        root: View,
        surface: String,
    ) {
        root.post { watch(null, root, surface) }
    }

    /** Reapplies after an agent edit, without waiting for the next layout pass. */
    fun refresh() {
        val entries =
            synchronized(watched) {
                watched.map { it.key to it.value }
            }
        for ((root, entry) in entries) apply(root, entry.surface ?: entry.activity?.let(::surfaceOf))
    }

    private fun watch(
        activity: Activity?,
        root: View,
        forced: String? = null,
    ) {
        val weakRoot = WeakReference(root)
        val listener =
            ViewTreeObserver.OnGlobalLayoutListener {
                weakRoot.get()?.let { live -> apply(live, forced ?: activity?.let(::surfaceOf)) }
            }
        synchronized(watched) {
            watched[root]?.let { runCatching { root.viewTreeObserver.removeOnGlobalLayoutListener(it.listener) } }
            watched[root] = Watched(activity, forced, listener)
        }
        root.viewTreeObserver.addOnGlobalLayoutListener(listener)
        apply(root, forced ?: activity?.let(::surfaceOf))
    }

    private fun surfaceOf(activity: Activity): String {
        val name = activity::class.java.simpleName
        return when {
            name == "MainActivity" -> "home"
            name.contains("Settings", true) -> "settings"
            name.contains("Window", true) -> "window"
            else -> "dialog"
        }
    }

    private fun apply(
        root: View,
        base: String?,
    ) {
        if (spec.rules.isEmpty()) return
        applyTree(root, base ?: "dialog")
    }

    private fun applyTree(
        view: View,
        inherited: String,
    ) {
        val surface = surfaceOf(view, inherited)
        applyTo(view, surface)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                view.getChildAt(i)?.let { applyTree(it, surface) }
            }
        }
    }

    private fun surfaceOf(
        view: View,
        inherited: String,
    ): String {
        val id = idName(view)
        val cls = view::class.java.simpleName
        return when {
            id == "taskBar" -> "taskbar"
            id in setOf("startMenuBg", "startMenuBehavior", "startMenuLayout", "startMenuApps", "startMenuSuggestedApps") -> "start-menu"
            id in setOf("viewWidgets", "stubDate", "deviceAudioLevel") -> "action-center"
            id in setOf("drawer", "panel_drawer") -> "drawer"
            id == "title_bar" -> "window"
            id in setOf("settings", "settings_content", "settings_nav_stub", "personalize") -> "settings"
            cls.contains("UISettingsCard", true) -> "settings"
            else -> inherited
        }
    }

    private fun idName(view: View): String? =
        view.takeIf { it.id != View.NO_ID && it.resources != null }?.let {
            runCatching { it.resources.getResourceEntryName(it.id) }.getOrNull()
        }

    private fun textOf(view: View): String? = (view as? TextView)?.text?.toString()

    private fun applyTo(
        view: View,
        surface: String,
    ) {
        val id = idName(view)
        val cls = view::class.java.name
        val text = textOf(view)
        val indices = spec.rules.indices.filter { spec.rules[it].matches(surface, id, cls, text) }
        if (indices.isEmpty()) return
        val signature = "${spec.hashCode()}:$surface:${indices.joinToString(",")}"
        if (signature == view.getTag(R.id.native_ui_signature)) return
        view.setTag(R.id.native_ui_signature, signature)

        val palette = spec.palette
        val props = linkedMapOf<String, Any>()
        for (i in indices) props.putAll(spec.rules[i].properties)

        val background = color(props["background"], palette)
        val border = color(props["border"], palette)
        val radius = dim(props["radius"], view)?.takeIf { it > 0 }
        val stroke = dim(props["border-width"], view) ?: 0
        if (background != null || border != null || radius != null) {
            val drawable = GradientDrawable()
            drawable.setColor(background ?: 0)
            if (border != null && stroke > 0) drawable.setStroke(stroke, border.toInt())
            if (radius != null) drawable.cornerRadius = radius.toFloat()
            view.background = drawable
            if (radius != null && props["clip"] as? Boolean != false) {
                view.outlineProvider = ViewOutlineProvider.BACKGROUND
                view.clipToOutline = true
            }
        }

        props["alpha"]?.asFloat()?.let { view.alpha = it.coerceIn(0f, 1f) }
        props["visibility"]?.asText()?.let { value ->
            view.visibility =
                when (value) {
                    "invisible" -> View.INVISIBLE
                    "gone" -> View.GONE
                    else -> View.VISIBLE
                }
        }
        props["elevation"]?.let { dim(it, view)?.let { px -> view.elevation = px.toFloat() } }
        props["translation-x"]?.let { dim(it, view)?.let { px -> view.translationX = px.toFloat() } }
        props["translation-y"]?.let { dim(it, view)?.let { px -> view.translationY = px.toFloat() } }
        props["rotation"]?.asFloat()?.let(view::setRotation)
        props["scale-x"]?.asFloat()?.let(view::setScaleX)
        props["scale-y"]?.asFloat()?.let(view::setScaleY)
        props["clickable"]?.asBool()?.let(view::setClickable)
        props["focusable"]?.asBool()?.let(view::setFocusable)
        props["clip"]?.asBool()?.let { view.clipToOutline = it }

        props["padding"]?.let { value ->
            val parts = NativeUiSpec.dims(value)
            val px = parts.map { it?.let { d -> dimOf(d, view, false) } ?: 0 }
            val (l, t, r, b) =
                when (px.size) {
                    1 -> listOf(px[0], px[0], px[0], px[0])
                    2 -> listOf(px[1], px[0], px[1], px[0])
                    else -> listOf(px[0], px[1], px.getOrNull(2) ?: px[0], px.getOrNull(3) ?: px[1])
                }
            view.setPadding(l, t, r, b)
        }

        val lp = view.layoutParams
        if (lp != null) {
            props["width"]?.let { dim(it, view, axis = true)?.let { lp.width = it } }
            props["height"]?.let { dim(it, view, axis = false)?.let { lp.height = it } }
            props["margin"]?.let { value ->
                if (lp is ViewGroup.MarginLayoutParams) {
                    val px = NativeUiSpec.dims(value).map { it?.let { d -> dimOf(d, view, false) } ?: 0 }
                    val (l, t, r, b) =
                        when (px.size) {
                            1 -> listOf(px[0], px[0], px[0], px[0])
                            2 -> listOf(px[1], px[0], px[1], px[0])
                            else -> listOf(px[0], px[1], px.getOrNull(2) ?: px[0], px.getOrNull(3) ?: px[1])
                        }
                    lp.setMargins(l, t, r, b)
                }
            }
            props["layout-gravity"]?.asText()?.let { value ->
                when (lp) {
                    is LinearLayout.LayoutParams -> lp.gravity = gravity(value)
                    is FrameLayout.LayoutParams -> lp.gravity = gravity(value)
                }
            }
            if (lp is LinearLayout.LayoutParams) {
                props["weight"]?.asFloat()?.let { lp.weight = it }
            }
            view.layoutParams = lp
        }

        props["min-width"]?.let { dim(it, view, axis = true)?.let(view::setMinimumWidth) }
        props["min-height"]?.let { dim(it, view, axis = false)?.let(view::setMinimumHeight) }

        if (view is TextView) {
            props["text-color"]?.let { color(it, palette)?.let { argb -> view.setTextColor(argb.toInt()) } }
            props["hint-color"]?.let { color(it, palette)?.let { argb -> view.setHintTextColor(argb.toInt()) } }
            props["text-size"]?.let { value ->
                NativeUiSpec.Dim.of(value)?.let { d ->
                    val px = if (d.kind == NativeUiSpec.Dim.Kind.SP || value !is String) d.value * view.resources.displayMetrics.scaledDensity else d.value * view.resources.displayMetrics.density
                    view.setTextSize(TypedValue.COMPLEX_UNIT_PX, px)
                }
            }
            props["text-style"]?.asText()?.let { value ->
                val style =
                    when {
                        "bold" in value && "italic" in value -> Typeface.BOLD_ITALIC
                        "bold" in value -> Typeface.BOLD
                        "italic" in value -> Typeface.ITALIC
                        else -> Typeface.NORMAL
                    }
                view.setTypeface(view.typeface, style)
            }
            props["text-align"]?.asText()?.let { view.gravity = gravity(it) }
        }

        if (view is ImageView) {
            props["icon-size"]?.let { value ->
                NativeUiSpec.Dim.of(value)?.let { d ->
                    val px = dimOf(d, view, false)
                    val params = view.layoutParams ?: ViewGroup.LayoutParams(px, px)
                    params.width = px
                    params.height = px
                    view.layoutParams = params
                }
            }
            val tint = props["icon-color"] ?: props["icon-tint"]
            val tintColor = tint?.let { color(it, palette) }
            if (tint != null && (tintColor == null || tintColor == 0L)) {
                view.clearColorFilter()
            } else {
                tintColor?.let { view.setColorFilter(it.toInt(), PorterDuff.Mode.SRC_IN) }
            }
        }
    }

    private fun color(
        value: Any?,
        palette: Map<String, String>,
    ) = NativeUiSpec.color(value, palette)

    private fun dim(
        value: Any?,
        view: View,
        spDefault: Boolean = false,
        axis: Boolean = false,
    ): Int? =
        NativeUiSpec.Dim.of(value)?.let { dimOf(it, view, spDefault, axis) }

    private fun dimOf(
        dim: NativeUiSpec.Dim,
        view: View,
        spDefault: Boolean = false,
        axis: Boolean = false,
    ): Int =
        when (dim.kind) {
            NativeUiSpec.Dim.Kind.MATCH -> ViewGroup.LayoutParams.MATCH_PARENT
            NativeUiSpec.Dim.Kind.WRAP -> ViewGroup.LayoutParams.WRAP_CONTENT
            NativeUiSpec.Dim.Kind.PX -> dim.value.roundToInt()
            NativeUiSpec.Dim.Kind.SP -> (dim.value * view.resources.displayMetrics.scaledDensity).roundToInt()
            NativeUiSpec.Dim.Kind.DP ->
                if (spDefault) (dim.value * view.resources.displayMetrics.scaledDensity).roundToInt() else (dim.value * view.resources.displayMetrics.density).roundToInt()
            NativeUiSpec.Dim.Kind.PERCENT -> {
                val parent = view.parent as? View
                val size =
                    if (axis) parent?.measuredWidth ?: view.resources.displayMetrics.widthPixels
                    else parent?.measuredHeight ?: view.resources.displayMetrics.heightPixels
                (size * dim.value / 100f).roundToInt()
            }
        }

    private fun gravity(value: String): Int {
        var g =
            when {
                "center" in value -> Gravity.CENTER
                "start" in value || "left" in value -> Gravity.START
                "end" in value || "right" in value -> Gravity.END
                else -> Gravity.NO_GRAVITY
            }
        if ("top" in value) g = g or Gravity.TOP
        if ("bottom" in value) g = g or Gravity.BOTTOM
        if ("vertical" in value) g = g or Gravity.CENTER_VERTICAL
        if ("horizontal" in value) g = g or Gravity.CENTER_HORIZONTAL
        return g
    }

    private fun Any.asFloat(): Float? = (this as? Number)?.toFloat() ?: (this as? String)?.toFloatOrNull()

    private fun Any.asBool(): Boolean? = this as? Boolean

    private fun Any.asText(): String? = (this as? String)?.trim()?.lowercase()
}
