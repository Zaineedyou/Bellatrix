package com.bellatrixos.windows.config

import android.content.Context

/** Perubahan bahasa aplikasi lewat SharedPreferences "SettingsPrefs". */
object Languages {
    @JvmField var f4887a: Array<String>? = null

    @JvmStatic
    fun a(context: Context, str: String, runnable: Runnable?) {
        val prefs = context.getSharedPreferences("SettingsPrefs", 0)
        val listener = object : android.content.SharedPreferences.OnSharedPreferenceChangeListener {
            override fun onSharedPreferenceChanged(sharedPreferences: android.content.SharedPreferences?, key: String?) {
                if (key == "language_code") {
                    prefs.unregisterOnSharedPreferenceChangeListener(this)
                    runnable?.run()
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        prefs.edit().putString("language_code", str).apply()
    }
}
