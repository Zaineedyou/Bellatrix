package com.bellatrixos.windows.icons.dialogs

/** Dialog pilih paket ikon. */
class DialogIconPacks {
    fun interface IconPackSelectionListener {
        fun d(label: String, packageName: String)
    }
}
