package com.bellatrixos.desktop.utils

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.app_explorer.navigation.a
import com.bellatrixos.desktop.interfaces.AllDirectionsDragCallback

/** ItemTouchHelper yang meneruskan drag hover ke callback tanpa reflection. */
class HoverAwareItemTouchHelper(
    @JvmField val callback: AllDirectionsDragCallback,
) : ItemTouchHelper(callback) {

    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        super.attachToRecyclerView(recyclerView)
        if (recyclerView == null) return
        recyclerView.setOnTouchListener(a(this, recyclerView, 2))
    }

    /** Nama attachToRecyclerView, dipakai pemanggil yang belum diubah. */
    fun i(recyclerView: RecyclerView?) {
        attachToRecyclerView(recyclerView)
    }
}
