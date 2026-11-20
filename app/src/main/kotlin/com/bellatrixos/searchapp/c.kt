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
            // UIExplorer.u0(FileMeta) belum tersedia di port Kotlin; lewati pembukaan berkas.
            if (b is UIExplorer && obj is FileMeta) {
                val file = obj.file
                // biarkan pemanggil yang membuka melalui jalur lain bila tersedia
                file.exists()
            }
        }
    }
}
