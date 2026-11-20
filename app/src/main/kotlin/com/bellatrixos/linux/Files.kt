package com.bellatrixos.linux

import java.io.File

/** Small file helpers the rootfs installer needs, replacing Bellatrix's storage/upgrade helpers. */

/** Recursive delete that doesn't follow symlinks out of [dir]. */
fun deleteTree(dir: File) {
    if (!dir.isDirectory || java.nio.file.Files.isSymbolicLink(dir.toPath())) {
        if (dir.exists()) dir.delete()
        return
    }
    dir.listFiles()?.forEach { child ->
        if (child.isDirectory && !java.nio.file.Files.isSymbolicLink(child.toPath())) deleteTree(child)
        else child.delete()
    }
    dir.delete()
}

/** Writes beside then renames over, so readers never see a half-written file or write through a symlink. */
fun writeAtomically(
    file: File,
    bytes: ByteArray,
) {
    file.parentFile?.mkdirs()
    val tmp = File(file.parentFile, "${file.name}.tmp-${System.nanoTime()}")
    try {
        tmp.writeBytes(bytes)
        val replaced = tmp.renameTo(file)
        if (!replaced) {
            file.delete()
            check(tmp.renameTo(file)) { "can't replace ${file.path}" }
        }
    } finally {
        tmp.delete()
    }
}

/** Lowercase hex of a digest, matching the OWL `hex()` helper Bellatrix's installer used. */
fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }
