package com.bellatrixos.app_notepad

import android.view.View
import androidx.core.util.Consumer
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.searchapp.OnFilterCompleteListener
import com.bellatrixos.searchapp.SearchItem

/** Callback menu dan hasil pencarian notepad. */
class a(
    @JvmField val v: Consumer<Any?>,
    @JvmField val u: Int
) : UIContextMenu.OnMenuItemClickListener, OnFilterCompleteListener {

    override fun a(searchItem: SearchItem) {
        v.accept(searchItem)
    }

    override fun f(view: View, i2: Int) {
        v.accept(i2)
    }
}
