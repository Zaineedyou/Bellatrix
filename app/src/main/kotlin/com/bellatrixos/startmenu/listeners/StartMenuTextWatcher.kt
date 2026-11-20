package com.bellatrixos.startmenu.listeners

import android.text.Editable
import android.text.TextWatcher
import android.widget.Filter

/** meneruskan perubahan teks ke Filter start menu. */
class StartMenuTextWatcher(@JvmField var u: Int, @JvmField val v: Filter) : TextWatcher {
    @JvmField var w: Filter? = null

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val filter = if (u == 0) v else w ?: return
        filter.filter(s)
    }
    override fun afterTextChanged(s: Editable?) = Unit
}
