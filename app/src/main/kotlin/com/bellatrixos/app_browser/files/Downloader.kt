package com.bellatrixos.app_browser.files

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bellatrixos.R
import com.bellatrixos.app_browser.widgets.dialogs.UIDownloads
import com.bellatrixos.widgets.UIToast
import org.mozilla.geckoview.WebResponse
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Unduhan berkas dari halaman: respons yang diserahkan mesin Firefox ([handle]) dan alamat dari menu tekan-tahan ([saveUrl]).
 *
 * Every download is written to a temporary file first, then moved into the public Downloads folder through MediaStore
 * (no storage permission needed, works under scoped storage) with the plain File API as a second try and Android's
 * DownloadManager as the last one. Whatever happens, the person is told: the browser's popup, a toast and a system
 * notification. Errors are logged under the tag "Downloader" instead of being swallowed.
 */
object Downloader {
    private const val TAG = "Downloader"
    private const val CHANNEL = "browser_downloads"
    private const val LOADING = 0
    private const val FAILED = 1
    private const val DONE = 2
    private val main = Handler(Looper.getMainLooper())

    /** A download Firefox's engine handed over. Its [WebResponse] body is saved as it is, whatever the address. */
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
        val guessed = try {
            URLUtil.guessFileName(response.uri, disposition, mime)
        } catch (_: Exception) {
            ""
        }
        val name = withMimeExtension(FileManager.a(context, "", guessed), mime)
        val web = response.uri.startsWith("http://") || response.uri.startsWith("https://")
        val body = response.body
        if (body == null) {
            if (web) enqueue(context, response.uri, mime, name, downloads, view, owner) else report(context, downloads, view, name, FAILED)
            return
        }
        report(context, downloads, view, name, LOADING)
        Thread {
            var temp: File? = null
            try {
                val file = File.createTempFile("dl", ".tmp", context.applicationContext.cacheDir)
                temp = file
                body.use { input -> file.outputStream().use { input.copyTo(it) } }
                val saved = store(context.applicationContext, file, name, mime)
                if (saved != null) {
                    report(context, downloads, view, saved, DONE)
                } else {
                    Log.e(TAG, "Could not store $name")
                    main.post { fallback(context, web, response.uri, mime, name, downloads, view, owner) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download of $name failed", e)
                main.post { fallback(context, web, response.uri, mime, name, downloads, view, owner) }
            } finally {
                temp?.delete()
            }
        }.start()
    }

    /**
     * Saves what [url] points to: a press-and-hold on an image, a video or a sound. [name] and [mime] may be null.
     * Web addresses are fetched here, data addresses decoded; if the fetch fails the DownloadManager gets it.
     */
    @JvmStatic
    fun saveUrl(
        context: Context,
        url: String,
        mime: String?,
        name: String?,
        downloads: UIDownloads?,
        view: View?,
        owner: LifecycleOwner,
    ) {
        val web = url.startsWith("http://") || url.startsWith("https://")
        val wanted = name?.takeIf { it.isNotBlank() }
            ?: try {
                withMimeExtension(FileManager.a(context, if (url.startsWith("data:")) url else "", URLUtil.guessFileName(url, null, mime)), mime)
            } catch (_: Exception) {
                FileManager.a(context, "", null)
            }
        report(context, downloads, view, wanted, LOADING)
        Thread {
            var temp: File? = null
            try {
                val file = File.createTempFile("dl", ".tmp", context.applicationContext.cacheDir)
                temp = file
                var type = mime
                var finalName = wanted
                if (url.startsWith("data:")) {
                    val header = url.substringBefore(',')
                    val payload = url.substringAfter(',', "")
                    val bytes = if (header.contains(";base64", true)) Base64.decode(payload, Base64.DEFAULT) else Uri.decode(payload).toByteArray()
                    file.writeBytes(bytes)
                    type = header.removePrefix("data:").substringBefore(';').ifBlank { null }
                    finalName = withMimeExtension(wanted, type)
                } else if (web) {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    try {
                        connection.connectTimeout = 15000
                        connection.readTimeout = 30000
                        connection.instanceFollowRedirects = true
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:150.0) Gecko/150.0 Firefox/150.0")
                        runCatching { connection.setRequestProperty("Referer", URL(url).let { "${it.protocol}://${it.host}/" }) }
                        val code = connection.responseCode
                        if (code !in 200..299) throw IOException("HTTP $code")
                        type = connection.contentType ?: mime
                        val disposition = connection.getHeaderField("Content-Disposition")
                        if (name.isNullOrBlank()) {
                            val guessed = runCatching { URLUtil.guessFileName(url, disposition, type) }.getOrDefault("")
                            finalName = withMimeExtension(FileManager.a(context, "", guessed), type)
                        }
                        connection.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
                    } finally {
                        connection.disconnect()
                    }
                } else {
                    throw IOException("Unsupported address: ${url.substringBefore(':')}")
                }
                if (file.length() == 0L) throw IOException("Empty response")
                val saved = store(context.applicationContext, file, finalName, type)
                if (saved != null) report(context, downloads, view, saved, DONE) else throw IOException("Could not store $finalName")
            } catch (e: Exception) {
                Log.e(TAG, "Download of $url failed", e)
                main.post { fallback(context, web, url, mime, wanted, downloads, view, owner) }
            } finally {
                temp?.delete()
            }
        }.start()
    }

    /** DownloadManager for web addresses; an error for anything else. Runs on the main thread. */
    private fun fallback(
        context: Context,
        web: Boolean,
        url: String,
        mime: String?,
        name: String,
        downloads: UIDownloads?,
        view: View?,
        owner: LifecycleOwner,
    ) {
        if (web) enqueue(context, url, mime, name, downloads, view, owner) else report(context, downloads, view, name, FAILED)
    }

    /** [source] copied into the public Downloads folder; the name it ended up with, or null. */
    private fun store(
        app: Context,
        source: File,
        name: String,
        mime: String?,
    ): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            storeInMediaStore(app, source, name, mime)?.let { return it }
        }
        return storeAsFile(app, source, name)
    }

    private fun storeInMediaStore(
        app: Context,
        source: File,
        name: String,
        mime: String?,
    ): String? {
        val resolver = app.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mimeFor(name, mime))
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        var uri: Uri? = null
        return try {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            val out = resolver.openOutputStream(uri) ?: throw IOException("No output stream for $uri")
            out.use { stream -> FileInputStream(source).use { it.copyTo(stream) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            var shown = name
            resolver.query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0)?.let { value -> shown = value }
            }
            shown
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore could not save $name", e)
            uri?.let { runCatching { resolver.delete(it, null, null) } }
            null
        }
    }

    private fun storeAsFile(
        app: Context,
        source: File,
        name: String,
    ): String? {
        val target = freeTarget(name) ?: return null
        return try {
            target.parentFile?.mkdirs()
            FileInputStream(source).use { input -> target.outputStream().use { input.copyTo(it) } }
            runCatching { MediaScannerConnection.scanFile(app, arrayOf(target.absolutePath), null, null) }
            target.name
        } catch (e: Exception) {
            Log.e(TAG, "File API could not save $name", e)
            target.delete()
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

    /** The type of [name] by its extension, else the response's own, else a generic one. */
    private fun mimeFor(
        name: String,
        mime: String?,
    ): String {
        val byExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase())
        val given = mime?.substringBefore(';')?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        return byExtension ?: given ?: "application/octet-stream"
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

    /** Tells the person how [name] is doing: the browser's popup, and for a result also a toast and a notification. */
    private fun report(
        context: Context,
        downloads: UIDownloads?,
        view: View?,
        name: String,
        state: Int,
    ) {
        main.post {
            if (view != null && view.isAttachedToWindow) {
                runCatching { downloads?.b(view, name, state) }.onFailure { Log.e(TAG, "Popup failed", it) }
            }
            if (state != LOADING) {
                val text = context.getString(if (state == DONE) R.string.downloader_file_download_success else R.string.downloader_file_download_error)
                runCatching { UIToast.a(context, "$text: $name", R.drawable.ui_browser_ic_downloaded, 2500) }
                    .onFailure { Log.e(TAG, "Toast failed", it) }
                notifyResult(context.applicationContext, name, state == DONE)
            }
        }
    }

    private fun notifyResult(
        app: Context,
        name: String,
        success: Boolean,
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_DEFAULT))
            val open = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pending = PendingIntent.getActivity(app, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val notification = NotificationCompat.Builder(app, CHANNEL)
                .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
                .setContentTitle(name)
                .setContentText(app.getString(if (success) R.string.downloader_file_download_success else R.string.downloader_file_download_error))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
            manager.notify(name.hashCode(), notification)
        } catch (e: Exception) {
            Log.e(TAG, "Notification failed", e)
        }
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
                        report(context, downloads, view, name, DONE)
                        runCatching { appContext.unregisterReceiver(this) }
                    }
                }
            }
            // A system broadcast, so the receiver is exported as far as Android is concerned.
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED,
            )
            owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    runCatching { appContext.unregisterReceiver(receiver) }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "DownloadManager could not take $url", e)
            report(context, downloads, view, name, FAILED)
        }
    }
}
