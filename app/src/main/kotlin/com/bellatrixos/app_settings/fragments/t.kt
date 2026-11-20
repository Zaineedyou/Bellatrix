package com.bellatrixos.app_settings.fragments

import com.bellatrixos.app_settings.utils.Scale
import com.bellatrixos.database.SharedPrefs

/** Runnable pengaturan skala tampilan. */
class t(@JvmField val v: UIDisplayScale, @JvmField val u: Int) : Runnable {
    override fun run() {
        when (u) {
            0 -> {
                val fragment = v
                if (!fragment.isAdded || fragment.view == null) return
                val context = fragment.s0 ?: return
                val scale = SharedPrefs.a(context)
                if (fragment.t0 != 2) {
                    fragment.w0?.alpha = 0.6f
                    fragment.u0?.isEnabled = false
                } else {
                    fragment.u0?.setText(((scale * 100.0f).toInt()).toString())
                    fragment.u0?.let { edit ->
                        fragment.x0?.let { button ->
                            edit.addTextChangedListener(Scale.a(button))
                            button.setOnClickListener(ViewOnClickListenerC0052a(1, fragment))
                        }
                    }
                    fragment.u0?.imeOptions = 301989888
                }
            }
            else -> v.v0?.n()
        }
    }
}
