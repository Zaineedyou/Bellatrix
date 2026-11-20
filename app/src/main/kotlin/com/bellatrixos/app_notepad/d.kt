package com.bellatrixos.app_notepad

import android.view.View
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize

/** Fokus dan perubahan ukuran jendela notepad. */
class d(
    @JvmField val a: UINotepad,
    @JvmField val b: View
) : Window.OnResizeStateChangeListener, FloatingLayout.OnAppFocusListener {

    override fun a() {
        val note = a
        b.bringToFront()
        note.A0.d(note)
        note.z0.setFocusState(true)
        note.j0(true)
    }

    override fun b(i2: Int) {
        val note = a
        WindowSize.d(b, note.z0, note.F0)
        WindowSize.f(note.z0, note.x0, i2)
    }
}
