package com.bellatrixos.wallpaper

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File

/** Konversi video wallpaper menjadi berkas siap pakai memakai Media3 Transformer. */
@OptIn(UnstableApi::class)
object WallpaperVideoProcessor {

    interface Callback {
        fun a()
        fun b(file: File, file2: File)
    }

    @JvmStatic
    fun a(context: Context, uri: Uri, file: File, runnable: Runnable, runnable2: Runnable) {
        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    runnable2.run()
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    runnable.run()
                }
            })
            .build()
        val mediaItem = MediaItem.fromUri(uri)
        val edited = EditedMediaItem.Builder(mediaItem).build()
        transformer.start(edited, file.absolutePath)
    }
}
