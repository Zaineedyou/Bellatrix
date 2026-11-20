package com.bellatrixos.app_explorer.navigation

import com.google.gson.annotations.SerializedName

/** Item navigasi samping pada file explorer. */
class NavigationItem {
    @JvmField val f4689a: Int
    @JvmField val b: Boolean
    @JvmField val c: Boolean
    @JvmField var f4690d: String? = null

    @SerializedName("imageResId") @JvmField val imageResId: Int
    @SerializedName("text") @JvmField val text: String?

    constructor(i2: Int, i3: Int, str: String?, z: Boolean) {
        b = true
        text = str
        f4689a = i3
        imageResId = i2
        c = z
    }

    constructor(i2: Int, str: String?, i3: Int, z: Boolean, i4: Int) {
        text = str
        f4689a = i3
        imageResId = i2
        c = false
        b = z
    }

    fun a(): Int = imageResId

    fun b(): String? = text
}
