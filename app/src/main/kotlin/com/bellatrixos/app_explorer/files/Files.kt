package com.bellatrixos.app_explorer.files

import android.content.Context
import android.os.Environment
import com.bellatrixos.R
import com.bellatrixos.widgets.UINotification
import java.io.File

/** Utilitas bersama untuk file manager dan downloader. */
object Files {
    @JvmField val f4677a: String =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
    const val b: Int = 1
    const val c: Int = 1
    const val f4678d: Int = 1

    @JvmField var f4679e: SortBy? = null
    @JvmField var f4680f: SortBy? = null
    @JvmField var g: Boolean = false
    @JvmField var f4681h: Int = -1

    @JvmStatic
    fun a(context: Context, str: String) {
        UINotification.a(context, str, context.getString(R.string.toast_title_files))
    }

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun b(list: MutableList<Any>, sortBy: SortBy) {
        val comparator = when (sortBy) {
            SortBy.NAME_ASC -> compareBy<Any> { (it as File).name.lowercase() }
            SortBy.NAME_DESC -> compareByDescending<Any> { (it as File).name.lowercase() }
            SortBy.DATE_MODIFIED_ASC -> compareBy<Any> { (it as File).lastModified() }
            SortBy.DATE_MODIFIED_DESC -> compareByDescending<Any> { (it as File).lastModified() }
        }
        list.sortWith(comparator)
    }
}
