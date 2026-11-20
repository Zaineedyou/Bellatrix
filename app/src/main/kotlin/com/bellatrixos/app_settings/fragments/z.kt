package com.bellatrixos.app_settings.fragments

import android.view.View

/** Klik kartu pada pengaturan personalisasi. */
class z(@JvmField val v: UIPersonalize, @JvmField val u: Int) : View.OnClickListener {
    override fun onClick(view: View) {
        val settings = v.t0 ?: return
        when (u) {
            0 -> settings.p0(33, true, null)
            1 -> settings.p0(32, true, null)
            2 -> settings.p0(31, true, null)
            3 -> settings.p0(34, true, null)
            4 -> v.a0("theme1")
            else -> v.a0("theme2")
        }
    }
}
