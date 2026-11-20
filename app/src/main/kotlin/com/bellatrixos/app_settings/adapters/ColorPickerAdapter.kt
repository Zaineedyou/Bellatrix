package com.bellatrixos.app_settings.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R

/** Pemilih warna aksen pada halaman warna. */
class ColorPickerAdapter(
    @JvmField val x: Context,
    @JvmField val y: List<String>,
    @JvmField val z: String
) : RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder>() {

    class ColorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField var u: View = view.findViewById(R.id.colorPickerButton)
    }

    fun interface OnItemClickListener {
        fun b(str: String)
    }

    @JvmField var A: OnItemClickListener? = null

    override fun getItemCount(): Int = y.size

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val color = y[position]
        holder.u.backgroundTintList = ColorStateList.valueOf(Color.parseColor(color))
        if (color == z) {
            holder.u.foreground = ContextCompat.getDrawable(x, R.drawable.ui_settings_color_picker_current)
        }
        holder.u.setOnClickListener(a(this, 0, color))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(x).inflate(R.layout.settings_color_item, parent, false)
        return ColorViewHolder(view)
    }
}
