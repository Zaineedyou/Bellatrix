package com.bellatrixos.app_browser.utils

import android.view.View
import android.widget.LinearLayout
import com.bellatrixos.app_browser.Const
import com.bellatrixos.widgets.MaxWidthHorizontalScrollView

/** Menyesuaikan lebar tombol akses cepat agar muat. */
object Actions {
    @JvmStatic
    fun a(scroll: MaxWidthHorizontalScrollView?, row: LinearLayout?) {
        if (row == null || scroll == null) return
        val count = row.childCount
        if (count == 0 || scroll.getMaxWidth() <= 0) return
        val maxWidth = scroll.getMaxWidth()
        var width = Const.b
        val available = maxWidth - Const.c * count
        if (width * count > available) {
            width = available / count
            if (width < Const.f4637a) width = Const.f4637a
        }
        for (i in 0 until count) {
            val child: View = row.getChildAt(i)
            val params = child.layoutParams as LinearLayout.LayoutParams
            params.width = width
            child.layoutParams = params
        }
        scroll.setFillViewport(width == Const.f4637a && width * count > maxWidth)
    }
}
