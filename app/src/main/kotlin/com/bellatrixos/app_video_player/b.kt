package com.bellatrixos.app_video_player

import android.view.View
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize

/** Fokus dan perubahan ukuran jendela pemutar video. */
class b(
    @JvmField val a: UIVideo,
    @JvmField val b: View
) : Window.OnResizeStateChangeListener, FloatingLayout.OnAppFocusListener {

    override fun a() {
        val video = a
        b.bringToFront()
        video.H0.d(video)
        video.A0.setFocusState(true)
        video.x0 = true
    }

    override fun b(i2: Int) {
        val video = a
        WindowSize.d(b, video.A0, video.T0)
        WindowSize.f(video.A0, video.z0, i2)
    }
}
