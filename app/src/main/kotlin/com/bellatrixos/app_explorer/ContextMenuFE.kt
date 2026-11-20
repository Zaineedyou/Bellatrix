package com.bellatrixos.app_explorer

import com.bellatrixos.app_explorer.files.Files
import com.bellatrixos.windows.files.clipboard.ClipboardItem
import com.bellatrixos.windows.files.clipboard.Clipboards
import java.io.File

/** Menyiapkan antrean clipboard dari berkas yang dipilih. */
object ContextMenuFE {
    private fun fill(files: Set<File>) {
        for (file in files) {
            val parent = file.parent ?: ""
            Clipboards.f4922a.add(ClipboardItem(if (file.isDirectory) 3 else 2, parent, file.name))
        }
    }

    @JvmStatic
    fun a(files: HashSet<File>) {
        Clipboards.f4922a.clear()
        Clipboards.b = false
        fill(files)
    }

    @JvmStatic
    fun b(files: HashSet<File>) {
        Clipboards.f4922a.clear()
        fill(files)
        Files.g = true
    }
}
