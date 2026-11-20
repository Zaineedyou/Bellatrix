package com.bellatrixos.app_explorer.files.headers

/** Satu grup tanggal pada daftar berkas. */
class DateGroup(@JvmField val f4683a: DateGrouping) {
    override fun equals(other: Any?): Boolean = other is DateGroup && f4683a == other.f4683a
    override fun hashCode(): Int = f4683a.hashCode()
}
