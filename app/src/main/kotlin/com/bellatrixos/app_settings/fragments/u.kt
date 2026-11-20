package com.bellatrixos.app_settings.fragments

import android.widget.CompoundButton
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.variables.Global

/** Menyimpan nilai switch pengaturan. */
class u(@JvmField val f4721a: Int) : CompoundButton.OnCheckedChangeListener {
    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        val key = when (f4721a) {
            0 -> "file_explorer_side_nav_enabled"
            1 -> "file_explorer_home_shortcuts_enabled"
            2 -> "system_notify_updates"
            else -> "start_menu_search_auto_focus"
        }
        SharedPrefs.f4755a?.edit()?.putBoolean(key, isChecked)?.apply()
        if (f4721a == 3) Global.m = isChecked
    }
}
