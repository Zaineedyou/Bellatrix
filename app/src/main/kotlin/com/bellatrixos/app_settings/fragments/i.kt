package com.bellatrixos.app_settings.fragments

import android.view.View
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.n
import com.bellatrixos.wallpaper.Default
import com.bellatrixos.widgets.UIToast
import com.bellatrixos.widgets.cards.UISettingsCardSpinner

/** Runnable pengaturan wallpaper: error, reset, inisialisasi spinner, dan post UI. */
class i(@JvmField val v: UIBackground, @JvmField val u: Int) : Runnable {
    override fun run() {
        val background = v
        when (u) {
            0 -> {
                background.b0(false)
                val context = background.t0 ?: return
                UIToast.a(context, context.getString(R.string.toast_wallpaper_saving_error), R.drawable.ic_image_24_regular, 2000)
            }
            1 -> {
                background.b0(false)
                val main = background.x0
                if (main != null) {
                    main.W(null)
                    background.y0?.setImageBitmap(main.G())
                }
            }
            2 -> {
                if (!background.isAdded || background.view == null) return
                val context = background.t0 ?: return
                val prefs = SharedPrefs.f4755a ?: return

                if (!prefs.getBoolean("wallpaper_enabled", true)) {
                    background.v0?.alpha = 0.5f
                    background.u0?.alpha = 0.5f
                    background.w0?.isEnabled = false
                }
                background.z0?.setArray(R.array.settings_bg_img_type)
                background.z0?.setSelection(prefs.getInt("wallpaper_type", 0))
                background.z0?.setItemSelectListener(C0059h(background))
                background.E0?.setArray(R.array.settings_bg_img_quality)
                background.E0?.setSelection(prefs.getInt("wallpaper_quality", 0))
                background.E0?.setItemSelectListener(n(2))

                background.w0?.setOnClickListener(themeClick(background, 0))
                background.C0?.setOnClickListener(themeClick(background, 1))
                background.A0?.setOnClickListener(themeClick(background, 2))
                background.B0?.setOnClickListener(themeClick(background, 3))
            }
            3 -> AppExecutors.c().post(i(background, 4))
            else -> {
                background.b0(false)
                val main = background.x0
                if (main != null) {
                    background.y0?.setImageBitmap(main.G())
                }
            }
        }
    }

    /** Helper sintetis `j`: 0/1 buka picker, 2 terapkan theme1, default theme2. */
    private fun themeClick(background: UIBackground, mode: Int) = View.OnClickListener {
        when (mode) {
            0, 1 -> background.a0()
            2 -> applyDefaultTheme(background, "theme1")
            else -> applyDefaultTheme(background, "theme2")
        }
    }

    private fun applyDefaultTheme(background: UIBackground, theme: String) {
        val context = background.t0 ?: return
        AppExecutors.d().execute {
            try {
                val bitmap = Default.a(context, theme)
                AppExecutors.c().post {
                    background.b0(false)
                    background.x0?.W(bitmap)
                    background.y0?.setImageBitmap(background.x0?.G())
                }
            } catch (_: Exception) {
                AppExecutors.c().post(i(background, 0))
            }
        }
    }
}
