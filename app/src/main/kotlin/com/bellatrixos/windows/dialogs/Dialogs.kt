package com.bellatrixos.windows.dialogs

import android.view.View
import android.view.animation.Animation
import com.bellatrixos.taskbar.center_action.ActionCenter

/** Animasi umum untuk popup dan jendela sistem. */
object Dialogs {
    @JvmField var f4892a: Animation? = null
    @JvmField var b: Animation? = null
    @JvmField var c: Animation? = null
    @JvmField var f4893d: Animation? = null
    @JvmField var f4894e: Runnable? = null

    @JvmField val f4895f: Animation.AnimationListener = object : Animation.AnimationListener {
        override fun onAnimationEnd(animation: Animation) {
            animation.setAnimationListener(null)
            val runnable = f4894e
            if (runnable != null) {
                f4894e = null
                runnable.run()
            }
        }

        override fun onAnimationRepeat(animation: Animation) {}
        override fun onAnimationStart(animation: Animation) {}
    }

    @JvmStatic
    fun a(view: View?, i2: Int, runnable: Runnable) {
        val closing = b
        if (view == null || closing == null) {
            runnable.run()
            return
        }
        f4894e = runnable
        val animation = if (i2 == 0) closing else f4893d
        animation?.setAnimationListener(f4895f)
        animation?.let { view.startAnimation(it) }
    }
}
