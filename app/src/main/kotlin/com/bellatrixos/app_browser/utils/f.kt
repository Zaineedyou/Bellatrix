package com.bellatrixos.app_browser.utils

import android.animation.ValueAnimator
import android.widget.TextView

/** Animasi alpha warna teks saat mengganti skala/zoom. */
class f(@JvmField val b: TextView, @JvmField val c: Int, @JvmField val f4653a: Int) :
    ValueAnimator.AnimatorUpdateListener {
    override fun onAnimationUpdate(animation: ValueAnimator) {
        val alpha = (animation.animatedValue as? Int) ?: return
        b.setTextColor((alpha shl 24) or (c and 0x00FFFFFF))
    }
}
