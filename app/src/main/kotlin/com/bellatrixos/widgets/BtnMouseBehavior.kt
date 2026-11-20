package com.bellatrixos.widgets

import android.view.MotionEvent
import android.view.View

/** Perilaku tombol saat disentuh dan di-hover. */
class BtnMouseBehavior {
    companion object {
        @JvmField val f4849a: BtnMouseBehavior = BtnMouseBehavior()

        @JvmStatic
        fun b(view: View, event: MotionEvent) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> view.isPressed = true
                MotionEvent.ACTION_UP -> view.post { view.isPressed = false }
                MotionEvent.ACTION_CANCEL -> view.isPressed = false
                else -> return
            }
            view.refreshDrawableState()
        }
    }

    fun a(view: View) {
        view.setOnHoverListener { _, _ -> false }
        view.setOnTouchListener { _, event ->
            b(view, event)
            false
        }
    }
}
