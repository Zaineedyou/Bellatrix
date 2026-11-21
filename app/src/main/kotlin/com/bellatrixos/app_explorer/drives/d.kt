package com.bellatrixos.app_explorer.drives

import android.content.Context
import android.content.DialogInterface
import android.view.View
import androidx.core.util.Consumer
import com.bellatrixos.App
import com.bellatrixos.R
import com.bellatrixos.app_explorer.files.FileAdapter
import com.bellatrixos.app_settings.fragments.DialogInterfaceOnClickListenerC0055d
import com.bellatrixos.app_notepad.b
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.context_menu.UIMenuItem
import com.bellatrixos.desktop.DesktopAdapter
import com.bellatrixos.desktop.DesktopItem
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.startmenu.StartMenuAdapter
import com.bellatrixos.startmenu.StartMenuItem
import com.bellatrixos.taskbar.TaskBarAdapter
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.FilesRecyclerView
import com.bellatrixos.widgets.OwlBars
import com.bellatrixos.widgets.WidgetEngine
import com.bellatrixos.windows.dialogs.UIDialog
import com.bellatrixos.windows.files.clipboard.Clipboards
import com.bellatrixos.windows.files.operations.New
import com.bellatrixos.windows.installed_apps.AppsDatabase
import com.bellatrixos.windows.installed_apps.IntentApps
import com.bellatrixos.windows.installed_apps.IntentSites
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/** Satu callback besar untuk pilih drive dan menu konteks desktop/start menu. */
@Suppress("UNCHECKED_CAST")
class d : DriveAdapter.OnItemClickListener, UIContextMenu.OnMenuItemClickListener {
    @JvmField val u: Int
    @JvmField val v: Context
    @JvmField val w: Any
    @JvmField val x: Any
    @JvmField val y: Any
    @JvmField val z: Any

    constructor(context: Context, str: String, startMenuItem: StartMenuItem, mainActivityIn: MainActivityIn, taskBarAdapter: TaskBarAdapter) {
        u = 2
        v = context
        w = str
        x = startMenuItem
        y = mainActivityIn
        z = taskBarAdapter
    }

    constructor(mainActivityIn: MainActivityIn, context: Context, desktopAdapter: DesktopAdapter, filesRecyclerView: FilesRecyclerView, uiContextMenu: UIContextMenu) {
        u = 1
        w = mainActivityIn
        v = context
        x = desktopAdapter
        y = filesRecyclerView
        z = uiContextMenu
    }

    constructor(list: List<DriveItem>, gVar: Consumer<Any>, context: Context, gVar2: Consumer<Any>, gVar3: Consumer<Any>) {
        u = 2
        v = context
        w = list
        x = gVar
        y = gVar2
        z = gVar3
    }

    override fun c(i2: Int) {
        val item = (w as List<DriveItem>)[i2]
        val value: Any
        val consumer: Consumer<Any>
        when {
            item.f() == 1 -> {
                value = item.f4674a
                consumer = x as Consumer<Any>
            }
            item.f() == 0 -> {
                val isInternal = item.b().contains(v.getString(R.string.storage_internal))
                value = item.c() ?: ""
                consumer = (if (isInternal) y else z) as Consumer<Any>
            }
            else -> return
        }
        consumer.accept(value)
    }

    override fun f(view: View, i2: Int) {
        if (u == 1) {
            desktopMenu(view, i2)
        } else {
            appMenu(i2)
        }
    }

    private fun desktopMenu(view: View, i2: Int) {
        val mainActivityIn = w as MainActivityIn
        when (i2) {
            8 -> {
                for (clipboardItem in Clipboards.f4922a) {
                    val str = clipboardItem.f4921a
                    val str2 = clipboardItem.b
                    mainActivityIn.j(clipboardItem.c, Clipboards.a(str, str2), str2)
                }
                Clipboards.f4922a.clear()
                return
            }
            15 -> {
                mainActivityIn.c0("UISettings", "3")
                return
            }
            260 -> {
                editOwlSurface(v, WidgetEngine.HOME_BAR)
                return
            }
            261 -> {
                editOwlSurface(v, WidgetEngine.TASKBAR_BAR)
                return
            }
            262 -> {
                val context = v
                val runtime = (context.applicationContext as App).runtime
                CoroutineScope(Dispatchers.Main).launch { OwlBars.newWidget(context, runtime) }
                return
            }
        }
        val context = v
        val menu = mainActivityIn.k() as? UIContextMenu ?: return
        val items = menu.f4745d
        var target = -1
        when (i2) {
            10 -> target = 11
            11 -> (y as FilesRecyclerView).animate().alpha(0f).setDuration(26L)
                .withEndAction(com.bellatrixos.desktop.c(y as FilesRecyclerView, 0)).start()
            25 -> {
                items.clear()
                items.add(UIMenuItem(0, 251, R.drawable.img_folder_sm, context.getString(R.string.context_menu_folder)))
                items.add(UIMenuItem(0, 252, R.drawable.img_file_txt_sm, context.getString(R.string.context_menu_doc_txt)))
                menu.c = UIContextMenu.OnMenuItemClickListener { _, which ->
                    if (which == 251) {
                        val dir = File(android.os.Environment.getExternalStorageDirectory(), "Bellatrix/Desktop")
                        val created = if (dir.exists() || dir.mkdirs()) New.a(context, dir) else null
                        if (created != null) {
                            mainActivityIn.j(3, created.absolutePath, created.name)
                        } else {
                            com.bellatrixos.app_explorer.files.Files.a(
                                context, context.getString(R.string.toast_folder_failed_created)
                            )
                        }
                    } else if (which == 252) {
                        mainActivityIn.c0("UINotepad", null)
                    }
                    menu.dismiss()
                }
            }
            12 -> {
                items.clear()
                val iconLg = context.getString(R.string.context_menu_ic_lg)
                var smallIcon = R.drawable.menu_ic_blank_6_regular
                if (Global.q == 0) smallIcon = R.drawable.menu_ic_dot_6_regular
                items.add(UIMenuItem(0, 123, if (Global.q == 2) R.drawable.menu_ic_dot_6_regular else R.drawable.menu_ic_blank_6_regular, iconLg))
                items.add(UIMenuItem(0, 122, if (Global.q == 1) R.drawable.menu_ic_dot_6_regular else R.drawable.menu_ic_blank_6_regular, context.getString(R.string.context_menu_ic_md)))
                items.add(UIMenuItem(0, 121, smallIcon, context.getString(R.string.context_menu_ic_sm)))
                items.add(UIMenuItem(1, 0, 0, ""))
                val arrange = UIMenuItem(0, 124, if (DesktopAdapter.H) R.drawable.menu_ic_checkmark_20_filled else R.drawable.menu_ic_blank_20_regular, context.getString(R.string.context_menu_desktop_ic_auto_arrange))
                arrange.f4751h = Global.f4836a == 2
                items.add(arrange)
                items.add(UIMenuItem(0, 120, if (Global.r) R.drawable.menu_ic_checkmark_20_filled else R.drawable.menu_ic_blank_20_regular, context.getString(R.string.context_menu_desktop_ic_show)))
                val desktopAdapter = x as DesktopAdapter
                val recyclerView = y as FilesRecyclerView
                menu.c = UIContextMenu.OnMenuItemClickListener { _, which ->
                    when (which) {
                        120 -> {
                            Global.r = !Global.r
                            desktopAdapter.notifyDataSetChanged()
                            com.bellatrixos.desktop.DesktopDB.f4759a?.edit()?.putBoolean("desktop_icon_show", Global.r)?.apply()
                        }
                        121 -> desktopAdapter.D(0)
                        122 -> desktopAdapter.D(1)
                        123 -> desktopAdapter.D(2)
                        124 -> {
                            DesktopAdapter.H = !DesktopAdapter.H
                            val arrayList = desktopAdapter.E
                            if (!DesktopAdapter.H) {
                                val dimen = recyclerView.resources.getDimensionPixelSize(R.dimen.explorer_file_width).coerceAtLeast(1)
                                val cols = recyclerView.width / dimen
                                DesktopAdapter.spanCount = cols.coerceAtLeast(1)
                                val size = (recyclerView.height / dimen) * cols - arrayList.size
                                if (size > 0) repeat(size) { arrayList.add(DesktopItem(-1, "", "blank_" + arrayList.size)) }
                            } else {
                                arrayList.removeAll { it.c() == -1 }
                            }
                            desktopAdapter.notifyDataSetChanged()
                            com.bellatrixos.desktop.DesktopDB.c(arrayList)
                            desktopAdapter.B()
                            com.bellatrixos.desktop.DesktopDB.f4759a?.edit()?.putBoolean("desktop_icons_arrange_auto", DesktopAdapter.H)?.apply()
                        }
                    }
                    menu.dismiss()
                }
            }
        }
        if (target != -1) mainActivityIn.c0("UISettings", target.toString())
        menu.a(0)
        menu.e(view, view.width, -view.height)
    }

    /** Opens one of the OWL surface files in Bellatrix's editor through the one app runtime. */
    private fun editOwlSurface(
        context: Context,
        name: String,
    ) {
        val runtime = (context.applicationContext as App).runtime
        CoroutineScope(Dispatchers.Main).launch { OwlBars.edit(context, runtime, name) }
    }

    private fun appMenu(i2: Int) {
        val context = v
        val str = w as String
        val mainActivityIn = y as MainActivityIn
        when (i2) {
            0 -> IntentApps.b(context, str, x as StartMenuItem, mainActivityIn)
            4 -> {
                // Save the real app name on the desktop shortcut. AppsDatabase.a(str)
                // alone falls back to "App" for APKs that aren't in our prefs yet.
                val fromItem = (x as? StartMenuItem)?.b()?.takeIf { it.isNotBlank() }
                mainActivityIn.j(1, str, fromItem ?: AppsDatabase.a(context, str))
            }
            23 -> mainActivityIn.s(str)
            24 -> {
                val title = StartMenuAdapter.G[str] as? String ?: "App"
                val listener = DialogInterfaceOnClickListenerC0055d(mainActivityIn, 2, str)
                val dialog = UIDialog(context, title, context.getString(R.string.dialog_app_uninstaller_subtitle))
                dialog.z = context.getString(R.string.context_menu_uninstall)
                dialog.C = b(1, listener)
                dialog.A = context.getString(R.string.dialog_actions_cancel)
                dialog.setCanceledOnTouchOutside(true)
                dialog.show()
            }
            15 -> IntentSites.a(context, str)
            16 -> (z as TaskBarAdapter).a(str)
            17 -> (z as TaskBarAdapter).e(str)
        }
    }
}
