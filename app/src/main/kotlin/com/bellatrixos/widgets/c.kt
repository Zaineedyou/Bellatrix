package com.bellatrixos.widgets

import com.bellatrixos.AppExecutors

/** Runnable animasi demi animasi untuk FloatingLayout. */
class c(@JvmField val v: FloatingLayout, @JvmField val u: Int) : Runnable {
    override fun run() {
        when (u) {
            0 -> {
                v.K?.let { AppExecutors.c().removeCallbacks(it) }
                val next = c(v, 2)
                v.K = next
                AppExecutors.c().postDelayed(next, 2000L)
            }
            1 -> v.J?.elevation = 10.0f
            2 -> v.J?.animate()?.alpha(0.2f)?.setDuration(400L)?.withEndAction(c(v, 3))?.start()
            else -> v.J?.alpha = 0.1f
        }
    }
}
