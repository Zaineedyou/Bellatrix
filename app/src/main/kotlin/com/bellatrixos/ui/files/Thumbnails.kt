package com.bellatrixos.ui.files

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Files that get a picture in place of their icon: images (not SVG), videos and PDFs. */
internal fun canThumbnail(name: String): Boolean = (isImage(name) && extension(name) != "svg") || isVideo(name) || extension(name) == "pdf"

internal fun isVideo(name: String) = mimeOf(name)?.startsWith("video/") == true

/** Decoded thumbnails, newest kept; a folder of photos scrolls back and forth without decoding twice. */
private val cache =
    object : LruCache<String, ImageBitmap>(24 shl 20) {
        override fun sizeOf(
            key: String,
            value: ImageBitmap,
        ) = value.width * value.height * 4
    }

/** Files that couldn't be decoded, so a broken photo isn't retried on every scroll. */
private val failed = ConcurrentHashMap.newKeySet<String>()

/** A few at a time: a fast scroll through a big folder mustn't decode a hundred photos at once. */
private val gate = Semaphore(3)

/**
 * A square thumbnail of [file], [px] on a side, or null when it can't be made (unreadable, corrupt,
 * a format this Android can't decode). Decodes off the main thread and keeps the result.
 */
internal suspend fun loadThumbnail(
    file: File,
    name: String,
    px: Int,
): ImageBitmap? {
    val key = "${file.path}|${file.lastModified()}|${file.length()}|$px"
    cache.get(key)?.let { return it }
    if (key in failed) return null
    return withContext(Dispatchers.IO) {
        gate.withPermit {
            val bitmap =
                runCatching {
                    when {
                        extension(name) == "pdf" -> pdfFirstPage(file, px)
                        isVideo(name) -> videoFrame(file, px)
                        else -> imageSquare(file, px)
                    }
                }.getOrNull()
            if (bitmap == null) {
                failed.add(key)
                null
            } else {
                bitmap.asImageBitmap().also { cache.put(key, it) }
            }
        }
    }
}

/** The platform decoder reads EXIF rotation and scales while decoding, so a 12 MP photo never exists in full. */
private fun imageSquare(
    file: File,
    px: Int,
): Bitmap {
    val decoded =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val scale = max(px.toFloat() / info.size.width, px.toFloat() / info.size.height)
            if (scale < 1f) {
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt().coerceAtLeast(1),
                    (info.size.height * scale).roundToInt().coerceAtLeast(1),
                )
            }
        }
    return square(decoded, px, fromTop = false)
}

private fun videoFrame(
    file: File,
    px: Int,
): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.path)
        val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: retriever.getFrameAtTime(0L) ?: return null
        square(frame, px, fromTop = false)
    } finally {
        runCatching { retriever.release() }
    }
}

/** The top of a PDF's first page, on white. */
private fun pdfFirstPage(
    file: File,
    px: Int,
): Bitmap? {
    val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    try {
        val renderer = PdfRenderer(fd)
        try {
            if (renderer.pageCount == 0) return null
            val page = renderer.openPage(0)
            try {
                val scale = px.toFloat() / min(page.width, page.height)
                val bitmap =
                    Bitmap.createBitmap(
                        (page.width * scale).roundToInt().coerceAtLeast(1),
                        (page.height * scale).roundToInt().coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888,
                    )
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return square(bitmap, px, fromTop = true)
            } finally {
                page.close()
            }
        } finally {
            renderer.close()
        }
    } finally {
        fd.close()
    }
}

/** [src] scaled so its short side is [px], then cropped to a square from the middle (or the top). */
private fun square(
    src: Bitmap,
    px: Int,
    fromTop: Boolean,
): Bitmap {
    val factor = px.toFloat() / min(src.width, src.height)
    val scaled =
        if (factor < 1f) {
            Bitmap.createScaledBitmap(src, (src.width * factor).roundToInt().coerceAtLeast(1), (src.height * factor).roundToInt().coerceAtLeast(1), true)
        } else {
            src
        }
    val side = min(scaled.width, scaled.height)
    val x = (scaled.width - side) / 2
    val y = if (fromTop) 0 else (scaled.height - side) / 2
    return if (x == 0 && y == 0 && side == scaled.width && side == scaled.height) scaled else Bitmap.createBitmap(scaled, x, y, side, side)
}
