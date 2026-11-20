package com.bellatrixos.startmenu

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.drawable.InsetDrawable
import android.view.View
import android.widget.RelativeLayout
import com.bellatrixos.AppExecutors
import com.bellatrixos.f
import com.bellatrixos.startmenu.listeners.StartMenuBG
import com.bellatrixos.widgets.TaskbarBtn
import com.bellatrixos.windows.WinLogoDrawable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

/** State dan animasi panel Start Menu. */
object StartMenu {
    @JvmField var f4778a: Boolean = false
    @JvmField var b: Float = 0.0f
    @JvmField var c: Boolean = true
    @JvmField var f4779d: Int = 0
    @JvmField var f4780e: StartMenuBG? = null
    @JvmField var f4781f: InsetDrawable? = null
    @JvmField var g: ValueAnimator? = null
    @JvmField var f4782h: WinLogoDrawable? = null

    @JvmStatic
    fun a(i2: Int, i3: Int, i4: Int, i5: Int) {
        g?.let { if (it.isRunning) it.cancel() }
        val animator = ValueAnimator.ofFloat(0.0f, 1.0f)
        g = animator
        animator.duration = 250L
        animator.addUpdateListener { valueAnimator ->
            val fraction = valueAnimator.animatedValue as Float
            val evaluator = ArgbEvaluator()
            val first = evaluator.evaluate(fraction, i2, i4) as Int
            val second = evaluator.evaluate(fraction, i3, i5) as Int
            f4782h?.b?.setColors(intArrayOf(first, second))
            f4782h?.invalidateSelf()
        }
        animator.start()
    }

    @JvmStatic
    fun b(
        view: View,
        fVar: f?,
        startMenuViews: StartMenuViews,
        taskbarBtn: TaskbarBtn
    ): BottomSheetBehavior.BottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                fVar?.run()
                StartMenuRecommend.f4789e = true
                return
            }
            if (newState != BottomSheetBehavior.STATE_HIDDEN) return
            StartMenuRecommend.f4789e = false
            taskbarBtn.isActivated = false
            view.visibility = View.GONE
            startMenuViews.f4793f.setText("")
            startMenuViews.f4793f.clearFocus()
            val root: RelativeLayout = startMenuViews.f4790a
            if (root.visibility == View.INVISIBLE) root.visibility = View.VISIBLE
            val recommended = startMenuViews.g
            if (recommended != null && StartMenuRecommend.b && StartMenuRecommend.c) {
                AppExecutors.c().post { recommended.visibility = View.GONE }
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            if (slideOffset >= -0.5f && f4778a) {
                val alpha = round(((slideOffset / 0.5f) + 1.0f) * 0.09f * 100.0f) / 100.0f
                b = alpha.toFloat()
                startMenuViews.f4791d.alpha = b
            } else if (!f4778a && b != 0.0f) {
                b = 0.0f
                startMenuViews.f4791d.alpha = 0.0f
            }
            val faded = max(0.0f, min((slideOffset + 1.0f) / 2.0f, 1.0f))
            startMenuViews.f4790a.alpha = if (faded < 0.24f) ((faded / 0.24f).toDouble().pow(2.200000047683716)).toFloat() else 1.0f
        }
    }
}
