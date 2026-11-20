package com.bellatrixos.app_browser.utils

import android.view.KeyEvent
import android.widget.TextView

/** Menghapus fokus saat aksi "done" ditekan. */
class e : TextView.OnEditorActionListener {
    override fun onEditorAction(view: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId != 6) return false
        view.clearFocus()
        return true
    }
}
