package com.bellatrixos.windows.files.clipboard

import java.io.File

/** Antrean salin/pindah berkas. */
object Clipboards {
    @JvmField val f4922a: MutableList<ClipboardItem> = mutableListOf()
    @JvmField var b: Boolean = false

    @JvmStatic
    fun a(str: String, str2: String): String =
        if (str.endsWith(File.separator)) str + str2 else str + File.separator + str2
}
