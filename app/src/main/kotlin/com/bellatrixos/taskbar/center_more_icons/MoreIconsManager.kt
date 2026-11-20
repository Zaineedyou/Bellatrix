package com.bellatrixos.taskbar.center_more_icons

import android.animation.ObjectAnimator
import android.widget.ImageButton

/** Animasi panah tombol " lebih banyak ikon". */
object MoreIconsManager {
    @JvmStatic
    fun a(expanded: Boolean, button: ImageButton) {
        val drawable = button.drawable ?: return
        ObjectAnimator.ofInt(drawable, "level", if (expanded) 0 else 10000, if (expanded) 10000 else 0)
            .apply { duration = 220L }
            .start()
        button.isActivated = expanded
    }
}
