package com.bellatrixos.app_browser.utils

import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import com.bellatrixos.UIFragment

/** Tampilan layar penuh untuk video HTML5 pada WebView. */
object Fullscreen {
    @JvmStatic
    fun a(fragment: UIFragment, webView: WebView, view: View, arr: FloatArray) {
        if (fragment.e0() != 0) fragment.l0()
        val container = fragment.a0() ?: return
        if (view.parent === container) return
        (view.parent as? ViewGroup)?.removeView(view)
        container.addView(view, FrameLayout.LayoutParams(-1, -1))
        container.visibility = View.VISIBLE
        webView.visibility = View.GONE
        fragment.m0(false)
        fragment.b0()?.let { app ->
            val scale = app.e()
            arr[0] = scale
            if (scale != 1.0f) webView.evaluateJavascript("document.body.style.zoom = '100%';", null)
        }
    }

    @JvmStatic
    fun b(
        fragment: UIFragment,
        webView: WebView,
        view: View?,
        callback: WebChromeClient.CustomViewCallback?,
        arr: FloatArray
    ) {
        val container = fragment.a0() ?: return
        if (view != null && view.parent === container) container.removeView(view)
        container.visibility = View.GONE
        webView.visibility = View.VISIBLE
        fragment.m0(true)
        val scale = arr[0]
        if (scale != 0.0f && scale != 1.0f) {
            webView.evaluateJavascript(
                "document.body.style.zoom = '${Math.round(scale * 100.0f)}%';",
                null
            )
        }
        callback?.onCustomViewHidden()
        arr[0] = 0.0f
    }
}
