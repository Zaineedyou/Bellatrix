package com.bellatrixos.wallpaper

import android.graphics.Bitmap
import com.bellatrixos.interfaces.MainActivityIn

/** Runnable sintetis yang meneruskan bitmap wallpaper ke Activity utama. */
class b(
    @JvmField val v: MainActivityIn?,
    @JvmField val w: Bitmap?,
    @JvmField val u: Int
) : Runnable {
    override fun run() {
        v?.W(w)
    }
}
