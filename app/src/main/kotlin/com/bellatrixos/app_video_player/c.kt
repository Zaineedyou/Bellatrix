package com.bellatrixos.app_video_player

import android.view.View
import androidx.media3.common.Player
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.windows.WindowSize

/** Tombol kontrol pemutar video. */
class c(
    @JvmField val v: UIVideo,
    @JvmField val u: Int
) : View.OnClickListener {

    override fun onClick(view: View) {
        when (u) {
            0 -> {
                val video = v
                AppExecutors.c().removeCallbacks(video.E0)
                video.trackSelections.b(video.u0, video.L0, video.D0, video.H0, d(video, 0))
            }
            1 -> WindowSize.c(v.T0)
            2 -> {
                val video = v
                video.F0.visibility = 8
                video.s0()
                AppExecutors.c().postDelayed(d(video, 2), 100L)
            }
            3 -> {
                val video = v
                video.H0.A(video.T)
            }
            4 -> {
                val video = v
                val loop = !video.y0
                video.y0 = loop
                video.D0?.repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                video.M0.setImageResource(
                    if (loop) R.drawable.ic_arrow_repeat_all_24_regular
                    else R.drawable.ic_arrow_repeat_all_off_24_regular
                )
            }
            5 -> {
                val video = v
                val player: Player? = video.D0
                if (player != null) {
                    if (!player.playWhenReady) {
                        player.playWhenReady = true
                        video.G0.setImageResource(R.drawable.img_app_music_pause)
                        AppExecutors.c().removeCallbacks(video.E0)
                        AppExecutors.c().postDelayed(video.E0, 3000L)
                    } else {
                        player.playWhenReady = false
                        video.G0.setImageResource(R.drawable.img_app_music_play)
                        AppExecutors.c().removeCallbacks(video.E0)
                    }
                }
            }
            else -> {
                val video = v
                AppExecutors.c().removeCallbacks(video.E0)
                if (video.I0.visibility != 0) {
                    video.I0.startAnimation(video.R0)
                    video.I0.visibility = 0
                    if (video.D0?.playWhenReady == true) {
                        AppExecutors.c().postDelayed(video.E0, 3000L)
                    }
                } else {
                    video.I0.startAnimation(video.S0)
                    video.I0.visibility = 8
                }
            }
        }
    }
}
