package com.bellatrixos.windows.files

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.bellatrixos.App
import com.bellatrixos.R
import com.bellatrixos.app_explorer.files.Files
import com.bellatrixos.ui.files.isText
import com.bellatrixos.widgets.UINotification
import java.io.File

/** Membuka berkas lewat aplikasi sistem atau jendela Bellatrix. */
object FileIntent {
    /** Diganti dari `MainActivityIn` sampai interface tersebut ikut dimigrasikan. */
    fun interface AppLauncher {
        fun c0(tag: String, extra: String?)
    }

    @JvmStatic
    fun a(context: Context, file: File) {
        if (!file.exists()) {
            notifyUnavailable(context)
            return
        }
        try {
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FileExtensions.c(file.name))
            if (mime != null) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uriFor(context, file), mime)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                    return
                } catch (_: Exception) {
                }
            }
            // No app outside Bellatrix knows this extension: the editor can still read and write it.
            openEditor(context, file)
        } catch (_: Exception) {
            openEditor(context, file)
        }
    }

    /** The Bellatrix app that shows [file] by its extension, or null when no app claims it. */
    @JvmStatic
    fun appTag(file: File): String? = when (FileExtensions.c(file.name)) {
        "3gp", "mkv", "mp4", "mov", "mpg", "mpeg", "webm" -> "UIVideo"
        "aac", "m4a", "mp3", "ogg", "wav" -> "UIMusic"
        "bmp", "gif", "jpg", "png", "jpeg", "webp" -> "UIPhotos"
        "htm", "svg", "html" -> "UIBrowser"
        "txt" -> "UINotepad"
        else -> null
    }

    @JvmStatic
    fun b(context: Context, file: File, launcher: AppLauncher) {
        if (!file.exists()) {
            notifyUnavailable(context)
            return
        }
        val ext = FileExtensions.c(file.name)
        val tag = appTag(file)
        if (tag != null) {
            launcher.c0(tag, file.absolutePath)
            return
        }
        // Text and extensionless configs open in Notepad's window, which edits and saves them in place. The separate
        // editor activity needs the Linux runtime and takes the whole screen, so it is only the fallback below.
        if (isText(file.name, file)) {
            launcher.c0("UINotepad", file.absolutePath)
            return
        }
        try {
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            if (mime != null) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uriFor(context, file), mime)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
            // Unknown to Android too: still give the user a readable, writable document.
            openEditor(context, file)
        } catch (_: Exception) {
            openEditor(context, file)
        }
    }

    /**
     * The editor takes a guest path; the legacy explorer hands us the host file
     * behind it. Convert through the one runtime, as the Files screen does.
     */
    private fun openEditor(context: Context, file: File) {
        try {
            val runtime = (context.applicationContext as? App)?.runtime ?: return notifyNoApp(context)
            runtime.edit(runtime.proot.guestPath(file) ?: file.absolutePath)
        } catch (_: Exception) {
            notifyNoApp(context)
        }
    }

    private fun uriFor(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

    private fun notifyUnavailable(context: Context) = UINotification.a(
        context,
        context.getString(R.string.toast_file_unavailable),
        context.getString(R.string.toast_title_files)
    )

    private fun notifyNoApp(context: Context) = UINotification.a(
        context,
        context.getString(R.string.toast_file_no_app_found),
        Files.f4677a
    )
}
