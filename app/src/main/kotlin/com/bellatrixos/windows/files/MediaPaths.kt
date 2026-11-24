package com.bellatrixos.windows.files

import android.net.Uri
import com.bellatrixos.storage.PhoneStorage
import java.io.File

/**
 * One place that understands the three names a picked file can have: a guest path
 * ("/sdcard/Download/a.mp3"), the Android host path behind it, and a content Uri
 * from MediaStore or the system picker. Media apps used to treat every one of
 * them as a plain [File], which is why a file the phone reported could not be
 * opened even though it was there.
 */
object MediaPaths {
    /** A content or file Uri carried in a path string, if that is what it is. */
    fun uri(value: String?): Uri? =
        value?.takeIf { it.startsWith("content://") || it.startsWith("file://") }?.let(Uri::parse)

    /** The Android file a path names, whether Linux called it by its guest or host name. */
    fun host(value: String?): File {
        val path = value.orEmpty()
        if (path.startsWith("file://")) return Uri.parse(path).path?.let(::File) ?: File(path)
        val guestRoot = PhoneStorage.GUEST
        if (path == guestRoot || path == "$guestRoot/") return PhoneStorage.root
        if (path.startsWith("$guestRoot/")) return File(PhoneStorage.root, path.removePrefix("$guestRoot/"))
        return File(path)
    }

    /** The guest name of a path Android reported, for callers that want Linux's spelling. Null when it isn't shared storage. */
    fun guest(value: String?): String? {
        val path = value.orEmpty()
        val hostRoot = PhoneStorage.root.absolutePath.trimEnd('/')
        return when {
            path == hostRoot || path == "$hostRoot/" -> PhoneStorage.GUEST
            path.startsWith("$hostRoot/") -> PhoneStorage.GUEST + path.removePrefix(hostRoot)
            path.startsWith(PhoneStorage.GUEST + "/") -> path
            else -> null
        }
    }
}
