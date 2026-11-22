package com.bellatrixos.app_browser

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.os.SystemClock
import android.view.Choreographer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.app_browser.files.Downloader
import com.bellatrixos.app_browser.files.Files
import com.bellatrixos.app_browser.utils.Fullscreen
import com.bellatrixos.app_browser.utils.Scaling
import com.bellatrixos.app_browser.widgets.ShortcutWebView
import com.bellatrixos.app_explorer.picker.FilePicker
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** Satu tab WebView pada browser. */
class UIBrowserTab : Fragment() {
    @JvmField var A0: WebChromeClient? = null
    @JvmField var B0: Boolean = false
    @JvmField var C0: MotionEvent? = null
    @JvmField var D0: WebView.HitTestResult? = null
    @JvmField var E0: PermissionRequest? = null
    @JvmField var F0: ActivityResultLauncher<Array<String>>? = null
    @JvmField var s0: Context? = null
    @JvmField var t0: View? = null
    @JvmField var u0: String? = null
    @JvmField var v0: Runnable? = null
    @JvmField var w0: UIBrowser? = null
    @JvmField var x0: TextView? = null
    @JvmField var y0: ShortcutWebView? = null
    @JvmField var z0: android.widget.ProgressBar? = null

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    /** WebView opened by target="_blank" or window.open(), hosted in [popupDialog] until the page closes it. */
    private var popupWebView: WebView? = null
    private var popupDialog: AlertDialog? = null

    /** WebView resources the pending request asked for, so a partial Android grant can be mapped back to them. */
    @JvmField var permissionResources: List<String>? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val request = E0 ?: return@registerForActivityResult
            val wanted = permissionResources ?: request.resources.toList()
            // Grant each resource independently: refusing the microphone must not also revoke an allowed camera.
            val granted = wanted.filter { resource ->
                when (resource) {
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> permitted(Manifest.permission.CAMERA, result)
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> permitted(Manifest.permission.RECORD_AUDIO, result)
                    else -> true
                }
            }
            if (granted.isNotEmpty()) request.grant(granted.toTypedArray()) else request.deny()
            E0 = null
            permissionResources = null
        }

    private fun permitted(permission: String, result: Map<String, Boolean>): Boolean {
        val context = s0 ?: return false
        return result[permission] == true ||
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private val fileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Files.a(result, filePathCallback)
            filePathCallback = null
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.app_browser_tab, container, false)
        s0 = view.context
        y0 = view.findViewById(R.id.webView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        w0 = parentFragment as? UIBrowser ?: activity as? UIBrowser
        val web = y0 ?: return
        UIWebMount.b(web, true, w0?.b0())
        F0 = permissionLauncher

        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val url = savedInstanceState?.getString("lastUrl", "about:blank")
                if (url != null) {
                    u0 = url
                    web.loadUrl(url)
                }
                v0?.run()

                web.setOnTouchListener { _, event ->
                    C0 = event
                    false
                }
                web.setOnLongClickListener {
                    val hit = web.hitTestResult
                    val context = s0
                    if (context != null) {
                        ContextMenuBR.a(
                            context,
                            hit,
                            C0,
                            Consumer<Any?> { value -> (value as? Int)?.let { a0(it, hit) } }
                        )
                    }
                    false
                }
                web.setOnGenericMotionListener { _, event ->
                    if (event.buttonState and 2 != 0 && event.action == 11) {
                        val x = event.x
                        val y = event.y
                        val now = SystemClock.uptimeMillis()
                        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
                        val up = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, x, y, 0)
                        web.dispatchTouchEvent(down)
                        web.dispatchTouchEvent(up)
                        down.recycle()
                        up.recycle()
                        Choreographer.getInstance().postFrameCallback(j(this@UIBrowserTab, 0, event))
                    }
                    false
                }
            }
        })

        val chrome = Chrome()
        A0 = chrome
        web.webChromeClient = chrome
        web.webViewClient = Client()
        Downloader.a(s0 ?: return, web, null, null, viewLifecycleOwner)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        u0?.let { outState.putString("lastUrl", it) }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        closePopup()
        y0?.let { web ->
            web.stopLoading()
            web.webChromeClient = null
            web.destroy()
        }
        t0?.isSelected = false
        t0 = null
        w0 = null
        super.onDestroy()
    }

    /** Closes a window.open() WebView, whether the page, the dialog or fragment destruction asked for it. */
    private fun closePopup() {
        val dialog = popupDialog
        popupDialog = null
        if (dialog != null) {
            dialog.setOnDismissListener(null)
            if (dialog.isShowing) dialog.dismiss()
        }
        val web = popupWebView
        popupWebView = null
        if (web != null) {
            (web.parent as? ViewGroup)?.removeView(web)
            web.stopLoading()
            web.webChromeClient = null
            web.webViewClient = null
            web.destroy()
        }
    }

    /** Jalankan aksi menu konteks pada hasil hit-test. */
    fun a0(num: Int?, hitTestResult: WebView.HitTestResult) {
        val extra = hitTestResult.extra ?: return
        if (extra.isEmpty()) return
        when (num) {
            1, 5 -> w0?.navigate(extra)
            2 -> copyImage(extra)
            3, 4 -> {
                val context = s0
                if (context != null) {
                    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    manager?.setPrimaryClip(ClipData.newPlainText("text", extra))
                }
            }
            6 -> {
                FilePicker.f4692a = extra
                // Membuka UIExplorer membutuhkan MainActivityIn pada UIBrowser.
            }
            else -> Unit
        }
    }

    private fun copyImage(url: String) {
        val context = s0 ?: return
        Glide.with(context).asBitmap().load(url)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    try {
                        val dir = File(context.cacheDir, "clipboard")
                        if (!dir.exists()) dir.mkdirs()
                        val image = File(dir, "img_${System.currentTimeMillis()}.png")
                        FileOutputStream(image).use { out ->
                            resource.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            image
                        )
                        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newUri(context.contentResolver, "image", uri)
                        // A FileProvider URI is meaningless to the pasting app without a read grant. Keep the URI
                        // item for normal pastes and carry the grant in an Intent item beside it.
                        val grant = Intent(Intent.ACTION_VIEW).apply {
                            setData(uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            clipData = clip
                        }
                        clip.addItem(ClipData.Item(grant))
                        manager?.setPrimaryClip(clip)
                    } catch (_: IOException) {
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) = Unit
            })
    }

    private inner class Chrome : WebChromeClient() {
        var customView: View? = null
        var callback: CustomViewCallback? = null
        val scale = FloatArray(1)

        override fun onCreateWindow(
            view: WebView,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message
        ): Boolean {
            val ctx = context ?: return false
            val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
            // A second WebView with no host leaks its renderer. Host it in a dialog we own and can close and destroy.
            closePopup()
            val popup = WebView(ctx)
            UIWebMount.b(popup, true, w0?.b0())
            // No nesting: another window.open() in the popup would have no dialog of its own.
            popup.webViewClient = object : WebViewClient() {
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean = false
            }
            val dialog = AlertDialog.Builder(ctx).setView(popup).setOnDismissListener { closePopup() }.create()
            popupWebView = popup
            popupDialog = dialog
            dialog.show()
            transport.webView = popup
            resultMsg.sendToTarget()
            return true
        }

        override fun onCloseWindow(window: WebView) {
            if (window === popupWebView) closePopup() else super.onCloseWindow(window)
        }

        override fun onHideCustomView() {
            val view = customView ?: return
            val web = y0
            if (web != null) Fullscreen.b(w0 ?: return, web, view, callback, scale)
            customView = null
            callback = null
        }

        override fun onPermissionRequest(request: PermissionRequest) {
            n(this@UIBrowserTab, 0, request).run()
        }

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            z0?.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            super.onReceivedTitle(view, title)
            x0?.text = title
        }

        override fun onShowCustomView(view: View, cb: CustomViewCallback) {
            if (customView != null) {
                cb.onCustomViewHidden()
                return
            }
            customView = view
            callback = cb
            val web = y0 ?: return
            val browser = w0 ?: return
            Fullscreen.a(browser, web, view, scale)
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            this@UIBrowserTab.filePathCallback?.onReceiveValue(null)
            this@UIBrowserTab.filePathCallback = filePathCallback
            val ctx = context ?: return false
            Files.b(ctx, fileChooserParams, fileLauncher)
            return true
        }
    }

    private inner class Client : WebViewClient() {
        override fun onPageCommitVisible(view: WebView, url: String) {
            super.onPageCommitVisible(view, url)
            Scaling.b(view, w0?.b0())
            view.evaluateJavascript(DOWNLOAD_INTERCEPTOR, null)
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            u0 = url
            view.evaluateJavascript(DOWNLOAD_INTERCEPTOR, null)
            B0 = false
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            B0 = true
        }
    }

    companion object {
        private const val DOWNLOAD_INTERCEPTOR = "(function() {" +
            "if (window._downloadInterceptorAdded) return;" +
            "window._downloadInterceptorAdded = true;" +
            "document.addEventListener('click', function(e) {" +
            "var a = e.target.closest('a');" +
            "if (a && a.href && a.href.startsWith('blob:')) {" +
            "e.preventDefault();" +
            "var filename = a.download || ('file_' + Date.now() + '.bin');" +
            "fetch(a.href).then(r => r.blob()).then(blob => {" +
            "const reader = new FileReader();" +
            "reader.onloadend = function() {" +
            "AndroidDownloader.downloadBase64File(reader.result, filename);" +
            "};" +
            "reader.readAsDataURL(blob);" +
            "});" +
            "}" +
            "}, true);" +
            "})();"
    }
}
