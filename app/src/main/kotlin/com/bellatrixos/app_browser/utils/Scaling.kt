package com.bellatrixos.app_browser.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.webkit.WebView
import android.widget.TextView
import com.bellatrixos.AppExecutors
import com.bellatrixos.windows.installed_apps.InstalledAppsDB
import com.bellatrixos.windows.installed_apps.models.InstalledApp

/** Skala tampilan dan zoom halaman web. */
class Scaling {
    fun interface OnScaleChangedListener {
        fun k(i2: Int)
    }

    companion object {
        @JvmStatic
        fun a(textView: TextView, str: String) {
            val color = textView.currentTextColor
            ValueAnimator.ofInt(255, 0).apply {
                duration = 180L
                startDelay = 280L
                addUpdateListener(f(textView, color, 1))
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        textView.text = str
                        ValueAnimator.ofInt(0, 255).apply {
                            duration = 200L
                            addUpdateListener(f(textView, color, 0))
                            start()
                        }
                    }
                })
                start()
            }
        }

        @JvmStatic
        fun b(webView: WebView, app: InstalledApp?) {
            val zoom = app?.g() ?: 100
            if (zoom != 100) webView.evaluateJavascript("document.body.style.zoom = '$zoom%;", null)
        }

        @JvmStatic
        fun c(webView: WebView, app: InstalledApp?, zoom: Int) {
            webView.evaluateJavascript("document.body.style.zoom = '$zoom%;", null)
            AppExecutors.c().removeCallbacksAndMessages(null)
            AppExecutors.c().postDelayed({
                if (app != null) {
                    app.n(zoom)
                    InstalledAppsDB.e(app.c(), app)
                }
            }, 1500L)
        }
    }
}
