package com.bellatrixos.windows.files

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bellatrixos.R
import com.bellatrixos.windows.files.clipboard.ClipboardItem
import com.bellatrixos.windows.icons.IconEngine
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import java.io.File
import java.util.Locale

/** Utilitas ekstensi dan ikon berkas. */
object FileExtensions {

    @JvmStatic
    fun a(i2: Int, i3: Int, str: String, str2: String): Boolean {
        val length = str2.length
        if (i2 == -1 || i3 - i2 != length) return false
        for (i4 in 0 until length) {
            if ((str[i2 + i4].code or 0x20) != str2[i4].code) return false
        }
        return true
    }

    @JvmStatic
    fun b(str: String, i2: Int): Int {
        for (i3 in i2 - 1 downTo 0) {
            if (str[i3] == '.') return i3 + 1
        }
        return -1
    }

    @JvmStatic
    fun c(str: String): String {
        val iLastIndexOf = str.lastIndexOf('.')
        return if (iLastIndexOf > 0) str.substring(iLastIndexOf + 1).lowercase(Locale.ROOT) else ""
    }

    private fun j(str: String, i2: Int, c1: Char, c2: Char, c3: Char): Boolean {
        return (str[i2].code or 0x20) == c1.code && (str[i2 + 1].code or 0x20) == c2.code && (str[i2 + 2].code or 0x20) == c3.code
    }

    private fun k(str: String, i2: Int, c1: Char, c2: Char, c3: Char, c4: Char): Boolean {
        return (str[i2].code or 0x20) == c1.code && (str[i2 + 1].code or 0x20) == c2.code &&
            (str[i2 + 2].code or 0x20) == c3.code && (str[i2 + 3].code or 0x20) == c4.code
    }

    @JvmStatic
    fun d(str: String, i2: Int, i3: Int): Int {
        if (i2 == -1) return R.drawable.img_file_default
        val i4 = i3 - i2
        if (i4 == 3 && (j(str, i2, 'j', 'p', 'g') || j(str, i2, 'p', 'n', 'g') || j(str, i2, 'g', 'i', 'f'))) {
            return R.drawable.img_file_image
        }
        if (i4 == 4 && (k(str, i2, 'j', 'p', 'e', 'g') || k(str, i2, 'w', 'e', 'b', 'p') || k(str, i2, 't', 'i', 'f', 'f') || k(str, i2, 'h', 'e', 'i', 'c'))) {
            return R.drawable.img_file_image
        }
        if (i4 == 3 && (j(str, i2, 'm', 'p', '4') || j(str, i2, 'm', 'k', 'v') || j(str, i2, 'a', 'v', 'i') || j(str, i2, 'm', 'o', 'v') || j(str, i2, 'w', 'm', 'v') || j(str, i2, 'f', 'l', 'v') || j(str, i2, 'm', 'p', 'g'))) {
            return R.drawable.img_file_video
        }
        if (i4 == 4 && k(str, i2, 'm', 'p', 'e', 'g')) {
            return R.drawable.img_file_video
        }
        if (i4 == 3 && (j(str, i2, 'm', 'p', '3') || j(str, i2, 'a', 'a', 'c') || j(str, i2, 'o', 'g', 'g') || j(str, i2, 'w', 'a', 'v') || j(str, i2, 'w', 'm', 'a') || j(str, i2, 'm', '4', 'a') || j(str, i2, 'd', 't', 's') || j(str, i2, 'm', '3', 'u'))) {
            return R.drawable.img_file_audio
        }
        if (i4 == 3 && j(str, i2, 't', 'x', 't')) return R.drawable.img_file_text
        if (i4 == 3 && j(str, i2, 'p', 'd', 'f')) return R.drawable.img_file_pdf
        if (i4 == 3 && j(str, i2, 'a', 'p', 'k')) return R.drawable.img_file_apk
        if (i4 == 3 && j(str, i2, 'z', 'i', 'p')) return R.drawable.img_file_zip
        if (i4 == 3 && j(str, i2, 'r', 'a', 'r')) return R.drawable.img_file_rar
        if (i4 == 3 && j(str, i2, 'i', 's', 'o')) return R.drawable.img_file_iso
        if (i4 == 4 && (k(str, i2, 'd', 'o', 'c', 'x') || k(str, i2, 'p', 'p', 't', 'x'))) {
            return R.drawable.img_file_word
        }
        if (i4 == 3 && (j(str, i2, 'd', 'o', 'c') || j(str, i2, 'p', 'p', 't'))) {
            return R.drawable.img_file_word
        }
        if (i4 == 4 && (k(str, i2, 'x', 'l', 's', 'x') || k(str, i2, 'x', 'l', 's', 'b'))) {
            return R.drawable.img_file_xlsx
        }
        if (i4 == 3 && (j(str, i2, 'h', 't', 'm') || j(str, i2, 's', 'v', 'g'))) {
            return R.drawable.img_file_web
        }
        return if (i4 == 4 && k(str, i2, 'h', 't', 'm', 'l')) R.drawable.img_file_web else R.drawable.img_file_default
    }

    @JvmStatic
    fun e(str: String?): Int {
        if (str == null) return R.drawable.img_file_default
        val length = str.length
        var i2 = -1
        for (i3 in length - 1 downTo 0) {
            if (str[i3] == '.') {
                i2 = i3 + 1
                return d(str, i2, length)
            }
        }
        return d(str, i2, length)
    }

    @JvmStatic
    fun f(str: String?): Boolean {
        if (str == null) return false
        val length = str.length
        val iB = b(str, length)
        return a(iB, length, str, "txt") || a(iB, length, str, "mp3") ||
            a(iB, length, str, "mkv") || a(iB, length, str, "webp")
    }

    @JvmStatic
    fun g(fileMeta: FileMeta): Boolean {
        if (fileMeta.isDirectory || fileMeta.extensionStart <= 0) return false
        val i2 = fileMeta.extensionStart
        val i3 = fileMeta.nameLength - i2
        val str = fileMeta.lowerName
        if (i3 == 3) {
            val c1 = str[i2]
            val c2 = str[i2 + 1]
            val c3 = str[i2 + 2]
            return (c1 == 'j' && c2 == 'p' && c3 == 'g') || (c1 == 'p' && c2 == 'n' && c3 == 'g') ||
                ((c1 == 'g' && c2 == 'i' && c3 == 'f') || (c1 == 'b' && c2 == 'm' && c3 == 'p'))
        }
        if (i3 != 4) return false
        val c1 = str[i2]
        val c2 = str[i2 + 1]
        val c3 = str[i2 + 2]
        val c4 = str[i2 + 3]
        return (c1 == 'j' && c2 == 'p' && c3 == 'e' && c4 == 'g') || (c1 == 'w' && c2 == 'e' && c3 == 'b' && c4 == 'p')
    }

    @JvmStatic
    fun h(str: String?): Boolean {
        if (str == null) return false
        val length = str.length
        val iB = b(str, length)
        return a(iB, length, str, "jpg") || a(iB, length, str, "jpeg") || a(iB, length, str, "png") ||
            a(iB, length, str, "gif") || a(iB, length, str, "webp")
    }

    @JvmStatic
    fun i(str: String?): Boolean {
        if (str == null) return false
        val length = str.length
        val iB = b(str, length)
        return a(iB, length, str, "txt") || a(iB, length, str, "jpg") || a(iB, length, str, "jpeg") ||
            a(iB, length, str, "png") || a(iB, length, str, "webp")
    }

    @JvmStatic
    fun l(j: Long): String {
        return when {
            j < 1024 -> "$j B"
            j < 1048576 -> {
                val v = (j * 100) / 1024
                "${v / 100}.${v % 100} KB"
            }
            j < 1073741824 -> {
                val v = (j * 100) / 1048576
                "${v / 100}.${v % 100} MB"
            }
            else -> {
                val v = (j * 100) / 1073741824
                "${v / 100}.${v % 100} GB"
            }
        }
    }

    @JvmStatic
    fun m(fileMeta: FileMeta, imageView: ImageView, z: Boolean) {
        imageView.foreground = if (fileMeta.linkFile) ContextCompat.getDrawable(imageView.context, R.drawable.image_shortcut) else null
        if (fileMeta.isDirectory) {
            imageView.setImageResource(R.drawable.img_folder_default)
            return
        }
        val file = fileMeta.file
        if (z && fileMeta.executable) {
            val path = file.absolutePath
            imageView.tag = path
            IconEngine.b(imageView.context).d(file, IconEngine.Callback { bitmap ->
                if (path == imageView.tag) imageView.setImageBitmap(bitmap)
            })
            return
        }
        if (fileMeta.linkFile) {
            val clipboardItem: ClipboardItem? = FileShortcut.a(file)
            if (clipboardItem != null) {
                if (clipboardItem.c != 1) {
                    imageView.setImageResource(d(fileMeta.lowerName, fileMeta.extensionStart, fileMeta.nameLength))
                    return
                }
                val path = file.absolutePath
                imageView.tag = path
                IconEngine.b(imageView.context).c(clipboardItem.f4921a, IconEngine.Callback { bitmap ->
                    if (path == imageView.tag) imageView.setImageBitmap(bitmap)
                })
                return
            }
        }
        if (z && fileMeta.extensionStart != -1) {
            val start = fileMeta.extensionStart
            val len = fileMeta.nameLength - start
            val str = fileMeta.lowerName
            val isImage = (len == 3 && (j(str, start, 'j', 'p', 'g') || j(str, start, 'p', 'n', 'g') || j(str, start, 'g', 'i', 'f'))) ||
                (len == 4 && (k(str, start, 'j', 'p', 'e', 'g') || k(str, start, 'w', 'e', 'b', 'p') || k(str, start, 't', 'i', 'f', 'f') || k(str, start, 'h', 'e', 'i', 'c')))
            if (isImage) {
                Glide.with(imageView.context)
                    .load(file)
                    .override(46, 46)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageView)
                return
            }
        }
        imageView.setImageResource(d(fileMeta.lowerName, fileMeta.extensionStart, fileMeta.nameLength))
    }

    /** Alias deskriptif untuk [e], dipakai kode Kotlin baru. */
    @JvmStatic
    fun iconFor(file: File): Int = e(file.name)
}
