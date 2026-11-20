package com.bellatrixos.windows.files.operations

import java.io.File

/** Runnable penghapus banyak berkas. */
class g(@JvmField val v: FileOperations, @JvmField val w: List<*>, @JvmField val u: Int) : Runnable {
    override fun run() {
        if (u != 0) {
            v.b()
            if (w.isEmpty()) {
                v.f4925d?.b()
            } else {
                v.c?.a(w)
            }
            return
        }
        @Suppress("UNCHECKED_CAST")
        val files = w as? List<File> ?: w.filterIsInstance<File>()
        var total = 0
        for (file in files) {
            try {
                total += Utils.a(file)
            } catch (ignored: Exception) {
            }
        }
        val counts = IntArray(1)
        val progress = IntArray(1)
        val deleted = ArrayList<File>()
        for (file in files) {
            if (v.a(file, counts, total, progress)) deleted.add(file)
        }
        v.b.post(g(v, deleted, 1))
    }
}
