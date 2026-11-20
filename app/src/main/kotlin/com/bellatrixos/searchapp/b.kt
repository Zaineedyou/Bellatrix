package com.bellatrixos.searchapp

import com.bellatrixos.context_menu.OnItemClickListener
import com.bellatrixos.startmenu.StartMenuItem
import com.bellatrixos.startmenu.interfaces.OnStartMenuItemClickListener

/** Pilih aplikasi hasil pencarian Start Menu. */
class b(listener: OnStartMenuItemClickListener, i2: Int) : OnItemClickListener<Any> {
    @JvmField val f4775a: Int = i2
    @JvmField val b: OnStartMenuItemClickListener = listener

    override fun a(obj: Any) {
        val item = obj as? StartMenuItem ?: return
        b.d(item, if (f4775a == 0) 1 else 0)
    }
}
