package com.bellatrixos.app_browser

import android.view.View
import com.bellatrixos.AppExecutors
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize

/** Callback fokus dan perubahan ukuran jendela UIBrowser. */
class b(@JvmField val f4643a: UIBrowser, @JvmField val b: View) :
    Window.OnResizeStateChangeListener,
    FloatingLayout.OnAppFocusListener {

    override fun a() {
        val browser = f4643a
        b.bringToFront()
        browser.D0?.d(browser)
        browser.y0?.setFocusState(true)
        browser.Y0 = true
    }

    override fun b(i2: Int) {
        val browser = f4643a
        val floating = browser.y0 ?: return
        WindowSize.d(b, floating, browser.d1)
        WindowSize.f(floating, null, i2)
        WindowSize.g(i2, "UIBrowser", browser.B0)
        AppExecutors.c().postDelayed({
            val titleBar = browser.A0
            val closeBtn = browser.R0
            val tabsScroll = browser.H0
            if (titleBar != null && closeBtn != null && tabsScroll != null) {
                tabsScroll.setMaxWidth(titleBar.width - (closeBtn.width * 4))
            }
        }, 400L)
    }
}
