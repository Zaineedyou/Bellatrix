package com.bellatrixos.app_webapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import com.bellatrixos.app_browser.UIWebMount
import com.bellatrixos.app_browser.files.Downloader
import com.bellatrixos.app_browser.files.Files
import com.bellatrixos.app_browser.h
import com.bellatrixos.app_browser.utils.Fullscreen
import com.bellatrixos.app_browser.utils.Scaling
import com.bellatrixos.app_browser.widgets.dialogs.UIDownloads
import com.bellatrixos.app_browser.widgets.dialogs.UIFinder
import com.bellatrixos.interfaces.BackPressHandler
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.widgets.UITitleBarApp
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize
import com.bellatrixos.windows.drivers.Printer
import com.bellatrixos.windows.icons.IconEngine
import com.bellatrixos.windows.installed_apps.AppsDatabase
import com.bellatrixos.windows.installed_apps.InstalledAppsDB
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import kotlin.math.round

/** Skrip yang dipasang pada halaman untuk menangkap unduhan blob:. */
private const val DOWNLOAD_INTERCEPTOR_JS =
    "(function() {  if (window._downloadInterceptorAdded) return;  window._downloadInterceptorAdded = true;  document.addEventListener('click', function(e) {    var a = e.target.closest('a');    if (a && a.href && a.href.startsWith('blob:')) {      e.preventDefault();      var filename = a.download || ('file_' + Date.now() + '.bin');      fetch(a.href).then(r => r.blob()).then(blob => {        const reader = new FileReader();        reader.onloadend = function() {          const base64 = reader.result;          AndroidDownloader.downloadBase64File(base64, filename);        };        reader.readAsDataURL(blob);      });    }  }, true);})();"

/** Aplikasi web yang berjalan di dalam sebuah jendela floating. */
@SuppressLint("SetJavaScriptEnabled")
class UIWebApp : UIFragment(), BackPressHandler {
    lateinit var A0: ImageView
    lateinit var B0: FrameLayout
    lateinit var C0: UITitleBarApp
    lateinit var D0: FloatingLayout

    // Nullable sesuai Java: aplikasi bisa tidak ditemukan di database.
    @JvmField var E0: InstalledApp? = null

    // Helper app_webapp/c.kt dan e.kt mengakses F0 langsung (non-null).
    lateinit var F0: MainActivityIn
    @JvmField var G0: WebChromeClient? = null
    lateinit var H0: FrameLayout
    @JvmField var I0: ValueCallback<Array<Uri>>? = null
    lateinit var J0: ActivityResultLauncher<Intent>
    lateinit var Q0: UIFinder
    lateinit var R0: UIDownloads
    @JvmField var S0: UISettingsMenu? = null
    @JvmField var u0: String? = null
    lateinit var v0: View
    lateinit var w0: WebView
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

    override fun onDestroy() {
        super.onDestroy()
        S0?.dismiss()
        S0 = null
        // Java juga menulis F0 = null di sini; helper memakai F0 sebagai lateinit non-null.
        if (::w0.isInitialized) {
            w0.stopLoading()
            w0.webChromeClient = null
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
        if (::w0.isInitialized) w0.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        if (::w0.isInitialized) w0.resumeTimers()
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
                UIWebMount.b(w0, E0?.h() ?: true, E0)
                w0.webViewClient = object : WebViewClient() {
                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                        super.onPageCommitVisible(view, url)
                        if (view != null) Scaling.b(view, E0)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        C0.setLoading(false)
                        view?.evaluateJavascript(DOWNLOAD_INTERCEPTOR_JS, null)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        if (request.isForMainFrame) {
                            val code = error.errorCode
                            if (code == -2 || code == -6 || code == -8) {
                                N0 = true
                                q0(true)
                            }
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest
                    ): Boolean {
                        C0.setLoading(true)
                        return false
                    }
                }

                val chromeClient = object : WebChromeClient() {
                    var customView: View? = null
                    var customViewCallback: CustomViewCallback? = null
                    val scale = FloatArray(1)

                    override fun onHideCustomView() {
                        val view = customView ?: return
                        Fullscreen.b(this@UIWebApp, w0, view, customViewCallback, scale)
                        customView = null
                        customViewCallback = null
                    }

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        if (newProgress > 60 && !N0) q0(false)
                    }

                    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                        val str = K0
                        if (str == null || str.trim().isEmpty()) {
                            val bitmap = icon ?: return
                            val old = M0
                            if (old != null && bitmap.width * bitmap.height <= old.height * old.width) {
                                return
                            }
                            M0 = bitmap
                            val engine = IconEngine.b(x0)
                            engine.f4948a.put("$y0|${IconEngine.g}", bitmap)
                            F0.d(this@UIWebApp)
                        }
                    }

                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        if (customView != null) {
                            callback?.onCustomViewHidden()
                            return
                        }
                        customView = view
                        customViewCallback = callback
                        if (view != null) Fullscreen.a(this@UIWebApp, w0, view, scale)
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        I0?.onReceiveValue(null)
                        I0 = filePathCallback
                        if (fileChooserParams != null) Files.b(x0, fileChooserParams, J0)
                        return true
                    }
                }
                G0 = chromeClient
                w0.webChromeClient = chromeClient

                u0?.let { url ->
                    R0 = UIDownloads(x0)
                    w0.loadUrl(url)
                    Downloader.a(x0, w0, R0, C0.getBtnMore(), viewLifecycleOwner)
                }
                Q0 = UIFinder(Supplier<WebView?> { w0 })
                Q0.f4670d = v0.findViewById<FrameLayout>(R.id.overlay_container)
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

    override fun a0(): FrameLayout? = if (::H0.isInitialized) H0 else null

    override fun b(): Boolean {
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
            val context = F0.f() ?: return
            Printer.d(context, w0)
            return
        }
        if (keyEvent.keyCode == 141 && w0.isLaidOut) {
            val isFullscreen = H0.visibility == View.VISIBLE
            F0.Z(!isFullscreen)
            if (isFullscreen) {
                G0?.onHideCustomView()
                return
            }
            G0?.onShowCustomView(w0, h())
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
            val installedApp = E0
            if (installedApp == null) {
                val base = Global.f4838e
                var fRound = 0.0f
                if (P0 > 0) {
                    fRound = round(100.0f / base).toFloat() * (P0 / 100.0f)
                } else if (base <= 0.8f) {
                    fRound = 100.0f / base
                }
                w0.setInitialScale(round(fRound).toInt())
            } else {
                UIWebMount.a(w0, installedApp)
            }
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
