package com.bellatrixos.app_settings

import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.constraintlayout.widget.ConstraintLayout

/** Mengatur tampilan panel pengaturan. */
object UISettingsManager {
    @JvmField var f4714a: Int = -1
    @JvmField var b: Int = 0

    @JvmStatic
    fun a(list: LinearLayout, scroll: ScrollView, button: ImageButton) {
        scroll.visibility = android.view.View.GONE
        scroll.translationZ = -1.0f
        button.visibility = android.view.View.VISIBLE
        val params = list.layoutParams as ConstraintLayout.LayoutParams
        params.horizontalWeight = 1.0f
        list.layoutParams = params
        val scrollParams: ViewGroup.LayoutParams = scroll.layoutParams
        scrollParams.width = b
        scroll.layoutParams = scrollParams
    }
}
