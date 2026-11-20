package com.bellatrixos.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Point
import android.net.Uri
import android.util.DisplayMetrics
import androidx.exifinterface.media.ExifInterface

/** Utilitas geometri, sampling, dan orientasi gambar wallpaper. */
object helper {
    /** Membaca EXIF orientation dan memutar bitmap sesuai metadata. */
    @JvmStatic
    fun a(context: Context, uri: Uri, bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) return null
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return bitmap
            stream.use { input ->
                val orientation = ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                f(orientation, bitmap)
            }
        } catch (ignored: Exception) {
            bitmap
        }
    }

    /** Membatasi ukuran hasil agar konsumsi memori tidak melebihi 48 MB. */
    @JvmStatic
    fun b(point: Point): Point {
        val bytes = point.x.toLong() * point.y.toLong() * 4L
        if (bytes <= 50331648L) return point
        val scale = Math.sqrt(50331648.0 / bytes).toFloat()
        return Point(
            Math.max(1, (point.x * scale).toInt()),
            Math.max(1, (point.y * scale).toInt())
        )
    }

    /** Menghitung inSampleSize untuk decodebitmap ke ukuran yang diminta. */
    @JvmStatic
    fun c(options: BitmapFactory.Options, i2: Int, i3: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var sample = 1
        if (height > i3 || width > i2) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / sample >= i3 && halfWidth / sample >= i2) {
                sample *= 2
            }
        }
        return sample
    }

    /** Decode file dengan sampling ke ukuran tertentu. */
    @JvmStatic
    fun d(str: String, i2: Int, i3: Int, config: Bitmap.Config?): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(str, options)
        options.inSampleSize = c(options, i2, i3)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = config
        return BitmapFactory.decodeFile(str, options)
    }

    /** Menentukan ukuran target berdasarkan preferensi kualitas wallpaper. */
    @JvmStatic
    fun e(context: Context, i2: Int, i3: Int, i4: Int): Point {
        val metrics: DisplayMetrics = context.resources.displayMetrics
        val point = Point(metrics.widthPixels, metrics.heightPixels)
        var height = i3
        val ratio = i2 / i3.toFloat()
        if (i4 == 1) height = (point.y * 1.5f).toInt()
        return Point((height * ratio).toInt(), height)
    }

    /** Menerapkan transformasi orientasi EXIF ke bitmap. */
    @JvmStatic
    fun f(i2: Int, bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) return null
        val matrix = Matrix()
        when (i2) {
            2 -> matrix.postScale(-1.0f, 1.0f)
            3 -> matrix.postRotate(180.0f)
            4 -> matrix.postScale(1.0f, -1.0f)
            5 -> {
                matrix.postRotate(90.0f)
                matrix.postScale(-1.0f, 1.0f)
            }
            6 -> matrix.postRotate(90.0f)
            7 -> {
                matrix.postRotate(270.0f)
                matrix.postScale(-1.0f, 1.0f)
            }
            8 -> matrix.postRotate(270.0f)
            else -> return bitmap
        }
        val result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (result !== bitmap) bitmap.recycle()
        return result
    }
}
