package com.bellatrixos.app_video_player

import androidx.media3.common.Player
import com.bellatrixos.AppExecutors

/** Tugas tertunda untuk kontrol pemutar video. */
class d(
    @JvmField val v: UIVideo,
    @JvmField val u: Int
) : Runnable {

    override fun run() {
        when (u) {
            0 -> {
                val video = v
                val player: Player? = video.D0
                if (player != null && player.playWhenReady) {
                    AppExecutors.c().postDelayed(video.E0, 5000L)
                }
            }
            1 -> {
                val video = v
                video.I0.startAnimation(video.S0)
                video.I0.visibility = 8
            }
            else -> {
                val video = v
                video.H0.P(video)
            }
        }
    }
}
