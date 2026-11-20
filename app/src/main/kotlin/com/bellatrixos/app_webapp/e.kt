package com.bellatrixos.app_webapp

import android.view.View
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize

/** Fokus dan perubahan ukuran jendela web app. */
class e(
    @JvmField val a: UIWebApp,
    @JvmField val b: View
) : Window.OnResizeStateChangeListener, FloatingLayout.OnAppFocusListener {

    override fun a() {
        val app = a
        b.bringToFront()
        app.F0.d(app)
        app.D0.setFocusState(true)
        app.L0 = true
    }

    override fun b(i2: Int) {
        val app = a
        WindowSize.d(b, app.D0, app.T0)
        WindowSize.f(app.D0, app.C0, i2)
        WindowSize.g(i2, app.y0, app.E0)
    }
}
