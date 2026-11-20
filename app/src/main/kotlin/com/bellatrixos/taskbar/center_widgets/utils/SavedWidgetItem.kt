package com.bellatrixos.taskbar.center_widgets.utils

import com.google.gson.annotations.SerializedName

/** Widget center yang disimpan di preferensi. */
class SavedWidgetItem(
    @SerializedName("providerClassName") private val providerClassName: String,
    @SerializedName("appWidgetId") private val appWidgetId: Int,
    @SerializedName("position") private val position: Int
) {
    @SerializedName("width") private var width: Int = 1
    @SerializedName("fullSpan") private var fullSpan: Boolean = false

    fun a(): Int = appWidgetId
    fun b(): Int = position
    fun c(): String = providerClassName
    fun d(): Boolean = fullSpan
    fun e(z: Boolean) { fullSpan = z }
}
