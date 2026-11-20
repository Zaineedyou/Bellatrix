package com.bellatrixos.taskbar.center_widgets

import android.appwidget.AppWidgetHost

/** Runnable sintetis untuk lifecycle host widget center. */
class a(
    @JvmField val v: WidgetCenter,
    @JvmField val u: Int
) : Runnable {
    override fun run() {
        if (u == 0) {
            try {
                v.r.startListening()
            } catch (ignored: Exception) {
                val host = AppWidgetHost(v.b.applicationContext, 1024)
                v.r = host
                host.startListening()
            }
        } else {
            v.f4821e.alpha = 0.09f
        }
    }
}
