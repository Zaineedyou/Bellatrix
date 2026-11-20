package com.bellatrixos.app_browser.widgets.dialogs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.view.View
import android.view.ViewGroup
import androidx.exifinterface.media.ExifInterface
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.variables.Global
import com.bellatrixos.wallpaper.Wallpapers
import com.bellatrixos.wallpaper.helper
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.widgets.UIToast
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize
import com.bellatrixos.windows.Windows
import java.io.IOException

/** Runnable unduhan, pengaturan wallpaper gambar, dan penyesuaian ukuran jendela. */
class d : Runnable {
    @JvmField var u: Int = 2
    @JvmField var v: Any? = null
    @JvmField var w: Int = 0
    @JvmField var x: Any? = null
    @JvmField var y: Any? = null

    constructor(i2: Int, view: View, window: Window, floatingLayout: FloatingLayout?) {
        w = i2
        y = view
        x = window
        v = floatingLayout
        u = 2
    }

    constructor(downloads: UIDownloads, view: View, str: String, i2: Int) {
        x = downloads
        y = view
        v = str
        w = i2
        u = 0
    }

    constructor(str: String, context: Context, i2: Int, activity: MainActivityIn) {
        v = str
        x = context
        w = i2
        y = activity
        u = 1
    }

    override fun run() {
        when (u) {
            0 -> {
                val downloads = x as? UIDownloads ?: return
                val view = y as? View ?: return
                downloads.a(view, v as? String, w)
            }
            1 -> runSetWallpaper()
            else -> runResizeWindow()
        }
    }

    private fun runSetWallpaper() {
        val path = v as? String ?: return
        val context = x as? Context ?: return
        val activity = y as? MainActivityIn ?: return
        try {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, options)
                val outW = options.outWidth
                val outH = options.outHeight
                if (outW <= 0 || outH <= 0) throw IOException("Invalid image")
                var point: Point = helper.e(context, outW, outH, w)
                if (w == 2) point = helper.b(point)
                var bitmap = helper.d(path, point.x, point.y, Bitmap.Config.ARGB_8888)
                    ?: throw IOException("Decode failed")
                try {
                    val orientation = ExifInterface(path).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    bitmap = helper.f(orientation, bitmap) ?: bitmap
                } catch (_: Exception) {
                }
                val applied = Wallpapers.a(context, bitmap, w)
                AppExecutors.c().post(com.bellatrixos.wallpaper.b(activity, applied, 1))
            } catch (_: Exception) {
                try {
                    AppExecutors.c().post(Runnable {
                        UIToast.a(
                            context,
                            context.getString(R.string.toast_wallpaper_saving_error),
                            R.drawable.ic_image_24_regular,
                            2000
                        )
                    })
                } catch (th: Throwable) {
                    AppExecutors.c().post(com.bellatrixos.wallpaper.b(activity, null, 1))
                    throw th
                }
            }
        } catch (th: Throwable) {
            AppExecutors.c().post(com.bellatrixos.wallpaper.b(activity, null, 1))
            throw th
        }
    }

    private fun runResizeWindow() {
        var width = WindowSize.g
        var height = WindowSize.f4875h
        if (Global.f4836a == 2) {
            if (w == 2) {
                width = (WindowSize.f4871a * 0.72).toInt()
                height = (WindowSize.b * 0.78).toInt()
            } else if (w == 3) {
                width = (WindowSize.f4871a * 0.6).toInt()
                height = (WindowSize.b * 0.74).toInt()
            }
        }

        val view = y as? View ?: return
        val params = view.layoutParams ?: ViewGroup.LayoutParams(width, height)
        params.width = width
        params.height = height
        view.layoutParams = params

        val window = x as? Window ?: return
        val layout = v as? FloatingLayout
        val xPos: Float
        val yPos: Float
        if (window.b == -1.0f) {
            val screenW = WindowSize.f4871a
            val screenH = WindowSize.b
            val left = (screenW - width) / 2.0f - 0.02f * screenW
            val top = (screenH - height) / 2.0f - 0.05f * screenH
            val savedX = WindowSize.f4876i
            val savedY = WindowSize.j
            if (savedX != -1.0f && savedY != -1.0f && !Windows.c) {
                val nextX = screenW * 0.018f + savedX
                val nextY = screenH * 0.018f + savedY
                if (width + nextX > screenW || height + nextY > screenH) {
                    WindowSize.f4876i = left
                    WindowSize.j = top
                } else {
                    WindowSize.f4876i = nextX
                    WindowSize.j = nextY
                }
            } else {
                WindowSize.f4876i = left
                WindowSize.j = top
            }
            view.x = WindowSize.f4876i
            view.y = WindowSize.j
            xPos = WindowSize.f4876i
            yPos = WindowSize.j
            window.f4868a = xPos
            window.b = yPos
            if (layout == null) return
        } else {
            view.x = window.f4868a
            view.y = window.b
            if (layout == null) return
            xPos = view.x
            yPos = view.y
        }
        layout.y = xPos
        layout.z = yPos
        layout.invalidate()
    }
}
