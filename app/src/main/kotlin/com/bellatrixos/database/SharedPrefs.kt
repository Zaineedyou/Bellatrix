package com.bellatrixos.database

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.bellatrixos.R
import com.bellatrixos.app_explorer.files.Files
import com.bellatrixos.app_explorer.files.SortBy

/** Penyimpanan preferensi global + callback perubahan. */
object SharedPrefs {
    @JvmField var f4755a: SharedPreferences? = null

    private val prefs: SharedPreferences
        get() = requireNotNull(f4755a) { "SharedPrefs belum diinisialisasi" }

    @JvmStatic
    fun a(context: Context): Float {
        val fallback = if (context.resources.getInteger(R.integer.settings_display_mounted_device) == 0) 0.75f else 1.0f
        return prefs.getFloat("system_display_scale", fallback)
    }

    @JvmStatic
    fun b(): Boolean = prefs.getBoolean("start_and_taskbar_has_ui_accent", false)

    /** Mendaftarkan listener sekali pakai untuk kunci tertentu, lalu menjalankan [runnable]. */
    private fun once(key: String, runnable: Runnable?) {
        val listener = object : SharedPreferences.OnSharedPreferenceChangeListener {
            override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, changed: String?) {
                if (key == changed) {
                    prefs.unregisterOnSharedPreferenceChangeListener(this)
                    runnable?.run()
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    @JvmStatic
    fun c(i2: Int, runnable: Runnable?) {
        once("system_display_orientation", runnable)
        prefs.edit().putInt("system_display_orientation", i2).apply()
    }

    @JvmStatic
    fun d(f2: Float, runnable: Runnable?) {
        once("system_display_scale", runnable)
        prefs.edit().putFloat("system_display_scale", f2).apply()
    }

    @JvmStatic
    fun e(sortBy: SortBy, i2: Int) {
        when (i2) {
            0 -> {
                prefs.edit().putInt("file_sort_by", sortBy.u).apply()
                Files.f4679e = sortBy
            }
            Files.f4678d -> {
                prefs.edit().putInt("downloads_sort_by", sortBy.u).apply()
                Files.f4680f = sortBy
            }
        }
    }

    @JvmStatic
    fun f(i2: Int, runnable: Runnable?) {
        once("system_input_tap_type", runnable)
        prefs.edit().putInt("system_input_tap_type", i2).apply()
    }

    @JvmStatic
    fun g(context: Context, str: String?, str2: String?) {
        if (str2 == null || str == null) {
            Toast.makeText(context, R.string.toast_wallpaper_saving_error, Toast.LENGTH_SHORT).show()
            return
        }
        prefs.edit()
            .putString("wallpaper_path", str)
            .putString("wallpaper_mini_path", str2)
            .apply()
    }
}
