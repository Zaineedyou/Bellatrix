package com.bellatrixos.app_installer

import com.bellatrixos.desktop.DesktopAdapter
import com.bellatrixos.startmenu.StartMenuAdapter
import com.bellatrixos.startmenu.StartMenuItem
import com.bellatrixos.taskbar.TaskBarAdapter
import com.bellatrixos.windows.installed_apps.InstalledAppsDB

/** Bersihkan entri aplikasi dari menu, taskbar, desktop, dan database. */
object Installer {

    @JvmStatic
    fun a(
        str: String,
        startMenuAdapter: StartMenuAdapter?,
        taskBarAdapter: TaskBarAdapter?,
        desktopAdapter: DesktopAdapter?,
        z: Boolean
    ) {
        if (startMenuAdapter != null) {
            StartMenuAdapter.G.remove(str)
            val list = startMenuAdapter.x
            val iterator = list.iterator()
            while (iterator.hasNext()) {
                if ((iterator.next() as? StartMenuItem)?.c() == str) {
                    iterator.remove()
                    break
                }
            }
            val filter = startMenuAdapter.A
            if (filter != null) {
                filter.filter("")
            } else {
                val target = startMenuAdapter.z
                target.clear()
                target.addAll(list)
                startMenuAdapter.h()
            }
        }
        taskBarAdapter?.removePinnedApp(str)
        desktopAdapter?.C(str)
        if (z) {
            val list = InstalledAppsDB.b().toMutableList()
            val index = InstalledAppsDB.b[str]
            if (index != null) {
                list.removeAt(index)
                InstalledAppsDB.f(list, true)
            }
        }
    }
}
