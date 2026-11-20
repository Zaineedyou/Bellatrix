package com.bellatrixos.desktop.interfaces

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.desktop.DesktopAdapter
import com.bellatrixos.desktop.DesktopItem
import java.util.Collections

/** Drag callback yang mengizinkan perpindahan ke segala arah. */
class AllDirectionsDragCallback(@JvmField val f4762d: DesktopAdapter) : ItemTouchHelper.Callback() {
    @JvmField val f4763e: Paint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 6.0f
        style = Paint.Style.STROKE
    }

    @JvmField val f4764f: Paint = Paint().apply {
        color = Color.parseColor("#33FAFAFA")
        style = Paint.Style.FILL
    }

    @JvmField var g: Int = -1
    @JvmField var f4765h: Int = -1
    @JvmField var f4766i: Boolean = false

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (g == -1 || f4765h == -1 || g == f4765h) {
            g = -1
            f4765h = -1
            f4766i = false
            return
        }
        val adapter = f4762d
        val list = adapter.E
        val from = g
        val to = f4765h
        if (list.getOrNull(to)?.c() == -1) {
            list[to] = list[from]
            list[from] = DesktopItem(-1, "", "blank_$from")
            adapter.notifyItemChanged(from)
            adapter.notifyItemChanged(to)
            if (!adapter.y) {
                adapter.C?.let {
                    adapter.y = true
                    it.j()
                    adapter.B()
                }
            }
        } else {
            Collections.swap(list, from, to)
            adapter.notifyItemMoved(from, to)
            if (!adapter.y) {
                adapter.C?.let {
                    adapter.y = true
                    it.j()
                    adapter.B()
                }
            }
        }
        g = -1
        f4765h = -1
        f4766i = false
    }

    override fun getMoveThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.4f

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val position = viewHolder.bindingAdapterPosition
        if (position == -1 || f4762d.E.getOrNull(position)?.c() == -1) return 0
        return makeMovementFlags(15, 15)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive && !f4766i) {
            if (kotlin.math.abs(dX) > 0f || kotlin.math.abs(dY) > 0f) {
                f4766i = true
                if (UIContextMenu.f4743f?.isShowing == true) {
                    UIContextMenu.f4743f?.dismiss()
                    UIContextMenu.f4743f = null
                }
            }
        }
    }

    override fun onChildDrawOver(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder?,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val targetPosition = f4765h
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive && targetPosition != -1) {
            val item = f4762d.E.getOrNull(targetPosition) ?: return
            val target = recyclerView.findViewHolderForAdapterPosition(targetPosition) ?: return
            val view = target.itemView
            if (item.c() == -1) {
                c.drawRoundRect(
                    view.left.toFloat(), view.top.toFloat(), view.right.toFloat(), view.bottom.toFloat(),
                    14.0f, 14.0f, f4764f
                )
            } else {
                c.drawLine(view.left.toFloat(), view.top.toFloat(), view.right.toFloat(), view.top.toFloat(), f4763e)
            }
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        k(viewHolder, target)
        return false
    }

    override fun onMoved(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        fromPos: Int,
        target: RecyclerView.ViewHolder,
        toPos: Int,
        x: Int,
        y: Int
    ) {
        k(viewHolder, target)
    }

    /** Catat posisi asal dan tujuan (nama dipertahankan untuk pemanggil hover). */
    fun k(viewHolder: RecyclerView.ViewHolder, viewHolder2: RecyclerView.ViewHolder) {
        val position = viewHolder2.bindingAdapterPosition
        if (g == -1) g = viewHolder.bindingAdapterPosition
        f4765h = position
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {}
}
