package com.bellatrixos.windows.drivers.volume

/** Dipanggil saat volume stream media berubah. */
fun interface OnVolumeChangeListener {
    fun X(current: Int, max: Int)
}
