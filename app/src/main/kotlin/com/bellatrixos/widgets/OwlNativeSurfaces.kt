package com.bellatrixos.widgets

import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.bellatrixos.App
import com.bellatrixos.R
import com.bellatrixos.Runtime
import com.bellatrixos.ui.Palette
import com.bellatrixos.ui.home.NativeGrid
import com.bellatrixos.ui.home.OwlHomeTiles
import com.bellatrixos.ui.home.OwlSurface
import com.bellatrixos.ui.home.PAGE_SWIPE_PX
import com.bellatrixos.ui.home.WidgetScreenPanel
import com.bellatrixos.widgets.render.Backdrop
import com.bellatrixos.widgets.render.LocalBackdrop
import com.bellatrixos.widgets.render.LocalHomeCovered
import com.bellatrixos.widgets.render.LocalInputFocus
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

/**
 * Attaches the declarative OWL surfaces to the Bellatrix views MainActivity actually uses.
 *
 * The home surface is inserted just above the native desktop grid and below the app windows, start menu
 * and taskbar; it draws `home.owl` and `widget`/`face` tiles on the real native grid. There is no second
 * taskbar: `taskbar.owl` is applied to the native taskbar views themselves by [NativeTaskbarSkin], so the
 * start button, app list and tray stay the real Bellatrix views and remain clickable. The home surface is
 * clipped to the native desktop bounds and transparent outside its tiles.
 */
object OwlNativeSurfaces {
    fun attach(activity: AppCompatActivity) {
        val parent = activity.findViewById<ViewGroup>(R.id.window_main)
        val desktop = activity.findViewById<ViewGroup>(R.id.desktop_icons)
        val taskBar = activity.findViewById<ViewGroup>(R.id.taskBar)
        if (parent !is ConstraintLayout || desktop == null || taskBar == null) return
        val runtime = (activity.application as? App)?.runtime ?: return

        // Animated tiles stop ticking while a native window, the start menu, the scrim or an owl
        // screen covers home. The sliding sheet moves by translation, which isn't a layout pass,
        // so this is sampled before each draw instead of in onGlobalLayout.
        val nativeCovered = mutableStateOf(false)
        fun refreshNativeCovered() {
            val scrim = parent.findViewById<View>(R.id.overlay)
            val windows = parent.findViewById<ViewGroup>(R.id.apps_window)
            val sheet = parent.findViewById<View>(R.id.startMenuBehavior)
            val sheetShown =
                sheet != null &&
                    runCatching { BottomSheetBehavior.from(sheet).state != BottomSheetBehavior.STATE_HIDDEN }.getOrDefault(false)
            val now =
                scrim?.visibility == View.VISIBLE || (windows?.childCount ?: 0) > 0 || sheetShown
            if (nativeCovered.value != now) nativeCovered.value = now
        }
        val coverListener =
            ViewTreeObserver.OnPreDrawListener {
                refreshNativeCovered()
                true
            }
        parent.viewTreeObserver.addOnPreDrawListener(coverListener)
        val covered: () -> Boolean = { runtime.widgets.openScreen != null || nativeCovered.value }

        val grid = mutableStateOf(measureGrid(desktop))
        val gridListener =
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    grid.value = measureGrid(desktop)
                }
            }
        desktop.viewTreeObserver.addOnGlobalLayoutListener(gridListener)

        // Sideways swipes on empty home change page. Read here, on the native desktop, and never consumed:
        // a pointer handler over the whole home ComposeView would swallow taps and long presses meant for the icons.
        (desktop as? RecyclerView)?.addOnItemTouchListener(
            object : RecyclerView.OnItemTouchListener {
                private var downX = 0f
                private var downY = 0f
                private var handled = true

                override fun onInterceptTouchEvent(
                    rv: RecyclerView,
                    e: MotionEvent,
                ): Boolean {
                    when (e.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            downX = e.x
                            downY = e.y
                            handled = false
                        }
                        MotionEvent.ACTION_MOVE ->
                            if (!handled) {
                                val dx = e.x - downX
                                val dy = e.y - downY
                                if (abs(dx) > abs(dy) && abs(dx) > PAGE_SWIPE_PX) {
                                    handled = true
                                    val ordered = runtime.widgets.pages
                                    val index = ordered.indexOf(runtime.widgets.activePage)
                                    val next = ordered.getOrNull(if (dx < 0) index + 1 else index - 1)
                                    if (next != null) {
                                        runtime.widgets.activePage = next
                                        rv.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    }
                                }
                            }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> handled = true
                    }
                    return false
                }

                override fun onTouchEvent(
                    rv: RecyclerView,
                    e: MotionEvent,
                ) = Unit

                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit
            },
        )

        val home =
            ComposeView(activity).apply {
                id = View.generateViewId()
                clipChildren = true
                clipToPadding = true
                layoutParams =
                    ConstraintLayout.LayoutParams(0, 0).apply {
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        bottomToTop = R.id.taskBar
                    }
                setOwlContent(activity, runtime, WidgetEngine.HOME_BAR, grid = { grid.value }, covered = covered)
                addOnAttachStateChangeListener(
                    object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: View) = Unit

                        override fun onViewDetachedFromWindow(v: View) {
                            desktop.viewTreeObserver.removeOnGlobalLayoutListener(gridListener)
                            parent.viewTreeObserver.removeOnPreDrawListener(coverListener)
                        }
                    },
                )
            }
        parent.addView(home, (parent.indexOfChild(desktop) + 1).coerceAtMost(parent.childCount))

        // A widget's screen { } belongs over the real launcher, above its windows, drawer and
        // taskbar. It is composed only while open; an empty ComposeView takes no taps or keys.
        ComposeView(activity).apply {
            id = View.generateViewId()
            layoutParams =
                ConstraintLayout.LayoutParams(0, 0).apply {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                }
            setViewTreeLifecycleOwner(activity)
            setViewTreeSavedStateRegistryOwner(activity)
            background = null
            isFocusable = false
            isFocusableInTouchMode = false
            setContent {
                runtime.widgets.screenInstance()?.let { (spec, instance) ->
                    CompositionLocalProvider(LocalInputFocus provides true) {
                        WidgetScreenPanel(spec, instance, onDismiss = { runtime.widgets.openScreen = null })
                    }
                }
            }
            parent.addView(this)
        }
    }

    /** The columns and rows GridAutofillLayoutManager gives the native desktop, in pixels. */
    private fun measureGrid(desktop: ViewGroup): NativeGrid {
        val iconWidth = desktop.resources.getDimension(R.dimen.explorer_file_width)
        val padLeft = desktop.paddingLeft
        val padTop = desktop.paddingTop
        val across = (desktop.width - padLeft - desktop.paddingRight).coerceAtLeast(1)
        val down = (desktop.height - padTop - desktop.paddingBottom).coerceAtLeast(1)
        val cols = max(1, floor(across / iconWidth).toInt())
        val cellW = across / cols.toFloat()
        // A real icon is the best measure of a row. Before the grid has children, use the icon's usual aspect.
        val rowHeight = desktop.getChildAt(0)?.height?.toFloat()?.takeIf { it > 0f } ?: (iconWidth * 1.25f)
        val rows = max(1, floor(down / rowHeight).toInt())
        val cellH = down / rows.toFloat()
        return NativeGrid(
            // attach() runs before the first layout pass on some devices (and on every relaunch),
            // so these can still be 0. setOwlContent() waits for a measured grid before composing tiles.
            width = desktop.width,
            height = desktop.height,
            cols = cols,
            rows = rows,
            cellW = cellW,
            cellH = cellH,
            gapX = (cellW - iconWidth).coerceAtLeast(0f),
            gapY = (cellH - rowHeight).coerceAtLeast(0f),
            padLeft = padLeft,
            padTop = padTop,
        )
    }

    private fun ComposeView.setOwlContent(
        activity: AppCompatActivity,
        runtime: Runtime,
        name: String,
        grid: (() -> NativeGrid)? = null,
        covered: () -> Boolean = { false },
    ) {
        setViewTreeLifecycleOwner(activity)
        setViewTreeSavedStateRegistryOwner(activity)
        background = null
        isFocusable = false
        isFocusableInTouchMode = false
        isClickable = false
        setContent {
            // Frosted OWL nodes blur the same wallpaper the Compose launcher would blur.
            val backdrop = remember(runtime) { Backdrop({ runtime.themes.background }, { 0f }) }
            if (name == WidgetEngine.HOME_BAR) {
                // The one home surface also watches the taskbar file; a save, a source or state change,
                // or a new theme restyles the real native bar.
                val files = runtime.widgets.specs.map { "${it.file.name}:${it.stamp}" }.joinToString("|")
                val bar = runtime.widgets.barInstance(WidgetEngine.TASKBAR_BAR)
                // -1 before the instance exists, then its revision: first appearance is a change too.
                val revision = bar?.revision?.intValue ?: -1
                val palette = Palette.current
                val theme = runtime.themes.current
                LaunchedEffect(files, revision, palette, theme) {
                    NativeTaskbarSkin.apply(activity, runtime)
                }
            }
            CompositionLocalProvider(LocalBackdrop provides backdrop, LocalHomeCovered provides covered) {
                OwlSurface(runtime, name, Modifier.fillMaxSize())
                if (name == WidgetEngine.HOME_BAR && grid != null) {
                    // The first composition can precede the desktop's first layout pass. Wait for a
                    // real size; grid() reads the state the layout listener updates, so it recomposes.
                    val measured = grid()
                    if (measured.width > 0 && measured.height > 0) OwlHomeTiles(runtime, measured)
                }
            }
        }
    }
}
