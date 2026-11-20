package com.bellatrixos.app_browser.files

import android.view.View
import com.bellatrixos.ads.AdManager
import com.bellatrixos.startmenu.StartMenuRecommend

/** Runnable kecil untuk mengubah status View pada browser. */
class c(@JvmField val u: Int, @JvmField val v: View) : Runnable {
    override fun run() {
        when (u) {
            0 -> {
                v.visibility = View.INVISIBLE
                v.alpha = 1.0f
            }
            1 -> {
                v.isFocusableInTouchMode = true
                v.requestFocus()
                v.isFocusableInTouchMode = false
            }
            2 -> v.visibility = View.GONE
            3 -> {
                v.visibility = View.VISIBLE
                if (StartMenuRecommend.f4788d) {
                    AdManager.a(v, StartMenuRecommend.f4787a)
                    StartMenuRecommend.f4788d = false
                }
            }
            4 -> {
                v.isPressed = false
                v.refreshDrawableState()
            }
            else -> v.isActivated = true
        }
    }
}
