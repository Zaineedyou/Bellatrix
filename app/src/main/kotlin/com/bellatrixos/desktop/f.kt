package com.bellatrixos.desktop

import androidx.core.util.Supplier
import com.bellatrixos.context_menu.ContextMenuManager

/** Supplier dan callback menu konteks untuk item desktop. */
class f(viewHolder: DesktopAdapter.ViewHolder, i2: Int) :
    Supplier<Any?>, ContextMenuManager.ContextMenuCallback<Any> {
    @JvmField val u: Int = i2
    @JvmField val v: DesktopAdapter.ViewHolder = viewHolder


    override fun d(iArr: IntArray, obj: Any) {
        val desktopItem = obj as? DesktopItem ?: return
        v.x?.i(iArr, desktopItem)
        v.y?.i(iArr, desktopItem)
    }

    override fun get(): Any? {
        if (u == 0) {
            val position = v.bindingAdapterPosition
            if (position == -1) return null
            val adapter = v.bindingAdapter as? DesktopAdapter ?: return null
            return adapter.E.getOrNull(position)
        }
        return v.w
    }
}
