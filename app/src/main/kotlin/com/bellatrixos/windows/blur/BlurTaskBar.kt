package com.bellatrixos.windows.blur

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.View
import com.bellatrixos.R
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.wallpaper.models.Bonds

/** Sumber bitmap blur untuk taskbar, dari wallpaper atau wallpaper aktif. */
object BlurTaskBar {
    @JvmStatic
    fun a(context: Context, view: View): Bitmap? {
        if (SharedPrefs.f4755a?.getBoolean("wallpaper_enabled", true) == true) {
            return UIBlurImage.a(Bonds.a(view))
        }
        val taskbarHeight = context.resources.getDimensionPixelSize(R.dimen.taskbar_height)
        val drawable = WallpaperManager.getInstance(context).drawable
        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return null
        val width = bitmap.width
        val height = bitmap.height
        val targetWidth = context.resources.displayMetrics.widthPixels
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            targetWidth,
            Math.round(height * (targetWidth / width.toFloat())),
            true
        )
        val result = Bitmap.createBitmap(
            scaled,
            0,
            Math.max(0, scaled.height - taskbarHeight),
            scaled.width,
            taskbarHeight
        )
        if (scaled !== bitmap) scaled.recycle()
        return result
    }
}
