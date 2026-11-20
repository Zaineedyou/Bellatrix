package com.bellatrixos.windows.drivers.network

import android.bluetooth.BluetoothAdapter

/** Status Bluetooth yang dipakai ikon taskbar. */
object Network {
    @JvmStatic
    fun a(): Boolean = try {
        BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
    } catch (_: SecurityException) {
        false
    } catch (_: Exception) {
        false
    }
}
