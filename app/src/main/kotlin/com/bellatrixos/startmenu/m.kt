package com.bellatrixos.startmenu

import android.content.Context
import android.view.View
import com.bellatrixos.AppExecutors

/** Penjadwal pemuatan iklan native Start Menu. */
class m(@JvmField val u: Int, @JvmField val v: Context, @JvmField val w: View) : Runnable {
    override fun run() {
        when (u) {
            0 -> AppExecutors.c().postDelayed(m(2, v, w), 200L)
            1 -> AppExecutors.c().postDelayed(m(3, v, w), 1600L)
            2, 3 -> AppExecutors.b().execute(l(v, w, 0, 0))
        }
    }
}
