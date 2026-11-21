package com.bellatrixos.windows.installed_apps

import android.content.Context
import com.bellatrixos.R
import com.bellatrixos.windows.installed_apps.models.AppDataDBItem
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Database nama/ikon kustom aplikasi yang tersimpan di preferensi. */
object AppsDatabase {
    @JvmField var f4953a: String = "App"
    @JvmField val b: HashMap<String, String> = HashMap()
    @JvmField var c: Map<String, Any?> = HashMap()
    @JvmField var f4954d: Map<String, AppDataDBItem?> = HashMap()

    /** Nama kustom Start Menu yang diisi adapter; dipertahankan untuk kompatibilitas. */
    @JvmField val startMenuNames: HashMap<String, String> = HashMap()

    @JvmStatic
    fun a(str: String): String {
        b[str]?.let { return it }
        f4954d[str]?.customName?.let { return it }
        return startMenuNames[str] ?: f4953a
    }

    /** Label with a live PackageManager fallback for APKs not yet in our prefs. */
    @JvmStatic
    fun a(
        context: Context,
        str: String,
    ): String {
        b[str]?.let { return it }
        f4954d[str]?.customName?.let { return it }
        startMenuNames[str]?.let { return it }
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(str, 0)).toString().ifBlank { f4953a }
        } catch (_: Exception) {
            f4953a
        }
    }

    @JvmStatic
    fun b(context: Context) {
        f4953a = context.getString(R.string.app_default_title)
        val gson = Gson()
        val raw = context.getSharedPreferences("AppDataPrefs", 0).getString("app_data", "{}") ?: "{}"
        val type = object : TypeToken<Map<String, AppDataDBItem>>() {}.type
        val parsed: Map<String, AppDataDBItem> = gson.fromJson(raw, type) ?: emptyMap()
        f4954d = HashMap(parsed)
        c = HashMap(parsed)

        val custom = f4954d as MutableMap<String, AppDataDBItem?>
        custom["This PC"] = AppDataDBItem(context.getString(R.string.explorer_str_this_pc), null)
        custom["UISettings"] = AppDataDBItem(context.getString(R.string.app_settings), null)
        custom["UIExplorer"] = AppDataDBItem(context.getString(R.string.app_explorer), null)
        custom["UINotepad"] = AppDataDBItem(context.getString(R.string.app_notepad), null)
        custom["UITerminal"] = AppDataDBItem(context.getString(R.string.app_terminal), null)
        custom["UILinuxWindow"] = AppDataDBItem(context.getString(R.string.linux_terminal_window), null)
        custom["UIVoice"] = AppDataDBItem(context.getString(R.string.voice_title), null)
        custom["UIBrowser"] = AppDataDBItem(context.getString(R.string.app_chrome), null)

        val installedRaw = context.getSharedPreferences("InstalledAppsPrefs", 0).getString("installed_apps", null)
        if (!installedRaw.isNullOrEmpty()) {
            val installedType = object : TypeToken<List<InstalledApp>>() {}.type
            val installed: List<InstalledApp> = gson.fromJson(installedRaw, installedType) ?: emptyList()
            for (app in installed) {
                if (!custom.containsKey(app.c())) {
                    custom[app.c()] = AppDataDBItem(app.b(), app.a())
                }
            }
        }

        b["UISettings"] = context.getString(R.string.app_settings)
        b["UIExplorer"] = context.getString(R.string.app_explorer)
        b["UIBrowser"] = context.getString(R.string.app_chrome)
        b["UIVideo"] = context.getString(R.string.app_media_player_title)
        b["UIMusic"] = context.getString(R.string.app_music)
        b["UINotepad"] = context.getString(R.string.app_notepad)
        b["UIInstaller"] = context.getString(R.string.app_installer)
        b["UIPhotos"] = context.getString(R.string.app_image)
        b["UITerminal"] = context.getString(R.string.app_terminal)
        b["UILinuxWindow"] = context.getString(R.string.linux_terminal_window)
        b["UIVoice"] = context.getString(R.string.voice_title)
    }
}
