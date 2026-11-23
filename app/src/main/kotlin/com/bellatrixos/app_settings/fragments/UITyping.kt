package com.bellatrixos.app_settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bellatrixos.App
import com.bellatrixos.R
import com.bellatrixos.Runtime
import com.bellatrixos.theme.Style
import com.bellatrixos.widgets.cards.UISettingsCardSwitch

/** Native Settings page for the terminal keyboard and its suggestions. */
class UITyping : Fragment() {
    private lateinit var runtime: Runtime

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.settings_typing, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        runtime = (requireContext().applicationContext as App).runtime

        val suggestions = view.findViewById<UISettingsCardSwitch>(R.id.switch_keyboard_suggestions)
        suggestions.setSwitchChecked(runtime.style.style[Style.TERMINAL_SUGGESTIONS])
        suggestions.setOnSwitchCheckedChangeListener { _, checked ->
            runtime.style.update { it.with(Style.TERMINAL_SUGGESTIONS, checked) }
        }
    }
}
