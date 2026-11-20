package com.bellatrixos.app_settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.widgets.cards.UISettingsCardSwitch

/** Pengaturan multi-tasking. */
class UIMultitasking : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_multitasking, container, false)
        view.findViewById<UISettingsCardSwitch>(R.id.switch_freeform).setSwitchChecked(true)
        return view
    }
}
