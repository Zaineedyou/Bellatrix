package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.app_settings.UISettings
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.cards.UISettingsCard
import com.bellatrixos.windows.drivers.Display

/** Halaman sistem. */
class UISystem : Fragment() {
    @JvmField var A0: UISettingsCard? = null
    @JvmField var B0: UISettingsCard? = null
    @JvmField var C0: UISettingsCard? = null
    @JvmField var s0: Context? = null
    @JvmField var t0: TextView? = null
    @JvmField var u0: UISettings? = null
    @JvmField var v0: MainActivityIn? = null
    @JvmField var w0: ImageView? = null
    @JvmField var x0: UISettingsCard? = null
    @JvmField var y0: UISettingsCard? = null
    @JvmField var z0: UISettingsCard? = null

    override fun onDestroyView() {
        super.onDestroyView()
        u0 = null
        v0 = null
        x0?.setOnCardClickListener(null)
        y0?.setOnCardClickListener(null)
        z0?.setOnCardClickListener(null)
        A0?.setOnCardClickListener(null)
        B0?.setOnCardClickListener(null)
        C0?.setOnCardClickListener(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        u0 = parentFragment as? UISettings
        t0?.text = "${s0?.let { Display.a(it) } ?: ""} - ${Build.MANUFACTURER}"
        w0?.clipToOutline = true
        w0?.setImageBitmap(v0?.G())

        x0?.setOnCardClickListener(F(this, 0))
        y0?.setOnCardClickListener(F(this, 1))
        z0?.setOnCardClickListener(F(this, 2))
        A0?.setOnCardClickListener(F(this, 3))
        B0?.setOnCardClickListener(F(this, 4))
        C0?.setOnCardClickListener(F(this, 5))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_system, container, false)
        val context = view.context
        s0 = context
        v0 = context as? MainActivityIn
        t0 = view.findViewById(R.id.deviceType)
        w0 = view.findViewById(R.id.desktop_wallpaper)
        x0 = view.findViewById(R.id.btn_device_display)
        y0 = view.findViewById(R.id.btn_device_about)
        z0 = view.findViewById(R.id.btn_device_sound)
        A0 = view.findViewById(R.id.btn_device_launcher)
        B0 = view.findViewById(R.id.btn_device_storage)
        C0 = view.findViewById(R.id.btn_device_multitasking)
        return view
    }

    /** Synthetic click listener (`F`). */
    class F(@JvmField val v: UISystem, @JvmField val u: Int) : View.OnClickListener {
        override fun onClick(view: View) {
            val target = when (u) {
                0 -> 11
                1 -> 15
                2 -> 16
                3 -> 14
                4 -> 12
                else -> 13
            }
            v.u0?.p0(target, true, null)
        }
    }
}
