package com.bellatrixos.windows.blur

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.RelativeLayout
import com.bellatrixos.AppExecutors
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.variables.Global
import com.bellatrixos.windows.Windows

/** Mengaktifkan atau menonaktifkan blur pada latar UI. */
object UIBlur {
    @JvmStatic
    fun a(context: Context, relativeLayout: RelativeLayout, imageView: ImageView, imageView2: ImageView) {
        Global.b = SharedPrefs.f4755a?.getBoolean("system_ui_blur", true) ?: true
        Global.f4839f = (context.resources.configuration.uiMode and 48) == 32
        UIBlurImage.f4886a?.let { bitmap ->
            bitmap.recycle()
            UIBlurImage.f4886a = null
        }
        if (Global.b) {
            AppExecutors.d().execute {
                try {
                    UIBlurImage.c(imageView2)
                    UIBlurImage.b()
                    val bitmap: Bitmap? = BlurTaskBar.a(context, relativeLayout)
                    Handler(Looper.getMainLooper()).post {
                        Windows.f4882e?.o0()
                        imageView.alpha = 0.0f
                        imageView.animate().alpha(0.09f).setDuration(1000L).start()
                        imageView.setImageBitmap(bitmap)
                    }
                } catch (ignored: Exception) {
                    if (SharedPrefs.b()) {
                        AppExecutors.c().post {
                            relativeLayout.backgroundTintList = ColorStateList.valueOf(Global.c)
                        }
                    }
                }
            }
        } else if (SharedPrefs.b()) {
            relativeLayout.backgroundTintList = ColorStateList.valueOf(Global.c)
        }
    }
}
