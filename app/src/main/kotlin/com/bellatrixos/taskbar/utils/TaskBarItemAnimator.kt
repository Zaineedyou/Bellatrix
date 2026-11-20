package com.bellatrixos.taskbar.utils

import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/** Animasi muncul/hilangnya tombol aplikasi pada taskbar. */
class TaskBarItemAnimator : DefaultItemAnimator() {
    override fun onAnimationFinished(holder: RecyclerView.ViewHolder) {
        holder.itemView.translationY = 0.0f
        super.onAnimationFinished(holder)
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        val view = holder.itemView
        view.translationY = view.height.toFloat()
        startAnimation(view, view.height.toFloat(), 0.0f, 1.28f, 280L, holder)
        return true
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        val view = holder.itemView
        startAnimation(view, 0.0f, view.height.toFloat(), 1.2f, 0L, holder)
        return true
    }

    private fun startAnimation(
        view: android.view.View,
        from: Float,
        to: Float,
        tension: Float,
        delay: Long,
        holder: RecyclerView.ViewHolder
    ) {
        ObjectAnimator.ofFloat(view, "translationY", from, to).apply {
            interpolator = OvershootInterpolator(tension)
            duration = 600L
            startDelay = delay
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    dispatchAnimationFinished(holder)
                }
            })
            start()
        }
    }
}
