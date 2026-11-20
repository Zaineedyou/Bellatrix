package com.bellatrixos.taskbar.center_widgets

import android.content.SharedPreferences
import com.bellatrixos.taskbar.center_widgets.utils.SavedWidgetItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Penyimpanan daftar widget center. */
object WidgetsDB {
    @JvmField var f4829a: SharedPreferences? = null

    private val prefs: SharedPreferences
        get() = requireNotNull(f4829a) { "WidgetsDB belum diinisialisasi" }

    @JvmStatic
    fun a(): MutableList<SavedWidgetItem> {
        val json = prefs.getString("widget_items", "") ?: ""
        if (json.isEmpty()) return mutableListOf()
        val type = object : TypeToken<List<SavedWidgetItem>>() {}.type
        return Gson().fromJson<List<SavedWidgetItem>>(json, type)?.toMutableList() ?: mutableListOf()
    }

    @JvmStatic
    fun b(items: List<SavedWidgetItem>) {
        prefs.edit().putString("widget_items", Gson().toJson(items)).apply()
    }
}
