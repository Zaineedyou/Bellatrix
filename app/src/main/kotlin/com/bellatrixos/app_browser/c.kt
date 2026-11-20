package com.bellatrixos.app_browser

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.webkit.WebView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.appcompat.app.AlertDialog
import com.bellatrixos.R
import com.bellatrixos.app_browser.files.Files
import com.bellatrixos.app_browser.utils.Scaling
import com.bellatrixos.app_browser.widgets.dialogs.DialogSettings
import com.bellatrixos.windows.drivers.Printer

/** Callback menu pengaturan dan hasil pemilih berkas pada UIBrowser. */
class c(@JvmField val u: UIBrowser) :
    ActivityResultCallback<ActivityResult>,
    DialogSettings.OnMenuItemClickListener {

    override fun a(i2: Int, i3: Int) {
        val browser = u
        if (i2 == 1) {
            if (browser.q0()) {
                val web = browser.z0?.y0 ?: return
                Scaling.c(web, browser.B0, i3)
            }
            return
        }
        if (i2 == 2) {
            if (browser.q0()) {
                val context = browser.D0?.f() ?: return
                val web = browser.z0?.y0 ?: return
                Printer.d(context, web)
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
            else -> Unit
        }
    }

    private fun showAbout(browser: UIBrowser) {
        val context: Context = browser.u0 ?: return
        val info = if (Build.VERSION.SDK_INT >= 26) {
            val pkg: PackageInfo? = WebView.getCurrentWebViewPackage()
            if (pkg != null) {
                val builder = StringBuilder()
                    .append("Package: ").append(pkg.packageName).append("\n")
                    .append("Version name: ").append(pkg.versionName).append("\n")
                if (Build.VERSION.SDK_INT >= 28) {
                    builder.append("Version code: ").append(pkg.longVersionCode).append("\n")
                }
                builder.toString()
            } else {
                "Chromium based browser."
            }
        } else {
            ""
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.browser_ct_menu_about))
            .setMessage(info)
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
