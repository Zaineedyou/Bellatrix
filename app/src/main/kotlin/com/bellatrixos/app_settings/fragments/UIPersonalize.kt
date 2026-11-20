package com.bellatrixos.app_settings.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.app_settings.UISettings
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.UIProgressCircular
import com.bellatrixos.widgets.cards.UISettingsCard

/** Halaman personalisasi. */
class UIPersonalize : Fragment() {
    @JvmField var A0: UISettingsCard? = null
    @JvmField var s0: Context? = null
    @JvmField var t0: UISettings? = null
    @JvmField var u0: MainActivityIn? = null
    @JvmField var v0: ImageView? = null
    @JvmField var w0: UIProgressCircular? = null
    @JvmField var x0: UISettingsCard? = null
    @JvmField var y0: UISettingsCard? = null
    @JvmField var z0: UISettingsCard? = null

    override fun onDestroyView() {
        super.onDestroyView()
        t0 = null
        u0 = null
        x0?.setOnCardClickListener(null)
        y0?.setOnCardClickListener(null)
        z0?.setOnCardClickListener(null)
        A0?.setOnCardClickListener(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        t0 = parentFragment as? UISettings
        val context = s0 ?: return
        u0 = context as? MainActivityIn
        v0?.clipToOutline = true
        v0?.setImageBitmap(u0?.G())
        x0?.setOnCardClickListener(z(this, 0))
        y0?.setOnCardClickListener(z(this, 1))
        z0?.setOnCardClickListener(z(this, 2))
        A0?.setOnCardClickListener(z(this, 3))
    }

    fun a0(str: String) {
        val context = s0 ?: return
        val dialog = Dialog(context, R.style.UIDialogDefault)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        u0?.g0()
        w0?.a()
        AppExecutors.d().execute(A(this, str, dialog, 0))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_personalize, container, false)
        s0 = view.context
        v0 = view.findViewById(R.id.desktop_wallpaper)
        x0 = view.findViewById(R.id.btn_settings_taskbar)
        y0 = view.findViewById(R.id.btn_settings_colors)
        A0 = view.findViewById(R.id.btn_settings_startMenu)
        z0 = view.findViewById(R.id.btn_settings_background)
        if (Global.f4836a == 2) {
            w0 = view.findViewById(R.id.progressBar)
            val first = view.findViewById<ImageView>(R.id.dkTheme1)
            val second = view.findViewById<ImageView>(R.id.dkTheme2)
            first.clipToOutline = true
            second.clipToOutline = true
            first.setOnClickListener(z(this, 4))
            second.setOnClickListener(z(this, 5))
        }
        return view
    }
}
