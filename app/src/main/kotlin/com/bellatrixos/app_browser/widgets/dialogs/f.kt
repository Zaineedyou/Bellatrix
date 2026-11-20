package com.bellatrixos.app_browser.widgets.dialogs

import android.widget.EditText

/** Runnable untuk mengatur fokus/seleksi nama berkas. */
class f(@JvmField val v: EditText, @JvmField val u: Int) : Runnable {
    override fun run() {
        if (u == 0) {
            v.requestFocus()
            return
        }
        val text = v.text.toString()
        var end = text.lastIndexOf('.')
        if (end <= 0) end = text.length
        v.setSelection(0, end)
    }
}
