package com.bellatrixos.windows.switcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R
import com.bellatrixos.UIFragment
import com.bellatrixos.interfaces.MainActivityIn

/** Adapter daftar instance jendela yang sedang berjalan. */
class InstanceAdapter(
    @JvmField val y: MainActivityIn?,
    @JvmField val x: OnInstancesChanged?
) : RecyclerView.Adapter<InstanceAdapter.VH>() {

    fun interface OnInstancesChanged {
        fun l()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField var u: TextView = view.findViewById(R.id.title)
        @JvmField var v: ImageView = view.findViewById(R.id.close)
    }

    @JvmField val z: ArrayList<UIFragment> = ArrayList()

    override fun getItemCount(): Int = z.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val uIFragment = z[position]
        holder.u.text = uIFragment.c0() ?: uIFragment.tag
        holder.itemView.setOnClickListener {
            val main = y
            if (main != null) {
                main.p(uIFragment)
            }
            val switcher = UIInstanceSwitcher.c
            if (switcher != null && switcher.isShowing) {
                switcher.dismiss()
            }
        }
        holder.v.setOnClickListener {
            val main = y
            if (main != null) {
                main.P(uIFragment)
            }
            val idx = z.indexOf(uIFragment)
            if (idx >= 0) {
                z.removeAt(idx)
                notifyItemRemoved(idx)
            }
            val listener = x
            if (listener != null) {
                listener.l()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val viewInflate = LayoutInflater.from(parent.context).inflate(R.layout.taskbar_task_switcher_item, parent, false)
        val vh = VH(viewInflate)
        vh.u = viewInflate.findViewById(R.id.title)
        vh.v = viewInflate.findViewById(R.id.close)
        return vh
    }
}
