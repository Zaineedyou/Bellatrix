package com.bellatrixos.desktop

import android.content.Context
import android.view.View
import com.bellatrixos.R
import com.bellatrixos.app_explorer.drives.d
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.context_menu.UIMenuItem
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.FilesRecyclerView
import com.bellatrixos.windows.files.clipboard.Clipboards

/** Menu konteks area kosong desktop. */
object ContextMenuDT {
    @JvmStatic
    fun a(
        context: Context,
        view: View,
        iArr: IntArray,
        desktopAdapter: DesktopAdapter,
        filesRecyclerView: FilesRecyclerView,
        mainActivityIn: MainActivityIn
    ) {
        view.x = iArr[0].toFloat()
        view.y = iArr[1].toFloat()
        val menu = mainActivityIn.l0() as? UIContextMenu ?: return
        val items = menu.f4745d
        items.clear()

        val viewItem = UIMenuItem(0, 12, R.drawable.menu_ic_blank_20_regular, context.getString(R.string.context_menu_view))
        viewItem.g = true
        val refreshItem = UIMenuItem(0, 11, R.drawable.menu_ic_blank_20_regular, context.getString(R.string.context_menu_refresh))
        val pasteItem = UIMenuItem(0, 8, R.drawable.menu_ic_blank_20_regular, context.getString(R.string.context_menu_paste))
        pasteItem.f4751h = Clipboards.f4922a.isNotEmpty()
        val newItem = UIMenuItem(0, 25, R.drawable.menu_ic_blank_20_regular, context.getString(R.string.context_menu_new))
        newItem.g = true

        items.addAll(
            listOf(
                viewItem,
                refreshItem,
                pasteItem,
                UIMenuItem(1, 0, 0, ""),
                newItem,
                UIMenuItem(1, 0, 0, ""),
                UIMenuItem(0, 10, R.drawable.menu_ic_settings_desktop_20_regular, context.getString(R.string.context_menu_display_settings)),
                UIMenuItem(0, 15, R.drawable.menu_ic_brush_20_regular, context.getString(R.string.context_menu_personalize))
            )
        )
        menu.c = d(mainActivityIn, context, desktopAdapter, filesRecyclerView, menu)
        menu.a(0)
        menu.f(view)
    }
}
