package com.bellatrixos.wayland

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView

/**
 * Draws one Linux toplevel into a TextureView so its pixels follow the Android
 * View hierarchy's z-order. A SurfaceView lives on its own compositing layer:
 * raising a floating terminal's frame could leave its content behind another
 * window's ordinary Views (for example Coding Agents). TextureView has no such
 * separate layer and is raised together with the fragment that owns it.
 *
 * [visibleHeight] limits the part the window uses so the keybar and soft
 * keyboard sit below it without stretching old frames.
 */
class WindowTextureView(
    context: Context,
    private val server: WaylandServer,
    private val wm: WindowViewManager,
    private val window: XdgToplevel,
) : TextureView(context),
    TextureView.SurfaceTextureListener {
    private var renderer: WindowRenderer? = null
    private var surface: Surface? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    /** How much of the view, from its top, the keyboard and keybar leave the window; 0 for all of it. */
    var visibleHeight = 0
        set(value) {
            if (value == field) return
            field = value
            layoutWindow()
        }

    init {
        isOpaque = true
        surfaceTextureListener = this
    }

    private fun layoutWindow() {
        if (surfaceWidth == 0) return
        val h = if (visibleHeight in 1..surfaceHeight) visibleHeight else surfaceHeight
        renderer?.visibleHeight = h
        wm.applyAppLayout(window, surfaceWidth, h)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val r = WindowRenderer(server, window).also { renderer = it }
        window.renderer = r
        surface?.let(::attachSurface)
    }

    override fun onDetachedFromWindow() {
        if (window.renderer === renderer) window.renderer = null
        renderer?.release()
        renderer = null
        super.onDetachedFromWindow()
    }

    private fun attachSurface(s: Surface) {
        renderer?.attach(s, surfaceWidth, surfaceHeight)
        layoutWindow()
    }

    override fun onSurfaceTextureAvailable(
        st: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        surfaceWidth = width
        surfaceHeight = height
        attachSurface(Surface(st).also { surface = it })
    }

    override fun onSurfaceTextureSizeChanged(
        st: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        surfaceWidth = width
        surfaceHeight = height
        surface?.let(::attachSurface)
    }

    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
        renderer?.detach()
        surface?.release()
        surface = null
        return true
    }

    override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
}
