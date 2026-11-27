package com.bellatrixos.app_browser.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.webkit.WebView
import android.widget.TextView
import com.bellatrixos.AppExecutors
import com.bellatrixos.app_browser.GeckoEngine
import com.bellatrixos.windows.installed_apps.InstalledAppsDB
import com.bellatrixos.windows.installed_apps.models.InstalledApp

/** Skala tampilan dan zoom halaman web. */
class Scaling {
    fun interface OnScaleChangedListener {
        fun k(i2: Int)
    }

    companion object {
        /** Only the pending zoom save, never the shared main Handler's other callbacks. Main thread only. */
        private var saveZoomRunnable: Runnable? = null

        /** A page's text zoom in percent; bad persisted or typed values must not reach CSS. */
        private fun percent(zoom: Int): Int = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)

        private const val MIN_ZOOM = 50
        private const val MAX_ZOOM = 300

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
            val zoom = percent(app?.g() ?: 100)
            if (zoom != 100) webView.evaluateJavascript("document.body.style.zoom = '$zoom%';", null)
        }

        /** Sets the Browser's text zoom to [zoom] percent and keeps it for [app], as [c] does for a WebView. */
        @JvmStatic
        fun d(context: Context, app: InstalledApp?, zoom: Int) {
            val value = percent(zoom)
            GeckoEngine.zoom(context, value)
            val handler = AppExecutors.c()
            saveZoomRunnable?.let { handler.removeCallbacks(it) }
            val pending = object : Runnable {
                override fun run() {
                    if (saveZoomRunnable === this) saveZoomRunnable = null
                    if (app != null) {
                        app.n(value)
                        InstalledAppsDB.e(app.c(), app)
                    }
                }
            }
            saveZoomRunnable = pending
            handler.postDelayed(pending, 1500L)
        }

        @JvmStatic
        fun c(webView: WebView, app: InstalledApp?, zoom: Int) {
            val value = percent(zoom)
            webView.evaluateJavascript("document.body.style.zoom = '$value%';", null)
            val handler = AppExecutors.c()
            // Supersede only the previous save: clearing the whole main Handler drops unrelated app callbacks.
            saveZoomRunnable?.let { handler.removeCallbacks(it) }
            val pending = object : Runnable {
                override fun run() {
                    if (saveZoomRunnable === this) saveZoomRunnable = null
                    if (app != null) {
                        app.n(value)
                        InstalledAppsDB.e(app.c(), app)
                    }
                }
            }
            saveZoomRunnable = pending
            handler.postDelayed(pending, 1500L)
        }
    }
}
