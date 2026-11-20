package com.bellatrixos.app_music_player

import android.view.View
import android.widget.ImageButton
import com.bellatrixos.R
import com.bellatrixos.windows.WindowSize

/** Tombol kontrol pemutar musik. */
class b(
    @JvmField val v: UIMusic,
    @JvmField val u: Int
) : View.OnClickListener {

    override fun onClick(view: View) {
        when (u) {
            0 -> v.A0.P(v)
            1 -> v.A0.A(v.T)
            2 -> {
                val music = v
                val looping = !music.T0
                music.T0 = looping
                music.z0?.isLooping = looping
                music.M0.setImageResource(
                    if (looping) R.drawable.ic_arrow_repeat_all_24_regular
                    else R.drawable.ic_arrow_repeat_all_off_24_regular
                )
            }
            3 -> {
                val music = v
                val button: ImageButton = music.L0
                if (music.z0.isPlaying) {
                    music.z0.pause()
                    button.setImageResource(R.drawable.img_app_music_play)
                } else {
                    music.z0.start()
                    button.setImageResource(R.drawable.img_app_music_pause)
                }
            }
            4 -> {
                val music = v
                val player = music.z0 ?: return
                val position = player.currentPosition
                val index = music.X0
                if (index == -1 || position > 5000 || index <= 0) {
                    music.z0.seekTo(0)
                    music.w0.progress = 0
                } else {
                    music.X0 = index - 1
                    music.s0()
                }
            }
            5 -> {
                val music = v
                if (music.X0 < music.R0.size - 1) {
                    music.X0++
                    music.s0()
                }
            }
            else -> WindowSize.c(v.P0)
        }
    }
}
