package com.bellatrixos.app_explorer.files.headers

import com.bellatrixos.R

/** Kategori tanggal untuk header daftar berkas. */
enum class DateGrouping(@JvmField val u: Int) {
    TODAY(R.string.explorer_date_today),
    YESTERDAY(R.string.explorer_date_yesterday),
    WEEK(R.string.explorer_date_this_week),
    MONTH(R.string.explorer_date_this_month),
    LAST_MONTH(R.string.explorer_date_last_month),
    OLD(R.string.explorer_date_old)
}
