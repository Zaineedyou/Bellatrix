package com.bellatrixos.windows.drivers.network

import android.widget.ImageView
import com.bellatrixos.R

/** Runnable sintetis yang memilih ikon jaringan pada taskbar. */
class b(@JvmField val v: ImageView, @JvmField val w: Int, @JvmField val u: Int) : Runnable {
    override fun run() {
        val icon = when (w) {
            1 -> R.drawable.ic_cellular_data_1_24_filled
            3 -> R.drawable.ui_tb_ethernet_24_regular
            else -> R.drawable.ui_tb_wifi_4_24_regular
        }
        v.setImageResource(icon)
    }
}
