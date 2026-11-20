package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.result.ActivityResultCallback
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.app_browser.files.b
import com.bellatrixos.wallpaper.WallpaperVideoProcessor
import com.bellatrixos.widgets.UIToast
import com.bellatrixos.widgets.cards.UISettingsCardSpinner
import java.io.File

/** Hasil pemilih wallpaper dan callback spinner jenis gambar. */
class C0059h(@JvmField val u: UIBackground) :
    ActivityResultCallback<Any?>,
    UISettingsCardSpinner.SpinnerCallback {

    override fun c(i2: Int) {
        if (i2 != 1) return
        u.z0?.setSelection(0)
        val context = u.t0 ?: return
        UIToast.a(context, context.getString(R.string.toast_device_coming_soon), R.drawable.ic_megaphone_24_regular, 2000)
    }

    override fun onActivityResult(result: Any?) {
        val activityResult = result as? androidx.activity.result.ActivityResult ?: return
        if (activityResult.resultCode != -1) return
        val intent: Intent = activityResult.data ?: return
        val data: Uri = intent.data ?: return
        val background = u
        val context = background.t0 ?: return
        background.b0(true)
        if (background.z0?.getSelectedPosition() != 1) {
            val quality = background.E0?.getSelectedPosition() ?: 0
            AppExecutors.d().execute(
                com.bellatrixos.wallpaper.a(context, data, quality, background.x0, i(background, 3))
            )
            return
        }
        val callback = object : WallpaperVideoProcessor.Callback {
            override fun a() {
                AppExecutors.c().post(k(0, background))
            }
            override fun b(file: File, file2: File) {
                AppExecutors.c().post(A(background, file, file2, 2))
            }
        }
        // Read the duration only to trim long clips; unknown or odd formats still get a chance.
        var duration: Long? = null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, data)
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (_: Exception) {
        } finally {
            runCatching { retriever.release() }
        }
        val maxMs = if (duration != null && duration > 30_000L) 30_000L else 0L
        val dir = File(context.filesDir, "wallpaper_videos")
        if (!dir.exists()) dir.mkdirs()
        val main = File(dir, "wallpaper_main.mp4")
        WallpaperVideoProcessor.a(
            context,
            data,
            main,
            b(context, data, File(dir, "wallpaper_blur.mp4"), callback, main, maxMs),
            Runnable { callback.a() },
            maxMs,
        )
    }

    // ActivityResultCallback.e(Object)
    fun e(obj: Any?) = onActivityResult(obj)
}
