package com.bellatrixos.app_browser.utils

import android.view.View
import android.widget.EditText
import com.bellatrixos.R
import kotlin.math.max
import kotlin.math.min

/** Memvalidasi nilai zoom saat EditText menerima atau kehilangan fokus. */
class d(
    @JvmField val f4652a: IntArray,
    @JvmField val b: IntArray,
    @JvmField val c: Scaling.OnScaleChangedListener?
) : View.OnFocusChangeListener {
    override fun onFocusChange(view: View, focused: Boolean) {
        val edit = view as EditText
        if (focused) {
            val width = edit.width
            f4652a[0] = width
            edit.minWidth = width
            edit.maxWidth = width
            edit.setText(edit.text.toString().replace("%", "").trim())
            return
        }
        val parsed = try {
            edit.text.toString().trim().toInt()
        } catch (_: NumberFormatException) {
            b[0]
        }
        val value = max(10, min(parsed, 300))
        b[0] = value
        c?.k(value)
        edit.setText(edit.context.getString(R.string.browser_zoom_value_sm, value))
        edit.minWidth = 0
        edit.maxWidth = Int.MAX_VALUE
    }
}
