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
import java.util.Collections
import java.util.IdentityHashMap

/** Konversi video wallpaper menjadi berkas siap pakai memakai Media3 Transformer. */
@OptIn(UnstableApi::class)
object WallpaperVideoProcessor {

    interface Callback {
        fun a()
        fun b(file: File, file2: File)
    }

    /** Exports still running, so a destroyed picker can release their codecs. */
    private val active = Collections.newSetFromMap(IdentityHashMap<Transformer, Boolean>())

    @Synchronized
    private fun track(transformer: Transformer) {
        active.add(transformer)
    }

    @Synchronized
    private fun untrack(transformer: Transformer) {
        active.remove(transformer)
    }

    /** Releases any export that hasn't reached its terminal callback yet. */
    @JvmStatic
    @Synchronized
    fun cancelActive() {
        for (transformer in active) runCatching { transformer.release() }
        active.clear()
    }

    @JvmStatic
    fun a(
        context: Context,
        uri: Uri,
        file: File,
        runnable: Runnable,
        runnable2: Runnable,
        maxMs: Long = 0L,
    ) {
        // The listener fires before this local `val` is initialized, so keep the reference in a holder.
        val holder = arrayOfNulls<Transformer>(1)
        val transformer =
            Transformer
                .Builder(context)
                .addListener(
                    object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            try {
                                runnable2.run()
                            } finally {
                                release(holder[0])
                                holder[0] = null
                            }
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            try {
                                runnable.run()
                            } finally {
                                release(holder[0])
                                holder[0] = null
                            }
                        }
                    },
                ).build()
        holder[0] = transformer
        track(transformer)
        val builder =
            MediaItem.Builder().setUri(uri).also { item ->
                context.contentResolver.getType(uri)?.let { type -> item.setMimeType(type) }
                if (maxMs > 0) {
                    item.setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder().setEndPositionMs(maxMs).build(),
                    )
                }
            }
        val edited = EditedMediaItem.Builder(builder.build()).build()
        try {
            transformer.start(edited, file.absolutePath)
        } catch (t: Throwable) {
            release(transformer)
            throw t
        }
    }

    /** Releases and forgets one export; safe from either terminal callback. */
    private fun release(transformer: Transformer?) {
        if (transformer == null) return
        runCatching { transformer.release() }
        untrack(transformer)
    }
}
