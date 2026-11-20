package com.bellatrixos.app_explorer.files.interfaces

/** Callback menu konteks dengan posisi item yang dipilih. */
fun interface OnContextMenuListener<T> {
    fun d(position: IntArray, obj: Any)
}
