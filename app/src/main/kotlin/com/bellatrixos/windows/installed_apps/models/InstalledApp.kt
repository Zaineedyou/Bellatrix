package com.bellatrixos.windows.installed_apps.models

import com.bellatrixos.variables.Global
import com.google.gson.annotations.SerializedName

/** Metadata satu aplikasi yang bisa dibuka sebagai jendela. */
class InstalledApp(
    @SerializedName("name") private var name: String?,
    @SerializedName("packageName") private val packageName: String,
    @SerializedName("iconPath") private var iconPath: String?,
    @SerializedName("resizeMode") private var resizeMode: Int,
    @SerializedName("cacheEnabled") private var cacheEnabled: Boolean
) {
    @SerializedName("url") private var url: String? = null

    @SerializedName("scale") private var scale: Float = 0.0f

    @SerializedName("zoom") private var zoom: Int = 100

    @SerializedName("userAgent") private var userAgent: Int = 1

    fun a(): String? = iconPath

    fun b(): String? = name

    fun c(): String = packageName

    fun d(): Int {
        val i2 = resizeMode
        return if (i2 == -1) 0 else i2
    }

    fun e(): Float {
        val f2 = scale
        return if (f2 == 0.0f) Global.f4838e else f2
    }

    fun f(): String {
        val str = url
        return str ?: packageName
    }

    fun g(): Int {
        val i2 = zoom
        return if (i2 == 0) 100 else i2
    }

    fun h(): Boolean = cacheEnabled

    fun i(str: String?) {
        iconPath = str
    }

    fun j(str: String?) {
        name = str
    }

    fun k(i2: Int) {
        resizeMode = i2
    }

    fun l(f2: Float) {
        scale = f2
    }

    fun m(str: String?) {
        url = str
    }

    fun n(i2: Int) {
        zoom = i2
    }
}
