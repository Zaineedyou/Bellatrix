package com.bellatrixos.windows.drivers

import android.content.Context
import com.bellatrixos.R

/** Label perangkat yang dipakai pengaturan.Display. */
object Display {
    @JvmField var f4898a: Boolean = false

    @JvmStatic
    fun a(context: Context): String {
        val integer = context.resources.getInteger(R.integer.settings_display_mounted_device)
        val resId = when (integer) {
            0 -> R.string.app_device_phone
            1 -> R.string.app_device_tablet
            else -> R.string.str_todo
        }
        return context.getString(resId)
    }
}
