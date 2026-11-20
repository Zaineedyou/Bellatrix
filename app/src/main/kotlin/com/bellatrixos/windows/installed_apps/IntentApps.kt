package com.bellatrixos.windows.installed_apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.net.Uri
import android.util.Log
import com.bellatrixos.R
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.startmenu.StartMenuItem
import com.bellatrixos.widgets.UINotification

/** Membuka aplikasi, web app, atau URL dari entri installed apps. */
object IntentApps {
    private const val TAG = "IntentApps"

    @JvmStatic
    fun a(str: String?): String? {
        if (str == null) return null
        val trimmed = str.trim()
        if (trimmed.isEmpty()) return null
        val scheme = Uri.parse(trimmed).scheme
        if (scheme.equals("javascript", ignoreCase = true)) return null
        return scheme?.let { trimmed } ?: "https://$trimmed"
    }

    @JvmStatic
    fun b(
        context: Context,
        str: String,
        startMenuItem: StartMenuItem?,
        mainActivityIn: MainActivityIn
    ) {
        if (AppsDatabase.b.containsKey(str)) {
            mainActivityIn.c0(str, null)
            return
        }
        var url: String? = null
        if (InstalledAppsDB.b.containsKey(str)) {
            InstalledAppsDB.c(str)?.let { app ->
                url = a(if (app.f() == str) str else app.f())
            }
        }
        if (url != null) {
            mainActivityIn.c0(str, url)
            return
        }
        val packageManager = context.packageManager
        try {
            var webapkUrl: String? = null
            if (str.startsWith("org.chromium.webapk.")) {
                try {
                    webapkUrl = packageManager.getApplicationInfo(str, 128).metaData
                        ?.getString("org.chromium.webapk.shell_apk.startUrl")
                } catch (ignored: Exception) {
                }
            }
            if (webapkUrl != null) {
                mainActivityIn.c0(str, webapkUrl)
                return
            }
            val handle: UserHandle? = startMenuItem?.b
            val component: ComponentName? = startMenuItem?.f4786a
            if (handle != null && component != null) {
                val launcher = context.getSystemService("launcherapps") as? LauncherApps
                launcher?.startMainActivity(component, handle, null, null)
                return
            }
            val intent = packageManager.getLaunchIntentForPackage(str)
            if (intent != null) {
                context.startActivity(intent)
            } else {
                UINotification.a(
                    context,
                    context.getString(R.string.not_found_app),
                    context.getString(R.string.st_menu_apps_title)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "cannot launch $str", e)
            UINotification.a(
                context,
                context.getString(R.string.not_found_app),
                context.getString(R.string.st_menu_apps_title),
            )
        }
    }
}
