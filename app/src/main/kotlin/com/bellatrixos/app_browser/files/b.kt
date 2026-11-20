package com.bellatrixos.app_browser.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.TypedValue
import android.view.View
import com.bellatrixos.app_browser.widgets.dialogs.UIDownloads
import com.bellatrixos.wallpaper.WallpaperVideoProcessor
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/** Runnable unduhan Base64 dan pemrosesan video wallpaper. */
class b : Runnable {
    @JvmField var u: Int = 1
    @JvmField var v: Any? = null
    @JvmField var w: Any? = null
    @JvmField var x: Any? = null
    @JvmField var y: Any? = null
    @JvmField var z: Any? = null
    @JvmField var maxMs: Long = 0L

    constructor(context: Context, uri: Uri, file: File, callback: Any?, file2: File, maxMs: Long = 0L) {
        v = context
        w = uri
        x = file
        y = callback
        z = file2
        this.maxMs = maxMs
        u = 1
    }

    constructor(str: String, context: Context, str2: String, downloads: UIDownloads?, view: View?) {
        w = str
        v = context
        x = str2
        y = downloads
        z = view
        u = 0
    }

    override fun run() {
        if (u == 0) {
            val data = w as? String ?: return
            val context = v as? Context ?: return
            val suggested = x as? String ?: return
            val downloads = y as? UIDownloads
            val view = z as? View
            try {
                val payload = data.substring(data.indexOf(',') + 1)
                val name = FileManager.a(context, data, suggested)
                val target = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    name
                )
                FileManager.b(target, payload)
                MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), null, null)
                if (view != null) downloads?.b(view, name, 2)
            } catch (_: Exception) {
                return
            }
            return
        }

        val context = v as? Context ?: return
        val uri = w as? Uri ?: return
        val callback = y as? WallpaperVideoProcessor.Callback ?: return
        val processedFile = z as? File ?: return
        val targetFile = x as? File ?: return
        val success = Runnable { callback.b(processedFile, targetFile) }
        val failure = Runnable { callback.a() }
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            if (frame != null) {
                val width = frame.width
                val height = frame.height
                var sampleHeight = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    40f,
                    context.resources.displayMetrics
                ).toInt()
                if (sampleHeight > height) sampleHeight = height / 4
                val bottom = Bitmap.createBitmap(frame, 0, height - sampleHeight, width, sampleHeight)
                val tiny = Bitmap.createScaledBitmap(
                    bottom,
                    max(1, width / 20),
                    max(1, sampleHeight / 4),
                    true
                )
                val blurred = Bitmap.createScaledBitmap(tiny, width, sampleHeight, false)
                val outBitmap = Bitmap.createBitmap(width, sampleHeight, Bitmap.Config.ARGB_8888)
                Canvas(outBitmap).drawBitmap(blurred, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
                val blurFile = File(context.cacheDir, "wallpaper_blur_frame.png")
                FileOutputStream(blurFile).use { stream ->
                    outBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    stream.flush()
                }
                WallpaperVideoProcessor.a(context, uri, targetFile, success, failure, maxMs)
            } else {
                failure.run()
            }
        } catch (_: Exception) {
            failure.run()
        }
    }
}
