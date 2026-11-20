package com.bellatrixos.taskbar

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Penyimpanan dan indeks aplikasi yang di-pin di taskbar. */
object TaskBarDB {
    @JvmField var f4804a: SharedPreferences? = null
    @JvmField val b: HashMap<String, Int> = HashMap()

    private val prefs: SharedPreferences
        get() = requireNotNull(f4804a) { "TaskBarDB belum diinisialisasi" }

    @JvmStatic
    fun a(str: String): Int = b[str] ?: -1

    @JvmStatic
    fun b(): List<TaskBarItemPins> {
        val json = prefs.getString("taskbar_apps", "") ?: ""
        if (json.isEmpty()) return emptyList()
        val type = object : TypeToken<List<TaskBarItemPins>>() {}.type
        return Gson().fromJson<List<TaskBarItemPins>>(json, type) ?: emptyList()
    }

    @JvmStatic
    fun c(list: List<TaskBarItem>) {
        b.clear()
        list.forEachIndexed { index, item -> b[item.a()] = index }
    }

    @JvmStatic
    fun d(list: List<TaskBarItem>) {
        b.clear()
        val pins = mutableListOf<TaskBarItemPins>()
        list.forEachIndexed { index, item ->
            if (item.h()) pins.add(TaskBarItemPins(item.a(), index))
            b[item.a()] = index
        }
        e(pins)
    }

    @JvmStatic
    fun e(list: List<TaskBarItemPins>) {
        prefs.edit().putString("taskbar_apps", Gson().toJson(list)).apply()
    }
}
