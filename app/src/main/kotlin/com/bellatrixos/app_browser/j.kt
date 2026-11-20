package com.bellatrixos.app_browser

import android.view.Choreographer
import android.view.MotionEvent

/** Stabilkan hasil hit-test sebelum menampilkan menu konteks (klik kanan). */
class j(
    @JvmField val u: UIBrowserTab,
    @JvmField val v: Int,
    @JvmField val w: MotionEvent
) : Choreographer.FrameCallback {

    override fun doFrame(frameTimeNanos: Long) {
        val tab = u
        val result = tab.y0?.hitTestResult ?: return
        val previous = tab.D0
        if (previous != null && result.type == previous.type &&
            result.extra == previous.extra && v < 1
        ) {
            Choreographer.getInstance().postFrameCallback(j(tab, v + 1, w))
        } else {
            tab.D0 = result
            val context = tab.s0 ?: return
            ContextMenuBR.a(context, result, w, e(1, tab))
        }
    }
}
