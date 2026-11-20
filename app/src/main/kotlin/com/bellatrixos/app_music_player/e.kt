package com.bellatrixos.app_music_player

import com.bellatrixos.widgets.UINavigationBar

/** Callback navigasi dan pilihan lagu. */
class e(@JvmField val a: UIMusic) :
    UINavigationBar.OnExpandStateChangedListener, AudioAdapter.OnItemClick {

    override fun a(z: Boolean) {
        a.y0.setTitleIconStyle(if (z) 0 else 1)
    }

    override fun b(audioModel: AudioModel) {
        val music = a
        music.X0 = music.R0.indexOf(audioModel)
        music.k0(audioModel.f4697a)
        music.Y0 = true
        music.t0()
    }
}
