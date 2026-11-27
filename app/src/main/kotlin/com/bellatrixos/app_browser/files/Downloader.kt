package com.bellatrixos.app_browser.files

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.view.View
import android.webkit.URLUtil
import org.mozilla.geckoview.WebResponse
import java.io.File
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bellatrixos.R
import com.bellatrixos.app_browser.widgets.dialogs.UIDownloads

/** Unduhan berkas dari halaman yang diserahkan mesin Firefox. */
object Downloader {
    /**
     * A download Firefox's engine handed over. A web address goes to the DownloadManager, as the WebView's downloads did;
     * a blob or data one has no address the manager could fetch, so its bytes are saved here, into Downloads.
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
        val name = FileManager.a(context, "", guessed)
        if (response.uri.startsWith("http://") || response.uri.startsWith("https://")) {
            enqueue(context, response.uri, mime, name, downloads, view, owner)
            return
        }
        val body = response.body ?: run {
            if (view != null) downloads?.b(view, name, 1)
            return
        }
        Thread {
            val saved = runCatching {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).apply { mkdirs() }
                body.use { input -> File(dir, name).outputStream().use { input.copyTo(it) } }
            }.isSuccess
            if (view != null) view.post { downloads?.b(view, name, if (saved) 2 else 1) }
        }.start()
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
