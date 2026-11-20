package com.bellatrixos.windows.icons

import android.graphics.Bitmap

/** Mengirim hasil decoding ikon kembali ke thread UI. */
class a(@JvmField val w: Any, @JvmField val v: Bitmap?, @JvmField val u: Int) : Runnable {
    override fun run() {
        when (u) {
            0 -> (w as? IconEngine.IconTask)?.deliver(v)
            else -> (w as? IconEngine.IconTaskExe)?.deliver(v)
        }
    }
}
