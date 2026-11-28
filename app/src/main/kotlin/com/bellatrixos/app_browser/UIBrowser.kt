package com.bellatrixos.app_browser

import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.webkit.ValueCallback
import android.widget.AdapterView
import android.widget.FilterQueryProvider
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.util.Supplier
import androidx.cursoradapter.widget.SimpleCursorAdapter
import com.bellatrixos.R
import com.bellatrixos.UIFragment
import com.bellatrixos.app_browser.utils.Clipboard
import com.bellatrixos.app_browser.utils.Suggestion
import com.bellatrixos.app_browser.widgets.AddressBarView
import com.bellatrixos.app_browser.widgets.Tabs
import com.bellatrixos.app_browser.widgets.dialogs.DialogInstaller
import com.bellatrixos.app_browser.widgets.dialogs.DialogSettings
import com.bellatrixos.app_browser.widgets.dialogs.UIDownloads
import com.bellatrixos.app_browser.widgets.dialogs.FinderTarget
import com.bellatrixos.app_browser.widgets.dialogs.UIFinder
import com.bellatrixos.app_browser.widgets.dialogs.asFinderTarget
import com.bellatrixos.interfaces.BackPressHandler
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.widgets.MaxWidthHorizontalScrollView
import com.bellatrixos.widgets.UINotification
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize
import com.bellatrixos.windows.Windows
import com.bellatrixos.windows.drivers.Display
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import java.lang.ref.WeakReference
import java.net.URLEncoder
import org.mozilla.geckoview.GeckoSession

/** Browser WebView bawaan, port lengkap dari kelas UIBrowser hasil dekompilasi. */
class UIBrowser : UIFragment(), BackPressHandler {

    @JvmField var A0: LinearLayout? = null
    @JvmField var B0: InstalledApp? = null
    @JvmField var C0: AddressBarView? = null
    @JvmField var D0: MainActivityIn? = null
    @JvmField var E0: DialogSettings? = null
    @JvmField var F0: FrameLayout? = null
    @JvmField var G0: FrameLayout? = null
    @JvmField var H0: MaxWidthHorizontalScrollView? = null
    @JvmField var I0: View? = null
    @JvmField var J0: ImageView? = null
    @JvmField var K0: ImageButton? = null
    @JvmField var L0: ImageButton? = null
    @JvmField var M0: ImageButton? = null
    @JvmField var N0: ImageButton? = null
    @JvmField var O0: ImageButton? = null

    /** Firefox's Extensions button; the popup of an extension opens under it. */
    @JvmField var extensionsButton: ImageButton? = null
    private var listTabsButton: ImageButton? = null
    @JvmField var P0: ImageButton? = null
    @JvmField var Q0: ImageButton? = null
    @JvmField var R0: ImageButton? = null
    @JvmField var S0: ImageButton? = null
    @JvmField var T0: ImageButton? = null
    @JvmField var V0: ActivityResultLauncher<Intent>? = null
    @JvmField var W0: ValueCallback<Array<Uri>>? = null
    @JvmField var a1: UIDownloads? = null
    @JvmField var b1: UIFinder? = null
    @JvmField var u0: Context? = null
    @JvmField var v0: View? = null
    @JvmField var w0: Tabs? = null
    @JvmField var x0: LinearLayout? = null
    @JvmField var y0: FloatingLayout? = null
    @JvmField var z0: UIBrowserTab? = null
    @JvmField var U0: String? = null
    @JvmField var X0: Boolean = false
    @JvmField var Y0: Boolean = true
    @JvmField var Z0: Boolean = false

    /** Whether F11 has hidden the window's title bar. */
    private var chromeHidden = false
    @JvmField val c1: ArrayList<Any?> = ArrayList()
    @JvmField val d1: Window = Window(0)

    companion object {
        private const val DEFAULT_URL = "https://www.google.com/webhp"
        private fun suggestionUri(path: String): Uri =
            Uri.parse("content://${MySuggestionProvider.AUTHORITY}/$path")
    }

    /** Callback synthetic yang menggantikan f hasil obfuskasi. */
    private inner class BrowserCallback(@JvmField val index: Int) :
        UIWebMount.StringCallback,
        Suggestion.StringCallback,
        UIDownloads.Callback,
        Supplier<Any?> {

        override fun a(str: String) {
            navigate(str)
        }

        override fun b(i2: Int) {
            if (i2 == 0) {
                val circle = J0 ?: return
                val button = T0 ?: return
                circle.post {
                    button.visibility = View.VISIBLE
                    button.scaleX = 1f
                    button.scaleY = 1f
                    button.y = 400f
                    button.alpha = 1f
                    val location = IntArray(2)
                    circle.getLocationOnScreen(location)
                    button.x = location[0] - button.width / 3.2f
                    button.animate()
                        .y(location[1] / 2f)
                        .scaleX(0.5f)
                        .scaleY(0.5f)
                        .alpha(0.4f)
                        .setDuration(600L)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction { button.visibility = View.GONE }
                        .start()
                }
                button.setImageResource(R.drawable.ui_browser_ic_downloading)
            } else if (i2 == 2) {
                X0 = true
                T0?.setImageResource(R.drawable.ui_browser_ic_downloaded)
            }
        }

        override fun get(): Any? = z0?.y0
    }

    /* Inner class UIBrowser$1 */
    inner class AnonymousClass1(@JvmField val u: View) : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            u.viewTreeObserver.removeOnGlobalLayoutListener(this)
            val context = u0 ?: return
            w0 = Tabs(context, this@UIBrowser)
            if (Const.f4637a == 0) {
                Const.f4637a = resources.getDimensionPixelSize(R.dimen.ui_84dp)
                Const.b = resources.getDimensionPixelSize(R.dimen.ui_156dp)
                Const.c = resources.getDimensionPixelSize(R.dimen.ui_4dp)
            }
            val tabs = w0 ?: return
            val back = K0 ?: return
            val forward = L0 ?: return
            val reload = M0 ?: return
            val menu = N0 ?: return
            back.setOnClickListener { toolbarNav(0) }
            forward.setOnClickListener { toolbarNav(1) }
            reload.setOnClickListener { toolbarNav(2) }
            menu.setOnClickListener { toolbarNav(3) }

            u.postDelayed(object : Runnable {
                var u: Int = 0

                override fun run() {
                    if (!isAdded && u < 12) {
                        u++
                        this@AnonymousClass1.u.postDelayed(this, 400L)
                        return
                    }
                    if (isAdded) {
                        H0?.setMaxWidth((A0?.width ?: 0) - (R0?.width ?: 0) * 4)
                        tabs.a(U0 ?: DEFAULT_URL)
                    }
                }
            }, 260L)

            val address = C0 ?: return
            val navigation = BrowserCallback(0)
            address.setOnEditorActionListener { _, action, keyEvent ->
                if (action != 2 && (keyEvent == null || keyEvent.keyCode != 66 ||
                        keyEvent.action != KeyEvent.ACTION_DOWN)
                ) {
                    return@setOnEditorActionListener false
                }
                val term = address.text.toString().trim()
                val url = if (Patterns.WEB_URL.matcher(term).matches()) {
                    if (term.startsWith("http://") || term.startsWith("https://")) {
                        term
                    } else {
                        "https://$term"
                    }
                } else {
                    try {
                        "https://www.google.com/search?q=" + URLEncoder.encode(term, "UTF-8")
                    } catch (_: Exception) {
                        "https://www.google.com/search?q=$term"
                    }
                }
                navigation.a(url)
                val values = ContentValues().apply {
                    put("term", term)
                    put("last_used", System.currentTimeMillis())
                }
                context.contentResolver.insert(suggestionUri("suggestions"), values)
                true
            }

            val addressBar = C0 ?: return
            val suggestion = BrowserCallback(1)
            val columns = intArrayOf(android.R.id.text1)
            val adapter = SimpleCursorAdapter(
                context,
                R.layout.app_browser_suggest,
                null,
                arrayOf("suggest_text_1"),
                columns,
                0
            )
            adapter.setDropDownViewResource(R.layout.app_browser_suggest)
            adapter.setCursorToStringConverter(com.bellatrixos.n(1))
            adapter.setViewBinder(object : SimpleCursorAdapter.ViewBinder {
                override fun setViewValue(view: View, cursor: Cursor, columnIndex: Int): Boolean {
                    if (view.id == android.R.id.text1) {
                        val delete = (view.parent as? View)?.findViewById<ImageButton>(R.id.btn_delete)
                        val index = cursor.getColumnIndex("suggest_text_1")
                        if (delete != null && index >= 0) {
                            delete.setOnClickListener(
                                com.bellatrixos.app_browser.utils.i(
                                    context,
                                    cursor.getString(index),
                                    addressBar,
                                    adapter
                                )
                            )
                        }
                    }
                    return false
                }
            })
            adapter.setFilterQueryProvider(FilterQueryProvider { constraint ->
                context.contentResolver.query(
                    suggestionUri("search_suggest_query"),
                    null,
                    null,
                    arrayOf(constraint?.toString() ?: ""),
                    null
                )
            })
            addressBar.setAdapter(adapter)
            addressBar.setDropDownBackgroundResource(R.drawable.ui_browser_suggest_bg)
            addressBar.setOnItemClickListener(AdapterView.OnItemClickListener { parent, _, position, _ ->
                val cursor = parent.getItemAtPosition(position) as? Cursor ?: return@OnItemClickListener
                var text = cursor.getString(cursor.getColumnIndex("suggest_text_1"))
                if (!Patterns.WEB_URL.matcher(text).matches()) {
                    text = try {
                        "https://www.google.com/search?q=" + URLEncoder.encode(text, "UTF-8")
                    } catch (_: Exception) {
                        "https://www.google.com/search?q=$text"
                    }
                } else if (!text.startsWith("http://") && !text.startsWith("https://")) {
                    text = "https://$text"
                }
                suggestion.a(text)
            })

            val downloads = UIDownloads(context)
            a1 = downloads
            downloads.f4665a = BrowserCallback(2)
            val finder = UIFinder(Supplier<FinderTarget?> { z0?.y0?.asFinderTarget() })
            b1 = finder
            finder.f4670d = v0?.findViewById<FrameLayout>(R.id.overlay_container)

            Q0?.setOnClickListener { toolbarAction(0) }
            R0?.setOnClickListener { toolbarAction(1) }
            P0?.setOnClickListener { toolbarAction(2) }
            S0?.setOnClickListener { toolbarAction(3) }
            T0?.setOnClickListener { toolbarAction(4) }
            O0?.setOnClickListener { toolbarAction(5) }
            extensionsButton?.setOnClickListener { button -> ExtensionUi.showPanel(button.context, button) }
            listTabsButton?.setOnClickListener { showTabList() }

            if (Clipboard.b == null || Clipboard.c == null) {
                val manager = context.getSystemService("clipboard") as? ClipboardManager ?: return
                Clipboard.b = manager
                val listener = com.bellatrixos.app_browser.utils.b()
                Clipboard.c = listener
                manager.addPrimaryClipChangedListener(listener)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = z(inflater, container, savedInstanceState)

    /** Nama asli hasil dekompilasi: z(LayoutInflater, ViewGroup, Bundle). */
    fun z(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.app_browser, container, false)
        v0 = view
        contentView = view
        val floating = view.findViewById<FloatingLayout>(R.id.main_view_br)
        y0 = floating
        WindowSize.a(floating, d1.c)
        val context = view.context
        u0 = context
        D0 = context as? MainActivityIn
        P0 = view.findViewById(R.id.btn_minimize)
        Q0 = view.findViewById(R.id.btn_resize)
        R0 = view.findViewById(R.id.btn_close)
        A0 = view.findViewById(R.id.titleBar)
        x0 = view.findViewById(R.id.tabStrip)
        C0 = view.findViewById(R.id.addressBar)
        K0 = view.findViewById(R.id.btn_back)
        L0 = view.findViewById(R.id.btn_fwd)
        M0 = view.findViewById(R.id.btn_reload)
        N0 = view.findViewById(R.id.btn_menu)
        S0 = view.findViewById(R.id.btn_new_tab)
        T0 = view.findViewById(R.id.btn_downloads)
        J0 = view.findViewById(R.id.btn_downloads_circle)
        O0 = view.findViewById(R.id.btn_install_ext)
        extensionsButton = view.findViewById(R.id.btn_extensions)
        listTabsButton = view.findViewById(R.id.btn_list_tabs)
        H0 = view.findViewById(R.id.tabScrollView)
        F0 = view.findViewById(R.id.overlay_container)
        G0 = view.findViewById(R.id.fullscreen_container)
        arguments?.let { U0 = it.getString("path") }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        L(view, savedInstanceState)
    }

    /** Nama asli hasil dekompilasi: L(View, Bundle). */
    fun L(view: View, bundle: Bundle?) {
        val activity = D0 ?: (view.context as? MainActivityIn ?: return)
        val floating = y0 ?: return
        WindowSize.e(view, floating, d1, A0 ?: view, activity, this)
        B0 = Windows.d(d1, "UIBrowser")
        Extensions.browser = WeakReference(this)
        val resizeListener = com.bellatrixos.app_browser.b(this, view)
        d1.f4870e = resizeListener
        view.viewTreeObserver.addOnGlobalLayoutListener(AnonymousClass1(view))
        floating.setOnFocusListener(resizeListener)
        if (d1.c != 0) {
            WindowSize.d(view, floating, d1)
            WindowSize.f(floating, null, 1)
        }
        V0 = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            com.bellatrixos.app_browser.c(this)
        )
    }

    override fun onDestroy() {
        C()
        super.onDestroy()
    }

    /** Whether the window is on screen: not minimised and not behind another Activity (the recent apps, say). */
    fun showsPages(): Boolean = isResumed && !isHidden

    /**
     * Gecko draws into a surface of its own, so a page of a minimised or stopped window must stop drawing, and is let go
     * of by its view (see [GeckoTabView.setShown]) while the window is minimised.
     */
    private fun syncPages() {
        z0?.y0?.setShown(showsPages())
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        syncPages()
    }

    override fun onPause() {
        super.onPause()
        z0?.y0?.setActive(false)
    }

    override fun onResume() {
        super.onResume()
        syncPages()
    }

    /** Nama asli hasil dekompilasi: C(). */
    fun C() {
        if (Extensions.browser?.get() === this) Extensions.browser = null
        val settings = E0
        if (settings != null && settings.isShowing) settings.dismiss()
        val finder = b1
        if (finder != null) {
            finder.a()
            b1 = null
        }
        val downloads = a1
        if (downloads != null) {
            downloads.dismiss()
            downloads.f4665a = null
            a1 = null
        }
        val manager = Clipboard.b
        val listener = Clipboard.c
        if (manager == null || listener == null) return
        manager.removePrimaryClipChangedListener(listener)
        Clipboard.c = null
        Clipboard.b = null
    }

    override fun a0(): FrameLayout? = G0

    override fun b(): Boolean {
        val current = z0
        // A full-screen page is left first, then the history is.
        if (current != null && current.fullScreen) {
            current.y0?.session?.exitFullScreen()
            return true
        }
        val currentWeb = current?.y0
        if (current == null || currentWeb == null || !currentWeb.canGoBack()) {
            D0?.A(tag ?: "")
            return true
        }
        if (currentWeb.canGoBack()) current.y0?.goBack()
        return true
    }

    override fun b0(): InstalledApp? = B0

    override fun e0(): Int = d1.c

    override fun f0(): Boolean = Y0

    override fun h0(i2: Int, keyEvent: KeyEvent) {
        if (keyEvent.isCtrlPressed) {
            when (i2) {
                34 -> {
                    u0?.let { b1?.b(it) }
                    return
                }
                44 -> {
                    val context = u0
                    if (context != null && q0()) UINotification.a(context, "Printing isn't available in this browser yet", null)
                    return
                }
                48 -> {
                    w0?.a(DEFAULT_URL)
                    return
                }
                else -> return
            }
        }
        if (keyEvent.keyCode != 141 || z0 == null) return
        // F11: the window's title bar goes away or comes back.
        chromeHidden = !chromeHidden
        D0?.Z(!chromeHidden)
    }

    override fun j0(z: Boolean) {
        Y0 = z
        if (z) Extensions.browser = WeakReference(this)
        y0?.setFocusState(z)
    }

    /** Nama asli hasil dekompilasi: k0(String). */
    override fun k0(str: String?) {
        w0?.a(str ?: DEFAULT_URL)
        contentView?.bringToFront()
    }

    /** Opens a tab for an extension (tabs.create); [ready] gets its session, which Gecko then loads the page into. */
    fun openExtensionTab(ready: (GeckoSession) -> Unit) {
        w0?.a("about:blank", ready)
        contentView?.bringToFront()
    }

    /** Firefox's "List all tabs": a menu of the open tabs, to jump to one. */
    private fun showTabList() {
        val anchor = listTabsButton ?: return
        val context = u0 ?: return
        val open = c1.filterIsInstance<UIBrowserTab>()
        val menu = PopupMenu(context, anchor)
        open.forEachIndexed { index, tab ->
            val title = tab.x0?.text?.toString().orEmpty()
            menu.menu.add(0, index, index, title.ifBlank { context.getString(R.string.browser_tab_new) })
        }
        menu.setOnMenuItemClickListener { item ->
            open.getOrNull(item.itemId)?.let { w0?.b(it, it.t0) }
            true
        }
        menu.show()
    }

    override fun m0(z: Boolean) {
        D0?.Z(z)
    }

    override fun l0() {
        d1.a(0)
    }

    /**
     * Kompatibilitas dengan kelas Kotlin lain: memuat URL ke tab aktif.
     * Nama ini dipakai UIBrowserTab dan callback menu hasil port.
     */
    fun navigate(url: String) {
        z0?.y0?.loadUrl(url)
        C0?.setText(url)
        contentView?.bringToFront()
    }

    fun p0() {
        if (!q0()) return
        val context = u0 ?: return
        val web = z0?.y0 ?: return
        val titleBar = A0 ?: return
        val activity = D0 ?: return
        val title = web.title
        val url = web.url
        if (title == null || url == null) {
            UINotification.a(context, "Page not loaded yet", null)
            return
        }
        val dialog = DialogInstaller(context, title, url, null)
        dialog.b = com.bellatrixos.app_browser.utils.a(
            web,
            context,
            activity,
            com.bellatrixos.app_browser.e(0, this)
        )
        val popup = dialog.f4656a
        popup.contentView?.measure(0, 0)
        popup.showAsDropDown(
            titleBar,
            (titleBar.width - (popup.contentView?.measuredWidth ?: 0)) / 2,
            0
        )
        val root = popup.contentView?.rootView ?: return
        val decor = root.rootView
        val manager = root.context.getSystemService("window") as? WindowManager
        val params = decor.layoutParams as? WindowManager.LayoutParams ?: return
        params.flags = 2
        params.dimAmount = 0.5f
        if (manager != null) manager.updateViewLayout(decor, params)
    }

    fun q0(): Boolean = z0?.y0 != null

    fun s0(tab: UIBrowserTab, z: Boolean) {
        if (tab !== z0) return
        if (z) {
            M0?.setImageResource(R.drawable.app_title_ic_close_16)
            M0?.contentDescription = getString(R.string.nav_stop)
        } else {
            M0?.setImageResource(R.drawable.ui_browser_ic_reload_20)
            M0?.contentDescription = getString(R.string.nav_reload)
        }
        Z0 = false
    }

    private fun toolbarNav(index: Int) {
        when (index) {
            0 -> {
                val tab = z0 ?: return
                val web = tab.y0 ?: return
                if (web.canGoBack()) tab.y0?.goBack()
            }
            1 -> {
                val tab = z0 ?: return
                val web = tab.y0 ?: return
                if (web.canGoForward()) tab.y0?.goForward()
            }
            2 -> {
                val tab = z0 ?: return
                val web = tab.y0
                if (web != null) {
                    if (!tab.B0) web.reload() else web.stopLoading()
                }
            }
            else -> showSettings()
        }
    }

    private fun toolbarAction(index: Int) {
        when (index) {
            0 -> WindowSize.c(d1)
            1 -> D0?.P(this)
            2 -> D0?.A(tag ?: "")
            3 -> w0?.a(DEFAULT_URL)
            4 -> {
                val button = T0 ?: return
                a1?.a(button, null, 3)
                if (X0) button.setImageResource(R.drawable.ui_browser_ic_downloads)
            }
            5 -> p0()
        }
    }

    private fun showSettings() {
        if (E0 == null) {
            val context = u0 ?: return
            val settings = DialogSettings(context, B0)
            E0 = settings
            settings.f4658d = com.bellatrixos.app_browser.c(this)
        }
        val settings = E0 ?: return
        val anchor = N0 ?: return
        settings.contentView?.measure(
            View.MeasureSpec.makeMeasureSpec(0, 0),
            View.MeasureSpec.makeMeasureSpec(0, 0)
        )
        val measuredWidth = settings.contentView?.measuredWidth ?: 0
        val offset = if (Display.f4898a) {
            -(anchor.width + settings.f4657a)
        } else {
            -(measuredWidth - settings.f4657a)
        }
        settings.showAsDropDown(anchor, offset, 0)
    }
}
