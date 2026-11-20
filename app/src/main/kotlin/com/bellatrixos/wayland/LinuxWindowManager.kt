package com.bellatrixos.wayland

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs

/**
 * Window state for Linux toplevels, ported from Bellatrix's ui.WindowManager.
 * Every toplevel is shown as its own Android task (LinuxWindowActivity), which
 * draws it with [WindowRenderer]; home-screen terminal tiles use [claim] to take
 * a toplevel instead. Lives on the main thread; protocol callbacks are posted over.
 */
class LinuxWindowManager : WindowManagerHooks, WindowViewManager {
    override lateinit var server: WaylandServer
    private val main = Handler(Looper.getMainLooper())

    @Volatile
    override var focused: XdgToplevel? = null
        private set

    /** Toplevels that have (or are getting) their own Android task. */
    override val appWindows = CopyOnWriteArrayList<XdgToplevel>()

    /** Each window's size in pixels, as its activity lays it out. */
    private val sizes = HashMap<XdgToplevel, Pair<Int, Int>>()

    /** Starts the activity for an app window; set by the runtime. */
    @Volatile
    var openAppWindow: (handle: String) -> Unit = {}

    /** Takes a new toplevel for a home-screen tile instead of its own task. */
    @Volatile
    var claim: (XdgToplevel) -> Boolean = { false }

    /** A claimed toplevel went away. */
    @Volatile
    var release: (XdgToplevel) -> Unit = {}

    /** A toplevel showed up, before it's claimed or opened. */
    @Volatile
    var created: (XdgToplevel) -> Unit = {}

    /** A window got the keyboard (again); used to catch terminals up on text size. */
    @Volatile
    var focusGained: (XdgToplevel) -> Unit = {}

    /** Notified when the app-window list changes, for task switchers. */
    @Volatile
    var onAppWindowsChanged: () -> Unit = {}

    private val embedded = HashSet<XdgToplevel>()

    /** Handles carry a per-process prefix so stale Recents cards never resolve to a new window. */
    private val handles = HashMap<String, XdgToplevel>()
    private val session = System.currentTimeMillis().toString(36)
    private var nextHandle = 1

    override fun windowByHandle(handle: String?) = handle?.let { synchronized(handles) { handles[it] } }

    fun handleOf(window: XdgToplevel) = synchronized(handles) { handles.entries.firstOrNull { it.value === window }?.key }

    /** Pixels per logical pixel (the wl_output scale). */
    val scale get() = server.output.scale

    override fun toplevelCreated(toplevel: XdgToplevel) {
        main.post {
            created(toplevel)
            if (claim(toplevel)) {
                embedded += toplevel
                return@post
            }
            appWindows += toplevel
            val handle = "$session-${nextHandle++}"
            synchronized(handles) { handles[handle] = toplevel }
            onAppWindowsChanged()
            openAppWindow(handle)
        }
    }

    override fun toplevelDestroyed(toplevel: XdgToplevel) {
        main.post {
            if (embedded.remove(toplevel)) release(toplevel)
            appWindows -= toplevel
            synchronized(handles) { handles.values.remove(toplevel) }
            synchronized(sizes) { sizes -= toplevel }
            onAppWindowsChanged()
            if (focused === toplevel) focus(null)
        }
    }

    /** Sizes an app window to its activity; called when that activity's view gets its size. */
    override fun applyAppLayout(
        window: XdgToplevel,
        width: Int,
        height: Int,
    ) {
        synchronized(sizes) { sizes[window] = width to height }
        synchronized(server) { configure(window) }
    }

    override fun close(window: XdgToplevel) = synchronized(server) { window.close() }

    override fun focus(toplevel: XdgToplevel?) {
        val previous = focused
        focused = toplevel
        synchronized(server) {
            server.input.setKeyboardFocus(toplevel?.surface)
            // Refresh the "activated" state on both windows.
            listOfNotNull(previous, toplevel).distinct().forEach { configure(it) }
        }
        toplevel?.let(focusGained)
    }

    /** Zooms the focused window's text with Ctrl+= or Ctrl+-, foot's own zoom keys. */
    override fun zoom(
        window: XdgToplevel,
        steps: Int,
    ) {
        if (window !== focused || steps == 0) return
        val code = if (steps > 0) Keys.KEY_EQUAL else Keys.KEY_MINUS
        server.postInput {
            key(Keys.KEY_LEFTCTRL, true)
            repeat(abs(steps)) {
                key(code, true)
                key(code, false)
            }
            key(Keys.KEY_LEFTCTRL, false)
        }
    }

    private fun configure(window: XdgToplevel) {
        val (w, h) = synchronized(sizes) { sizes[window] } ?: return
        window.configure(w / scale, h / scale, window === focused)
    }

    override fun pointerMove(hit: SurfaceHit?) = server.postInput { pointerMotion(hit?.surface, hit?.x ?: 0f, hit?.y ?: 0f) }

    override fun pointerButton(
        button: Int,
        pressed: Boolean,
    ) = server.postInput { pointerButton(button, pressed) }

    override fun click(
        hit: SurfaceHit,
        button: Int,
    ) = server.postInput {
        pointerMotion(hit.surface, hit.x, hit.y)
        pointerButton(button, true)
        pointerButton(button, false)
    }

    override fun scroll(
        dx: Float,
        dy: Float,
        source: Int,
        steps: Int,
    ) = server.postInput { pointerAxis(dx, dy, source, steps) }

    override fun wantsTouch(surface: Surface) = synchronized(server) { server.input.wantsTouch(surface) }

    override fun touchDown(
        hit: SurfaceHit,
        id: Int,
    ) = server.postInput { touchDown(hit.surface, id, hit.x, hit.y) }

    override fun touchMotion(
        id: Int,
        x: Float,
        y: Float,
    ) = server.postInput { touchMotion(id, x, y) }

    override fun touchUp(id: Int) = server.postInput { touchUp(id) }

    /** Whether [hit] is on the row where its client last said its text cursor is: a terminal's prompt line. */
    override fun onTextCursorRow(hit: SurfaceHit) =
        synchronized(server) {
            val (_, y, _, height) = server.input.textCursor(hit.surface) ?: return@synchronized false
            hit.y >= y && hit.y < y + height
        }

    override fun scrollStop() = server.postInput { pointerAxisStop() }

    /** Closes [window]'s popups, newest first. */
    private fun dismissPopups(window: XdgToplevel) =
        synchronized(server) {
            server.popups.filter { server.toplevelOf(it.surface) === window }.asReversed().forEach { it.dismiss() }
        }

    /** Focus handling for a press at [hit] in [window]; returns false if the press only dismissed popups. */
    override fun onPress(
        window: XdgToplevel,
        hit: SurfaceHit?,
    ): Boolean {
        if (window.renderer?.hasPopups == true && hit?.inPopup != true) {
            dismissPopups(window)
            return false
        }
        if (window !== focused) focus(window)
        return hit != null
    }
}
