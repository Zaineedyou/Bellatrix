package com.bellatrixos.startmenu

import android.view.MotionEvent
import android.view.View

/** Menangani scroll roda dan tombol mouse tengah pada Start Menu. */
class f : View.OnGenericMotionListener {
    override fun onGenericMotion(view: View, event: MotionEvent): Boolean {
        if (event.source and 2 == 0 || event.action != MotionEvent.ACTION_SCROLL) {
            return event.buttonState and 2 != 0 && event.action == 11
        }
        return true
    }
}
