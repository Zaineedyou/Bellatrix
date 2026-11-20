package com.bellatrixos.wayland

import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Draws one Linux toplevel's surfaces straight into an Android Surface. The
 * view is always full screen; [visibleHeight] limits the part the window uses
 * so the keybar and soft keyboard sit below it without stretching old frames.
 */
class WindowSurfaceView(
    context: Context,
    private val runtime: com.bellatrixos.linux.LinuxRuntime,
    private val window: XdgToplevel,
    translucent: Boolean = false,
) : SurfaceView(context),
    SurfaceHolder.Callback {
    private val renderer = WindowRenderer(runtime.server, window, translucent)

    var background: Int by renderer::background

    private var surfaceWidth = 0
    private var surfaceHeight = 0

    /** How much of the view, from its top, the keyboard and keybar leave the window; 0 for all of it. */
    var visibleHeight = 0
        set(value) {
            if (value == field) return
            field = value
            layoutWindow()
        }

    private fun layoutWindow() {
        if (surfaceWidth == 0) return
        val h = if (visibleHeight in 1..surfaceHeight) visibleHeight else surfaceHeight
        renderer.visibleHeight = h
        runtime.wm.applyAppLayout(window, surfaceWidth, h)
    }

    init {
        holder.addCallback(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        window.renderer = renderer
    }

    override fun onDetachedFromWindow() {
        if (window.renderer === renderer) window.renderer = null
        renderer.release()
        super.onDetachedFromWindow()
    }

    override fun surfaceCreated(holder: SurfaceHolder) = Unit

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) {
        surfaceWidth = width
        surfaceHeight = height
        renderer.attach(holder.surface, width, height)
        layoutWindow()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) = renderer.detach()
}
