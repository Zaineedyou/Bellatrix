package com.bellatrixos.app_browser.widgets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.bellatrixos.R
import com.bellatrixos.app_browser.UIBrowser
import com.bellatrixos.app_browser.Extensions
import com.bellatrixos.app_browser.UIBrowserTab
import com.bellatrixos.app_browser.utils.Actions
import com.bellatrixos.windows.Windows
import org.mozilla.geckoview.GeckoSession

/** Pengelola tab pada UIBrowser. */
class Tabs(@JvmField val f4655a: android.content.Context, @JvmField val b: UIBrowser) {

    /**
     * Opens [str] in a new tab. With [ready] the tab loads nothing itself and hands its session over instead, for
     * a page an extension opens: Gecko loads that one into the session.
     */
    fun a(str: String, ready: ((GeckoSession) -> Unit)? = null) {
        val tab = UIBrowserTab()
        val fm: FragmentManager = b.childFragmentManager
        val tx: FragmentTransaction = fm.beginTransaction()
        @Suppress("UNCHECKED_CAST")
        val list = b.c1 as ArrayList<UIBrowserTab>
        tx.add(R.id.tabContainer, tab, "tab_${list.size}")
        Windows.i(fm, tx)
        list.add(tab)

        val container = b.x0 ?: return
        val item = LayoutInflater.from(b.requireContext())
            .inflate(R.layout.app_browser_tab_item, container, false)
        val title = item.findViewById<TextView>(R.id.tabTitle)
        val close = item.findViewById<ImageButton>(R.id.btn_close_tab)
        title.text = f4655a.getString(R.string.browser_tab_new)

        item.setOnClickListener { b(tab, item) }
        close.setOnClickListener {
            val fm2 = b.childFragmentManager
            val tx2 = fm2.beginTransaction()
            tx2.remove(tab)
            tx2.commitAllowingStateLoss()
            @Suppress("UNCHECKED_CAST")
            val list2 = b.c1 as ArrayList<UIBrowserTab>
            list2.remove(tab)
            b.x0?.removeView(item)
            Actions.a(b.H0, b.x0)
            if (list2.isNotEmpty()) {
                val next = list2[list2.size - 1]
                val strip = b.x0
                val nextView = strip?.getChildAt((strip.childCount - 1).coerceAtLeast(0))
                if (nextView != null) b(next, nextView) else b(next, item)
            } else {
                b.z0 = null
                b.I0 = null
            }
        }

        container.addView(item)
        tab.t0 = item
        tab.z0 = item.findViewById(R.id.spinner)
        tab.x0 = item.findViewById(R.id.tabTitle)
        Actions.a(b.H0, b.x0)
        tab.v0 = Runnable {
            if (ready == null) tab.y0?.loadUrl(str) else tab.y0?.session?.let(ready)
            b(tab, item)
        }
    }

    fun b(tab: UIBrowserTab?, view: View?) {
        val browser = b
        if (tab == null || view == null) return
        if (!browser.isAdded || !tab.isAdded) return
        val current = browser.z0
        current?.y0?.session?.let { Extensions.setTabActive(it, false) }
        tab.y0?.session?.let { Extensions.setTabActive(it, true) }
        if (current != null) {
            val tx = browser.childFragmentManager.beginTransaction()
            tx.hide(current)
            tx.commitAllowingStateLoss()
        }
        val tx2 = browser.childFragmentManager.beginTransaction()
        tx2.show(tab)
        tx2.commitAllowingStateLoss()
        browser.z0 = tab
        browser.I0?.isSelected = false
        view.isSelected = true
        browser.I0 = view

        val web = tab.y0
        val url = web?.url
        if (url != null) {
            browser.C0?.setAddress(url)
        }
        browser.s0(tab, tab.B0)

        val active = browser.z0 ?: return
        val activeWeb = active.y0
        browser.K0?.isEnabled = activeWeb?.canGoBack() == true
        browser.L0?.isEnabled = activeWeb?.canGoForward() == true
    }
}
