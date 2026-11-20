package com.bellatrixos.desktop

import com.google.gson.annotations.SerializedName

/** Versi tersimpan dari [DesktopItem] beserta posisinya. */
class DesktopItemPins(
    @SerializedName("type") @JvmField val type: Int,
    @SerializedName("position") @JvmField val position: Int,
    @SerializedName("name") @JvmField val name: String?,
    @SerializedName("data") @JvmField val data: String?
) {
    fun a(): String? = data
    fun b(): String? = name
    fun c(): Int = type
}
