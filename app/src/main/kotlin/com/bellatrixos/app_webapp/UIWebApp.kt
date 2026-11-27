package com.bellatrixos.app_webapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.util.Supplier
import com.bellatrixos.R
import com.bellatrixos.UIFragment
import com.bellatrixos.app_browser.GeckoEngine
import com.bellatrixos.app_browser.GeckoPopup
import com.bellatrixos.app_browser.GeckoPrompts
import com.bellatrixos.app_browser.SitePermissionDelegate
import com.bellatrixos.app_browser.GeckoTabView
import com.bellatrixos.app_browser.files.Downloader
import com.bellatrixos.app_browser.files.Files
import com.bellatrixos.app_browser.widgets.dialogs.FinderTarget
import com.bellatrixos.app_browser.widgets.dialogs.UIDownloads
import com.bellatrixos.app_browser.widgets.dialogs.UIFinder
import com.bellatrixos.app_browser.widgets.dialogs.asFinderTarget
import com.bellatrixos.interfaces.BackPressHandler
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.widgets.UINotification
import com.bellatrixos.widgets.UITitleBarApp
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize
import com.bellatrixos.windows.icons.IconEngine
import com.bellatrixos.windows.installed_apps.AppsDatabase
import com.bellatrixos.windows.installed_apps.InstalledAppsDB
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebRequestError

/** Aplikasi web yang berjalan di dalam sebuah jendela floating, ditampilkan oleh mesin Firefox (GeckoView). */
class UIWebApp : UIFragment(), BackPressHandler {
    lateinit var A0: ImageView
    lateinit var B0: FrameLayout
    lateinit var C0: UITitleBarApp
    lateinit var D0: FloatingLayout

    // Nullable sesuai Java: aplikasi bisa tidak ditemukan di database.
    @JvmField var E0: InstalledApp? = null

    // Helper app_webapp/c.kt dan e.kt mengakses F0 langsung (non-null).
    lateinit var F0: MainActivityIn
    lateinit var H0: FrameLayout
    lateinit var J0: ActivityResultLauncher<Intent>
    lateinit var Q0: UIFinder
    lateinit var R0: UIDownloads
    @JvmField var S0: UISettingsMenu? = null
    @JvmField var u0: String? = null
    lateinit var v0: View
    lateinit var w0: GeckoTabView
    lateinit var x0: Context
    @JvmField var y0: String? = null
    lateinit var z0: TextView
    @JvmField var K0: String? = ""
    @JvmField var L0: Boolean = true
    @JvmField var M0: Bitmap? = null
    @JvmField var N0: Boolean = false
    @JvmField var O0: Boolean = false
    @JvmField var P0: Int = 0
    @JvmField val T0: Window = Window(0)


    private var androidPermissions: GeckoSession.PermissionDelegate.Callback? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val callback = androidPermissions ?: return@registerForActivityResult
            androidPermissions = null
            // Gecko asked for them together; one refusal is a refusal.
            if (result.values.all { it }) callback.grant() else callback.reject()
        }

    /** Whether the window shows the page full screen (a video, F11); Back and F11 leave it. */
    private var fullScreen = false

    /** The file chooser the page opened, answered when the picker closes. */
    private var filePrompt: Pair<GeckoSession.PromptDelegate.FilePrompt, GeckoResult<GeckoSession.PromptDelegate.PromptResponse>>? = null

    override fun onDestroy() {
        super.onDestroy()
        S0?.dismiss()
        S0 = null
        filePrompt?.let { (prompt, answer) -> answer.complete(prompt.dismiss()) }
        filePrompt = null
        if (fullScreen && ::F0.isInitialized) F0.Z(true)
        if (::w0.isInitialized) {
            w0.stopLoading()
            w0.destroy()
        }
        if (::R0.isInitialized) {
            R0.dismiss()
            R0.f4665a = null
        }
        if (::Q0.isInitialized) Q0.a()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // The settings popup is anchored to the fragment's view; don't leave it over a destroyed window.
        S0?.dismiss()
        S0 = null
        if (::F0.isInitialized) {
            val str = K0
            val bitmap = M0
            if ((str == null || str.trim().isEmpty()) && bitmap != null) {
                F0.b(y0 ?: "", BitmapDrawable(x0.applicationContext.resources, bitmap))
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::w0.isInitialized) w0.setActive(false)
    }

    override fun onResume() {
        super.onResume()
        if (::w0.isInitialized) w0.setActive(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        WindowSize.e(view, D0, T0, C0, F0, this)
        IconEngine.b(x0).c(y0 ?: "", c(this))
        s0 = 1
        C0.setLoading(true)

        val installedApp = InstalledAppsDB.c(y0 ?: "")
        E0 = installedApp
        val window = T0
        val strA: String?
        if (installedApp != null) {
            C0.setTitle(installedApp.b())
            window.a(installedApp.d())
            if (installedApp.f() != y0) u0 = installedApp.f()
            strA = installedApp.a()
        } else {
            C0.setTitle(AppsDatabase.a(y0 ?: ""))
            strA = "Already has"
        }
        K0 = strA

        v0.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                v0.viewTreeObserver.removeOnGlobalLayoutListener(this)
                // Gecko has one text size for the whole engine: it follows this app's saved zoom.
                GeckoEngine.zoom(x0, E0?.g() ?: 100)
                R0 = UIDownloads(x0)
                w0.bypassCache = E0?.h() == false
                wire(w0)
                Q0 = UIFinder(Supplier<FinderTarget?> { w0.asFinderTarget() })
                Q0.f4670d = v0.findViewById<FrameLayout>(R.id.overlay_container)
                u0?.let { url -> w0.loadUrl(url) }
            }
        })

        window.f4870e = e(this, view)
        C0.setResizeBtnListener { onTitleBarButton(0) }
        C0.setReloadBtnListener { onTitleBarButton(1) }
        C0.setSettingsBtnListener { onTitleBarButton(2) }
        C0.setCloseBtnListener { onTitleBarButton(3) }
        C0.setMinimizeBtnListener { onTitleBarButton(4) }
        z0.setOnClickListener { onTitleBarButton(5) }
        D0.setOnFocusListener(e(this, view))

        val resultCallback = c(this)
        J0 = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult -> resultCallback.onActivityResult(result) }

        if (window.c != 0) {
            WindowSize.d(view, D0, window)
            WindowSize.f(D0, C0, 1)
        }
        WindowSize.b(view, D0)
    }

    /** Hooks the page's events to this window: its loading, errors, downloads, dialogs and full screen. */
    private fun wire(web: GeckoTabView) {
        web.onProgress = { progress -> if (progress > 60 && !N0) q0(false) }
        web.onPageStart = { C0.setLoading(true) }
        web.onPageStop = { C0.setLoading(false) }
        // No connection to the site (unknown host, refused, timed out, offline) shows the retry splash.
        web.onLoadError = { code ->
            if (code == WebRequestError.ERROR_UNKNOWN_HOST ||
                code == WebRequestError.ERROR_CONNECTION_REFUSED ||
                code == WebRequestError.ERROR_NET_TIMEOUT ||
                code == WebRequestError.ERROR_OFFLINE
            ) {
                N0 = true
                q0(true)
            }
        }
        web.onFullScreen = { on -> setFullScreen(on) }
        // Sign-in popups (window.open) get a window of their own so they can answer the page; other links open here.
        web.onNewWindow = { uri ->
            if (GeckoPopup.wantsPopup(uri)) {
                GeckoPopup.open(x0)
            } else {
                web.loadUrl(uri)
                null
            }
        }
        web.onDownload = { response ->
            Downloader.handle(x0, response, R0, C0.getBtnMore(), viewLifecycleOwner)
        }
        GeckoPrompts.install(web.session, { if (isAdded) x0 else null }) { prompt -> chooseFiles(prompt) }
        web.session.permissionDelegate =
            SitePermissionDelegate({ if (isAdded) x0 else null }) { permissions, callback ->
                androidPermissions = callback
                permissionLauncher.launch(permissions)
            }
    }

    private fun chooseFiles(
        prompt: GeckoSession.PromptDelegate.FilePrompt,
    ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
        // A second chooser replaces the first, which is answered with nothing.
        filePrompt?.let { (old, answer) -> answer.complete(old.dismiss()) }
        val answer = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
        filePrompt = prompt to answer
        Files.c(
            x0,
            prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE,
            prompt.mimeTypes,
            J0,
        )
        return answer
    }

    /** Answers the page's file chooser with what the picker returned (null when cancelled). */
    fun onFilesChosen(result: ActivityResult?) {
        val (prompt, answer) = filePrompt ?: return
        filePrompt = null
        val uris = Files.uris(result)
        answer.complete(
            when {
                uris == null -> prompt.dismiss()
                uris.size == 1 -> prompt.confirm(x0, uris[0])
                else -> prompt.confirm(x0, uris)
            },
        )
    }

    /**
     * The page fills the window: the title bar steps aside and the window leaves the system bars, as the
     * custom view did over the WebView. Gecko draws the page's own full screen inside its view.
     */
    private fun setFullScreen(on: Boolean) {
        if (fullScreen == on || !::w0.isInitialized) return
        fullScreen = on
        if (on && e0() != 0) l0()
        (w0.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
            params.topMargin = if (on) 0 else x0.resources.getDimensionPixelSize(R.dimen.app_titlebar_height)
            w0.layoutParams = params
        }
        C0.visibility = if (on) View.GONE else View.VISIBLE
        F0.Z(!on)
    }

    override fun a0(): FrameLayout? = if (::H0.isInitialized) H0 else null

    override fun b(): Boolean {
        if (fullScreen) {
            w0.exitFullScreen()
            setFullScreen(false)
            return true
        }
        if (!::w0.isInitialized || !w0.canGoBack()) {
            F0.A(tag ?: "")
            return true
        }
        w0.goBack()
        return true
    }

    override fun b0(): InstalledApp? = E0

    override fun e0(): Int = T0.c

    override fun f0(): Boolean = L0

    override fun h0(i2: Int, keyEvent: KeyEvent) {
        if (keyEvent.isCtrlPressed) {
            if (i2 == 34) {
                Q0.b(x0)
                return
            }
            if (i2 != 44) return
            UINotification.a(x0, x0.getString(R.string.browser_print_unavailable), null)
            return
        }
        if (keyEvent.keyCode == 141 && w0.isLaidOut) {
            if (fullScreen) w0.exitFullScreen()
            setFullScreen(!fullScreen)
        }
    }

    override fun j0(z: Boolean) {
        if (::D0.isInitialized) {
            D0.setFocusState(z)
            L0 = z
        }
    }

    override fun l0() {
        T0.a(0)
    }

    override fun m0(z: Boolean) {
        F0.Z(z)
    }

    fun p0() {
        z0.visibility = View.GONE
        N0 = false
        if (O0) {
            S0?.let { menu ->
                menu.m = false
                menu.f4735i?.let { textView ->
                    textView.text = textView.context.getString(R.string.browser_scale_info)
                }
            }
        }
        w0.reload()
        C0.setLoading(true)
        A0.foreground = null
    }

    fun q0(z: Boolean) {
        if (z) z0.visibility = View.VISIBLE
        w0.visibility = if (z) View.INVISIBLE else View.VISIBLE
        B0.visibility = if (z) View.VISIBLE else View.GONE
        C0.B.visibility = if (z) View.GONE else View.VISIBLE
        A0.foreground = if (z) ContextCompat.getDrawable(x0, R.drawable.ui_browser_ic_error) else null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null) {
            u0 = args.getString("path")
            y0 = args.getString("packageName")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.app_web, container, false)
        v0 = view
        D0 = view.findViewById(R.id.main_view_web)
        WindowSize.a(D0, T0.c)
        val context = v0.context
        x0 = context
        F0 = context as MainActivityIn
        w0 = view.findViewById(R.id.webView)
        C0 = view.findViewById(R.id.titleBar)
        B0 = view.findViewById(R.id.splash_bg)
        z0 = view.findViewById(R.id.splash_msg)
        A0 = view.findViewById(R.id.splash_img)
        H0 = view.findViewById(R.id.fullscreen_container)
        return v0
    }

    /** Tombol title bar dan pesan splash memakai satu dispatch seperti class f di Java. */
    private fun onTitleBarButton(which: Int) {
        when (which) {
            0 -> WindowSize.c(T0)
            1 -> p0()
            2 -> {
                if (S0 == null) {
                    val menu = UISettingsMenu(x0, E0)
                    S0 = menu
                    menu.f4731d = c(this)
                    menu.setOnDismissListener { C0.B.isActivated = false }
                }
                val menu = S0 ?: return
                val settingsView = C0.B
                menu.showAsDropDown(
                    settingsView,
                    -(settingsView.width * 1.6f).toInt(),
                    menu.f4732e,
                    17
                )
                C0.B.isActivated = true
            }
            3 -> F0.P(this)
            4 -> F0.A(tag ?: "")
            else -> p0()
        }
    }
}
