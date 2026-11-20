package com.bellatrixos.startmenu.listeners

import androidx.recyclerview.widget.DiffUtil
import com.bellatrixos.startmenu.StartMenuItem

/** DiffUtil untuk daftar item Start Menu. */
class StartMenuItemDiffCallback : DiffUtil.Callback() {
    @JvmField var f4801a: List<StartMenuItem> = emptyList()
    @JvmField var b: List<StartMenuItem> = emptyList()

    override fun getOldListSize(): Int = f4801a.size
    override fun getNewListSize(): Int = b.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = f4801a[oldItemPosition]
        val new = b[newItemPosition]
        return old.b() == new.b() && old.c() == new.c() && old.a() == new.a()
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        f4801a[oldItemPosition].c() == b[newItemPosition].c()
}
