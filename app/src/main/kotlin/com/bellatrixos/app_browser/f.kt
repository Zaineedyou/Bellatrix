package com.bellatrixos.app_browser

import android.webkit.WebView
import androidx.core.util.Supplier
import com.bellatrixos.app_browser.utils.Suggestion
import com.bellatrixos.app_browser.widgets.ShortcutWebView
import com.bellatrixos.app_browser.widgets.dialogs.UIDownloads

/** Callback saran alamat, unduhan, dan pemasok WebView aktif. */
class f(@JvmField val v: Any?, @JvmField val u: Int) :
    UIWebMount.StringCallback,
    Suggestion.StringCallback,
    UIDownloads.Callback,
    Supplier<Any?> {

    override fun a(str: String) {
        val target = when (val owner = v) {
            is UIBrowserTab -> owner.y0
            is ShortcutWebView -> owner
            else -> null
        }
        target?.loadUrl(str)
    }

    /** Perubahan ikon unduhan; port Kotlin UIBrowser tidak menyimpan ikon tersebut. */
    override fun b(i2: Int) = Unit

    override fun get(): Any? = when (val owner = v) {
        is UIBrowserTab -> owner.y0
        is WebView -> owner
        else -> null
    }
}
