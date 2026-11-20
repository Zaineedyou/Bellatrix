package com.bellatrixos.app_notepad

import android.content.DialogInterface

/** Callback dialog notepad: jalankan aksi atau delegate. */
class b(@JvmField val u: Int, @JvmField val v: Any?) : DialogInterface.OnClickListener {
    override fun onClick(dialog: DialogInterface, which: Int) {
        if (u == 0) {
            (v as? Runnable)?.run()
        } else {
            (v as? DialogInterface.OnClickListener)?.onClick(dialog, which)
        }
    }
}
