package com.bellatrixos.windows.files.operations

import android.widget.Toast
import com.bellatrixos.R

/** Runnable sintetis untuk notifikasi kegagalan operasi berkas. */
class d(@JvmField val u: Int, @JvmField val v: FileOperations) : Runnable {
    override fun run() {
        v.b()
        when (u) {
            0, 3 -> Toast.makeText(v.f4924a, R.string.toast_file_copy_error, Toast.LENGTH_SHORT).show()
            1 -> Toast.makeText(v.f4924a, R.string.toast_folder_copy_error, Toast.LENGTH_SHORT).show()
            2 -> v.f4925d?.b()
            else -> Toast.makeText(v.f4924a, R.string.toast_file_copy_error, Toast.LENGTH_SHORT).show()
        }
    }
}
