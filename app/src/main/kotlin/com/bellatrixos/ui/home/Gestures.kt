package com.bellatrixos.ui.home

import android.content.Context
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.bellatrixos.R
import com.bellatrixos.Runtime
import com.bellatrixos.storage.LauncherData
import com.bellatrixos.theme.Style
import com.bellatrixos.theme.StyleStore
import com.bellatrixos.ui.LauncherStyle
import com.bellatrixos.widgets.WidgetEngine

/**
 * What swiping up and swiping down on home open, kept in style.toml so agents can set them too: the file name of a
 * panel (`panel "…" { }`; swiping up opens drawer.owl, the app drawer, by default), or "none".
 */
class Gestures(
    /** The app's, for home's error banner and settings, which say what a gesture opens. */
    internal val context: Context,
    private val style: StyleStore,
) {
    /** drawer.owl by default: the app drawer, like stock launchers, written in owl. Saved as it's set. */
    var swipeUp: String
        get() = LauncherStyle.style[Style.SWIPE_UP]
        set(choice) = style.update { it.with(Style.SWIPE_UP, choice) }

    /** Nothing by default. Saved as it's set. */
    var swipeDown: String
        get() = LauncherStyle.style[Style.SWIPE_DOWN]
        set(choice) = style.update { it.with(Style.SWIPE_DOWN, choice) }

    init {
        // Launchers before style.toml kept swipe up in preferences; moved over once, and left there for a downgrade.
        val prefs = LauncherData.prefs(context)
        if (!prefs.getBoolean(MOVED, false)) {
            if (prefs.getString(OLD_SWIPE_UP, null) == NONE) swipeUp = NONE
            prefs.edit().putBoolean(MOVED, true).apply()
        }
    }

    companion object {
        const val NONE = "none"
        private const val OLD_SWIPE_UP = "gesture_swipe_up"
        private const val MOVED = "gestures_in_style"
    }
}

/** The panel [choice] names, if it's one that runs; null for nothing, and a panel that's gone or broken. */
internal fun Runtime.panelOf(choice: String) = choice.takeIf { it != Gestures.NONE }?.takeIf { widgets.panelInstance(it) != null }

/** What [choice] is called in settings: nothing, or the panel's name (its file's, while it has none). */
internal fun Runtime.gestureLabel(choice: String) =
    if (choice == Gestures.NONE) gestures.context.getString(R.string.gestures_nothing) else widgets.panels().firstOrNull { it.file.name == choice }?.name ?: choice.removeSuffix(".owl")

/**
 * Picked panels that don't compile, drawer.owl and keybar.owl, for home's error banner: without a tile, nothing else would say so,
 * and the drawer quietly shows another until they're fixed.
 */
internal fun Runtime.gestureProblems(): List<Pair<String, List<String>>> {
    val context = gestures.context
    val drawer =
        widgets.drawerPanel()?.let { if (it == WidgetEngine.STOCK_DRAWER) context.getString(R.string.gestures_stock_drawer) else gestureLabel(it) }
            ?: context.getString(R.string.gestures_nothing)
    val drawerShows = context.getString(R.string.gestures_drawer_shows, drawer)
    val meanwhile =
        linkedMapOf(gestures.swipeUp to drawerShows, WidgetEngine.DRAWER to drawerShows) +
            (gestures.swipeDown to context.getString(R.string.gestures_swipe_down_nothing))
    val panels =
        meanwhile.mapNotNull { (choice, what) ->
            val spec = widgets.panels().firstOrNull { it.file.name == choice && it.error != null } ?: return@mapNotNull null
            spec.file.name to listOf(context.getString(R.string.gestures_until_fixed, what, spec.error!!.lineSequence().first()))
        }
    // keybar.owl too: the stock keys stand in for it as quietly.
    val keys = widgets.specs.firstOrNull { it.file.name == WidgetEngine.KEYBAR && it.error != null }?.let { it.file.name to listOf(context.getString(R.string.gestures_keybar_until_fixed, it.error!!.lineSequence().first())) }
    return panels + listOfNotNull(keys)
}

/**
 * Dragging up opens the drawer under the finger and dragging down closes it, as on stock launchers: the panel
 * follows the drag and a fling or how far it got decides where it settles. Scrollables inside (a widget's
 * list, the recent apps strip) take their own direction first.
 */
@Composable
internal fun Modifier.drawerSwipe(
    state: LauncherState,
    enabled: Boolean = true,
): Modifier {
    val screenHeight = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    return draggable(
        orientation = Orientation.Vertical,
        enabled = enabled,
        state = rememberDraggableState { dy -> state.dragBy(-dy / screenHeight) },
        onDragStopped = { velocity -> state.settle(velocity) },
    )
}

/** What a drag on home turned out to be, decided by where the drawer is and which way it first moves. */
private enum class HomeDrag { Undecided, Drawer, Pull, Ignored }

/** The drag under way: not state, nothing draws from it. */
private class DragTrack {
    var kind = HomeDrag.Undecided
}

/**
 * Swiping on home (the pages, the recent apps strip, the bar) does what's picked in gesture settings: up for the
 * drawer or a panel in its place, down for a panel from the top. With the drawer open, the swipe drags it whatever
 * the setting, so it can still be pulled closed.
 */
@Composable
internal fun Modifier.homeSwipe(
    runtime: Runtime,
    state: LauncherState,
    enabled: Boolean = true,
): Modifier {
    val gestures = runtime.gestures
    val drawerShown by remember(state) { derivedStateOf { state.drawer.value > 0f } }
    // Whether the drawer shows is only read when it matters: read here, it would recompose all of home (its Box is
    // inline) each time the drawer opens or closes.
    val drags = gestures.swipeUp != Gestures.NONE || gestures.swipeDown != Gestures.NONE || drawerShown
    val screenHeight = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val drag = remember { DragTrack() }
    // One drag handler, only turned on or off: a new one mid-swipe (as the drawer starts to show) would drop the drag.
    return draggable(
        orientation = Orientation.Vertical,
        enabled = enabled && drags,
        state =
            rememberDraggableState { dy ->
                if (drag.kind == HomeDrag.Undecided && dy != 0f) {
                    drag.kind =
                        when {
                            state.drawer.value > 0f -> HomeDrag.Drawer
                            dy < 0f && gestures.swipeUp != Gestures.NONE -> HomeDrag.Drawer
                            // Picked as the drag starts: it comes down from out of sight above with the finger.
                            dy > 0f -> runtime.panelOf(gestures.swipeDown)?.let { name -> state.beginPull(name); HomeDrag.Pull } ?: HomeDrag.Ignored
                            else -> HomeDrag.Ignored
                        }
                }
                when (drag.kind) {
                    HomeDrag.Drawer -> state.dragBy(-dy / screenHeight)
                    HomeDrag.Pull -> state.pullBy(dy / screenHeight)
                    else -> {}
                }
            },
        onDragStarted = {
            drag.kind = HomeDrag.Undecided
            state.startPull()
        },
        onDragStopped = { velocity ->
            when (drag.kind) {
                HomeDrag.Drawer -> state.settle(velocity)
                HomeDrag.Pull -> state.settlePull(velocity)
                else -> {}
            }
            drag.kind = HomeDrag.Undecided
        },
    )
}

/**
 * Dragging the open swipe-down panel moves it under the finger; it settles open or closed like the drawer. A list in
 * it scrolls first: pushing up past its end moves the panel on, as pulling down past the drawer's top closes it.
 */
@Composable
internal fun Modifier.pullSwipe(state: LauncherState): Modifier {
    val screenHeight = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val pushToClose =
        remember(state, screenHeight) {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    // Partly pushed up and dragged back down: it comes back before the list scrolls.
                    if (source != NestedScrollSource.UserInput || available.y <= 0f || state.pull.value >= 1f) return Offset.Zero
                    state.pullBy(available.y / screenHeight)
                    return Offset(0f, available.y)
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source != NestedScrollSource.UserInput || available.y >= 0f) return Offset.Zero
                    if (state.pull.value >= 1f) state.startPull()
                    state.pullBy(available.y / screenHeight)
                    return Offset(0f, available.y)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (state.pull.value >= 1f) return Velocity.Zero
                    state.settlePull(available.y)
                    return available
                }
            }
        }
    return nestedScroll(pushToClose).draggable(
        orientation = Orientation.Vertical,
        state = rememberDraggableState { dy -> state.pullBy(dy / screenHeight) },
        onDragStarted = { state.startPull() },
        onDragStopped = { velocity -> state.settlePull(velocity) },
    )
}

