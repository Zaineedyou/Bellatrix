package com.bellatrixos.app_settings.fragments

import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.GridLayoutManager
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.app_settings.adapters.ColorPickerAdapter
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.n
import com.bellatrixos.windows.config.Themes

/**
 * Runnable sintetis UIColors (`l`). u=0: jadwal muat data; u=1: siapkan adapter/UI.
 * Perilaku `run()` disusun ulang dari pemanggilnya.
 */
class l(colors: UIColors, mode: Int) : Runnable {
    @JvmField val u: Int = mode
    @JvmField val v: UIColors = colors

    override fun run() {
        val colors = v
        when (u) {
            0 -> {
                if (!colors.isAdded || colors.view == null) return
                AppExecutors.d().execute(l(colors, 1))
            }
            1 -> {
                val prefs = SharedPrefs.f4755a ?: return
                val blur = prefs.getBoolean("system_ui_blur", true)
                val accent = SharedPrefs.b()
                val context = colors.s0 ?: return
                colors.u0 = AnimationUtils.loadAnimation(context, R.anim.settings_page_enter_to_top)

                var theme = Themes.f4891a
                if (theme == null) {
                    theme = prefs.getString("system_primary_color", null)
                    Themes.f4891a = theme
                }
                val currentHex = NAME_TO_HEX[theme] ?: "#0078D4"
                val spanCount = colors.resources.getInteger(R.integer.settings_color_palette_num_col)
                colors.A0 = GridLayoutManager(context, spanCount)
                val list = colors.v0 ?: return
                colors.y0 = ColorPickerAdapter(context, list, currentHex)

                AppExecutors.c().post {
                    if (!colors.isAdded || colors.view == null) return@post
                    colors.w0?.layoutManager = colors.A0
                    colors.w0?.adapter = colors.y0
                    colors.y0?.A = C0054c(3, colors)
                    colors.w0?.startAnimation(colors.u0)

                    colors.B0?.setArray(R.array.settings_colors_current_scheme)
                    colors.B0?.setSelection(prefs.getInt("system_color_scheme", 2))
                    colors.B0?.setItemSelectListener(n(3))

                    colors.C0?.setSwitchChecked(blur)
                    colors.C0?.setOnSwitchCheckedChangeListener { _, isChecked ->
                        colors.x0?.l(isChecked)
                    }

                    colors.D0?.setSwitchChecked(accent)
                    colors.D0?.setOnSwitchCheckedChangeListener { _, isChecked ->
                        colors.x0?.q0(isChecked)
                    }
                }
            }
        }
    }

    companion object {
        private val NAME_TO_HEX = hashMapOf(
            "Yellow0" to "#FFB900",
            "Yellow1" to "#FF8C00",
            "Orange0" to "#F7630C",
            "Brown0" to "#CA5010",
            "Brown1" to "#DA3B01",
            "Red0" to "#EF6950",
            "Red1" to "#D13438",
            "Red2" to "#FF4343",
            "Red3" to "#E74856",
            "Red4" to "#E81123",
            "Pink0" to "#EA005E",
            "Pink1" to "#C30052",
            "Pink2" to "#E3008C",
            "Pink3" to "#BF0077",
            "Pink4" to "#C239B3",
            "Pink5" to "#9A0089",
            "Blue0" to "#0078D4",
            "Blue1" to "#0063B1",
            "Blue2" to "#8E8CD8",
            "Blue3" to "#6B69D6",
            "Purple0" to "#8764B8",
            "Purple1" to "#744DA9",
            "Purple2" to "#B146C2",
            "Purple3" to "#881798",
            "Cyan0" to "#0099BC",
            "Cyan1" to "#2D7D9A",
            "Cyan2" to "#00B7C3",
            "Cyan3" to "#038387",
            "Green0" to "#00B294",
            "Green1" to "#018574",
            "Green2" to "#00CC6A",
            "Green3" to "#10893E",
            "Grey0" to "#7A7574",
            "Grey1" to "#5D5A58",
            "Grey2" to "#68768A",
            "Grey3" to "#515C6B",
            "Grey4" to "#567C73",
            "Grey5" to "#486860",
            "Green4" to "#498205",
            "Green5" to "#107C10",
            "Grey6" to "#767676",
            "Grey7" to "#4C4A48",
            "Grey8" to "#69797E",
            "Grey9" to "#4A5459",
            "Brown2" to "#647C64",
            "Brown3" to "#525E54",
            "Brown4" to "#847545",
            "Brown5" to "#7E735F"
        )
    }
}
