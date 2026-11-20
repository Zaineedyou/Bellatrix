package com.bellatrixos.windows

import android.app.Activity
import android.os.Handler
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bellatrixos.R
import com.bellatrixos.windows.config.LogonController

/** Runnable sintetis yang menampilkan layar shutdown/restart. */
class b(
    @JvmField val v: Array<LogonController?>,
    @JvmField val w: Activity,
    @JvmField val x: ViewGroup,
    @JvmField val y: Handler,
    @JvmField val z: Runnable,
    @JvmField val u: Int
) : Runnable {
    override fun run() {
        val controller = v.getOrNull(0)
        if (controller == null || controller.f4890d) return
        if (w.isFinishing || w.isDestroyed) return
        val view = LayoutInflater.from(w).inflate(R.layout.shutdown_layout, x, false)
        val image = view.findViewById<ImageView>(R.id.boot_gif_loader)
        view.findViewById<TextView>(R.id.boot_txt).text =
            w.getString(R.string.bootstrap_txt_restarting)
        image?.alpha = 1.0f
        x.addView(view)
        y.postDelayed(z, 4000L)
    }
}
