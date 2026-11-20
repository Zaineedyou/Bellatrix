package com.bellatrixos.desktop

import com.bellatrixos.windows.files.operations.FileOperations

/** Hapus item desktop setelah operasi berkas selesai atau gagal. */
class e(
    @JvmField val f4761a: DesktopAdapter,
    @JvmField val b: DesktopItem
) : FileOperations.TaskSuccessListener, FileOperations.TaskFailedListener {
    override fun a(list: List<Any?>) {
        f4761a.C(b.a() ?: "")
    }

    override fun b() {
        f4761a.C(b.a() ?: "")
    }
}
