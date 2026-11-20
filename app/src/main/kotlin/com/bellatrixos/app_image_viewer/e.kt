package com.bellatrixos.app_image_viewer

import android.view.View
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize

/** Fokus dan perubahan ukuran jendela penampil gambar. */
class e(
    @JvmField val a: UIImageViewer,
    @JvmField val b: View
) : Window.OnResizeStateChangeListener, FloatingLayout.OnAppFocusListener {

    override fun a() {
        val viewer = a
        b.bringToFront()
        viewer.D0.d(viewer)
        viewer.B0.setFocusState(true)
        viewer.P0 = true
    }

    override fun b(i2: Int) {
        val viewer = a
        WindowSize.d(b, viewer.B0, viewer.S0)
        WindowSize.f(viewer.B0, viewer.A0, i2)
        WindowSize.g(i2, "UIPhotos", viewer.C0)
        viewer.H0.post(b(viewer, 1))
    }
}
