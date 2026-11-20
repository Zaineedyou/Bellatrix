package com.bellatrixos.desktop

import android.content.Context
import android.view.View
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.FilesRecyclerView

/** Buka menu konteks desktop baik dari area kosong maupun dari item. */
class d(
    @JvmField val u: Context,
    @JvmField val v: View,
    @JvmField val w: DesktopAdapter,
    @JvmField val x: FilesRecyclerView,
    @JvmField val y: MainActivityIn
) : FilesRecyclerView.OnEmptyLongPressListener, DesktopAdapter.OnContextMenuListener {
    override fun e(iArr: IntArray) {
        ContextMenuDT.a(u, v, iArr, w, x, y)
    }

    override fun i(iArr: IntArray, desktopItem: DesktopItem) {
        ContextMenuDT.a(u, v, iArr, w, x, y)
    }
}
