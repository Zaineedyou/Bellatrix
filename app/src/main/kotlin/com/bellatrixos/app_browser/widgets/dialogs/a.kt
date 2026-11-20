package com.bellatrixos.app_browser.widgets.dialogs

import android.view.View
import com.bellatrixos.app_browser.widgets.AddressBarView
import com.bellatrixos.app_settings.UINavigation
import com.bellatrixos.startmenu.StartMenuViews
import com.bellatrixos.taskbar.center_action.ActionCenter
import com.bellatrixos.taskbar.center_more_icons.DialogMoreIcons
import com.bellatrixos.widgets.UINavigationBar

/** Klik tombol pada dialog/popup browser dan beberapa widget lintas modul. */
class a(@JvmField val u: Int, @JvmField val v: Any?) : View.OnClickListener {
    override fun onClick(view: View) {
        when (u) {
            0 -> (v as? DialogInstaller)?.f4656a?.dismiss()
            1 -> (v as? UIDownloads)?.dismiss()
            2 -> {
                val startMenuViews = v as? StartMenuViews ?: return
                startMenuViews.f4793f.setText("")
                startMenuViews.j.visibility = View.GONE
            }
            3 -> (v as? ActionCenter)?.dismiss()
            4 -> {
                val navBar = v as? UINavigationBar ?: return
                if (!navBar.w) {
                    navBar.d()
                } else {
                    navBar.c(false)
                }
            }
            5 -> (v as? Runnable)?.run()
            6 -> {
                val addressBar = v as? AddressBarView ?: return
                if (addressBar.hasFocus() && !addressBar.z) {
                    addressBar.setText(addressBar.y)
                    addressBar.setSelection(0, addressBar.text?.length ?: 0)
                    addressBar.z = true
                }
            }
            7 -> (v as? UINavigation.OnNavItemSelected)?.a(5)
            else -> (v as? DialogMoreIcons)?.dismiss()
        }
    }
}
