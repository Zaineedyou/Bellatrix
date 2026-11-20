package com.bellatrixos.app_explorer.files.interfaces

import androidx.recyclerview.widget.DiffUtil

/** DiffUtil untuk daftar berkas; diisi adapter pemiliknya. */
class FileDiffCallback : DiffUtil.Callback() {
    override fun getOldListSize(): Int = TODO()
    override fun getNewListSize(): Int = TODO()
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = TODO()
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = TODO()
}
