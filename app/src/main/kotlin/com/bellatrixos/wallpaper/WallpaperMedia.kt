package com.bellatrixos.wallpaper

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.bellatrixos.theme.BACKGROUND_EXTENSIONS
import com.bellatrixos.theme.WALLPAPER_IMAGE_EXTENSIONS
import com.bellatrixos.theme.VIDEO_EXTENSIONS
import java.io.File
import java.util.Locale

/** What a picked file is, before the wallpaper page decides how to apply it. */
enum class WallpaperKind {
    IMAGE,
    VIDEO,
    UNKNOWN,
}

private val MIME_EXTENSIONS =
    mapOf(
        "image/jpeg" to "jpg", "image/png" to "png", "image/webp" to "webp", "image/gif" to "gif",
        "image/heic" to "heic", "image/heif" to "heif", "image/avif" to "avif", "image/bmp" to "bmp",
        "image/x-ms-bmp" to "bmp", "image/tiff" to "tiff", "image/x-icon" to "ico", "image/apng" to "apng",
        "image/svg+xml" to "svg", "image/x-adobe-dng" to "dng", "image/dng" to "dng",
        "image/vnd.adobe.photoshop" to "psd",
        "video/mp4" to "mp4", "video/x-m4v" to "m4v", "video/3gpp" to "3gp", "video/3gpp2" to "3g2",
        "video/quicktime" to "mov", "video/webm" to "webm", "video/x-matroska" to "mkv",
        "video/mpeg" to "mpeg", "video/mp2t" to "ts", "video/x-msvideo" to "avi", "video/x-flv" to "flv",
        "video/x-ms-wmv" to "wmv", "video/ogg" to "ogv", "video/ogv" to "ogv",
    )

/** The picked file's own name, from a document provider or a media Uri, without trusting the MIME type alone. */
fun Uri.displayName(context: Context): String? {
    if (scheme == "content") {
        context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index)?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }
    }
    // Content providers often use "raw:/storage/.../clip.mkv" or "msf:123" as the last segment.
    return lastPathSegment
        ?.substringAfterLast('/')
        ?.substringAfterLast(':')
        ?.takeIf { it.isNotBlank() }
}

/** Extension from the file name, or from its MIME type, restricted to formats this launcher lists as backgrounds. */
fun Uri.backgroundExtension(context: Context): String? {
    val fromName =
        displayName(context)
            ?.substringAfterLast('.', "")
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it.isNotBlank() && it.all { c -> c.isLetterOrDigit() } && it in BACKGROUND_EXTENSIONS }
    if (fromName != null) return fromName
    val mime = context.contentResolver.getType(this)?.lowercase(Locale.ROOT) ?: return null
    return MIME_EXTENSIONS[mime] ?: mime.substringAfter('/').takeIf { it in BACKGROUND_EXTENSIONS }
}

/** Detects by MIME first, then by file name, then by whether the file actually has a video track. */
fun Uri.wallpaperKind(context: Context): WallpaperKind {
    val mime = context.contentResolver.getType(this)?.lowercase(Locale.ROOT)
    if (mime != null) {
        if (mime.startsWith("image/")) return WallpaperKind.IMAGE
        if (mime.startsWith("video/")) return WallpaperKind.VIDEO
    }
    val extension = backgroundExtension(context)
    if (extension != null) {
        if (extension in WALLPAPER_IMAGE_EXTENSIONS) return WallpaperKind.IMAGE
        if (extension in VIDEO_EXTENSIONS) return WallpaperKind.VIDEO
    }
    // A file manager may report application/octet-stream for a perfectly good clip.
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, this)
        if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) != null ||
            retriever.frameAtTime != null
        ) {
            return WallpaperKind.VIDEO
        }
    } catch (_: Exception) {
    } finally {
        runCatching { retriever.release() }
    }
    return if (mime?.startsWith("image/") == true) WallpaperKind.IMAGE else WallpaperKind.UNKNOWN
}

/** Copies a picked file into the launcher's own storage, keeping its extension so the live wallpaper can open it. */
fun Uri.copyToBackgroundFile(
    context: Context,
    target: File,
): Boolean =
    runCatching {
        target.parentFile?.mkdirs()
        context.contentResolver.openInputStream(this)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return false
        target.length() > 0L
    }.getOrDefault(false)
