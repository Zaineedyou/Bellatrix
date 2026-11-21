package com.bellatrixos.wayland

/**
 * The part of a Wayland window manager that Android views need.
 *
 * The launcher's single Runtime owns the window manager. Linux window views talk
 * to this interface so the same activity or floating tile can draw every window.
 */
interface WindowViewManager {
    val server: WaylandServer
    val focused: XdgToplevel?
    val appWindows: Collection<XdgToplevel>

    fun windowByHandle(handle: String?): XdgToplevel?

    fun applyAppLayout(
        window: XdgToplevel,
        width: Int,
        height: Int,
    )

    fun close(window: XdgToplevel)

    fun zoom(
        window: XdgToplevel,
        steps: Int,
    )

    fun focus(toplevel: XdgToplevel?)

    fun pointerMove(hit: SurfaceHit?)

    fun pointerButton(
        button: Int,
        pressed: Boolean,
    )

    fun click(
        hit: SurfaceHit,
        button: Int,
    )

    fun scroll(
        dx: Float,
        dy: Float,
        source: Int,
        steps: Int = 0,
    )

    fun wantsTouch(surface: Surface): Boolean

    fun touchDown(
        hit: SurfaceHit,
        id: Int,
    )

    fun touchMotion(
        id: Int,
        x: Float,
        y: Float,
    )

    fun touchUp(id: Int)

    fun onTextCursorRow(hit: SurfaceHit): Boolean

    fun scrollStop()

    fun onPress(
        window: XdgToplevel,
        hit: SurfaceHit?,
    ): Boolean
}
