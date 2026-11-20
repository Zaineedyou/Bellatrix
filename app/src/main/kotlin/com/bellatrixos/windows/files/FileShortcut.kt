package com.bellatrixos.windows.files

import com.bellatrixos.windows.files.clipboard.ClipboardItem
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

/** Format berkas pintasan Bellatrix (int tipe + int panjang + UTF-8 path). */
object FileShortcut {
    @JvmStatic
    fun a(file: File?): ClipboardItem? {
        if (file == null || !file.exists()) return null
        return try {
            DataInputStream(FileInputStream(file)).use { input ->
                if (file.length() < 8) return null
                val type = input.readInt()
                val length = input.readInt()
                if (length <= 0 || length > 4096 || length > file.length() - 8) return null
                val bytes = ByteArray(length)
                input.readFully(bytes)
                ClipboardItem(type, String(bytes, StandardCharsets.UTF_8), "")
            }
        } catch (_: Exception) {
            null
        }
    }

    @JvmStatic
    fun b(file: File, item: ClipboardItem) {
        try {
            DataOutputStream(FileOutputStream(file)).use { output ->
                val bytes = item.f4921a.toByteArray(StandardCharsets.UTF_8)
                output.writeInt(item.c)
                output.writeInt(bytes.size)
                output.write(bytes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
