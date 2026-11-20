package com.bellatrixos.windows.files.operations

import com.bellatrixos.app_explorer.files.FileAdapter
import com.bellatrixos.app_explorer.files.Files
import com.bellatrixos.windows.files.FileMeta
import com.bellatrixos.windows.files.clipboard.Clipboards
import java.io.File

/** Callback keberhasilan operasi hapus/salin pada FileAdapter. */
class b(@JvmField val b: FileAdapter, @JvmField val f4944a: Int) : FileOperations.TaskSuccessListener {
    override fun a(list: List<*>) {
        if (f4944a == 0) {
            val deleted = HashSet<File>(list.filterIsInstance<File>())
            if (deleted.isNotEmpty()) {
                val iterator = b.H.iterator()
                while (iterator.hasNext()) {
                    if (deleted.contains(iterator.next().file)) iterator.remove()
                }
                b.submitList(b.D(b.H))
                b.I.removeAll(deleted)
                b.D?.b(b.H.size)
            }
            b.F()
            return
        }
        (list.firstOrNull() as? File)?.let { b.H(it) }
        Clipboards.f4922a.clear()
        Files.g = false
        b.E()
    }
}
