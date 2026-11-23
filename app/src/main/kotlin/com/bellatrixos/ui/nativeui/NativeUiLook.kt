package com.bellatrixos.ui.nativeui

import android.app.Activity
import android.app.Application
import android.content.res.ColorStateList
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bellatrixos.R
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.taskbar.TaskBarStore
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.roundToInt

/**
 * Applies look.toml and ui.lua to Bellatrix's existing views. It walks the real
 * view hierarchy, matches native ids and classes, and changes their layout and
 * drawables in place. No replacement activity, screen or renderer is involved.
 */
object NativeUiLook {
    private data class Watched(
        val activity: Activity?,
        val surface: String?,
        val listener: ViewTreeObserver.OnGlobalLayoutListener,
    )

    private data class Original(
        val background: Drawable?,
        val backgroundTint: ColorStateList?,
        val alpha: Float,
        val visibility: Int,
        val elevation: Float,
        val translationX: Float,
        val translationY: Float,
        val rotation: Float,
        val scaleX: Float,
        val scaleY: Float,
        val clickable: Boolean,
        val focusable: Boolean,
        val clipToOutline: Boolean,
        val outlineProvider: ViewOutlineProvider?,
        val padding: IntArray,
        val width: Int,
        val height: Int,
        val margins: IntArray?,
        val gravity: Int?,
        val weight: Float?,
        val minWidth: Int,
        val minHeight: Int,
        val text: String?,
        val hint: String?,
        val textColor: ColorStateList?,
        val hintColor: ColorStateList?,
        val textSize: Float,
        val typeface: Typeface?,
        val viewGravity: Int,
        val colorFilter: ColorFilter?,
        val imageTint: ColorStateList?,
    )

    /** One touch listener per hooked native view; it observes gestures without consuming Bellatrix's own actions. */
    private class HookState {
        val events = HashSet<String>()
        var owner = WeakReference<View>(null)
        var surfaceName = ""
        var downX = 0f
        var downY = 0f
        var longFired = false
        var attached = false
        var previousTouch: View.OnTouchListener? = null
        val handler = Handler(Looper.getMainLooper())
        val longPress =
            Runnable {
                val view = owner.get()
                if (view != null && !longFired && "long-click" in events) {
                    longFired = true
                    NativeUiLook.invokeHooks("long-click", view, surfaceName, "long-click", downX, downY)
                }
            }
    }

    private data class Hook(
        val event: String,
        val selector: NativeUiSpec.Selector,
        val surface: String,
        val fn: LuaFunction,
    )

    private val watched = Collections.synchronizedMap(WeakHashMap<View, Watched>())
    private var spec: NativeUiSpec.Spec = NativeUiSpec.Spec.EMPTY
    private var scriptRules: List<NativeUiSpec.Rule> = emptyList()
    private val hooks = mutableListOf<Hook>()
    private val directOriginals = Collections.synchronizedMap(WeakHashMap<View, Original>())
    private val directAdapters = Collections.synchronizedMap(WeakHashMap<ArrayAdapter<String>, List<String>>())
    private val pendingWindowActions = mutableListOf<(MainActivityIn) -> Unit>()
    private var active: WeakReference<Activity>? = null

    /** Runtime Lua errors are handed to the store so they land in errors.txt. */
    var onScriptError: (String) -> Unit = {}

    private fun entries(): List<Pair<View, Watched>> =
        synchronized(watched) { watched.map { it.key to it.value } }

    /** Called once by the Application; the one runtime owns the store, the applier and the Lua API. */
    fun install(
        app: Application,
        store: NativeUiStore,
        taskbar: TaskBarStore,
    ) {
        spec = store.spec
        store.onExternalChange = ::setSpec
        store.attachApi(NativeUiApi(app, taskbar))
        app.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?,
                ) {
                    activity.window.decorView.post { watch(activity, activity.window.decorView) }
                }

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityResumed(activity: Activity) {
                    active = WeakReference(activity)
                    flushPendingWindowActions(activity)
                    refresh()
                }

                override fun onActivityPaused(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle,
                ) = Unit

                override fun onActivityDestroyed(activity: Activity) {
                    if (active?.get() === activity) active = null
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

    fun currentActivity(): Activity? = active?.get()

    /** Runs a window action now, or after Bellatrix home resumes; scripts can start before it does. */
    fun whenMainReady(action: (MainActivityIn) -> Unit) {
        val activity = currentActivity()
        if (activity is MainActivityIn) {
            action(activity)
        } else {
            synchronized(pendingWindowActions) { pendingWindowActions += action }
        }
    }

    private fun flushPendingWindowActions(activity: Activity) {
        if (activity !is MainActivityIn) return
        val pending =
            synchronized(pendingWindowActions) {
                val copy = pendingWindowActions.toList()
                pendingWindowActions.clear()
                copy
            }
        for (action in pending) {
            runCatching { action(activity) }
                .onFailure { error -> onScriptError("ui.lua window action: ${error.message ?: "failed"}") }
        }
    }

    /** Reapplies after an agent edit, without waiting for the next layout pass. */
    fun refresh() {
        for ((root, entry) in entries()) apply(root, entry.surface ?: entry.activity?.let(::surfaceOf))
    }

    /** Drops Lua-added rules and hooks so a deleted script restores Bellatrix's own look. */
    fun clearScript() {
        if (scriptRules.isEmpty() && hooks.isEmpty() && directOriginals.isEmpty() && directAdapters.isEmpty()) return
        scriptRules = emptyList()
        hooks.clear()
        synchronized(pendingWindowActions) { pendingWindowActions.clear() }
        restoreDirect()
        for ((view, _) in entries()) {
            (view.getTag(R.id.native_ui_hooks) as? HookState)?.let { state ->
                state.handler.removeCallbacks(state.longPress)
                state.events.clear()
                if (state.attached) {
                    view.setOnTouchListener(state.previousTouch)
                    state.attached = false
                    state.previousTouch = null
                }
            }
        }
        refresh()
    }

    /** Lua's direct view setters capture their original state here so deleting the script can undo them. */
    fun rememberDirect(view: View) {
        synchronized(directOriginals) {
            if (!directOriginals.containsKey(view)) directOriginals[view] = capture(view)
        }
    }

    /** Lua's adapter edits are captured here so deleting the script restores Bellatrix's own data. */
    fun rememberAdapter(adapter: ArrayAdapter<String>) {
        synchronized(directAdapters) {
            if (!directAdapters.containsKey(adapter)) {
                directAdapters[adapter] = (0 until adapter.count).map { adapter.getItem(it).toString() }
            }
        }
    }

    private fun restoreDirect() {
        val entries =
            synchronized(directOriginals) {
                val copy = directOriginals.toList()
                directOriginals.clear()
                copy
            }
        for ((view, original) in entries) restore(view, original)
        val adapters =
            synchronized(directAdapters) {
                val copy = directAdapters.toList()
                directAdapters.clear()
                copy
            }
        for ((adapter, items) in adapters) {
            adapter.clear()
            adapter.addAll(items)
            adapter.notifyDataSetChanged()
        }
    }

    fun addScriptRule(rule: NativeUiSpec.Rule) {
        scriptRules = scriptRules + rule
        refresh()
    }

    fun setHook(
        event: String,
        match: String,
        surface: String,
        fn: LuaFunction,
    ) {
        val selector =
            NativeUiSpec.selector(match)
                ?: throw IllegalArgumentException("match must start with id:, class: or text:")
        setHook(event, selector, surface, fn)
    }

    fun setHook(
        event: String,
        selector: NativeUiSpec.Selector,
        surface: String,
        fn: LuaFunction,
    ) {
        hooks += Hook(event.lowercase(), selector, surface, fn)
        refresh()
    }

    fun idOf(view: View): String? =
        view.takeIf { it.id != View.NO_ID && it.resources != null }?.let {
            runCatching { it.resources.getResourceEntryName(it.id) }.getOrNull()
        }

    /** Public surface lookup for Lua handles; walks parents when a child has no id of its own. */
    fun surfaceOfView(view: View): String {
        var current: View? = view
        while (current != null) {
            val found = surfaceOf(current, "")
            if (found.isNotEmpty()) return found
            current = current.parent as? View
        }
        return "dialog"
    }

    /** All matching native views, from the roots currently attached to this runtime. */
    fun findViews(
        selector: NativeUiSpec.Selector,
        surface: String,
    ): List<View> {
        val found = LinkedHashSet<View>()
        for ((root, entry) in entries()) {
            collect(root, entry.surface ?: entry.activity?.let(::surfaceOf) ?: "dialog", selector, surface, found)
        }
        return found.toList()
    }

    fun setSpec(next: NativeUiSpec.Spec) {
        spec = next
        refresh()
    }

    private fun collect(
        view: View,
        inherited: String,
        selector: NativeUiSpec.Selector,
        wanted: String,
        found: MutableSet<View>,
    ) {
        val surface = surfaceOf(view, inherited)
        if ((wanted == "all" || surface == wanted) && selector.matches(surface, idOf(view), view::class.java.name, textOf(view))) {
            found += view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                view.getChildAt(i)?.let { collect(it, surface, selector, wanted, found) }
            }
        }
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
    ) = applyTree(root, base ?: "dialog")

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
        val id = idOf(view)
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

    private fun textOf(view: View): String? = (view as? TextView)?.text?.toString()

    private fun applyTo(
        view: View,
        surface: String,
    ) {
        val id = idOf(view)
        val cls = view::class.java.name
        val text = textOf(view)
        val rules = spec.rules + scriptRules
        val indices = rules.indices.filter { rules[it].matches(surface, id, cls, text) }
        val matchedHooks = hooks.filter { (it.surface == "all" || it.surface == surface) && it.selector.matches(surface, id, cls, text) }
        val signature = "${rules.hashCode()}:$surface:${indices.joinToString(",")}:${hooks.indexOf(matchedHooks.firstOrNull())}:${matchedHooks.size}"
        val original = view.getTag(R.id.native_ui_original) as? Original

        if (indices.isEmpty() && matchedHooks.isEmpty()) {
            if (original != null) {
                restore(view, original)
                view.setTag(R.id.native_ui_original, null)
                view.setTag(R.id.native_ui_signature, null)
                view.setTag(R.id.native_ui_hooks, null)
            }
            return
        }
        if (signature == view.getTag(R.id.native_ui_signature)) return
        if (original == null) {
            view.setTag(R.id.native_ui_original, capture(view))
        } else {
            // A newer rule may omit a property the old one set; start from stock before applying it.
            restore(view, original)
        }
        view.setTag(R.id.native_ui_signature, signature)

        val palette = spec.palette
        val props = linkedMapOf<String, Any>()
        for (i in indices) props.putAll(rules[i].properties)

        val background = color(props["background"], palette)
        val border = color(props["border"], palette)
        val radius = dim(props["radius"], view)?.takeIf { it > 0 }
        val stroke = dim(props["border-width"], view) ?: 0
        if (background != null || border != null || radius != null) {
            val drawable = GradientDrawable()
            drawable.setColor(background?.toInt() ?: 0)
            if (border != null && stroke > 0) drawable.setStroke(stroke, border.toInt())
            if (radius != null) drawable.cornerRadius = radius.toFloat()
            view.background = drawable
            view.backgroundTintList = null
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
            val px = NativeUiSpec.dims(value).map { it?.let { d -> dimOf(d, view) } ?: 0 }
            val (l, t, r, b) = sides(px)
            view.setPadding(l, t, r, b)
        }

        val lp = view.layoutParams
        if (lp != null) {
            props["width"]?.let { dim(it, view, axis = true)?.let { lp.width = it } }
            props["height"]?.let { dim(it, view, axis = false)?.let { lp.height = it } }
            props["margin"]?.let { value ->
                if (lp is ViewGroup.MarginLayoutParams) {
                    val px = NativeUiSpec.dims(value).map { it?.let { d -> dimOf(d, view) } ?: 0 }
                    val (l, t, r, b) = sides(px)
                    lp.setMargins(l, t, r, b)
                }
            }
            props["layout-gravity"]?.asText()?.let { value ->
                when (lp) {
                    is LinearLayout.LayoutParams -> lp.gravity = gravity(value)
                    is FrameLayout.LayoutParams -> lp.gravity = gravity(value)
                }
            }
            if (lp is LinearLayout.LayoutParams) props["weight"]?.asFloat()?.let { lp.weight = it }
            view.layoutParams = lp
        }

        props["min-width"]?.let { dim(it, view, axis = true)?.let(view::setMinimumWidth) }
        props["min-height"]?.let { dim(it, view, axis = false)?.let(view::setMinimumHeight) }

        if (view is TextView) {
            props["text-color"]?.let { color(it, palette)?.let { argb -> view.setTextColor(argb.toInt()) } }
            props["hint-color"]?.let { color(it, palette)?.let { argb -> view.setHintTextColor(argb.toInt()) } }
            props["text-size"]?.let { value ->
                NativeUiSpec.Dim.of(value)?.let { d ->
                    val px =
                        if (d.kind == NativeUiSpec.Dim.Kind.SP || value !is String) {
                            d.value * view.resources.displayMetrics.scaledDensity
                        } else {
                            d.value * view.resources.displayMetrics.density
                        }
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
            val tint = props["icon-color"] ?: props["icon-tint"]
            val tintColor = tint?.let { color(it, palette) }
            if (tint != null && (tintColor == null || tintColor == 0L)) {
                view.clearColorFilter()
            } else {
                tintColor?.let {
                    view.imageTintList = null
                    view.setColorFilter(it.toInt(), PorterDuff.Mode.SRC_IN)
                }
            }
            props["icon-size"]?.let { value ->
                NativeUiSpec.Dim.of(value)?.let { d ->
                    val px = dimOf(d, view)
                    val params = view.layoutParams ?: ViewGroup.LayoutParams(px, px)
                    params.width = px
                    params.height = px
                    view.layoutParams = params
                }
            }
        }

        attachHooks(view, surface, matchedHooks.map { it.event }.toSet())
    }

    private fun sides(px: List<Int>): IntArray =
        when (px.size) {
            1 -> intArrayOf(px[0], px[0], px[0], px[0])
            2 -> intArrayOf(px[1], px[0], px[1], px[0])
            else -> intArrayOf(px[0], px[1], px.getOrNull(2) ?: px[0], px.getOrNull(3) ?: px[1])
        }

    private fun capture(view: View): Original {
        val lp = view.layoutParams
        return Original(
            background = view.background,
            backgroundTint = view.backgroundTintList,
            alpha = view.alpha,
            visibility = view.visibility,
            elevation = view.elevation,
            translationX = view.translationX,
            translationY = view.translationY,
            rotation = view.rotation,
            scaleX = view.scaleX,
            scaleY = view.scaleY,
            clickable = view.isClickable,
            focusable = view.isFocusable,
            clipToOutline = view.clipToOutline,
            outlineProvider = view.outlineProvider,
            padding = intArrayOf(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom),
            width = lp?.width ?: 0,
            height = lp?.height ?: 0,
            margins =
                (lp as? ViewGroup.MarginLayoutParams)?.let {
                    intArrayOf(it.leftMargin, it.topMargin, it.rightMargin, it.bottomMargin)
                },
            gravity =
                when (lp) {
                    is LinearLayout.LayoutParams -> lp.gravity
                    is FrameLayout.LayoutParams -> lp.gravity
                    else -> null
                },
            weight = (lp as? LinearLayout.LayoutParams)?.weight,
            minWidth = view.minimumWidth,
            minHeight = view.minimumHeight,
            text = (view as? TextView)?.text?.toString(),
            hint = (view as? TextView)?.hint?.toString(),
            textColor = (view as? TextView)?.textColors,
            hintColor = (view as? TextView)?.hintTextColors,
            textSize = (view as? TextView)?.textSize ?: 0f,
            typeface = (view as? TextView)?.typeface,
            viewGravity = (view as? TextView)?.gravity ?: Gravity.NO_GRAVITY,
            colorFilter = (view as? ImageView)?.colorFilter,
            imageTint = (view as? ImageView)?.imageTintList,
        )
    }

    private fun restore(
        view: View,
        original: Original,
    ) {
        view.background = original.background
        view.backgroundTintList = original.backgroundTint
        view.alpha = original.alpha
        view.visibility = original.visibility
        view.elevation = original.elevation
        view.translationX = original.translationX
        view.translationY = original.translationY
        view.rotation = original.rotation
        view.scaleX = original.scaleX
        view.scaleY = original.scaleY
        view.isClickable = original.clickable
        view.isFocusable = original.focusable
        view.clipToOutline = original.clipToOutline
        view.outlineProvider = original.outlineProvider
        view.setPadding(original.padding[0], original.padding[1], original.padding[2], original.padding[3])

        val lp = view.layoutParams
        if (lp != null) {
            lp.width = original.width
            lp.height = original.height
            if (lp is ViewGroup.MarginLayoutParams && original.margins != null) {
                lp.setMargins(original.margins[0], original.margins[1], original.margins[2], original.margins[3])
            }
            when {
                lp is LinearLayout.LayoutParams && original.gravity != null -> lp.gravity = original.gravity
                lp is FrameLayout.LayoutParams && original.gravity != null -> lp.gravity = original.gravity
            }
            if (lp is LinearLayout.LayoutParams && original.weight != null) lp.weight = original.weight
            view.layoutParams = lp
        }
        view.minimumWidth = original.minWidth
        view.minimumHeight = original.minHeight

        if (view is TextView) {
            if (original.text != null) view.text = original.text
            if (original.hint != null) view.hint = original.hint
            if (original.textColor != null) view.setTextColor(original.textColor)
            if (original.hintColor != null) view.setHintTextColor(original.hintColor)
            if (original.textSize > 0) view.setTextSize(TypedValue.COMPLEX_UNIT_PX, original.textSize)
            if (original.typeface != null) view.typeface = original.typeface
            view.gravity = original.viewGravity
        }
        if (view is ImageView) {
            view.colorFilter = original.colorFilter
            view.imageTintList = original.imageTint
        }
    }

    /**
     * Attaches one passive touch observer. Bellatrix's own touch, click and
     * long-click listeners keep running; the observer returns whatever the
     * native touch listener returned and never consumes a touch by itself.
     */
    private fun attachHooks(
        view: View,
        surface: String,
        events: Set<String>,
    ) {
        // Look-only rules must not install a touch listener on native views.
        if (events.isEmpty()) return
        val state =
            view.getTag(R.id.native_ui_hooks) as? HookState ?: HookState().also { view.setTag(R.id.native_ui_hooks, it) }
        state.owner = WeakReference(view)
        state.surfaceName = surface
        state.events.clear()
        state.events += events
        if (state.attached) return
        state.previousTouch = existingTouchListener(view)
        state.attached = true
        view.setOnTouchListener { v, event ->
            val consumed = state.previousTouch?.onTouch(v, event) ?: false
            val x = event.x
            val y = event.y
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    state.downX = x
                    state.downY = y
                    state.longFired = false
                    if ("long-click" in state.events) state.handler.postDelayed(state.longPress, 500L)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (kotlin.math.abs(x - state.downX) > 12 || kotlin.math.abs(y - state.downY) > 12) {
                        state.handler.removeCallbacks(state.longPress)
                        state.longFired = true
                    }
                }
                MotionEvent.ACTION_UP -> {
                    state.handler.removeCallbacks(state.longPress)
                    if (!state.longFired && "click" in state.events) {
                        invokeHooks("click", v, surface, "click", x, y)
                    }
                }
                MotionEvent.ACTION_CANCEL -> state.handler.removeCallbacks(state.longPress)
            }
            if ("gesture" in state.events) invokeHooks("gesture", v, surface, actionOf(event), x, y)
            consumed
        }
    }

    /** Best-effort access to a listener Bellatrix already put on the view; hidden API access may be blocked. */
    private fun existingTouchListener(view: View): View.OnTouchListener? =
        runCatching {
            val infoField = View::class.java.getDeclaredField("mListenerInfo")
            infoField.isAccessible = true
            val info = infoField.get(view) ?: return null
            val listenerField = info.javaClass.getDeclaredField("mOnTouchListener")
            listenerField.isAccessible = true
            listenerField.get(info) as? View.OnTouchListener
        }.getOrNull()

    private fun actionOf(event: MotionEvent): String =
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> "down"
            MotionEvent.ACTION_UP -> "up"
            MotionEvent.ACTION_MOVE -> "move"
            MotionEvent.ACTION_CANCEL -> "cancel"
            else -> "move"
        }

    private fun invokeHooks(
        event: String,
        view: View,
        surface: String,
        action: String,
        x: Float,
        y: Float,
    ) {
        val id = idOf(view)
        val cls = view::class.java.name
        val text = textOf(view)
        hooks
            .filter { it.event == event && (it.surface == "all" || it.surface == surface) && it.selector.matches(surface, id, cls, text) }
            .forEach { hook ->
                runCatching {
                    hook.fn.invoke(
                        arrayOf(
                            CoerceJavaToLua.coerce(LuaView(view, surface)),
                            LuaValue.valueOf(action),
                            LuaValue.valueOf(x.toDouble()),
                            LuaValue.valueOf(y.toDouble()),
                        ),
                    )
                }.onFailure { error ->
                    onScriptError("ui.lua $event hook: ${error.message ?: "failed"}")
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
    ): Int? = NativeUiSpec.Dim.of(value)?.let { dimOf(it, view, spDefault, axis) }

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
                if (spDefault) {
                    (dim.value * view.resources.displayMetrics.scaledDensity).roundToInt()
                } else {
                    (dim.value * view.resources.displayMetrics.density).roundToInt()
                }
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
