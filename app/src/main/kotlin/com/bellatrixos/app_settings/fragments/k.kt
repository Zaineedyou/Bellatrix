package com.bellatrixos.app_settings.fragments

import android.content.Intent
import android.os.Looper
import android.view.View
import android.widget.CompoundButton
import android.widget.RadioGroup
import android.widget.Toast
import com.bellatrixos.R
import com.bellatrixos.app_settings.utils.b
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.startmenu.StartMenu
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.UIToast
import com.bellatrixos.widgets.cards.UISettingsCardSpinnerCompact
import com.bellatrixos.windows.Windows
import com.bumptech.glide.Glide
import java.util.Locale
import java.util.TimeZone

/** Runnable bersama: wallpaper error, start menu, taskbar, dan waktu. */
class k(@JvmField val u: Int, @JvmField val v: Any?) : Runnable {
    override fun run() {
        val obj = v
        when (u) {
            0 -> {
                val background = v as? UIBackground ?: return
                background.b0(false)
                val context = background.t0 ?: return
                UIToast.a(context, context.getString(R.string.toast_wallpaper_saving_error), R.drawable.ic_image_24_regular, 2000)
            }
            1 -> (obj as? UIDisplay)?.x0?.n()
            2 -> {
                val menu = obj as? UIStartMenu ?: return
                if (!menu.isAdded || menu.view == null) return
                val columns = SharedPrefs.f4755a?.getInt("start_menu_app_columns", 5) ?: 5
                val checked = when (columns) {
                    4 -> menu.u0
                    5 -> menu.v0
                    6 -> menu.w0
                    else -> null
                }
                checked?.isChecked = true
                menu.s0?.setOnCheckedChangeListener { _, id ->
                    val value = when (id) {
                        R.id.radio_button_1 -> 4
                        R.id.radio_button_2 -> 5
                        else -> 6
                    }
                    menu.t0?.Y(value)
                    SharedPrefs.f4755a?.edit()?.putInt("start_menu_app_columns", value)?.apply()
                    StartMenu.f4780e?.a(true)
                }
                menu.A0?.setSwitchChecked(Global.m)
                menu.A0?.setOnSwitchCheckedChangeListener(com.bellatrixos.app_settings.fragments.u(3))
            }
            3 -> {
                val storage = obj as? UIStorage ?: return
                val context = storage.s0 ?: return
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    throw IllegalArgumentException("You must call this method on a background thread")
                }
                Glide.get(context).clearDiskCache()
            }
            4 -> {
                val taskbar = obj as? UITaskbar ?: return
                if (!taskbar.isAdded || taskbar.view == null) return
                val prefs = SharedPrefs.f4755a ?: return

                taskbar.w0?.setArray(R.array.settings_taskbar_alignment_types)
                taskbar.w0?.setSelection(prefs.getInt("taskbar_position", 0))
                taskbar.w0?.setItemSelectListener(object : UISettingsCardSpinnerCompact.SpinnerCallback {
                    override fun c(i2: Int) {
                        if (taskbar.t0 != 2) {
                            val context = taskbar.s0 ?: return
                            Toast.makeText(context, context.getString(R.string.toast_supports_only_landscape), 0).show()
                            return
                        }
                        val main = taskbar.u0 ?: return
                        main.x(i2)
                        prefs.edit().putInt("taskbar_position", i2).apply()
                        Global.f4843n = i2
                        if (i2 == 1) {
                            Windows.g = R.anim.app_anim_maximize_left
                            Windows.f4884h = R.anim.app_anim_minimize_left
                        } else if (i2 == 0) {
                            Windows.g = R.anim.app_anim_maximize_right
                            Windows.f4884h = R.anim.app_anim_minimize_right
                        }
                        StartMenu.f4780e?.a(true)
                    }
                })
                taskbar.x0?.setArray(R.array.settings_taskbar_search_app_types)
                taskbar.x0?.setSelection(prefs.getInt("taskbar_search_app_type", 3))
                taskbar.x0?.setItemSelectListener(object : UISettingsCardSpinnerCompact.SpinnerCallback {
                    override fun c(i2: Int) {
                        if (taskbar.t0 != 2) {
                            val context = taskbar.s0 ?: return
                            Toast.makeText(context, context.getString(R.string.toast_supports_only_landscape), 0).show()
                            return
                        }
                        val main = taskbar.u0 ?: return
                        main.i(i2)
                        prefs.edit().putInt("taskbar_search_app_type", i2).apply()
                    }
                })
                taskbar.v0?.isChecked = Global.o
                if (taskbar.t0 == 2) {
                    taskbar.v0?.setOnCheckedChangeListener(q(3, taskbar))
                } else {
                    taskbar.v0?.isEnabled = false
                    taskbar.v0?.alpha = 0.5f
                }
            }
            else -> {
                val time = obj as? UITimeLang ?: return
                if (!time.isAdded || time.view == null) return
                val context = time.s0 ?: return
                val text = try {
                    val zone = TimeZone.getDefault()
                    val offset = zone.getOffset(System.currentTimeMillis())
                    val absMinutes = Math.abs(offset / 60000)
                    context.getString(
                        R.string.settings_timezone_display,
                        String.format(
                            Locale.US,
                            "UTC%s%02d:%02d",
                            if (offset >= 0) "+" else "-",
                            absMinutes / 60,
                            absMinutes % 60
                        ),
                        zone.displayName
                    )
                } catch (_: Exception) {
                    context.getString(R.string.settings_timezone_default)
                }
                time.x0?.text = text
                val dateClick = View.OnClickListener {
                    context.startActivity(Intent("android.settings.DATE_SETTINGS"))
                }
                val languageClick = View.OnClickListener {
                    time.t0?.p0(61, true, null)
                }
                val typingClick = View.OnClickListener {
                    time.t0?.p0(62, true, null)
                }
                time.y0?.setOnCardClickListener(dateClick)
                time.z0?.setOnCardClickListener(languageClick)
                time.A0?.setOnCardClickListener(typingClick)
            }
        }
    }
}
