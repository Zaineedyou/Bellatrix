package com.bellatrixos.taskbar

import com.google.gson.annotations.SerializedName

/** Versi tersimpan dari item taskbar yang di-pin. */
class TaskBarItemPins(
    @SerializedName("data") @JvmField val data: String,
    @SerializedName("position") @JvmField val position: Int
) {
    fun a(): String = data
}
