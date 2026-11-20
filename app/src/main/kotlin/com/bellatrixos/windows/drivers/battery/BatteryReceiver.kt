package com.bellatrixos.windows.drivers.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Menerima ACTION_BATTERY_CHANGED dan memperbarui [BatteryState]. */
class BatteryReceiver(
    @JvmField val callback: Callback
) : BroadcastReceiver() {

    fun interface Callback {
        fun g()
    }

    override fun onReceive(context: Context, intent: Intent) {
        val level = intent.getIntExtra("level", -1)
        val scale = intent.getIntExtra("scale", -1)
        val percent = if (level < 0 || scale <= 0) 0 else Math.round(level * 100.0f / scale)
        val status = intent.getIntExtra("status", -1)
        val charging = status == 2 || status == 5

        synchronized(BatteryState) {
            val previous = BatteryState.f4906a
            if (previous == null || previous.level != percent || previous.charging != charging) {
                BatteryState.f4906a = Battery.Info(percent, charging)
                BatteryState.b = true
            }
        }
        callback.g()
    }
}
