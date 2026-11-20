package com.bellatrixos.startmenu

import android.widget.ImageView

/** Runnable mengubah tampilan ikon Start Menu. */
class d(@JvmField val v: ImageView, @JvmField val u: Int) : Runnable {
    override fun run() {
        if (u == 1) {
            v.setImageBitmap(null)
        } else {
            v.alpha = 0.09f
        }
    }
}
