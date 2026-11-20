package com.bellatrixos.taskbar

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

/** Status satu tombol aplikasi di taskbar. */
class TaskBarItem : Parcelable {
    @SerializedName("packageName") @JvmField val packageName: String
    @SerializedName("isPinned") @JvmField var isPinned: Boolean = false
    @SerializedName("isMinimized") @JvmField var isMinimized: Boolean = false
    @SerializedName("isClosed") @JvmField var isClosed: Boolean = false
    @SerializedName("multiple") @JvmField var multiple: Boolean = false

    constructor(str: String, pinned: Boolean) {
        packageName = str
        isPinned = pinned
    }

    constructor(parcel: Parcel) {
        packageName = parcel.readString() ?: ""
        isPinned = parcel.readByte().toInt() != 0
        isMinimized = parcel.readByte().toInt() != 0
        isClosed = parcel.readByte().toInt() != 0
    }

    fun a(): String = packageName
    fun b(): Boolean = isClosed
    fun c(): Boolean = isMinimized
    fun d(): Boolean = multiple
    fun h(): Boolean = isPinned
    fun j() { isClosed = true }
    fun m(z: Boolean) { isMinimized = z; isClosed = false }
    fun n(z: Boolean) { multiple = z }
    fun o() { isPinned = true }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, i2: Int) {
        parcel.writeString(packageName)
        parcel.writeByte(if (isPinned) 1 else 0)
        parcel.writeByte(if (isMinimized) 1 else 0)
        parcel.writeByte(if (isClosed) 1 else 0)
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<TaskBarItem> = object : Parcelable.Creator<TaskBarItem> {
            override fun createFromParcel(parcel: Parcel): TaskBarItem = TaskBarItem(parcel)
            override fun newArray(size: Int): Array<TaskBarItem?> = arrayOfNulls(size)
        }
    }
}
