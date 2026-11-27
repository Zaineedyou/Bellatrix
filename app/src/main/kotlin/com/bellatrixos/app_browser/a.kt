package com.bellatrixos.app_browser

import android.view.View
import androidx.core.util.Consumer
import com.bellatrixos.context_menu.UIContextMenu

/**
 * Callback synthetic hasil dekompilasi: penerima klik menu konteks.
 */
class a(@JvmField var u: Any?) : UIContextMenu.OnMenuItemClickListener {

    override fun f(view: View, i2: Int) {
        @Suppress("UNCHECKED_CAST")
        (u as? Consumer<Int>)?.accept(i2)
    }
}
