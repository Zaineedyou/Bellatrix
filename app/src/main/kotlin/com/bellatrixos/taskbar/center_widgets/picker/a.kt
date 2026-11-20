package com.bellatrixos.taskbar.center_widgets.picker

import android.view.View
import com.bellatrixos.app_settings.UINavigation
import com.bellatrixos.app_settings.UINavigator

/** OnClickListener sintetis untuk daftar widget dan navigasi pengaturan. */
class a(
    @JvmField val v: Int,
    @JvmField val u: Int,
    @JvmField val w: Any
) : View.OnClickListener {
    override fun onClick(view: View) {
        when (u) {
            0 -> (w as? WidgetPickerAdapter)?.A?.c(v)
            1 -> (w as? UINavigation.OnNavItemSelected)?.a(v)
            else -> (w as? UINavigator.ClickAction)?.d(v / 10)
        }
    }
}
