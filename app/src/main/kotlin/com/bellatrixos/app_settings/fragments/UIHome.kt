package com.bellatrixos.app_settings.fragments

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bellatrixos.LauncherActivity
import com.bellatrixos.R
import com.bellatrixos.storage.LauncherData
import com.bellatrixos.widgets.cards.UISettingsCard
import com.bellatrixos.widgets.cards.UISettingsCardSwitch

/** Pengaturan homescreen / launcher. */
class UIHome : Fragment() {
    @JvmField var s0: Context? = null
    @JvmField var t0: UISettingsCard? = null
    @JvmField var u0: UISettingsCardSwitch? = null
    @JvmField var v0: UISettingsCardSwitch? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = s0 ?: return
        val enabled = context.packageManager.getComponentEnabledSetting(
            ComponentName(context, LauncherActivity::class.java)
        ) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        t0?.isEnabled = enabled
        u0?.setSwitchChecked(enabled)
        t0?.setOnCardClickListener(ViewOnClickListenerC0052a(2, this))
        u0?.setOnSwitchCheckedChangeListener(q(2, this))
        v0?.setSwitchChecked(LauncherData.useOwlHome(context))
        v0?.setOnSwitchCheckedChangeListener(q(3, this))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_home, container, false)
        s0 = view.context
        t0 = view.findViewById(R.id.btn_home_select)
        u0 = view.findViewById(R.id.btn_home_enable)
        v0 = view.findViewById(R.id.btn_owl_home)
        return view
    }
}
