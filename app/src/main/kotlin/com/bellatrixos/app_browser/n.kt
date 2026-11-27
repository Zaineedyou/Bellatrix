package com.bellatrixos.app_browser

import android.content.Context
import com.bellatrixos.interfaces.MainActivityIn

/** Pembuka halaman hasil notifikasi pemasangan aplikasi. */
class n(@JvmField val v: Any?, @JvmField val u: Int, @JvmField val w: Any?) : Runnable {

    override fun run() {
        // u == 1: buka halaman dari notifikasi hasil pemasangan aplikasi.
        val browser = v as? UIBrowser ?: return
        val url = w as? String ?: return
        val context: Context? = browser.context
        val activity = if (context is MainActivityIn) {
            context
        } else {
            browser.activity as? MainActivityIn
        }
        activity?.c0(url, url)
    }
}
