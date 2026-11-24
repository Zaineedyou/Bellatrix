package com.bellatrixos

import android.os.Build
import android.view.View
import com.bellatrixos.context_menu.ContextMenuGesture
import com.bellatrixos.context_menu.OnItemClickListener
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.context_menu.UIMenuItem
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.desktop.DesktopAdapter
import com.bellatrixos.desktop.DesktopDB
import com.bellatrixos.desktop.DesktopItem
import com.bellatrixos.desktop.interfaces.AllDirectionsDragCallback
import com.bellatrixos.desktop.utils.HoverAwareItemTouchHelper
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.FilesRecyclerView
import com.bellatrixos.widgets.GridAutofillLayoutManager
import com.bellatrixos.windows.drivers.network.NetworkMonitor
import com.bellatrixos.windows.installed_apps.AppsDatabase
import java.util.Objects

/** Runnable synthetic dari MainActivity: recreate, taskbar, desktop dan start menu. */
class c(@JvmField val v: MainActivity, @JvmField val u: Int) : Runnable {

    override fun run() {
        val mainActivity = v
        // W adalah referensi activity itu sendiri; field ini nullable.
        val act: MainActivity = mainActivity.W ?: mainActivity
        when (u) {
            0 -> {
                if (!mainActivity.isFinishing && !mainActivity.isDestroyed) {
                    mainActivity.recreate()
                }
            }
            1 -> {
                if (mainActivity.V == 2) {
                    mainActivity.Q0?.setOnClickListener(i(0, mainActivity))
                    mainActivity.G0?.setOnClickListener(i(1, mainActivity))
                    mainActivity.F0?.setOnClickListener(i(2, mainActivity))
                }
                val networkMonitor = NetworkMonitor(act, mainActivity.x0)
                mainActivity.k0 = networkMonitor
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                    networkMonitor.f4909a != null && !networkMonitor.c
                ) {
                    networkMonitor.f4909a.registerDefaultNetworkCallback(networkMonitor.b)
                    networkMonitor.c = true
                }
            }
            2 -> {
                val view = mainActivity.Z ?: return
                val iArr = Global.A
                view.x = iArr[0].toFloat()
                view.y = iArr[1].toFloat()
                val uIContextMenu = mainActivity.i0 ?: return
                val arrayList = uIContextMenu.f4745d
                arrayList.clear()
                arrayList.add(
                    UIMenuItem(
                        0, 11, R.drawable.menu_ic_clockwise_20_regular,
                        act.getString(R.string.context_menu_refresh)
                    )
                )
                arrayList.add(
                    UIMenuItem(
                        0, 15, R.drawable.menu_ic_settings_20_regular,
                        act.getString(R.string.st_menu_power_btn_start_settings)
                    )
                )
                uIContextMenu.c =
                    com.bellatrixos.startmenu.a(MainActivityBridge.asIn(act), 2)
                uIContextMenu.setOnDismissListener(com.bellatrixos.desktop.b(view, 0))
                uIContextMenu.a(0)
                uIContextMenu.f(view)
            }
            3 -> mainActivity.O0?.b()
            4 -> {
                AppsDatabase.b(act)
                val mainActivity3 = act
                if (DesktopDB.f4759a == null) {
                    DesktopDB.f4759a = mainActivity3.getSharedPreferences("DesktopPrefs", 0)
                }
                val desktopPrefs = DesktopDB.f4759a!!
                // Sekali migrasi: aktifkan penempatan bebas supaya ikon tidak selalu
                // bertambah ke kanan dan pengguna bisa menggeser ke slot kosong.
                if (!desktopPrefs.getBoolean("desktop_free_place_migrated", false)) {
                    desktopPrefs.edit()
                        .putBoolean("desktop_icons_arrange_auto", false)
                        .putBoolean("desktop_free_place_migrated", true)
                        .apply()
                }
                Global.q = desktopPrefs.getInt("desktop_icon_size", 1)
                Global.r = desktopPrefs.getBoolean("desktop_icon_show", true)
                DesktopAdapter.H = desktopPrefs.getBoolean("desktop_icons_arrange_auto", false)
                Global.p = SharedPrefs.f4755a?.getInt("system_input_tap_type", 0) ?: 0
                mainActivity.p0 = DesktopAdapter(act)
                mainActivity.runOnUiThread(c(mainActivity, 5))
            }
            else -> {
                val mainActivity4 = v
                val mainActivity5 = act
                val view2 = mainActivity4.Z ?: return
                val filesRecyclerView: FilesRecyclerView = mainActivity4.q0 ?: return
                val desktopAdapter: DesktopAdapter = mainActivity4.p0 ?: return

                // Konstruktor dipanggil dengan (1, 0); kelas ini menerima Context.
                val gridAutofillLayoutManager = GridAutofillLayoutManager(mainActivity5)
                gridAutofillLayoutManager.O = -1.0f
                gridAutofillLayoutManager.P = -1
                gridAutofillLayoutManager.Q = -1
                gridAutofillLayoutManager.M = mainActivity5
                gridAutofillLayoutManager.r1(-1)
                // Home is a fixed desktop, not a scrolling list: every icon lives in the cells on screen.
                gridAutofillLayoutManager.fixed = true
                filesRecyclerView.layoutManager = gridAutofillLayoutManager
                filesRecyclerView.adapter = desktopAdapter
                filesRecyclerView.itemAnimator = null
                val emptyPress = com.bellatrixos.app_explorer.drives.c(view2)
                filesRecyclerView.setOnEmptyPressListener(
                    FilesRecyclerView.OnEmptyPressListener { emptyPress.a() }
                )
                val emptyLongPress = com.bellatrixos.desktop.d(
                    mainActivity5, view2, desktopAdapter, filesRecyclerView,
                    MainActivityBridge.asIn(mainActivity4)
                )
                filesRecyclerView.setOnEmptyLongPressListener(
                    FilesRecyclerView.OnEmptyLongPressListener { iArr -> emptyLongPress.e(iArr) }
                )
                HoverAwareItemTouchHelper(AllDirectionsDragCallback(desktopAdapter)).i(filesRecyclerView)
                // Isi slot kosong setelah ukuran grid diketahui agar drag bebas langsung bisa dipakai.
                filesRecyclerView.post {
                    val cell = filesRecyclerView.resources
                        .getDimensionPixelSize(R.dimen.explorer_file_width)
                        .coerceAtLeast(1)
                    desktopAdapter.ensureFreeCells(
                        filesRecyclerView.height / cell,
                        filesRecyclerView.width / cell,
                    )
                }
                if (Global.p == 1) {
                    desktopAdapter.z = true
                }
                desktopAdapter.B = com.bellatrixos.desktop.d(
                    mainActivity5, view2, desktopAdapter, filesRecyclerView,
                    MainActivityBridge.asIn(mainActivity4)
                )
                mainActivity4.i0 = UIContextMenu(mainActivity5)
                mainActivity4.j0 = UIContextMenu(mainActivity5)
                ContextMenuGesture.a(mainActivity5)
                val desktopAdapter2 = mainActivity4.p0 ?: return
                desktopAdapter2.D = OnItemClickListener<Any> { obj ->
                    mainActivity4.n0(obj as DesktopItem)
                }
                desktopAdapter2.A = e(mainActivity4)
                desktopAdapter2.C = e(mainActivity4)
            }
        }
    }
}
