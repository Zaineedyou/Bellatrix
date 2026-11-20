package com.bellatrixos.taskbar.center_widgets.picker

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R

/** Daftar widget yang tersedia untuk ditambahkan ke widget center. */
class WidgetPickerAdapter(
    @JvmField val x: Context,
    @JvmField val y: MutableList<Any>,
    @JvmField val z: PackageManager
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    fun interface OnItemClickListener {
        fun c(i2: Int)
    }

    class AppHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField var u: TextView = view.findViewById(R.id.appNameTextView)
    }

    class WidgetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField var u: ImageView = view.findViewById(R.id.widgetIcon)
        @JvmField var v: TextView = view.findViewById(R.id.widgetName)
        @JvmField var w: TextView = view.findViewById(R.id.widgetSize)
    }

    @JvmField var A: OnItemClickListener? = null

    override fun getItemCount(): Int = y.size

    override fun getItemViewType(position: Int): Int = if (y[position] is String) 0 else 1

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AppHeaderViewHolder) {
            holder.u.text = y[position] as String
            return
        }
        val info = y[position] as? AppWidgetProviderInfo ?: return
        val viewHolder = holder as WidgetViewHolder
        viewHolder.v.text = info.loadLabel(z)
        var minWidth = info.minWidth
        if (minWidth == 0) minWidth = 1
        var minHeight = info.minHeight
        if (minHeight == 0) minHeight = 1
        viewHolder.w.text = if (minWidth >= minHeight) {
            "${minWidth / minHeight} x 1"
        } else {
            "1 x ${minHeight / minWidth}"
        }
        val density = x.resources.displayMetrics.densityDpi
        val drawable = info.loadPreviewImage(x, density) ?: info.loadIcon(x, density)
        viewHolder.u.setImageDrawable(drawable)
        viewHolder.itemView.setOnClickListener(a(position, 0, this))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 0) {
            val view = LayoutInflater.from(x).inflate(R.layout.widget_pick_item_header, parent, false)
            return AppHeaderViewHolder(view)
        }
        val view = LayoutInflater.from(x).inflate(R.layout.widget_pick_item, parent, false)
        return WidgetViewHolder(view)
    }
}
