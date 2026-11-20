package com.bellatrixos.windows.drivers.battery

/** State baterai terakhir yang diterima dari sistem. */
object BatteryState {
    @JvmField var f4906a: Battery.Info? = null
    @JvmField var b: Boolean = false

    @JvmStatic
    @Synchronized
    fun a(): Battery.Info? = f4906a
}
