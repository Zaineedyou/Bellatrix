package com.bellatrixos.searchapp

import com.bellatrixos.app_explorer.UIExplorer
import com.bellatrixos.context_menu.OnItemClickListener
import com.bellatrixos.windows.files.FileMeta

/** Callback hasil pencarian: pengaturan atau berkas di explorer. */
class c(@JvmField val f4776a: Int, @JvmField val b: Any) : OnItemClickListener<Any> {
    override fun a(obj: Any) {
        if (f4776a == 0) {
            (b as? OnSettingsItemClickListener)?.b(obj as? String ?: "")
        } else {
            // A tap (or double tap, per the input setting) on a file or folder in Explorer.
            if (b is UIExplorer && obj is FileMeta) b.u0(obj)
        }
    }
}
