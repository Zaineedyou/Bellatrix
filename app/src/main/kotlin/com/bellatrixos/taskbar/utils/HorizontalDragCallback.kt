package com.bellatrixos.taskbar.utils

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.interfaces.ItemRearrangeInterface

/** Drag horizontal untuk mengurutkan item taskbar. */
class HorizontalDragCallback(@JvmField val f4833d: ItemRearrangeInterface) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int = makeMovementFlags(48, 0)

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        f4833d.c(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        f4833d.b(viewHolder.bindingAdapterPosition)
    }
}
