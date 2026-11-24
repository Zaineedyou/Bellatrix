package com.bellatrixos.app_settings.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResult
import androidx.lifecycle.lifecycleScope
import com.bellatrixos.App
import com.bellatrixos.AppExecutors
import com.bellatrixos.app_browser.files.b
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.wallpaper.WallpaperKind
import com.bellatrixos.wallpaper.WallpaperVideoProcessor
import com.bellatrixos.wallpaper.backgroundExtension
import com.bellatrixos.wallpaper.copyToBackgroundFile
import com.bellatrixos.wallpaper.wallpaperKind
import com.bellatrixos.widgets.cards.UISettingsCardSpinner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/** Hasil pemilih wallpaper dan callback spinner jenis gambar. */
class C0059h(@JvmField val u: UIBackground) :
    ActivityResultCallback<Any?>,
    UISettingsCardSpinner.SpinnerCallback {

    /** Remember the chosen type, and only offer volume while it is Video. */
    override fun c(i2: Int) {
        SharedPrefs.f4755a?.edit()?.putInt("wallpaper_type", i2)?.apply()
        u.setVolumeEnabled(SharedPrefs.f4755a?.getBoolean("wallpaper_enabled", true) != false && i2 == 1)
    }

    override fun onActivityResult(result: Any?) {
        val activityResult = result as? ActivityResult ?: return
        if (activityResult.resultCode != Activity.RESULT_OK) return
        val intent: Intent = activityResult.data ?: return
        val data: Uri = intent.data ?: return
        val background = u
        val context = background.t0 ?: return
        background.b0(true)

        // The same picked file also becomes the theme's saved background, so Bellatrix's own
        // live wallpaper sees it even when the launcher is not in front. The Background page
        // stays the only place the user has to go to.
        (context.applicationContext as? App)?.runtime?.let { runtime ->
            background.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                runCatching { runtime.themes.addBackground(data) }
            }
        }

        val hinted =
            when (background.z0?.getSelectedPosition()) {
                0 -> WallpaperKind.IMAGE
                1 -> WallpaperKind.VIDEO
                else -> null
            }
        val detected = data.wallpaperKind(context)
        val useVideo = detected == WallpaperKind.VIDEO || (detected == WallpaperKind.UNKNOWN && hinted == WallpaperKind.VIDEO)
        // A set video stays Video. The file's real kind wins over the row, so a clip picked with
        // Picture selected still leaves the page saying Video once it has been applied.
        val type = if (useVideo) 1 else 0
        if (background.z0?.getSelectedPosition() != type) background.z0?.setSelection(type)
        SharedPrefs.f4755a?.edit()?.putInt("wallpaper_type", type)?.apply()
        background.setVolumeEnabled(SharedPrefs.f4755a?.getBoolean("wallpaper_enabled", true) != false && useVideo)
        if (!useVideo) {
            val quality = background.E0?.getSelectedPosition() ?: 0
            AppExecutors.d().execute(
                com.bellatrixos.wallpaper.a(context, data, quality, background.x0, i(background, 3)),
            )
            return
        }
        processVideo(context, data, background)
    }

    private fun processVideo(
        context: Context,
        data: Uri,
        background: UIBackground,
    ) {
        val callback =
            object : WallpaperVideoProcessor.Callback {
                override fun a() = fallbackVideo(context, data, background)
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
        val dir = File(context.filesDir, "wallpaper_videos").apply { mkdirs() }
        val main = File(dir, "wallpaper_main.mp4")
        val blur = File(dir, "wallpaper_blur.mp4")
        try {
            WallpaperVideoProcessor.a(
                context,
                data,
                main,
                b(context, data, blur, callback, main, maxMs),
                Runnable { callback.a() },
                maxMs,
            )
        } catch (_: Throwable) {
            // An unsupported container can fail before Media3 gives us a callback.
            callback.a()
        }
    }

    /** Media3 can't export every container; keep the original clip and let the player decode it directly. */
    private fun fallbackVideo(
        context: Context,
        data: Uri,
        background: UIBackground,
    ) {
        AppExecutors.d().execute {
            val extension = data.backgroundExtension(context) ?: "mp4"
            val dir = File(context.filesDir, "wallpaper_videos").apply { mkdirs() }
            val main = File(dir, "wallpaper_main.$extension")
            val blur = File(dir, "wallpaper_blur.$extension")
            val copied = data.copyToBackgroundFile(context, main)
            if (copied) data.copyToBackgroundFile(context, blur)
            AppExecutors.c().post(
                if (copied) A(background, main, blur, 2) else k(0, background),
            )
        }
    }

    // ActivityResultCallback.e(Object)
    fun e(obj: Any?) = onActivityResult(obj)
}
