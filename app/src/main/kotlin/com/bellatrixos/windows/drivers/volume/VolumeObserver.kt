package com.bellatrixos.windows.drivers.volume

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

/** Mengamati perubahan volume stream media. */
class VolumeObserver(
    context: Context,
    @JvmField val listener: OnVolumeChangeListener?
) : ContentObserver(Handler(Looper.getMainLooper())) {

    @JvmField var f4912a: Int = -1
    @JvmField val b: Int = AudioManager.STREAM_MUSIC
    @JvmField val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    @JvmField val resolver: ContentResolver = context.contentResolver

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        val current = audioManager.getStreamVolume(b)
        if (current != f4912a) {
            f4912a = current
            listener?.X(current, audioManager.getStreamMaxVolume(b))
        }
    }

    fun register() {
        resolver.registerContentObserver(
            android.provider.Settings.System.CONTENT_URI,
            true,
            this
        )
    }

    fun unregister() = resolver.unregisterContentObserver(this)
}
