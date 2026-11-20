package com.bellatrixos.app_explorer.navigation

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R

/** Adapter daftar navigasi samping file explorer. */
class NavigationAdapter(
    @JvmField val x: Context,
    @JvmField val z: MutableList<NavigationItem>
) : RecyclerView.Adapter<NavigationAdapter.ViewHolder>() {

    fun interface OnItemClickListener {
        fun c(str: String?, i2: Int, i3: Int)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField var u: TextView? = null
        @JvmField var v: View? = null
    }

    @JvmField var A: OnItemClickListener? = null
    @JvmField var y: Int = -1

    /** Cari posisi item berdasarkan id navigasi. */
    fun A(i2: Int): Int {
        for (i in z.indices) if (z[i].f4689a == i2) return i
        return -1
    }

    /** Ganti item terpilih dan beri tahu adapter. */
    fun B(i2: Int) {
        if (i2 == y) return
        if (y != -1) notifyItemChanged(y)
        y = i2
        notifyItemChanged(i2)
    }

    override fun getItemCount(): Int = z.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = z[position]
        val text = holder.u ?: return
        text.setCompoundDrawablesRelativeWithIntrinsicBounds(
            ContextCompat.getDrawable(x, item.a()), null, null, null
        )
        text.text = item.b()
        holder.itemView.alpha = if (item.b) 1.0f else 0.6f
        holder.itemView.setOnClickListener(d(this, item, holder, 0))
        holder.itemView.isSelected = position == y
        holder.v?.visibility = if (item.c) View.VISIBLE else View.GONE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.explorer_nav_item, parent, false)
        val holder = ViewHolder(view)
        holder.u = view.findViewById(R.id.text_view)
        holder.v = view.findViewById(R.id.divider)
        return holder
    }
}
