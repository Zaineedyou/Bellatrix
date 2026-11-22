package com.bellatrixos.app_browser

import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.bellatrixos.variables.Global
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import kotlin.math.round

/** Konfigurasi awal WebView yang dipakai browser dan web app. */
class UIWebMount {
    fun interface StringCallback {
        fun a(str: String)
    }

    companion object {
        @JvmStatic
        fun a(webView: WebView, installedApp: InstalledApp?) {
            val base = Global.f4838e
            val scale: Int
            if (installedApp != null && installedApp.e() != base) {
                scale = round(installedApp.e() * round(100.0f / base)).toInt()
            } else {
                if (base <= 0.8f) return
                scale = round(100.0f / base).toInt()
            }
            webView.setInitialScale(scale)
        }

        @JvmStatic
        fun b(webView: WebView, z: Boolean, installedApp: InstalledApp?) {
            val settings: WebSettings = webView.settings
            settings.userAgentString = WebSettings.getDefaultUserAgent(webView.context)
                .replace("Android", "X11; Linux x86_64")
                .replace("; Mobile", "")
                .replace("Mobile Safari", "Safari")
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            // File origins must not reach other origins (or the app's own files): a page's script may read any file
            // it can name once either of these is on, including content the WebView shares with the app.
            settings.setAllowUniversalAccessFromFileURLs(false)
            settings.setAllowFileAccessFromFileURLs(false)
            if (Build.VERSION.SDK_INT >= 29) {
                settings.setForceDark(WebSettings.FORCE_DARK_AUTO)
            }
            settings.setBuiltInZoomControls(true)
            settings.setSupportZoom(true)
            settings.displayZoomControls = false
            webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
            if (z) settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            CookieManager.getInstance().setAcceptCookie(true)
            a(webView, installedApp)
        }
    }
}
