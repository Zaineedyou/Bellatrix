package com.bellatrixos.context_menu

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R
import com.bellatrixos.windows.drivers.Display

/** Popup menu konteks yang dipakai hampir semua modul. */
class UIContextMenu(context: Context) : PopupWindow(context) {
    fun interface OnMenuItemClickListener {
        fun f(view: View, i2: Int)
    }

    private class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val u: ImageView = view.findViewById(R.id.menu_icon)
        val v: TextView = view.findViewById(R.id.menu_title)
        val w: TextView = view.findViewById(R.id.menu_subtitle)
    }

    private class DividerViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private inner class MenuAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount(): Int = f4745d.size

        override fun getItemViewType(position: Int): Int = f4745d[position].f4747a

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(f4744a)
            return if (viewType == 0) {
                ItemViewHolder(inflater.inflate(R.layout.widget_context_menu_item, parent, false))
            } else {
                DividerViewHolder(inflater.inflate(R.layout.widget_context_menu_divider, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = f4745d[position]
            if (holder !is ItemViewHolder) return
            holder.v.text = item.c
            if (item.f4749e == 0) {
                holder.u.visibility = View.GONE
            } else {
                holder.u.visibility = View.VISIBLE
                holder.u.setImageResource(item.f4749e)
            }
            holder.v.setTypeface(null, if (item.f4750f) Typeface.BOLD else Typeface.NORMAL)
            val chevron = if (item.g || item.f4752i) {
                ContextCompat.getDrawable(holder.itemView.context, R.drawable.menu_ic_chevron_right_14)
            } else {
                null
            }
            holder.w.setCompoundDrawablesWithIntrinsicBounds(null, null, chevron, null)
            holder.w.text = item.f4748d ?: ""
            holder.itemView.setOnClickListener(d(this@UIContextMenu, 0, item))
            holder.itemView.isEnabled = item.f4751h
            holder.v.isEnabled = item.f4751h
        }
    }

    companion object {
        @JvmField var f4743f: UIContextMenu? = null
    }

    @JvmField val f4744a: Context = context
    @JvmField val b: View
    @JvmField var c: OnMenuItemClickListener? = null
    @JvmField val f4745d: ArrayList<UIMenuItem> = ArrayList()
    @JvmField val f4746e: RecyclerView.Adapter<*>

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.widget_context_menu_dialog, null)
        b = view
        val adapter = MenuAdapter()
        f4746e = adapter
        view.findViewById<RecyclerView>(R.id.recycler_view).apply {
            layoutManager = LinearLayoutManager(f4744a, 1, false)
            this.adapter = adapter
        }
        contentView = view
        windowLayoutType = 1002
        elevation = 2.0f
        isFocusable = true
        setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.widget_popup_bg))
    }

    fun a(i2: Int) {
        animationStyle = i2
        f4746e.notifyDataSetChanged()
    }

    fun b(view: View?): Boolean {
        f4743f = this
        return view == null || view.windowToken == null || !view.isAttachedToWindow
    }

    fun c() {
        val spec = View.MeasureSpec.makeMeasureSpec(0, 0)
        b.measure(spec, spec)
        width = b.measuredWidth
        height = b.measuredHeight
    }

    fun d(listener: Any?) {
        c = listener as? OnMenuItemClickListener
    }

    override fun dismiss() {
        super.dismiss()
        if (f4743f === this) f4743f = null
    }

    fun e(view: View, i2: Int, i3: Int) {
        if (b(view)) return
        c()
        showAsDropDown(view, i2, i3)
    }

    fun f(view: View) {
        if (b(view)) return
        c()
        showAsDropDown(view)
    }

    fun g(view: View, i2: Int) {
        if (b(view)) return
        c()
        showAsDropDown(view, view.width / 2 - width / 2, i2)
    }

    fun h(view: View) {
        if (b(view)) return
        c()
        val x = if (Display.f4898a) width / 2 - view.width / 2 else view.width / 2 - width / 2
        showAsDropDown(view, x, -height - view.height)
    }

    /** Shows this native menu at pixel coordinates inside [view], for anchors that aren't a single View. */
    fun showAt(view: View, x: Int, y: Int) {
        if (b(view)) return
        c()
        showAtLocation(view, Gravity.TOP or Gravity.START, x, y)
    }
}
