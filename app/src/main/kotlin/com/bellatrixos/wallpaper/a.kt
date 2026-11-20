package com.bellatrixos.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.app_explorer.UIExplorer
import com.bellatrixos.app_settings.utils.b
import com.bellatrixos.interfaces.MainActivityIn
import java.io.File

/**
 * Runnable sintetis untuk dua alur wallpaper: decode gambar dari [Uri] dan
 * memberikan daftar berkas ke explorer. Nama field sintetis dipertahankan.
 */
class a : Runnable {
    @JvmField val u: Int
    @JvmField val v: Int
    @JvmField val w: Any?
    @JvmField val x: Comparable<*>?
    @JvmField val y: Any?
    @JvmField val z: Any?

    constructor(context: Context, uri: Uri, i2: Int, mainActivityIn: MainActivityIn?, iVar: Runnable?) {
        u = 0
        v = i2
        w = context
        x = uri
        y = mainActivityIn
        z = iVar
    }

    constructor(explorer: UIExplorer, file: File, fileArr: Array<File>?, list: ArrayList<*>, i2: Int) {
        u = 1
        v = i2
        w = explorer
        x = file
        y = fileArr
        z = list
    }

    override fun run() {
        if (u == 0) runDecode() else runExplorer()
    }

    private fun runDecode() {
        val context = w as? Context ?: return
        val uri = x as? Uri ?: return
        val quality = v
        val activity = y as? MainActivityIn
        val done = z as? Runnable
        var bitmap: Bitmap? = null
        var imageDecoderOk = false

        // ImageDecoder handles HEIC/HEIF, AVIF, WebP, GIF and the newer RAW variants.
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                bitmap = helper.decodeUri(context, uri, quality)
                imageDecoderOk = bitmap != null
            } catch (_: Exception) {
            }
        }

        if (!imageDecoderOk) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val first = context.contentResolver.openInputStream(uri)
                    ?: run {
                        AppExecutors.c().post(b(activity, 4))
                        done?.run()
                        return
                    }
                first.use { BitmapFactory.decodeStream(it, null, bounds) }
                val point: Point = helper.e(context, bounds.outWidth, bounds.outHeight, quality)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = helper.c(bounds, point.x, point.y)
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val second = context.contentResolver.openInputStream(uri)
                    ?: run {
                        AppExecutors.c().post(b(activity, 4))
                        done?.run()
                        return
                    }
                bitmap = second.use { BitmapFactory.decodeStream(it, null, options) }
            } catch (_: Exception) {
                AppExecutors.c().post {
                    Toast.makeText(context, R.string.toast_wallpaper_saving_error, Toast.LENGTH_SHORT).show()
                }
                done?.run()
                return
            }
        }

        val decoded = bitmap
        if (decoded == null) {
            AppExecutors.c().post(b(activity, 4))
        } else {
            // ImageDecoder already applies EXIF orientation; only the fallback needs the legacy rotation.
            val finalBitmap = if (imageDecoderOk) decoded else helper.a(context, uri, decoded)
            AppExecutors.c().post(b(activity, Wallpapers.a(context, finalBitmap, quality), 0))
        }
        done?.run()
    }

    private fun runExplorer() {
        val explorer = w as? UIExplorer ?: return
        val file = x as? File ?: return
        val files = y as? Array<*>
        val list = z as? List<*>
        if (files == null || list == null) return
        val context = explorer.context ?: return
        val message = if (file.isDirectory) "${file.name} (${list.size})" else file.name
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
