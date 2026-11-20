package com.bellatrixos.wayland

/**
 * The part of a Wayland window manager that Android views need.
 *
 * Bellatrix has two window managers (the legacy Linux one and the OWL launcher's),
 * with the same behavior. Linux window views talk to this interface so a window
 * opened by either runtime can be drawn by the same activity or floating tile.
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
