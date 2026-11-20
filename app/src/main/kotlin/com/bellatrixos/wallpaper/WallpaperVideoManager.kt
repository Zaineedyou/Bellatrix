package com.bellatrixos.wallpaper

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/** Dua ExoPlayer untuk wallpaper video (depan dan belakang). */
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
        f4844a.prepare()
        b.prepare()
    }

    fun a() {
        for (player in arrayOf(f4844a, b)) {
            player.seekTo(0L)
            player.playWhenReady = true
        }
    }
}
