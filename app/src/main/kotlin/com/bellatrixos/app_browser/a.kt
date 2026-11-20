package com.bellatrixos.app_browser

import android.view.View
import androidx.activity.result.ActivityResultCallback
import androidx.core.util.Consumer
import com.bellatrixos.context_menu.UIContextMenu

/**
 * Callback synthetic hasil dekompilasi: permintaan izin WebView sekaligus
 * penerima klik menu konteks.
 */
class a(@JvmField var u: Any?) : UIContextMenu.OnMenuItemClickListener, ActivityResultCallback<Any?> {

    override fun onActivityResult(result: Any?) {
        e(result)
    }

    /** Hasil permintaan izin: tolak bila ada yang ditolak, jika tidak berikan semua. */
    fun e(obj: Any?) {
        @Suppress("UNCHECKED_CAST")
        val map = obj as? Map<*, *> ?: return
        val tab = u as? UIBrowserTab ?: return
        if (tab.E0 != null) {
            val denied = map.values.any { it as? Boolean == false }
            if (denied) {
                tab.E0?.deny()
                tab.E0 = null
                return
            }
            val request = tab.E0 ?: return
            request.grant(request.resources)
            tab.E0 = null
        }
    }

    override fun f(view: View, i2: Int) {
        @Suppress("UNCHECKED_CAST")
        (u as? Consumer<Int>)?.accept(i2)
    }
}
