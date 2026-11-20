package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.app_settings.UISettings
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.cards.UISettingsCard
import com.bellatrixos.widgets.cards.UISettingsCardCollapse

/** Halaman aplikasi. */
class UIApps : Fragment() {
    @JvmField var A0: UISettingsCard? = null
    @JvmField var B0: UISettingsCard? = null
    @JvmField var C0: Boolean = false
    @JvmField var s0: Context? = null
    @JvmField var t0: UISettings? = null
    @JvmField var u0: MainActivityIn? = null
    @JvmField var v0: ArrayList<Any?>? = null
    @JvmField var w0: UISettingsCardCollapse? = null
    @JvmField var x0: TextView? = null
    @JvmField var y0: TextView? = null
    @JvmField var z0: UISettingsCard? = null

    override fun onDestroyView() {
        super.onDestroyView()
        u0 = null
        t0 = null
        v0 = null
        w0?.setOnCardClickListener(null)
        A0?.setOnCardClickListener(null)
        z0?.setOnCardClickListener(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        z0?.setOnCardClickListener(ViewOnClickListenerC0056e(this, 0))
        w0?.setOnCardClickListener(ViewOnClickListenerC0056e(this, 1))
        A0?.setOnCardClickListener(ViewOnClickListenerC0056e(this, 2))
        B0?.setOnCardClickListener(ViewOnClickListenerC0056e(this, 3))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_apps, container, false)
        val context = view.context
        s0 = context
        u0 = context as? MainActivityIn
        t0 = parentFragment as? UISettings
        z0 = view.findViewById(R.id.btn_device_installed_apps)
        A0 = view.findViewById(R.id.btn_app_installer)
        w0 = view.findViewById(R.id.btn_app_icons)
        B0 = view.findViewById(R.id.btn_file_manager)
        x0 = view.findViewById(R.id.ic_pack_title_txt)
        y0 = view.findViewById(R.id.ic_pack_select_btn)
        return view
    }
}
