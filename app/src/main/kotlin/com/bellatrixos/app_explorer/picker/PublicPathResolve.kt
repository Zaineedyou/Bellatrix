package com.bellatrixos.app_explorer.picker

import java.io.File

/** Mengambil bagian path relatif terhadap direktori publik. */
object PublicPathResolve {
    @JvmStatic
    fun a(root: File, file: File): String {
        val base = root.path
        val path = file.path
        return if (path.length == base.length) "" else path.substring(base.length + 1)
    }
}
