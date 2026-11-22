package com.bellatrixos.app_browser.files

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.view.View
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bellatrixos.R
import com.bellatrixos.app_browser.widgets.dialogs.UIDownloads

/** Unduhan berkas biasa dan Base64 dari halaman WebView. */
object Downloader {
    @JvmStatic
    fun a(context: Context, webView: WebView, downloads: UIDownloads?, view: View?, owner: LifecycleOwner) {
        webView.addJavascriptInterface(object : Any() {
            @JavascriptInterface
            fun downloadBase64File(str: String, str2: String) {
                if (context is Activity && (context.isFinishing || context.isDestroyed)) return
                Thread(b(str, context, str2, downloads, view)).start()
            }
        }, "AndroidDownloader")

        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            if (!url.startsWith("data:") && !url.startsWith("blob:")) {
                // Even the browser's own download listener treats the server-supplied name as untrusted.
                val guessed = try {
                    URLUtil.guessFileName(url, contentDisposition, mimeType)
                } catch (_: Exception) {
                    ""
                }
                val name = FileManager.a(context, "", guessed)
                val appContext = context.applicationContext
                try {
                    val request = DownloadManager.Request(Uri.parse(url)).apply {
                        setMimeType(mimeType)
                        addRequestHeader("User-Agent", userAgent)
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
                    // The receiver may outlive the window: tie its lifetime to the caller's lifecycle.
                    owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            runCatching { appContext.unregisterReceiver(receiver) }
                        }
                    })
                } catch (_: Exception) {
                    if (view != null) downloads?.b(view, name, 1)
                }
            }
        })
    }
}
