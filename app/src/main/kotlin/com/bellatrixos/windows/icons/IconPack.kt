package com.bellatrixos.windows.icons

import android.content.Context
import android.content.Intent
import com.bellatrixos.windows.icons.models.IconPackItem

/** Mendeteksi paket ikon launcher pihak ketiga. */
object IconPack {
    private val ACTIONS = arrayOf(
        "com.novalauncher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "com.anddoes.launcher.THEME",
        "com.actionlauncher.playstore.iconpack",
        "org.adw.launcher.THEMES",
        "com.sonymobile.home.ICON_PACK",
        "home.solo.launcher.free.THEMES",
        "com.lge.launcher2.THEME",
        "ginlemon.smartlauncher.THEMES",
        "com.phonemetra.turbo.launcher.THEMES",
        "com.fede.launcher.THEME_ICONPACK",
        "com.gau.go.launcherex.theme"
    )

    @JvmStatic
    fun a(context: Context, out: MutableList<IconPackItem>) {
        val pm = context.packageManager
        val seen = HashSet<String>()
        for (action in ACTIONS) {
            for (info in pm.queryIntentActivities(Intent(action), 0)) {
                val app = info.activityInfo.applicationInfo ?: continue
                if (seen.add(app.packageName)) {
                    out.add(
                        IconPackItem(
                            app.loadIcon(pm),
                            app.packageName,
                            app.loadLabel(pm).toString()
                        )
                    )
                }
            }
        }
    }
}
