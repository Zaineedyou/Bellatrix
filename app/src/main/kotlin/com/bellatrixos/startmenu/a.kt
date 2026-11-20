package com.bellatrixos.startmenu

import android.view.View
import com.bellatrixos.R
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.variables.Global

/** Menu konteks daftar aplikasi Start Menu. */
class a(mainActivityIn: MainActivityIn, i2: Int) : UIContextMenu.OnMenuItemClickListener {
    @JvmField val u: Int = i2
    @JvmField val v: MainActivityIn = mainActivityIn

    override fun f(view: View, i2: Int) {
        when (u) {
            0 -> {
                val mode = when (i2) {
                    R.id.action_list_freeform -> 2
                    R.id.action_list_work -> 3
                    else -> 0
                }
                Global.f4842k = mode
                v.E(mode)
            }
            1 -> {
                val order = when (i2) {
                    R.id.action_sort_asc -> 1
                    R.id.action_sort_desc -> 2
                    else -> 0
                }
                SharedPrefs.f4755a?.edit()?.putInt("start_menu_apps_order", order)?.apply()
                v.b0(order)
            }
            else -> when (i2) {
                11 -> v.F()
                15 -> v.c0("UISettings", 34.toString())
            }
        }
    }
}
