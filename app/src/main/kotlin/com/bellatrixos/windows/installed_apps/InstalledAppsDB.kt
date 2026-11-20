package com.bellatrixos.windows.installed_apps

import android.content.Context
import android.content.SharedPreferences
import com.bellatrixos.R
import com.bellatrixos.widgets.UINotification
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Daftar aplikasi web yang dipasang pengguna. */
object InstalledAppsDB {
    @JvmField var f4955a: SharedPreferences? = null
    @JvmField val b: HashMap<String, Int> = HashMap()

    private val prefs: SharedPreferences
        get() = requireNotNull(f4955a) { "InstalledAppsDB belum diinisialisasi" }

    @JvmStatic
    fun a(context: Context, str: String, str2: String, str3: String?, i2: Int) {
        if (b.containsKey(str)) {
            UINotification.a(
                context,
                context.getString(R.string.app_already_installed),
                context.getString(R.string.app_installer)
            )
            return
        }
        val list = b().toMutableList()
        list.add(InstalledApp(str2, str, str3, i2, true))
        f(list, false)
        b[str] = list.size - 1
    }

    @JvmStatic
    fun b(): List<InstalledApp> {
        val json = prefs.getString("installed_apps", "") ?: ""
        if (json.isEmpty()) return emptyList()
        val type = object : TypeToken<List<InstalledApp>>() {}.type
        return Gson().fromJson<List<InstalledApp>>(json, type) ?: emptyList()
    }

    @JvmStatic
    fun c(str: String): InstalledApp? {
        val index = b[str] ?: return null
        val list = b()
        return list.getOrNull(index)
    }

    @JvmStatic
    fun d(list: List<InstalledApp>) {
        b.clear()
        list.forEachIndexed { index, app -> b[app.c()] = index }
    }

    @JvmStatic
    fun e(str: String, installedApp: InstalledApp) {
        val index = b[str] ?: return
        val list = b().toMutableList()
        if (index in list.indices) {
            list[index] = installedApp
            f(list, false)
        }
    }

    @JvmStatic
    fun f(list: List<InstalledApp>, z: Boolean) {
        prefs.edit().putString("installed_apps", Gson().toJson(list)).apply()
        if (z) d(list)
    }
}
