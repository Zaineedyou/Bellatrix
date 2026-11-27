package com.bellatrixos.app_browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.app_browser.files.Downloader
import com.bellatrixos.app_browser.files.Files
import com.bellatrixos.app_explorer.picker.FilePicker
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** What a press-and-hold on a page landed on: [kind] 7 is a link, 5 an image; [extra] is its address. */
class ContextHit(
    @JvmField val kind: Int,
    @JvmField val extra: String,
)

/** Satu tab pada browser, ditampilkan oleh mesin Firefox (GeckoView). */
class UIBrowserTab : Fragment() {
    @JvmField var B0: Boolean = false
    @JvmField var D0: ContextHit? = null
    @JvmField var s0: Context? = null
    @JvmField var t0: View? = null
    @JvmField var u0: String? = null
    @JvmField var v0: Runnable? = null
    @JvmField var w0: UIBrowser? = null
    @JvmField var x0: TextView? = null
    @JvmField var y0: GeckoTabView? = null
    @JvmField var z0: android.widget.ProgressBar? = null

    /** Whether the page is full screen (a video, say); Back and F11 leave it. */
    @JvmField var fullScreen: Boolean = false

    /** The file chooser the page opened, answered when the picker closes. */
    private var filePrompt: Pair<GeckoSession.PromptDelegate.FilePrompt, GeckoResult<GeckoSession.PromptDelegate.PromptResponse>>? = null

    private var androidPermissions: GeckoSession.PermissionDelegate.Callback? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val callback = androidPermissions ?: return@registerForActivityResult
            androidPermissions = null
            // Gecko asked for them together; one refusal is a refusal.
            if (result.values.all { it }) callback.grant() else callback.reject()
        }

    private val fileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val (prompt, answer) = filePrompt ?: return@registerForActivityResult
            filePrompt = null
            val context = s0
            val uris = Files.uris(result)
            answer.complete(
                when {
                    context == null || uris == null -> prompt.dismiss()
                    uris.size == 1 -> prompt.confirm(context, uris[0])
                    else -> prompt.confirm(context, uris)
                },
            )
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.app_browser_tab, container, false)
        s0 = view.context
        y0 = view.findViewById(R.id.webView)
        return view
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        w0 = parentFragment as? UIBrowser ?: activity as? UIBrowser
        val web = y0 ?: return
        w0?.b0()?.let { GeckoEngine.zoom(view.context, it.g()) }
        wire(web)

        view.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val url = savedInstanceState?.getString("lastUrl", "about:blank")
                    if (url != null) {
                        u0 = url
                        web.loadUrl(url)
                    }
                    v0?.run()
                }
            },
        )
    }

    /** Hooks the page's events to this tab: its progress, title and address, its dialogs, downloads and permissions. */
    private fun wire(web: GeckoTabView) {
        web.onProgress = { progress -> z0?.visibility = if (progress < 100) View.VISIBLE else View.GONE }
        web.onTitle = { title -> x0?.text = title }
        web.onPageStart = {
            B0 = true
            w0?.s0(this, true)
        }
        web.onPageStop = {
            B0 = false
            u0 = web.url ?: u0
            w0?.s0(this, false)
        }
        web.onLocation = { url ->
            u0 = url
            if (w0?.z0 === this) w0?.C0?.setAddress(url)
        }
        web.onHistory = {
            if (w0?.z0 === this) {
                w0?.K0?.isEnabled = web.canGoBack()
                w0?.L0?.isEnabled = web.canGoForward()
            }
        }
        web.onContext = { element, x, y ->
            val context = s0
            val src = element.srcUri
            val link = element.linkUri
            val hit =
                when {
                    src != null && element.type == GeckoSession.ContentDelegate.ContextElement.TYPE_IMAGE -> ContextHit(5, src)
                    link != null -> ContextHit(7, link)
                    else -> null
                }
            if (context != null && hit != null) {
                D0 = hit
                ContextMenuBR.a(context, hit.kind, x, y, Consumer<Any?> { value -> (value as? Int)?.let { a0(it, hit) } })
            }
        }
        web.onFullScreen = { on ->
            fullScreen = on
            // The window's title bar steps aside for a full-screen page and comes back after it.
            w0?.m0(!on)
        }
        // Sign-in popups (window.open) get a window of their own so they can answer the page; other links open in this tab.
        web.onNewWindow = { uri ->
            val context = s0
            if (context != null && GeckoPopup.wantsPopup(uri)) {
                GeckoPopup.open(context)
            } else {
                web.loadUrl(uri)
                null
            }
        }
        web.onCloseRequest = { w0?.navigate("about:blank") }
        web.onDownload = { response ->
            val context = s0
            if (context != null) Downloader.handle(context, response, null, null, viewLifecycleOwner)
        }
        GeckoPrompts.install(web.session, { s0 }) { prompt -> chooseFiles(prompt) }
        web.session.permissionDelegate =
            SitePermissionDelegate({ s0 }) { permissions, callback ->
                androidPermissions = callback
                permissionLauncher.launch(permissions)
            }
    }

    private fun chooseFiles(prompt: GeckoSession.PromptDelegate.FilePrompt): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
        val context = s0 ?: return GeckoResult.fromValue(prompt.dismiss())
        // A second chooser replaces the first, which is answered with nothing.
        filePrompt?.let { (old, answer) -> answer.complete(old.dismiss()) }
        val answer = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
        filePrompt = prompt to answer
        Files.c(
            context,
            prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE,
            prompt.mimeTypes,
            fileLauncher,
        )
        return answer
    }

    override fun onSaveInstanceState(outState: Bundle) {
        u0?.let { outState.putString("lastUrl", it) }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        filePrompt?.let { (prompt, answer) -> answer.complete(prompt.dismiss()) }
        filePrompt = null
        y0?.destroy()
        t0?.isSelected = false
        t0 = null
        w0 = null
        super.onDestroy()
    }

    /** Jalankan aksi menu konteks pada hasil sentuhan tahan. */
    fun a0(
        num: Int?,
        hit: ContextHit,
    ) {
        val extra = hit.extra
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
}
