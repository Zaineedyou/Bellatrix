package com.bellatrixos.widgets

import android.view.MotionEvent
import android.view.View

/** Khusus hover tombol kustom (mode 0) dan spinner (mode 1). */
class a(@JvmField val u: Int) : View.OnHoverListener {
    override fun onHover(view: View, event: MotionEvent): Boolean {
        val action = if (u == 0) event.action else event.actionMasked
        if (action == MotionEvent.ACTION_HOVER_ENTER) {
            view.isHovered = true
        } else if (action == MotionEvent.ACTION_HOVER_EXIT) {
            view.isHovered = false
        }
        if (u == 0) view.refreshDrawableState()
        return false
    }
}
