package com.bellatrixos.context_menu

import android.view.View
import androidx.core.util.Supplier

/** Klik item menu konteks (mode 0) atau lembar item generik (mode 1). */
class d(@JvmField val v: Any?, @JvmField val u: Int, @JvmField val w: Any?) : View.OnClickListener {
    override fun onClick(view: View) {
        when (u) {
            0 -> {
                val menu = v as? UIContextMenu ?: return
                val item = w as? UIMenuItem ?: return
                menu.c?.f(view, item.b)
                if (!item.g) menu.dismiss()
            }
            else -> {
                val obj = (v as? Supplier<*>)?.get() ?: return
                @Suppress("UNCHECKED_CAST")
                val listener = (w as? Supplier<*>)?.get() as? OnItemClickListener<*> ?: return
                listener.a(obj as Any)
            }
        }
    }
}
