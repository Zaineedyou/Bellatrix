package com.bellatrixos.windows.files.operations

import android.content.Context
import com.bellatrixos.R
import com.bellatrixos.app_explorer.files.FileAdapter
import com.bellatrixos.app_explorer.files.Files
import com.bellatrixos.windows.files.clipboard.Clipboards
import java.io.File

/** Menjalankan operasi tempel dari clipboard berkas. */
object Paste {
    @JvmStatic
    fun a(context: Context, str: String, fileAdapter: FileAdapter) {
        val items = Clipboards.f4922a
        if (items.isEmpty()) return
        val operations = FileOperations.c(context)
        if (items.size == 1) {
            val item = items[0]
            val source = File(item.f4921a, item.b)
            val title = if (Files.g) R.string.msg_files_title_moving else R.string.msg_files_title_copy
            operations.f(operations.f4924a.getString(title), item.b)
            val executor = FileOperations.f4923i ?: return
            if (item.c == 3) {
                executor.execute(c(operations, source, str, item.b))
            } else {
                executor.execute(c(operations, str, item.b, source))
            }
        } else {
            val title = if (Files.g) R.string.msg_files_title_moving else R.string.msg_files_title_copy
            operations.g = FileOperations.ConflictAction.u
            operations.f(operations.f4924a.getString(title), items[0].b)
            FileOperations.f4923i?.execute(h(operations, ArrayList(items), str))
        }
        operations.c = b(fileAdapter, 1)
    }
}
