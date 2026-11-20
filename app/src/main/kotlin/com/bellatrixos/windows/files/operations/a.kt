package com.bellatrixos.windows.files.operations

import android.content.Context
import android.content.DialogInterface
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R
import com.bellatrixos.app_explorer.files.FileAdapter
import com.bellatrixos.desktop.DesktopAdapter
import com.bellatrixos.desktop.DesktopDB
import com.bellatrixos.desktop.DesktopItem
import com.bellatrixos.windows.dialogs.UIDialog
import java.io.File

/** Callback konfirmasi hapus dan ganti nama item. */
class a(
    @JvmField val v: Context,
    @JvmField val x: Int,
    @JvmField val w: File,
    @JvmField val y: Any,
    @JvmField val z: RecyclerView.Adapter<*>
) : DialogInterface.OnClickListener {

    @JvmField var u: Int = 0

    constructor(context: Context, file: File, dialog: UIDialog, adapter: DesktopAdapter, i2: Int) :
        this(context, i2, file, dialog, adapter) {
        u = 1
    }

    override fun onClick(dialogInterface: DialogInterface, clicked: Int) {
        val operations = FileOperations.c(v)
        if (u == 0) {
            val files = if (x == 1) {
                listOf(File(w.absolutePath))
            } else {
                (y as? Collection<*>)?.filterIsInstance<File>() ?: emptyList()
            }
            if (files.isNotEmpty()) {
                operations.f(operations.f4924a.getString(R.string.msg_files_title_delete), files[0].name)
                FileOperations.f4923i?.execute(g(operations, files, 0))
            }
            operations.c = b(z as FileAdapter, 0)
            operations.f4925d = com.bellatrixos.taskbar.center_action.b(v)
            return
        }
        val dialog = y as UIDialog
        val renamed = Rename.a(v, w, dialog.w?.text?.toString() ?: "") ?: return
        val adapter = z as DesktopAdapter
        val item: DesktopItem = adapter.E.getOrNull(x) ?: return
        item.e(renamed.name)
        item.d(renamed.absolutePath)
        adapter.notifyItemChanged(x)
        DesktopDB.b(x, item)
        dialog.dismiss()
    }
}
