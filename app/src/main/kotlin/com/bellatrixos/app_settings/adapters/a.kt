package com.bellatrixos.app_settings.adapters

import android.app.Activity
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.App
import com.bellatrixos.R
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.context_menu.UIMenuItem
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.restartLauncher
import com.bellatrixos.taskbar.TaskBarDB
import com.bellatrixos.taskbar.TaskBarItem

/**
 * Listener klik bersama untuk adapter pengaturan dan taskbar.
 * u=0 => ColorPickerAdapter.OnItemClickListener.b(warna)
 * u=1 => tombol "more" instal
 * u=3 => tombol power Start Menu (v = MainActivityIn): menu Settings, Shut down, Reload, Install updates
 * u=4 => tombol Start di taskbar (v = MainActivityIn, w = daftar item taskbar)
 */
class a(@JvmField val v: Any, @JvmField val u: Int, @JvmField val w: Any) : View.OnClickListener {
    @Suppress("UNCHECKED_CAST")
    override fun onClick(view: View) {
        when (u) {
            0 -> (v as? ColorPickerAdapter)?.A?.b(w as String)
            1 -> {
                val adapter = v as? InstalledAppsAdapter ?: return
                val holder = w as? RecyclerView.ViewHolder ?: return
                val position = holder.bindingAdapterPosition - 1
                if (position >= 0) adapter.A?.a(view, position)
            }
            3 -> showPowerMenu(view)
            4 -> {
                val main = v as? MainActivityIn ?: return
                if (main.K()) {
                    // Mode susun ulang taskbar aktif: simpan urutan lalu keluar dari mode itu.
                    (w as? List<TaskBarItem>)?.let { TaskBarDB.d(it) }
                    main.R(false)
                } else {
                    main.o(0)
                }
            }
        }
    }

    /** The Start Menu's power button: the four entries of menu/start_menu_power_btn.xml, above the button. */
    private fun showPowerMenu(button: View) {
        val main = v as? MainActivityIn ?: return
        val context = button.context
        val menu = UIContextMenu(context)
        menu.f4745d.add(UIMenuItem(0, POWER_SETTINGS, R.drawable.menu_ic_settings_20_regular, context.getString(R.string.st_menu_power_btn_start_settings)))
        menu.f4745d.add(UIMenuItem(0, POWER_SHUTDOWN, R.drawable.ic_power_20_regular, context.getString(R.string.st_menu_power_btn_shutdown)))
        menu.f4745d.add(UIMenuItem(0, POWER_RESTART, R.drawable.menu_ic_clockwise_20_regular, context.getString(R.string.st_menu_power_btn_restart_app)))
        menu.f4745d.add(UIMenuItem(0, POWER_UPDATE, R.drawable.menu_ic_clockwise_20_regular, context.getString(R.string.st_menu_power_btn_install_updates)))
        menu.c =
            UIContextMenu.OnMenuItemClickListener { _, id ->
                val runtime = (context.applicationContext as App).runtime
                when (id) {
                    POWER_SETTINGS -> main.c0("UISettings", null)
                    // Settings page 8 is PC Update (UINavigator).
                    POWER_UPDATE -> main.c0("UISettings", "8")
                    POWER_RESTART -> restartLauncher(context.applicationContext, runtime.proot)
                    POWER_SHUTDOWN -> {
                        // Guest processes first: a proot outlives the app's process.
                        runCatching { runtime.proot.killAll() }
                        (v as? Activity)?.finishAndRemoveTask()
                    }
                }
            }
        menu.a(0)
        menu.h(button)
    }

    private companion object {
        const val POWER_SETTINGS = 201
        const val POWER_SHUTDOWN = 202
        const val POWER_RESTART = 203
        const val POWER_UPDATE = 204
    }
}
