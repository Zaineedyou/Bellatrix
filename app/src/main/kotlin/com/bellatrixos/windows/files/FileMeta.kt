package com.bellatrixos.windows.files

import java.io.File
import java.util.Locale

/** Metadata ringan untuk pengurutan dan pencarian berkas. */
class FileMeta(@JvmField val file: File) {
    /**
     * A folder: [File.isDirectory], or, when the system will not say what an entry is (it is neither a file nor a
     * folder to `stat`), an entry that still lists. Such folders were shown and opened as unreadable files.
     */
    @JvmField val isDirectory: Boolean = file.isDirectory || (!file.isFile && file.canRead() && file.list() != null)
    @JvmField val modified: Long = file.lastModified()
    @JvmField val size: Long = file.length()
    @JvmField val lowerName: String = file.name.lowercase(Locale.ROOT)
    @JvmField val pathHash: Long = file.absolutePath.hashCode().toLong()
    @JvmField var linkFile: Boolean = false
    @JvmField var executable: Boolean = false

    @JvmField val nameLength: Int
    @JvmField val extensionStart: Int

    init {
        var length = lowerName.length
        if (length > 5 && lowerName.endsWith(".link")) {
            length -= 5
            linkFile = true
        }
        if (lowerName.length > 5 && lowerName.endsWith(".hexe")) executable = true
        nameLength = length

        var start = -1
        for (i in length - 1 downTo 0) {
            if (lowerName[i] == '.') {
                start = i + 1
                break
            }
        }
        extensionStart = start
    }
}
