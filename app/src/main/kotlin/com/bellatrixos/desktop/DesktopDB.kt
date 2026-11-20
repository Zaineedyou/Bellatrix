package com.bellatrixos.desktop

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Penyimpanan item desktop. */
object DesktopDB {
    @JvmField var f4759a: SharedPreferences? = null
    @JvmField var b: MutableList<DesktopItemPins>? = null

    private val prefs: SharedPreferences
        get() = requireNotNull(f4759a) { "DesktopDB belum diinisialisasi" }

    @JvmStatic
    fun a(): MutableList<DesktopItemPins> {
        b?.let { return it }
        val json = prefs.getString("desktop_items", "") ?: ""
        val type = object : TypeToken<List<DesktopItemPins>>() {}.type
        val list = if (json.isEmpty()) mutableListOf()
        else Gson().fromJson<List<DesktopItemPins>>(json, type)?.toMutableList() ?: mutableListOf()
        b = list
        return list
    }

    @JvmStatic
    fun b(i2: Int, desktopItem: DesktopItem) {
        val list = a()
        if (i2 !in list.indices) return
        list[i2] = DesktopItemPins(desktopItem.c(), i2, desktopItem.b(), desktopItem.a())
        save(list)
    }

    @JvmStatic
    fun c(list: List<DesktopItem>) {
        val pins = list.mapIndexed { index, item -> DesktopItemPins(item.c(), index, item.b(), item.a()) }
            .toMutableList()
        b = pins
        save(pins)
    }

    private fun save(list: List<DesktopItemPins>) {
        prefs.edit().putString("desktop_items", Gson().toJson(list)).apply()
    }
}
