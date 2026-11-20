package com.bellatrixos.app_explorer.files

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.util.Supplier
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.app_explorer.files.interfaces.OnContextMenuListener
import com.bellatrixos.context_menu.ContextMenuManager
import com.bellatrixos.context_menu.OnItemClickListener
import com.bellatrixos.windows.files.FileMeta

/** ViewHolder dasar untuk berkas dengan dukungan menu konteks. */
abstract class BaseFileViewHolder(
    view: View,
    @JvmField val u: ImageView,
    @JvmField val v: TextView,
    @JvmField val w: FileAdapter
) : RecyclerView.ViewHolder(view) {
    @JvmField var x: OnItemClickListener<Any>? = null
    @JvmField var y: OnContextMenuListener<Any>? = null
    @JvmField var z: FileMeta? = null

    init {
        val supplier = Supplier<Any?> { z }
        ContextMenuManager.a(-1, view, supplier, ContextMenuManager.ContextMenuCallback<Any> { iArr, obj ->
            val fileMeta = obj as? FileMeta ?: return@ContextMenuCallback
            y?.d(iArr, fileMeta)
            if (w.I.isEmpty()) {
                val file = fileMeta.file
                if (w.I.add(file)) {
                    val index = w.G(file)
                    if (index != -1) w.notifyItemChanged(index)
                }
            }
        }, supplier)
    }

    abstract fun t(fileMeta: FileMeta)
}
