package com.bellatrixos.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import com.bellatrixos.R
import com.bellatrixos.database.SharedPrefs

/** Wallpaper bawaan yang dipilih berdasarkan tema dan kualitas layar. */
object Default {
    @JvmStatic
    fun a(context: Context, str: String): Bitmap {
        val resource = if (str == "theme2") R.drawable.theme_2 else R.drawable.theme_1
        val quality = SharedPrefs.f4755a?.getInt("wallpaper_quality", 0) ?: 0
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resource, bounds)
        val point: Point = helper.e(context, bounds.outWidth, bounds.outHeight, quality)
        val options = BitmapFactory.Options().apply {
            inSampleSize = helper.c(bounds, point.x, point.y)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeResource(context.resources, resource, options)
        return Wallpapers.a(context, decoded, quality)
    }
}
