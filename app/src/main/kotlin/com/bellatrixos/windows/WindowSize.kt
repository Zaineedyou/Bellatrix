package com.bellatrixos.windows

import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bellatrixos.R
import com.bellatrixos.UIFragment
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.widgets.TitleBar
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import kotlin.math.abs
import kotlin.math.max

/** Mengelola ukuran, posisi, dan drag jendela aplikasi. */
object WindowSize {
    @JvmField var f4871a: Int = -1
    @JvmField var b: Int = -1
    @JvmField var c: Runnable? = null
    @JvmField var f4872d: Handler? = null
    @JvmField var f4873e: Float = 0.0f
    @JvmField var f4874f: Float = 0.0f
    @JvmField var g: Int = -1
    @JvmField var f4875h: Int = -1
    @JvmField var f4876i: Float = -1.0f
    @JvmField var j: Float = -1.0f
    @JvmField var f4877k: Int = 0
    @JvmField var l: Int = 0
    @JvmField var m: Int = -1

    class ResizeState {
        @JvmField var f4878a: Float = 0.0f
        @JvmField var b: Float = 0.0f
        @JvmField var c: Int = 0
        @JvmField var f4879d: Int = 0
    }

    @JvmStatic
    fun a(layout: FloatingLayout, style: Int) {
        layout.clipToOutline = true
        if (style == 0 && Global.f4836a == 1) {
            val params = layout.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(f4877k, f4877k, f4877k, f4877k)
            layout.layoutParams = params
        }
    }

    @JvmStatic
    fun b(view: View, layout: FloatingLayout) {
        if (!layout.L) {
            layout.L = true
            val parent = layout.parent
            if (parent is FrameLayout) {
                val handle = View(layout.context)
                val size = (layout.resources.displayMetrics.density * 40.0f).toInt()
                handle.layoutParams = FrameLayout.LayoutParams(size, size).apply { gravity = 8388693 }
                handle.setBackgroundResource(R.drawable.ic_app_resize_handle)
                handle.isClickable = true
                handle.isFocusable = true
                handle.alpha = 0.1f
                parent.addView(handle)
                layout.J = handle
                layout.post { handle.elevation = 10.0f }
            }
        }
        layout.getDragHandle()?.setOnTouchListener { _, event ->
            val state = view.getTag(R.id.resize_state_tag) as? ResizeState ?: ResizeState().also {
                view.setTag(R.id.resize_state_tag, it)
            }
            val rawX = event.rawX
            val rawY = event.rawY
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // The resize grip is a sibling of FloatingLayout, so it never
                    // goes through dispatchTouchEvent. Restore focus here as well.
                    val focus = layout.C
                    if (!layout.B && focus != null) focus.a() else layout.D?.c()
                    state.f4878a = rawX
                    state.b = rawY
                    state.c = view.width
                    state.f4879d = view.height
                    val handle = layout.J ?: return@setOnTouchListener true
                    handle.animate().alpha(1.0f).setDuration(40L)
                        .withEndAction {
                            Handler(android.os.Looper.getMainLooper()).postDelayed({
                                handle.animate().alpha(0.2f).setDuration(400L)
                                    .withEndAction { handle.alpha = 0.1f }.start()
                            }, 2000L)
                        }.start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val width = max(state.c + (rawX - state.f4878a).toInt(), (f4871a * 0.44f).toInt())
                    val height = max(state.f4879d + (rawY - state.b).toInt(), (b * 0.54f).toInt())
                    val params: ViewGroup.LayoutParams = view.layoutParams
                    params.width = width
                    params.height = height
                    view.layoutParams = params
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    @JvmStatic
    fun c(window: Window) {
        window.a(if (window.c == 0) window.f4869d else 0)
    }

    @JvmStatic
    fun d(view: View, layout: FloatingLayout?, window: Window) {
        if (window.c != 0) {
            if (f4871a != -1) {
                view.post { applySize(view, layout, window, window.c) }
            }
        } else {
            val params: ViewGroup.LayoutParams = view.layoutParams
            params.width = -1
            params.height = -1
            view.layoutParams = params
            view.x = 0.0f
            view.y = 0.0f
            if (layout != null) {
                layout.y = 0.0f
                layout.z = 0.0f
                layout.invalidate()
            }
        }
        WindowManager.a(view, window.c == 0)
    }

    private fun applySize(view: View, layout: FloatingLayout?, window: Window, style: Int) {
        var width = g
        var height = f4875h
        if (Global.f4836a == 2) {
            when (style) {
                2 -> {
                    width = (f4871a * 0.72).toInt()
                    height = (b * 0.78).toInt()
                }
                3 -> {
                    width = (f4871a * 0.6).toInt()
                    height = (b * 0.74).toInt()
                }
            }
        }
        val params: ViewGroup.LayoutParams = view.layoutParams
        params.width = width
        params.height = height
        view.layoutParams = params
        if (window.b == -1.0f) {
            val left = (f4871a - width) / 2.0f - 0.02f * f4871a
            val top = (b - height) / 2.0f - 0.05f * b
            if (f4876i == -1.0f) {
                f4876i = left
                j = top
            } else if (j == -1.0f || Windows.c) {
                f4876i = left
                j = top
            } else {
                val nextX = f4876i + f4871a * 0.018f
                val nextY = j + b * 0.018f
                if (width + nextX > f4871a || height + nextY > b) {
                    f4876i = left
                    j = top
                } else {
                    f4876i = nextX
                    j = nextY
                }
            }
            view.x = f4876i
            view.y = j
            window.f4868a = f4876i
            window.b = j
        } else {
            view.x = window.f4868a
            view.y = window.b
        }
        if (layout != null) {
            layout.y = view.x
            layout.z = view.y
            layout.invalidate()
        }
    }

    @JvmStatic
    fun e(
        view: View,
        layout: FloatingLayout?,
        window: Window,
        handle: View,
        activity: MainActivityIn,
        fragment: UIFragment
    ) {
        view.bringToFront()
        var offsetX = 0.0f
        var offsetY = 0.0f
        var lastX = 0.0f
        var lastY = 0.0f
        val listener = object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (!fragment.f0()) {
                            view.bringToFront()
                            fragment.setWindowFocused(true)
                            activity.d(fragment)
                        }
                        offsetX = event.rawX - view.x
                        offsetY = event.rawY - view.y
                        lastX = event.rawX
                        lastY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (window.c == 0) {
                            val dx = event.rawX - lastX
                            val dy = event.rawY - lastY
                            if (abs(dx) > 10.0f || abs(dy) > 10.0f) window.a(window.f4869d)
                        }
                        view.x = event.rawX - offsetX
                        view.y = event.rawY - offsetY
                        if (layout != null) {
                            layout.y = view.x
                            layout.z = view.y
                            layout.invalidate()
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        var y = event.rawY - offsetY
                        if (y < 0.0f) y = 0.0f
                        if (y > f4873e) y = f4874f
                        view.y = y
                        return true
                    }
                    else -> return false
                }
            }
        }
        if (handle is TitleBar) handle.setOnDragListener(listener) else handle.setOnTouchListener(listener)
    }

    @JvmStatic
    fun f(layout: FloatingLayout, titleBar: TitleBar?, style: Int) {
        val params = layout.layoutParams as ViewGroup.MarginLayoutParams
        val margin: Int
        if (style == 0) {
            layout.J?.visibility = View.GONE
            margin = f4877k
        } else {
            val handle = layout.J
            handle?.visibility = View.VISIBLE
            handle?.animate()?.alpha(0.6f)?.setDuration(40L)?.withEndAction {
                Handler(android.os.Looper.getMainLooper()).postDelayed({
                    handle.animate().alpha(0.2f).setDuration(400L)
                        .withEndAction { handle.alpha = 0.1f }.start()
                }, 2000L)
            }?.start()
            margin = l
        }
        params.setMargins(margin, margin, margin, margin)
        layout.layoutParams = params
        layout.setBgStyle(style)
        titleBar?.setResizeBtn(style)
    }

    @JvmStatic
    fun g(style: Int, tag: String?, app: InstalledApp?) {
        if (app == null) return
        val handler = f4872d ?: return
        c?.let { handler.removeCallbacks(it) }
        val runnable = Runnable { }
        c = runnable
        handler.postDelayed(runnable, 4000L)
    }
}
