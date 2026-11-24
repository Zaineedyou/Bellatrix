package com.bellatrixos.wallpaper

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bellatrixos.database.SharedPrefs

/** Dua ExoPlayer untuk wallpaper video (depan dan belakang). Yang belakang dibisuin: satu sumber audio saja. */
class WallpaperVideoManager(context: Context, front: PlayerView, back: PlayerView) {
    @JvmField val f4844a: ExoPlayer = ExoPlayer.Builder(context).build()
    @JvmField val b: ExoPlayer = ExoPlayer.Builder(context).build()

    init {
        front.useController = false
        back.useController = false
        front.player = f4844a
        back.player = b
        front.setKeepContentOnPlayerReset(true)
        back.setKeepContentOnPlayerReset(true)
        f4844a.repeatMode = ExoPlayer.REPEAT_MODE_ALL
        b.repeatMode = ExoPlayer.REPEAT_MODE_ALL
        setVolume(SharedPrefs.f4755a?.getFloat("wallpaper_volume", 1f) ?: 1f)
        f4844a.prepare()
        b.prepare()
    }

    fun a() {
        for (player in arrayOf(f4844a, b)) {
            player.seekTo(0L)
            player.playWhenReady = true
        }
    }

    /** Only the front player is heard; the blurred copy stays silent so video audio isn't doubled. */
    fun setVolume(volume: Float) {
        f4844a.volume = volume.coerceIn(0f, 1f)
        b.volume = 0f
    }
}
