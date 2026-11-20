package com.bellatrixos.app_settings.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageButton

/** Memvalidasi nilai skala 45–135 sebelum tombol diaktifkan. */
object Scale {
    @JvmStatic
    fun a(button: ImageButton): TextWatcher {
        button.isEnabled = false
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(editable: Editable?) {
                val value = editable.toString().trim()
                val valid = try {
                    val scale = value.toFloat()
                    scale in 45.0f..135.0f
                } catch (_: NumberFormatException) {
                    false
                }
                button.isEnabled = valid
                button.alpha = if (valid) 1.0f else 0.6f
            }
        }
    }
}
