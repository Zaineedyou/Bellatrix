package com.bellatrixos.app_settings.fragments

import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.bellatrixos.R
import com.bellatrixos.app_settings.adapters.InstalledAppsAdapter
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.taskbar.center_widgets.WidgetCenter
import com.bellatrixos.windows.installed_apps.models.InstalledApp

/** Klik item menu konteks untuk aplikasi terpasang dan widget center. */
class C0058g : UIContextMenu.OnMenuItemClickListener {
    @JvmField val u: Int
    @JvmField val v: Int
    @JvmField val w: Any?
    @JvmField val x: Any?

    constructor(i2: Int, i3: Int, obj: Any?, obj2: Any?) {
        u = i3
        v = i2
        w = obj
        x = obj2
    }

    override fun f(view: View, i2: Int) {
        val position = v
        when (u) {
            0 -> {
                val installed = w as? UIAppsInstalled ?: return
                val str = x as? String
                val adapter = installed.t0?.e0() as? com.bellatrixos.taskbar.TaskBarAdapter
                when (i2) {
                    16 -> if (str != null) adapter?.a(str)
                    17 -> if (str != null) adapter?.e(str)
                    24 -> {
                        if (position <= 1) {
                            Toast.makeText(installed.s0, installed.getString(R.string.context_menu_uninstall_restricted), 0).show()
                        } else {
                            if (str != null) installed.t0?.D(str)
                            val appsAdapter = installed.v0 ?: return
                            val list = appsAdapter.y
                            if (position >= 0 && position < list.size) {
                                val removed = list.removeAt(position)
                                appsAdapter.z.remove(removed)
                                appsAdapter.notifyItemRemoved(position + 1)
                                appsAdapter.B?.text = appsAdapter.x.getString(
                                    R.string.settings_apps_installed,
                                    list.size.toString()
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                val center = w as? WidgetCenter ?: return
                val widgets = center.l
                var pinned = false
                if (position >= 0 && position < widgets.A.size) {
                    pinned = widgets.A[position].b
                }
                when (i2) {
                    41 -> if (pinned) widgets.C(position)
                    43 -> if (!pinned) widgets.C(position)
                }
                (x as? UIContextMenu)?.dismiss()
            }
        }
    }
}
