package com.bellatrixos.widgets

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.bellatrixos.App
import com.bellatrixos.R
import com.bellatrixos.Runtime
import com.bellatrixos.ui.home.NativeGrid
import com.bellatrixos.ui.home.OwlHomeTiles
import com.bellatrixos.ui.home.OwlSurface
import com.bellatrixos.widgets.render.Backdrop
import com.bellatrixos.widgets.render.LocalBackdrop
import kotlin.math.floor
import kotlin.math.max

/**
 * Attaches the declarative OWL surfaces to the Bellatrix views MainActivity actually uses.
 *
 * Nothing is replaced or hidden: the home surface is inserted just above the native desktop grid and
 * below the app windows, start menu and taskbar; the taskbar surface is the last child of the native
 * taskbar. The home surface also draws `widget`/`face` files as tiles on the real native grid, the way an
 * Oma-style launcher puts declarative widgets on home. The ComposeViews are transparent outside those
 * tiles, so Bellatrix's icons, search and start button keep working.
 */
object OwlNativeSurfaces {
    fun attach(activity: AppCompatActivity) {
        val parent = activity.findViewById<ViewGroup>(R.id.window_main)
        val desktop = activity.findViewById<View>(R.id.desktop_icons)
        val taskBar = activity.findViewById<ViewGroup>(R.id.taskBar)
        if (parent !is ConstraintLayout || desktop == null || taskBar == null) return
        val runtime = (activity.application as? App)?.runtime ?: return

        val grid = mutableStateOf(measureGrid(desktop))
        val gridListener =
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    grid.value = measureGrid(desktop)
                }
            }
        desktop.viewTreeObserver.addOnGlobalLayoutListener(gridListener)

        val home =
            ComposeView(activity).apply {
                id = View.generateViewId()
                layoutParams =
                    ConstraintLayout.LayoutParams(0, 0).apply {
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        bottomToTop = R.id.taskBar
                    }
                setOwlContent(activity, runtime, WidgetEngine.HOME_BAR) { grid.value }
                addOnAttachStateChangeListener(
                    object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: View) = Unit

                        override fun onViewDetachedFromWindow(v: View) {
                            desktop.viewTreeObserver.removeOnGlobalLayoutListener(gridListener)
                        }
                    },
                )
            }
        parent.addView(home, (parent.indexOfChild(desktop) + 1).coerceAtMost(parent.childCount))

        val bar =
            ComposeView(activity).apply {
                id = View.generateViewId()
                setOwlContent(activity, runtime, WidgetEngine.TASKBAR_BAR)
            }
        taskBar.addView(bar, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    /** The columns and rows GridAutofillLayoutManager gives the native desktop, in pixels. */
    private fun measureGrid(desktop: View): NativeGrid {
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
    ) {
        setViewTreeLifecycleOwner(activity)
        setViewTreeSavedStateRegistryOwner(activity)
        isFocusable = false
        isClickable = false
        setContent {
            // Frosted OWL nodes blur the same wallpaper the Compose launcher would blur.
            val backdrop = remember(runtime) { Backdrop({ runtime.themes.background }, { 0f }) }
            CompositionLocalProvider(LocalBackdrop provides backdrop) {
                OwlSurface(runtime, name, Modifier.fillMaxSize())
                if (name == WidgetEngine.HOME_BAR && grid != null) OwlHomeTiles(runtime, grid())
            }
        }
    }
}
