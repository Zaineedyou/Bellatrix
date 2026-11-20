package com.bellatrixos.desktop

import com.bellatrixos.widgets.FilesRecyclerView

/** Animasi tampil kembali daftar desktop. */
class c(filesRecyclerView: FilesRecyclerView, i2: Int) : Runnable {
    @JvmField val u: Int = i2
    @JvmField val v: FilesRecyclerView = filesRecyclerView

    override fun run() {
        // Kasus 1 (selection overlay) memerlukan field C1/o1/r1/x1/y1 yang
        // belum tersedia pada FilesRecyclerView versi Kotlin; cukup tampilkan alpha.
        v.animate().alpha(1.0f).setDuration(58L).start()
    }
}
