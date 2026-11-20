package com.bellatrixos.taskbar.center_widgets.utils

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/** RecyclerView widget yang bisa menyerahkan scroll ke child saat tidak dragging. */
class WidgetsRecyclerView(context: Context, attributeSet: AttributeSet?) : RecyclerView(context, attributeSet) {
    @JvmField var e1: Boolean = false

    companion object {
        @JvmStatic
        fun n0(view: View, z: Boolean): Boolean {
            if (view !is ViewGroup) return false
            for (i2 in 0 until view.childCount) {
                val child = view.getChildAt(i2)
                val scrollable = if (z) {
                    child.canScrollVertically(-1) || child.canScrollVertically(1)
                } else {
                    child.canScrollHorizontally(-1) || child.canScrollHorizontally(1)
                }
                if (scrollable || n0(child, z)) return true
            }
            return false
        }
    }

    fun o0(motionEvent: MotionEvent): Boolean {
        if (e1) return false
        val child = findChildViewUnder(motionEvent.x, motionEvent.y) ?: return false
        if (child !is ViewGroup) return false
        return n0(child, true) || n0(child, false)
    }

    override fun onInterceptTouchEvent(motionEvent: MotionEvent): Boolean {
        if (!e1 && o0(motionEvent)) return false
        return super.onInterceptTouchEvent(motionEvent)
    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        if (!e1 && o0(motionEvent)) return false
        return super.onTouchEvent(motionEvent)
    }

    fun setDraggingEnabled(z: Boolean) {
        e1 = z
    }
}
