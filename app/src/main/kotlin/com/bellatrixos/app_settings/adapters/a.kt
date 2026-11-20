package com.bellatrixos.app_settings.adapters

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.taskbar.TaskBarDB
import com.bellatrixos.taskbar.TaskBarItem

/**
 * Listener klik bersama untuk adapter pengaturan dan taskbar.
 * u=0 => ColorPickerAdapter.OnItemClickListener.b(warna)
 * u=1 => tombol "more" instal
 * u=4 => tombol Start di taskbar (v = MainActivityIn, w = daftar item taskbar)
 */
class a(@JvmField val v: Any, @JvmField val u: Int, @JvmField val w: Any) : View.OnClickListener {
    @Suppress("UNCHECKED_CAST")
    override fun onClick(view: View) {
        when (u) {
            0 -> (v as? ColorPickerAdapter)?.A?.b(w as String)
            1 -> {
                val adapter = v as? InstalledAppsAdapter ?: return
                val holder = w as? RecyclerView.ViewHolder ?: return
                val position = holder.bindingAdapterPosition - 1
                if (position >= 0) adapter.A?.a(view, position)
            }
            4 -> {
                val main = v as? MainActivityIn ?: return
                if (main.K()) {
                    // Mode susun ulang taskbar aktif: simpan urutan lalu keluar dari mode itu.
                    (w as? List<TaskBarItem>)?.let { TaskBarDB.d(it) }
                    main.R(false)
                } else {
                    main.o(0)
                }
            }
        }
    }
}
