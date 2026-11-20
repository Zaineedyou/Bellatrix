package com.bellatrixos.windows.files.operations

import android.content.Context
import com.bellatrixos.R
import java.io.File
import kotlin.math.log
import kotlin.math.pow

/** Utilitas penghitungan berkas, ukuran, dan nama unik. */
object Utils {
    @JvmStatic
    fun a(file: File): Int {
        if (file.isFile) return 1
        var count = 1
        file.listFiles()?.forEach { count += a(it) }
        return count
    }

    @JvmStatic
    fun b(context: Context, counts: IntArray, total: Int?, bytes: Long, done: Long): String {
        val remain = Math.max((total ?: 0) - counts[0], 0)
        val delta = bytes - done
        val size = if (delta < 1024) {
            "$delta B"
        } else {
            val exponent = log(delta.toDouble(), 1024.0).toInt()
            val value = delta / 1024.0.pow(exponent)
            context.getString(
                R.string.msg_files_format_file_size,
                value,
                "KMGTPE"[exponent - 1].toString()
            )
        }
        return context.getString(R.string.msg_files_copy_item_remain, remain, size)
    }

    @JvmStatic
    fun c(file: File, str: String): File {
        val dot = str.lastIndexOf('.')
        val name = if (dot == -1) str else str.substring(0, dot)
        val ext = if (dot == -1) "" else str.substring(dot)
        var target = File(file, str)
        var index = 2
        while (target.exists()) {
            target = File(file, "$name ($index)$ext")
            index++
        }
        return target
    }
}
