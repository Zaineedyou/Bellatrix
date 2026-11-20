package com.bellatrixos.app_settings.fragments

import android.app.Dialog
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.wallpaper.Default
import com.bellatrixos.widgets.UIToast
import java.io.File

/** Runnable sintetis untuk tema personalisasi dan callback wallpaper. */
class A(
    @JvmField val v: Any?,
    @JvmField val w: Any?,
    @JvmField val x: Any?,
    @JvmField val u: Int
) : Runnable {
    override fun run() {
        when (u) {
            0 -> {
                val personalize = v as? UIPersonalize ?: return
                val str = w as? String ?: return
                val dialog = x as? Dialog
                val context = personalize.s0 ?: return
                try {
                    Default.a(context, str)
                    AppExecutors.c().post(A(personalize, str, dialog, 1))
                } catch (_: Exception) {
                    AppExecutors.c().post {
                        dialog?.dismiss()
                        UIToast.a(context, context.getString(R.string.toast_wallpaper_saving_error), R.drawable.ic_image_24_regular, 2000)
                    }
                }
            }
            1 -> {
                val personalize = v as? UIPersonalize ?: return
                if (!personalize.isAdded) return
                val str = w as? String ?: return
                val dialog = x as? Dialog
                val color = when (str) {
                    "theme1" -> "Grey3"
                    "theme2" -> "Blue0"
                    else -> "Blue2"
                }
                SharedPrefs.f4755a?.edit()?.putString("system_primary_color", color)?.apply()
                if (dialog?.isShowing == true) dialog.dismiss()
                personalize.w0?.b()
                personalize.u0?.n()
            }
            else -> {
                val background = v as? UIBackground ?: return
                background.b0(false)
                background.x0?.e(w as? File, x as? File)
            }
        }
    }
}
