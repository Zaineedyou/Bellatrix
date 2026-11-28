package com.bellatrixos.app_browser

import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.appcompat.app.AlertDialog
import com.bellatrixos.R
import com.bellatrixos.app_browser.files.Files
import com.bellatrixos.app_browser.utils.Scaling
import com.bellatrixos.app_browser.widgets.dialogs.DialogSettings
import com.bellatrixos.widgets.UINotification

/** Callback menu pengaturan dan hasil pemilih berkas pada UIBrowser. */
class c(@JvmField val u: UIBrowser) :
    ActivityResultCallback<ActivityResult>,
    DialogSettings.OnMenuItemClickListener {

    override fun a(i2: Int, i3: Int) {
        val browser = u
        if (i2 == 1) {
            if (browser.q0()) {
                val context = browser.u0 ?: return
                Scaling.d(context, browser.B0, i3)
            }
            return
        }
        if (i2 == 2) {
            if (browser.q0()) {
                val context = browser.u0 ?: return
                UINotification.a(context, "Printing isn't available in this browser yet", null)
            }
            return
        }
        if (i2 == 11) {
            if (browser.q0()) {
                browser.Z0 = true
            }
            return
        }
        when (i2) {
            4 -> browser.w0?.a("https://www.google.com/webhp")
            5 -> showAbout(browser)
            6 -> browser.p0()
            7 -> browser.w0?.a("https://translate.google.com/")
            8 -> if (browser.q0()) browser.b1?.b(browser.u0 ?: return)
            12 -> SiteSettings.show(browser.u0 ?: return, null) { }
            13 -> ExtensionUi.showManager(browser.u0 ?: return)
            else -> Unit
        }
    }

    private fun showAbout(browser: UIBrowser) {
        val context: Context = browser.u0 ?: return
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.browser_ct_menu_about))
            .setMessage("Firefox engine (GeckoView).")
            .setPositiveButton(context.getString(R.string.context_menu_close), r(0))
            .create()
            .show()
    }
    override fun onActivityResult(result: ActivityResult) {
        e(result)
    }

    fun e(result: ActivityResult) {
        val browser = u
        Files.a(result, browser.W0)
        browser.W0 = null
    }
}
