package com.bellatrixos.windows.installed_apps.models

import com.google.gson.annotations.SerializedName

/** Nama dan ikon kustom untuk sebuah aplikasi. */
class AppDataDBItem(
    @SerializedName("customName") @JvmField var customName: String?,
    @SerializedName("iconPath") @JvmField var iconPath: String?
)
