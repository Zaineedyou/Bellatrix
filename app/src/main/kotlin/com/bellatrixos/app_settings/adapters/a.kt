package com.bellatrixos.app_settings.adapters

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Listener klik bersama untuk adapter pengaturan.
 * Perilaku metode disusun ulang dari pemanggilnya:
 * u=0 => ColorPickerAdapter.OnItemClickListener.b(warna), u=1 => tombol "more" instal.
 */
class a(@JvmField val v: Any, @JvmField val u: Int, @JvmField val w: Any) : View.OnClickListener {
    override fun onClick(view: View) {
        when (u) {
            0 -> (v as? ColorPickerAdapter)?.A?.b(w as String)
            1 -> {
                val adapter = v as? InstalledAppsAdapter ?: return
                val holder = w as? RecyclerView.ViewHolder ?: return
                val position = holder.bindingAdapterPosition - 1
                if (position >= 0) adapter.A?.a(view, position)
            }
        }
    }
}
