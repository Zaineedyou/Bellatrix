package com.bellatrixos.windows.drivers.battery

import com.bellatrixos.R

/** Ikon baterai berdasarkan persentase dan status pengisian. */
object Battery {
    class Info(@JvmField val level: Int, @JvmField val charging: Boolean)

    @JvmStatic
    fun a(i2: Int, charging: Boolean): Int = when {
        i2 < 20 -> if (charging) R.drawable.ui_tb_battery_2_charge_24 else R.drawable.ui_tb_battery_2_24
        i2 < 48 -> if (charging) R.drawable.ui_tb_battery_4_charge_24 else R.drawable.ui_tb_battery_4_24
        i2 < 85 -> if (charging) R.drawable.ui_tb_battery_7_charge_24 else R.drawable.ui_tb_battery_7_24
        charging -> R.drawable.ui_tb_battery_10_charge_24
        else -> R.drawable.ui_tb_battery_10_24
    }
}
