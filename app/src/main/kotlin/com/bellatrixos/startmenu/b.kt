package com.bellatrixos.startmenu

import android.view.View

/** Long-click Start Menu: jalankan aksi kecuali sedang mode drag. */
class b(@JvmField val u: Int, @JvmField val v: Runnable) : View.OnLongClickListener {
    override fun onLongClick(view: View): Boolean {
        if (u == 0 && StartMenu.f4779d == 1) return true
        v.run()
        return true
    }
}
