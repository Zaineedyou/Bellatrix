package com.bellatrixos.desktop

import com.google.gson.annotations.SerializedName

/** Item yang tampil di desktop. */
class DesktopItem(
    @SerializedName("type") @JvmField val type: Int,
    @SerializedName("name") @JvmField var name: String?,
    @SerializedName("data") @JvmField var data: String?
) {
    fun a(): String? = data
    fun b(): String? = name
    fun c(): Int = type
    fun d(str: String?) { data = str }
    fun e(str: String?) { name = str }
}
