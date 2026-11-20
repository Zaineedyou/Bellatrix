package com.bellatrixos.startmenu.interfaces

import com.bellatrixos.startmenu.StartMenuItem

/** Callback klik item Start Menu. */
fun interface OnStartMenuItemClickListener {
    fun d(item: StartMenuItem, position: Int)
}
