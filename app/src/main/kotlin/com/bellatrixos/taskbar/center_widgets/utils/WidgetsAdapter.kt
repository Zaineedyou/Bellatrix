package com.bellatrixos.taskbar.center_widgets.utils

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bellatrixos.R
import com.bellatrixos.interfaces.ItemRearrangeInterface
import com.bellatrixos.taskbar.center_widgets.WidgetItem
import com.bellatrixos.taskbar.center_widgets.WidgetsDB
import com.bellatrixos.taskbar.center_widgets.callbacks.DragCallback
import com.bellatrixos.taskbar.center_widgets.utils.SavedWidgetItem
import java.util.Collections

/** Adapter untuk widget yang sudah ditempatkan di widget center. */
class WidgetsAdapter(@JvmField val x: Context) :
    RecyclerView.Adapter<WidgetsAdapter.WidgetViewHolder>(), ItemRearrangeInterface {

    fun interface OnItemClickListener {
        fun a(view: View, i2: Int)
    }

    class WidgetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField var u: FrameLayout = view.findViewById(R.id.widget_container)
        @JvmField var v: FrameLayout = view.findViewById(R.id.root_container)
        @JvmField var w: ImageButton = view.findViewById(R.id.action_button)
    }

    @JvmField val A: ArrayList<WidgetItem> = ArrayList()
    @JvmField var y: ItemTouchHelper? = null
    @JvmField var z: OnItemClickListener? = null

    fun A(view: AppWidgetHostView, i2: Int) {
        A.add(WidgetItem(view, false))
        k(A.size - 1)
        val className = view.appWidgetInfo?.provider?.className ?: ""
        val size = A.size - 1
        val saved = WidgetsDB.a()
        saved.add(SavedWidgetItem(className, i2, size))
        WidgetsDB.b(saved)
    }

    fun B(z: Boolean, recyclerView: WidgetsRecyclerView?) {
        if (recyclerView == null) return
        if (z) {
            val helper = ItemTouchHelper(DragCallback(this))
            y = helper
            helper.attachToRecyclerView(recyclerView)
        } else {
            y?.attachToRecyclerView(null)
            y = null
        }
    }

    fun C(i2: Int) {
        if (i2 < 0 || i2 >= A.size) return
        val item = A[i2]
        item.b = !item.b
        i(i2)
        val widgetId = item.f4827a.appWidgetId
        val saved = WidgetsDB.a()
        for (entry in saved) {
            if (entry.a() == widgetId) {
                entry.e(item.b)
                break
            }
        }
        WidgetsDB.b(saved)
    }

    override fun b(i2: Int) {
        if (i2 !in A.indices) return
        A.removeAt(i2)
        p(i2)
    }

    override fun c(i2: Int, i3: Int) {
        Collections.swap(A, i2, i3)
        l(i2, i3)
    }

    override fun getItemCount(): Int = A.size

    override fun onBindViewHolder(holder: WidgetViewHolder, position: Int) {
        val item = A[position]
        val view = item.f4827a
        try {
            if (holder.u.childCount == 0) {
                (view.parent as? ViewGroup)?.removeView(view)
                holder.u.addView(view)
            }
            val params = holder.itemView.layoutParams
            if (params is StaggeredGridLayoutManager.LayoutParams) {
                params.isFullSpan = item.b
            }
            holder.w.setOnClickListener { z?.a(it, holder.bindingAdapterPosition) }
        } catch (ignored: Exception) {
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WidgetViewHolder {
        val view = LayoutInflater.from(x).inflate(R.layout.taskbar_dialog_widgets_layout, parent, false)
        return WidgetViewHolder(view)
    }

    private fun k(index: Int) = notifyItemInserted(index)
    private fun i(index: Int) = notifyItemChanged(index)
    private fun p(index: Int) = notifyItemRemoved(index)
    private fun l(from: Int, to: Int) = notifyItemMoved(from, to)
}
