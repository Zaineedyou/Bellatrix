package com.bellatrixos.windows.files.operations

import android.widget.Toast
import com.bellatrixos.R
import java.io.File

/** Runnable sintetis yang mengakhiri operasi dan memanggil callback. */
class f(
    @JvmField val v: FileOperations,
    @JvmField val w: Boolean,
    @JvmField val x: File,
    @JvmField val u: Int
) : Runnable {
    override fun run() {
        v.b()
        when (u) {
            1 -> {
                if (w) v.c?.a(listOf(x)) else v.f4925d?.b()
            }
            else -> {
                if (w) {
                    v.c?.a(listOf(x))
                } else {
                    val message = if (u == 0) R.string.toast_folder_copy_error else R.string.toast_file_failed_copy
                    Toast.makeText(v.f4924a, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
