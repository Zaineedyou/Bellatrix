package com.bellatrixos.startmenu

import android.content.ComponentName
import android.os.UserHandle
import com.google.gson.annotations.SerializedName

/** Entitas aplikasi di Start Menu. */
class StartMenuItem {
    @JvmField val f4786a: ComponentName?
    @JvmField val b: UserHandle?

    @SerializedName("category") @JvmField val category: Int
    @SerializedName("name") @JvmField var name: String?
    @SerializedName("packageName") @JvmField val packageName: String?

    constructor(i2: Int, str: String?, str2: String?) : this(str, str2, i2, null, null)

    constructor(
        str: String?,
        str2: String?,
        i2: Int,
        componentName: ComponentName?,
        userHandle: UserHandle?
    ) {
        name = str
        packageName = str2
        category = i2
        f4786a = componentName
        b = userHandle
    }

    fun a(): Int = category
    fun b(): String? = name
    fun c(): String? = packageName
    fun d(str: String?) { name = str }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is StartMenuItem) return packageName == other.packageName
        return false
    }

    override fun hashCode(): Int = packageName?.hashCode() ?: 0
}
