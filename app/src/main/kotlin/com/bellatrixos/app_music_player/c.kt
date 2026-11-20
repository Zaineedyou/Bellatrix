package com.bellatrixos.app_music_player

import android.view.View
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize

/** Fokus dan perubahan ukuran jendela pemutar musik. */
class c(
    @JvmField val a: UIMusic,
    @JvmField val b: View
) : FloatingLayout.OnAppFocusListener, Window.OnResizeStateChangeListener {

    override fun a() {
        val music = a
        b.bringToFront()
        music.A0.d(music)
        music.v0.setFocusState(true)
        music.U0 = true
    }

    override fun b(i2: Int) {
        val music = a
        WindowSize.d(b, music.v0, music.P0)
        WindowSize.f(music.v0, music.y0, i2)
        WindowSize.g(i2, "UIMusic", music.E0)
    }
}
