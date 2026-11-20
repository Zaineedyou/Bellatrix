package com.bellatrixos.searchapp

import android.view.View
import com.bellatrixos.interfaces.MainActivityIn

/** Klik tombol panel pencarian: buka sebagai jendela baru. */
class a(mainActivityIn: MainActivityIn?, i2: Int) : View.OnClickListener {
    @JvmField val u: Int = i2
    @JvmField val v: MainActivityIn? = mainActivityIn

    override fun onClick(view: View) {
        v?.o(1)
    }
}
