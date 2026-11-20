package com.bellatrixos

import android.view.View
import com.bellatrixos.app_browser.utils.Clipboard
import com.bellatrixos.taskbar.TaskBarAdapter
import com.bellatrixos.widgets.UINotification
import com.bellatrixos.widgets.UIToast
import com.bellatrixos.windows.Windows

/** Runnable utilitas dari Activity utama. */
class h(@JvmField val u: Int) : Runnable {
    override fun run() {
        when (u) {
            0 -> {
                val tag = TaskBarAdapter.J
                val manager = Windows.f4881d ?: return
                var found: UIFragment? = null
                for (fragment in manager.fragments) {
                    val name = fragment?.tag ?: continue
                    if (name.startsWith(tag) && fragment is UIFragment) found = fragment
                }
                found?.contentView?.let { view: View ->
                    view.bringToFront()
                    Windows.f4882e = found
                }
            }
            1 -> Clipboard.f4649a = false
            2 -> UINotification.b?.takeIf { it.isShowing }?.dismiss()
            else -> UIToast.b?.takeIf { it.isShowing }?.dismiss()
        }
    }
}
