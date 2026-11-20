package com.bellatrixos.widgets

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup

/** Animator update listener bersama untuk progress dan navigation bar. */
class d(@JvmField val b: View, @JvmField val f4860a: Int) : ValueAnimator.AnimatorUpdateListener {
    override fun onAnimationUpdate(animation: ValueAnimator) {
        when (f4860a) {
            0 -> {
                val bar = b as? UINavigationBar ?: return
                val params = bar.u.layoutParams ?: return
                params.width = animation.animatedValue as Int
                bar.u.layoutParams = params
            }
            1 -> {
                val value = animation.animatedValue as Int
                (b as? BtnRadioButton)?.setTextColor(value)
            }
            2 -> {
                val bar = b as? UIProgressBarLinear ?: return
                val value = animation.animatedValue as Float
                if (value < 0.6f) {
                    val t = value / 0.6f
                    val width = bar.w.toFloat()
                    val start = 0.2f * width
                    bar.E = ((0.65f * width) - start) * t + start
                    bar.D = lerp(width, -start, t)
                } else {
                    val t = (value - 0.6f) / 0.4f
                    val width = bar.w.toFloat()
                    val end = 0.9f * width
                    bar.D = lerp(width, -end, t)
                    bar.E = lerp(0.1f * width, end, t)
                }
                bar.v.set(bar.D, 0.0f, bar.E + bar.D, bar.x.toFloat())
                bar.postInvalidateOnAnimation()
            }
            3 -> {
                val bar = b as? UIProgressBarLinear ?: return
                val value = animation.animatedValue as Float
                bar.D = value * bar.w
                bar.E = bar.w.toFloat()
                bar.v.set(bar.D, 0.0f, bar.E, bar.x.toFloat())
                bar.postInvalidateOnAnimation()
            }
            4 -> {
                val bar = b as? UIProgressCircular ?: return
                bar.D = animation.animatedValue as Float
                bar.invalidate()
            }
            5 -> {
                val bar = b as? UIProgressCircular ?: return
                bar.E = animation.animatedValue as Float
            }
            else -> {
                val bar = b as? UIProgressCircular ?: return
                bar.F = animation.animatedValue as Float
                bar.invalidate()
            }
        }
    }

    private fun lerp(start: Float, end: Float, t: Float): Float = start + (end - start) * t
}
