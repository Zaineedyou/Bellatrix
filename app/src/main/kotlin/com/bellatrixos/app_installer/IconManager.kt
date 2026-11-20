package com.bellatrixos.app_installer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.bellatrixos.interfaces.SaveCallback
import com.bellatrixos.windows.icons.IconEngine
import java.io.File
import java.io.FileOutputStream

/** Simpan ikon aplikasi hasil pemasangan ke direktori privat. */
object IconManager {

    @JvmStatic
    fun a(context: Context, drawable: Drawable?, str: String, saveCallback: SaveCallback) {
        if (drawable == null) return
        val bitmap: Bitmap?
        if (drawable !is BitmapDrawable) {
            val width = drawable.intrinsicWidth
            val height = drawable.intrinsicHeight
            if (width <= 0 || height <= 0) return
            val created = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(created)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bitmap = created
        } else {
            bitmap = drawable.bitmap
            if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) return
        }
        try {
            val scaled = Bitmap.createScaledBitmap(
                bitmap, 128,
                (128 * (bitmap.height.toFloat() / bitmap.width.toFloat())).toInt(), true
            )
            val dir = File(context.filesDir, "app_data")
            if (!dir.exists()) dir.mkdirs()
            var safeName = str.replace("[\\\\/:*?\"<>|]".toRegex(), "")
                .replace("\\s+".toRegex(), "_")
                .replace("\\.+".toRegex(), ".")
                .replace("[. ]+$".toRegex(), "")
            if (safeName.length > 28) safeName = safeName.substring(0, 28)
            val out = File(dir, "$safeName.png")
            FileOutputStream(out).use { stream ->
                scaled.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
            }
            val path = out.absolutePath
            val engine = IconEngine.b(context)
            engine.f4948a.put("$str|${IconEngine.g}", scaled)
            saveCallback.e(path, true)
        } catch (_: Exception) {
            saveCallback.e("", false)
        }
    }
}
