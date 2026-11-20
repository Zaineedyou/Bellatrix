package com.bellatrixos.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.opengl.GLES20
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.interfaces.MainActivityIn
import java.io.File
import java.io.FileOutputStream

/** Menyimpan dan menskalakan bitmap wallpaper ke ukuran layar. */
object Wallpapers {
    @JvmField var f4846a: String? = null
    @JvmField var b: String? = null

    @JvmStatic
    fun a(context: Context, source: Bitmap?, i2: Int): Bitmap {
        var bitmap = checkNotNull(source) { "bitmap wallpaper tidak valid" }
        var scaled: Bitmap? = null
        if (i2 == 2) {
            try {
                val maxTexture = IntArray(1)
                GLES20.glGetIntegerv(3379, maxTexture, 0)
                val limit = maxTexture[0]
                if (limit > 0 && (bitmap.width > limit || bitmap.height > limit)) {
                    val ratio = Math.min(limit.toFloat() / bitmap.width, limit.toFloat() / bitmap.height)
                    val reduced = Bitmap.createScaledBitmap(
                        bitmap,
                        Math.max(1, (bitmap.width * ratio).toInt()),
                        Math.max(1, (bitmap.height * ratio).toInt()),
                        true
                    )
                    if (reduced !== bitmap) bitmap.recycle()
                    bitmap = reduced
                }
            } catch (t: Throwable) {
                SharedPrefs.g(context, f4846a, b)
                if (bitmap !== scaled && !bitmap.isRecycled) bitmap.recycle()
                throw t
            }
        }
        val metrics = context.resources.displayMetrics
        val point = Point(metrics.widthPixels, metrics.heightPixels)
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        val factor = Math.max(point.x / width, point.y / height)
        scaled = Bitmap.createScaledBitmap(
            bitmap,
            Math.round(width * factor),
            Math.round(height * factor),
            true
        )
        val result = scaled!!
        b(context, result, "wallpaper.png")
        try {
            val mini = Bitmap.createScaledBitmap(
                result,
                (256 * (result.width / result.height.toFloat())).toInt(),
                256,
                true
            )
            b(context, mini, "wallpaper_mini.webp")
            (context as? MainActivityIn)?.v(mini)
        } catch (ignored: Exception) {
        }
        SharedPrefs.g(context, f4846a, b)
        if (bitmap !== result && !bitmap.isRecycled) bitmap.recycle()
        return result
    }

    @JvmStatic
    @Throws(Throwable::class)
    fun b(context: Context, bitmap: Bitmap, str: String) {
        val file = File(context.filesDir, str)
        val stream = FileOutputStream(file)
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            if (str == "wallpaper.png") f4846a = file.absolutePath else b = file.absolutePath
            stream.flush()
        } finally {
            stream.close()
        }
    }
}
