package com.bellatrixos.app_explorer.drives

import com.google.gson.annotations.SerializedName

/** Satu entri drive atau folder pada daftar penyimpanan. */
class DriveItem {
    @JvmField val f4674a: Int

    @SerializedName("iconResource") @JvmField var iconResource: Int = 0

    @SerializedName("name") @JvmField val name: String

    @SerializedName("path") @JvmField var path: String? = null

    @SerializedName("storageDetails") @JvmField var storageDetails: String? = null

    @SerializedName("storageOccupied") @JvmField var storageOccupied: Int = 0

    @SerializedName("type") @JvmField val type: Int

    constructor(str: String?) {
        f4674a = 0
        path = null
        type = 2
        name = str ?: ""
    }

    constructor(str: String?, i2: Int, i3: Int) {
        path = null
        type = 1
        name = str ?: ""
        iconResource = i2
        f4674a = i3
    }

    constructor(str: String?, i2: Int, str2: String?, i3: Int, str3: String?) {
        f4674a = 0
        type = 0
        name = str ?: ""
        path = str3
        iconResource = i2
        storageDetails = str2
        storageOccupied = i3
    }

    fun a(): Int = iconResource

    fun b(): String = name

    fun c(): String? = path

    fun d(): String? = storageDetails

    fun e(): Int = storageOccupied

    fun f(): Int = type
}
