package com.bellatrixos.windows.files.operations

import android.content.Context
import com.bellatrixos.R
import com.bellatrixos.app_explorer.files.Files
import java.io.File

/** Ganti nama berkas dengan validasi. */
object Rename {
    @JvmStatic
    fun a(context: Context, file: File?, str: String): File? {
        val errorRes = when {
            file == null || !file.exists() -> R.string.toast_file_rename_not_exist
            file.parentFile == null -> R.string.toast_file_rename_parent_undetermined
            else -> {
                val target = File(file.parentFile, str)
                when {
                    target.exists() -> R.string.toast_file_rename_already_exist
                    file.renameTo(target) -> return target
                    else -> R.string.toast_file_rename_failed
                }
            }
        }
        Files.a(context, context.getString(errorRes))
        return null
    }
}
