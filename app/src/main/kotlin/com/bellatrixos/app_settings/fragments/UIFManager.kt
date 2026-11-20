package com.bellatrixos.app_settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.widgets.cards.UISettingsCardSwitchCompact

/** Pengaturan File Manager. */
class UIFManager : Fragment() {
    @JvmField var s0: UISettingsCardSwitchCompact? = null
    @JvmField var t0: UISettingsCardSwitchCompact? = null

    override fun onDestroyView() {
        super.onDestroyView()
        s0?.setOnSwitchCheckedChangeListener(null)
        t0?.setOnSwitchCheckedChangeListener(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = SharedPrefs.f4755a ?: return
        s0?.setSwitchChecked(prefs.getBoolean("file_explorer_side_nav_enabled", true))
        t0?.setSwitchChecked(prefs.getBoolean("file_explorer_home_shortcuts_enabled", true))
        s0?.setOnSwitchCheckedChangeListener(u(0))
        t0?.setOnSwitchCheckedChangeListener(u(1))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_file_explorer, container, false)
        s0 = view.findViewById(R.id.checkbox_show_side_nav)
        t0 = view.findViewById(R.id.checkbox_show_home_shortcuts)
        return view
    }
}
