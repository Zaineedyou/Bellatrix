package com.bellatrixos.searchapp

/** Dipanggil saat hasil filter pencarian selesai dihitung. */
fun interface OnFilterCompleteListener {
    fun a(item: SearchItem)
}
