package com.bellatrixos.widgets

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.bellatrixos.App
import com.bellatrixos.R
import com.bellatrixos.Runtime
import com.bellatrixos.ui.home.OwlSurface
import com.bellatrixos.widgets.render.Backdrop
import com.bellatrixos.widgets.render.LocalBackdrop

/**
 * Attaches the two declarative OWL surfaces to the Bellatrix views MainActivity actually uses.
 *
 * Nothing is replaced or hidden: the home surface is inserted just above the native desktop grid and
 * below the app windows, start menu and taskbar; the taskbar surface is the last child of the native
 * taskbar. The ComposeViews are transparent and don't take focus or clicks for themselves, so Bellatrix's
 * icons, search and start button keep working. An OWL file with an opaque background or positioned nodes
 * draws on top of those native views; an empty/transparent file leaves the shipped Bellatrix look alone.
 */
object OwlNativeSurfaces {
    fun attach(activity: AppCompatActivity) {
        val parent = activity.findViewById<ViewGroup>(R.id.window_main)
        val desktop = activity.findViewById<View>(R.id.desktop_icons)
        val taskBar = activity.findViewById<ViewGroup>(R.id.taskBar)
        if (parent !is ConstraintLayout || desktop == null || taskBar == null) return
        val runtime = (activity.application as? App)?.runtime ?: return

        val home = ComposeView(activity).apply {
            id = View.generateViewId()
            layoutParams =
                ConstraintLayout.LayoutParams(0, 0).apply {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToTop = R.id.taskBar
                }
            setOwlContent(activity, runtime, WidgetEngine.HOME_BAR)
        }
        parent.addView(home, (parent.indexOfChild(desktop) + 1).coerceAtMost(parent.childCount))

        val bar = ComposeView(activity).apply {
            id = View.generateViewId()
            setOwlContent(activity, runtime, WidgetEngine.TASKBAR_BAR)
        }
        taskBar.addView(bar, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun ComposeView.setOwlContent(
        activity: AppCompatActivity,
        runtime: Runtime,
        name: String,
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
            }
        }
    }
}
