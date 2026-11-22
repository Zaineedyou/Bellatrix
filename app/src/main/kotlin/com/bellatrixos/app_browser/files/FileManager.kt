package com.bellatrixos.app_browser.files

import android.content.Context
import android.os.Environment
import android.util.Base64InputStream
import android.webkit.MimeTypeMap
import com.bellatrixos.R
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

/** Penentuan nama berkas hasil unduhan dan penulisan data Base64. */
object FileManager {
    /**
     * A file name coming from page script is untrusted: keep only a bare basename, and reject paths, traversal,
     * absolute/drive-qualified names and control characters instead of letting them reach the filesystem.
     */
    @JvmStatic
    fun safeName(raw: String?): String? {
        val name = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val base = name.substringAfterLast('/').substringAfterLast('\\')
        if (base != name) return null
        if (base == "." || base == ".." || base.contains("..") ||
            base.any { it.isISOControl() || it == '/' || it == '\\' } ||
            (base.length > 1 && base[1] == ':') || base.length > 200
        ) return null
        return base
    }

    /** Resolves [name] under Downloads, checking the canonical path so a symlink can't lead elsewhere. */
    @JvmStatic
    fun downloadsTarget(name: String): File? = try {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).canonicalFile
        val target = File(downloads, safeName(name) ?: return null).canonicalFile
        if (target.parentFile == downloads) target else null
    } catch (_: Exception) {
        null
    }

    @JvmStatic
    fun a(context: Context, str: String, str2: String?): String {
        var extension = "bin"
        try {
            val comma = str.indexOf(',')
            if (comma != -1) {
                val payload = str.substring(comma + 1)
                val guessed = Extensions.a(payload.substring(0, kotlin.math.min(10, payload.length)))
                try {
                    if (guessed == "bin") {
                        val mime = if (str.startsWith("data:")) {
                            str.substring(5, comma).split(";")[0]
                        } else {
                            "application/octet-stream"
                        }
                        MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)?.let { extension = it }
                    } else {
                        extension = guessed
                    }
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }

        if (str2 != null && str2.trim().isNotEmpty()) {
            val name = safeName(str2) ?: return generated(context, extension)
            return if (!name.contains(".")) "$name.$extension" else name
        }
        return generated(context, extension)
    }

    private fun generated(context: Context, extension: String): String =
        context.getString(R.string.downloader_file_downloaded) +
            UUID.randomUUID().toString().substring(0, 8) + "." + extension

    @JvmStatic
    @Throws(java.io.IOException::class)
    fun b(file: File, str: String) {
        val bytes = ByteArrayInputStream(str.toByteArray(StandardCharsets.US_ASCII))
        val decoder = Base64InputStream(bytes, 0)
        val output = FileOutputStream(file)
        try {
            val buffer = ByteArray(8192)
            while (true) {
                val read = decoder.read(buffer)
                if (read == -1) break
                output.write(buffer, 0, read)
            }
            output.flush()
        } finally {
            output.close()
            decoder.close()
            bytes.close()
        }
    }
}
