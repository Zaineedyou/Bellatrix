package com.bellatrixos.app_settings

import android.view.View
import android.view.animation.Animation
import android.widget.RadioButton
import android.widget.ScrollView
import com.bellatrixos.AppExecutors

/** Navigasi halaman pengaturan. */
class UINavigation {
    interface OnNavItemSelected {
        fun a(i2: Int)
    }

    companion object {
        @JvmField var f4705a: Array<RadioButton>? = null
        @JvmField var b: Animation? = null
        @JvmField var c: Animation? = null

        @JvmStatic
        fun a(str: String): IntArray {
            val index = str.indexOf('_')
            return if (index == -1) {
                intArrayOf(str.toInt(), -1)
            } else {
                intArrayOf(str.substring(0, index).toInt(), str.substring(index + 1).toInt())
            }
        }

        @JvmStatic
        fun b(i2: Int) {
            var abs = kotlin.math.abs(i2)
            while (abs >= 10) abs /= 10
            val index = abs - 1
            val buttons = f4705a ?: return
            if (index < 0 || index >= buttons.size) return
            buttons[index].isChecked = true
        }

        @JvmStatic
        fun c(z: Boolean, scrollView: ScrollView, view: View) {
            if (!z) {
                scrollView.startAnimation(c)
                AppExecutors.c().postDelayed({
                    scrollView.visibility = View.GONE
                    view.visibility = View.GONE
                    scrollView.translationZ = -1.0f
                }, 120L)
            } else {
                scrollView.startAnimation(b)
                scrollView.visibility = View.VISIBLE
                view.visibility = View.VISIBLE
                scrollView.translationZ = 1.0f
            }
        }
    }
}
