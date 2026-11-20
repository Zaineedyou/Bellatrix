package com.bellatrixos.app_explorer.navigation

import android.content.Context
import android.view.View
import com.bellatrixos.app_music_player.AudioAdapter
import com.bellatrixos.app_music_player.AudioModel
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.taskbar.center_more_icons.DialogMoreIcons
import com.bellatrixos.windows.dialogs.UIDialogMouse

/** Klik item navigasi, lagu, atau tombol mouse pada dialog ikon. */
class d(
    @JvmField val v: Any?,
    @JvmField val w: Any?,
    @JvmField val x: Any?,
    @JvmField val u: Int
) : View.OnClickListener {
    override fun onClick(view: View) {
        when (u) {
            0 -> {
                val adapter = v as? NavigationAdapter ?: return
                val item = w as? NavigationItem ?: return
                val listener = adapter.A ?: return
                if (!item.b) return
                val position = (x as? NavigationAdapter.ViewHolder)?.bindingAdapterPosition ?: return
                listener.c(item.f4690d, position, item.f4689a)
            }
            1 -> {
                val adapter = v as? AudioAdapter ?: return
                val previous = adapter.x
                val position = (w as? AudioAdapter.ViewHolder)?.bindingAdapterPosition ?: return
                adapter.x = position
                if (previous != -1) adapter.notifyItemChanged(previous)
                adapter.notifyItemChanged(adapter.x)
                adapter.z.b(x as? AudioModel ?: return)
            }
            else -> {
                (v as? DialogMoreIcons)?.dismiss()
                UIDialogMouse(w as Context, x as MainActivityIn).show()
            }
        }
    }
}
