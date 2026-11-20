package com.bellatrixos.app_browser

import android.webkit.WebChromeClient

/** Callback custom view WebChrome yang tidak melakukan apa-apa. */
class h : WebChromeClient.CustomViewCallback {
    override fun onCustomViewHidden() = Unit
}
