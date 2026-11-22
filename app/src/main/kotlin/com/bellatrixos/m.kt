package com.bellatrixos

import android.content.Context
import android.view.View
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.context_menu.UIMenuItem
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.taskbar.TaskBarAdapter
import com.bellatrixos.taskbar.TaskBarItem
import com.bellatrixos.windows.Updates
import com.bellatrixos.windows.installed_apps.AppsDatabase
import com.bellatrixos.windows.installed_apps.InstalledAppsDB
import com.bellatrixos.windows.installed_apps.IntentSites
import com.bellatrixos.windows.installed_apps.InstalledAppsDB.b

/** Callback synthetic untuk menu konteks taskbar dan hasil pengecekan update. */
class m(@JvmField val f4767a: Any) :
    Updates.UpdateListener,
    TaskBarAdapter.OnContextMenuListener {

    private val mainActivity: MainActivity
        get() = MainActivityBridge.outer(f4767a) ?: error("MainActivity tidak ditemukan")

    /**Listener menu konteks yang dipakai kedua cabang hasil dekompilasi. */
    private fun menuListener(
        mainActivityIn: MainActivityIn,
        mainActivity2: Context,
        taskBarAdapter: TaskBarAdapter,
        view: View,
        str: String,
        i4: Int
    ): UIContextMenu.OnMenuItemClickListener = UIContextMenu.OnMenuItemClickListener { _, i3 ->
        if (i3 == 0) {
            mainActivityIn.t(view, i4)
        }
        when (i3) {
            15 -> IntentSites.a(mainActivity2, str)
            16 -> taskBarAdapter.a(str)
            17 -> taskBarAdapter.d(i4)
            18 -> mainActivityIn.R(true)
        }
    }

    override fun a(view: View, i2: Int) {
        if (mainActivity.b0) return
        val mainActivityIn = MainActivityBridge.asIn(mainActivity)
        val act: MainActivity = mainActivity.W ?: mainActivity
        val taskBarAdapter: TaskBarAdapter = mainActivity.t0 ?: return

        // One menu path: the real package when the index names a task, the fallback name otherwise.
        val list2 = taskBarAdapter.D
        val strA = if (i2 in list2.indices) (list2[i2] as TaskBarItem).a() else ""
        val uIContextMenu = mainActivity.i0 ?: return
        val arrayList = uIContextMenu.f4745d
        arrayList.clear()
        var strA2 = AppsDatabase.a(strA)
        if (strA2 == act.getString(R.string.app_default_title)) {
            strA2 = act.getString(R.string.context_menu_open)
        }
        arrayList.add(UIMenuItem(0, 0, R.drawable.menu_ic_blank_20_regular, strA2))
        arrayList.add(UIMenuItem(1, 0, 0, ""))
        arrayList.add(
            UIMenuItem(
                0, 18, R.drawable.menu_ic_rearrange_20_regular,
                act.getString(R.string.context_menu_rearrange)
            )
        )
        if (!b.containsKey(strA)) {
            arrayList.add(
                UIMenuItem(
                    0, 15, R.drawable.menu_ic_settings_20_regular,
                    act.getString(R.string.context_menu_app_settings)
                )
            )
        }
        val zH = i2 in list2.indices && (list2[i2] as TaskBarItem).h()
        arrayList.add(
            if (zH) {
                UIMenuItem(
                    0, 17, R.drawable.menu_ic_pin_off_20_regular,
                    act.getString(R.string.context_menu_unpin_from_taskbar)
                )
            } else {
                UIMenuItem(
                    0, 16, R.drawable.menu_ic_pin_20_regular,
                    act.getString(R.string.context_menu_pin_to_taskbar)
                )
            }
        )
        uIContextMenu.c = menuListener(mainActivityIn, act, taskBarAdapter, view, strA, i2)
        uIContextMenu.a(R.style.AnimationDialogFadeUp)
        uIContextMenu.g(view, -act.resources.getDimensionPixelSize(R.dimen.ui_12dp))
        view.isActivated = true
        uIContextMenu.setOnDismissListener(com.bellatrixos.desktop.b(view, 1))
    }

    override fun b() {
        mainActivity.runOnUiThread(l(f4767a, 2))
    }
}
