package com.bellatrixos.app_explorer.files

/** Mode pengurutan daftar file. */
enum class SortBy(val u: Int) {
    NAME_ASC(0),
    NAME_DESC(1),
    DATE_MODIFIED_ASC(2),
    DATE_MODIFIED_DESC(3)
}
