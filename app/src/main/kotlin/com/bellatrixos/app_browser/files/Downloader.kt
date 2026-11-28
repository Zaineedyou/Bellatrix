package com.bellatrixos.app_browser.files

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import org.mozilla.geckoview.WebResponse
import java.io.File
import java.io.InputStream
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bellatrixos.R
import com.bellatrixos.app_browser.widgets.dialogs.UIDownloads

/** Unduhan berkas dari halaman yang diserahkan mesin Firefox. */
object Downloader {
    /**
     * A download Firefox's engine handed over. The engine has already asked the server, with the page's cookies, referrer
     * and user agent, so its [WebResponse] body is saved here, into Downloads, whatever the address (a web one, blob or
     * data). Only a response without a body, or one that can't be written, goes to the DownloadManager (web addresses
     * only), which asks the server again without any of those: a video host that answers that second request with an
     * error page or nothing was the "download" that came out as a .bin.
     */
    @JvmStatic
    fun handle(
        context: Context,
        response: WebResponse,
        downloads: UIDownloads?,
        view: View?,
        owner: LifecycleOwner,
    ) {
        val disposition = response.headers.entries.firstOrNull { it.key.equals("Content-Disposition", true) }?.value
        val mime = response.headers.entries.firstOrNull { it.key.equals("Content-Type", true) }?.value
        // Even Firefox's download treats the server-supplied name as untrusted.
        val guessed = try {
            URLUtil.guessFileName(response.uri, disposition, mime)
        } catch (_: Exception) {
            ""
        }
        val name = withMimeExtension(FileManager.a(context, "", guessed), mime)
        val web = response.uri.startsWith("http://") || response.uri.startsWith("https://")
        val body = response.body
        if (body == null) {
            if (web) enqueue(context, response.uri, mime, name, downloads, view, owner) else if (view != null) downloads?.b(view, name, 1)
            return
        }
        val appContext = context.applicationContext
        if (view != null) downloads?.b(view, name, 0)
        Thread {
            val saved = save(body, name)
            when {
                saved != null -> {
                    runCatching { MediaScannerConnection.scanFile(appContext, arrayOf(saved.absolutePath), null, null) }
                    if (view != null) view.post { downloads?.b(view, saved.name, 2) }
                }
                // The manager is started from the main thread, as the receiver it registers is.
                web -> Handler(Looper.getMainLooper()).post { enqueue(context, response.uri, mime, name, downloads, view, owner) }
                else -> if (view != null) view.post { downloads?.b(view, name, 1) }
            }
        }.start()
    }

    /** [body] written into Downloads under [name], or one like it if that is taken; null when it couldn't be written. */
    private fun save(
        body: InputStream,
        name: String,
    ): File? {
        val file = freeTarget(name) ?: return null
        return try {
            file.parentFile?.mkdirs()
            body.use { input -> file.outputStream().use { input.copyTo(it) } }
            file
        } catch (_: Exception) {
            file.delete()
            null
        }
    }

    /** Where [name] goes in Downloads without replacing a file already there: "name (1).ext", "name (2).ext"... */
    private fun freeTarget(name: String): File? {
        val first = FileManager.downloadsTarget(name) ?: return null
        if (!first.exists()) return first
        val stem = name.substringBeforeLast('.', name)
        val extension = name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
        for (i in 1..999) {
            val next = FileManager.downloadsTarget("$stem ($i)$extension") ?: return null
            if (!next.exists()) return next
        }
        return null
    }

    /** [name] with the extension of [mime] when it ends in .bin, which a name guessed with no type to go by gets. */
    private fun withMimeExtension(
        name: String,
        mime: String?,
    ): String {
        if (!name.endsWith(".bin", true)) return name
        val type = mime?.substringBefore(';')?.trim()?.lowercase()?.takeIf { it.isNotEmpty() && it != "application/octet-stream" } ?: return name
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type) ?: return name
        return name.dropLast(3) + extension
    }

    private fun enqueue(
        context: Context,
        url: String,
        mimeType: String?,
        name: String,
        downloads: UIDownloads?,
        view: View?,
        owner: LifecycleOwner,
    ) {
        val appContext = context.applicationContext
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                mimeType?.let { setMimeType(it) }
                setDescription(context.getString(R.string.downloader_file_downloading))
                setTitle(name)
                allowScanningByMediaScanner()
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val id = manager.enqueue(request)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) == id) {
                        if (view != null) downloads?.b(view, name, 2)
                        runCatching { appContext.unregisterReceiver(this) }
                    }
                }
            }
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    runCatching { appContext.unregisterReceiver(receiver) }
                }
            })
        } catch (_: Exception) {
            if (view != null) downloads?.b(view, name, 1)
        }
    }
}
