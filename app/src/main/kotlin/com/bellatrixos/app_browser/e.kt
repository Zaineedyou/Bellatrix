package com.bellatrixos.app_browser

import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.widgets.UINotification

/** Consumer hasil pemasangan aplikasi dan hasil klik menu konteks tab. */
class e(@JvmField val u: Int, @JvmField val v: Fragment) : Consumer<Any?> {

    override fun accept(obj: Any?) {
        when (u) {
            0 -> {
                val browser = v as? UIBrowser ?: return
                val context = browser.requireContext()
                UINotification.b(
                    context,
                    context.getString(R.string.browser_app_installed_success),
                    null,
                    4800,
                    "Open",
                    n(browser, 1, obj as? String)
                )
            }
            else -> {
                val tab = v as? UIBrowserTab ?: return
                val hit = tab.D0 ?: return
                tab.a0(obj as? Int ?: 0, hit)
            }
        }
    }
}
