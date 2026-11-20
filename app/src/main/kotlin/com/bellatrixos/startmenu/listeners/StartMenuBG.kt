package com.bellatrixos.startmenu.listeners

/** Mendeteksi perubahan state background Start Menu. */
class StartMenuBG {
    fun interface OnChangeListener {
        fun a()
    }

    @JvmField var f4800a: Boolean = false
    @JvmField var b: OnChangeListener? = null

    fun a(changed: Boolean) {
        if (f4800a == changed) return
        f4800a = changed
        b?.a()
    }
}
