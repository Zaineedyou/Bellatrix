package com.bellatrixos.searchapp

import android.view.MotionEvent
import android.view.View
import com.bellatrixos.variables.Global

/** Touch listener pencarian: mode 0 menahan interception induk. */
class d(@JvmField val u: Int) : View.OnTouchListener {
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (u == 0) {
            view.parent.requestDisallowInterceptTouchEvent(true)
        } else if (event.action == MotionEvent.ACTION_DOWN) {
            Global.A[0] = event.rawX.toInt()
            Global.A[1] = event.rawY.toInt()
        }
        return false
    }
}
