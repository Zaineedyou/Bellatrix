package com.bellatrixos.windows.files.operations

import android.content.Context
import com.bellatrixos.R
import java.io.File

/** Membuat folder baru dengan nama unik. */
object New {
    @JvmStatic
    fun a(context: Context, file: File): File? {
        if (!file.exists() || !file.isDirectory) return null
        val base = context.getString(R.string.files_new_folder)
        var target = File(file, base)
        var index = 2
        while (target.exists()) {
            target = File(file, "$base ($index)")
            index++
        }
        return if (target.mkdirs()) target else null
    }
}
