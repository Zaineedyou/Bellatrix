package com.bellatrixos.wallpaper.models

import android.view.View

/** Mengubah lokasi view menjadi [UIBounds]. */
object Bonds {
    @JvmStatic
    fun a(view: View): UIBounds {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return UIBounds(location[0], location[1], view.width, view.height)
    }
}
