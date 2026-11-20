package com.bellatrixos.app_settings.utils

import com.bellatrixos.interfaces.MainActivityIn

/** Runnable yang meneruskan aksi pengaturan ke Activity utama. */
class b(@JvmField val v: MainActivityIn?, @JvmField val u: Int) : Runnable {
    override fun run() {
        when (u) {
            0 -> v?.Q(14)
            1 -> v?.Q(6)
            2 -> v?.Q(6)
            3 -> v?.Q(1)
            else -> v?.W(null)
        }
    }
}
