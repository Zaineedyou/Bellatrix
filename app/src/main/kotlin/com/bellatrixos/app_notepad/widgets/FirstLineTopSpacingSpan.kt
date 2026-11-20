package com.bellatrixos.app_notepad.widgets

import android.graphics.Paint
import android.text.style.LineHeightSpan

/** Menjaga tinggi baris pertama teks notepad. */
class FirstLineTopSpacingSpan : LineHeightSpan {
    override fun chooseHeight(
        text: CharSequence,
        start: Int,
        end: Int,
        spanstartv: Int,
        lineHeight: Int,
        fm: Paint.FontMetricsInt
    ) {
        if (start == 0) {
            fm.ascent = fm.ascent
            fm.top = fm.top
        }
    }
}
