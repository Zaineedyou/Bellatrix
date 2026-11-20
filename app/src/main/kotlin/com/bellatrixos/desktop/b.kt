package com.bellatrixos.desktop

import android.view.View
import android.widget.PopupWindow

/** Dismiss popup: kembalikan fokus atau status tombol. */
class b(view: View, i2: Int) : PopupWindow.OnDismissListener {
    @JvmField val u: Int = i2
    @JvmField val v: View = view

    override fun onDismiss() {
        when (u) {
            0 -> v.requestFocus()
            1 -> v.isActivated = false
            else -> v.isActivated = false
        }
    }
}
