package com.bellatrixos.startmenu

import android.graphics.Bitmap
import android.widget.ImageView
import com.bellatrixos.windows.Windows

/** Animasi gambar latar Start Menu. */
class j(imageView: ImageView, bitmap: Bitmap, i2: Int) : Runnable {
    @JvmField val u: Int = i2
    @JvmField val v: ImageView = imageView
    @JvmField val w: Bitmap = bitmap

    override fun run() {
        when (u) {
            0 -> {
                v.alpha = 0.0f
                if (StartMenu.f4778a) {
                    v.animate().alpha(0.09f).setDuration(400L).withEndAction(d(v, 0)).start()
                }
                v.setImageBitmap(w)
            }
            1 -> {
                v.alpha = 0.0f
                v.animate().alpha(0.09f).setDuration(1000L).withEndAction(d(v, 3)).start()
                v.setImageBitmap(w)
                Windows.f4882e?.i0(true)
            }
            else -> {
                Windows.f4882e?.o0()
                v.alpha = 0.0f
                v.animate().alpha(0.09f).setDuration(1000L).withEndAction(d(v, 2)).start()
                v.setImageBitmap(w)
            }
        }
    }
}
