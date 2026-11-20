package com.bellatrixos.app_explorer.drives

import com.google.gson.annotations.SerializedName

/** Informasi kapasitas penyimpanan. */
data class StorageStat(
    @SerializedName("str") private val str: String,
    @SerializedName("num") private val num: Int,
    @SerializedName("totalSpace") private val totalSpace: Double,
    @SerializedName("absolutePath") private val absolutePath: String?
) {
    fun a(): String? = absolutePath
    fun b(): Int = num
    fun c(): String = str
    fun d(): Double = totalSpace
}
