package com.bellatrixos.windows.config

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.bellatrixos.R
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.variables.Global

/** Menghitung warna aksen dari wallpaper atau preferensi pengguna. */
object Themes {
    @JvmField var f4891a: String? = null

    @JvmStatic
    fun a(context: Context, bitmap: Bitmap?) {
        var color = ContextCompat.getColor(context, R.color.color_primary_default)

        runCatching {
            if (SharedPrefs.f4755a?.getBoolean("wallpaper_enabled", true) == true && bitmap != null) {
                val swatch = Palette.Builder(bitmap).generate().getSwatchForTarget(androidx.palette.graphics.Target.VIBRANT)
                color = swatch?.rgb ?: -0x1000000
            } else {
                val drawable = WallpaperManager.getInstance(context).fastDrawable
                if (drawable is BitmapDrawable) {
                    val swatch = Palette.Builder(drawable.bitmap).generate().getSwatchForTarget(androidx.palette.graphics.Target.VIBRANT)
                    color = swatch?.rgb ?: color
                }
            }
        }

        Global.c = ColorUtils.setAlphaComponent(color, 20)
    }
}
