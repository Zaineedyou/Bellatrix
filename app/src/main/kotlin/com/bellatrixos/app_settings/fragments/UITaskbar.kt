package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.cards.UISettingsCardSpinnerCompact

/** Halaman taskbar. */
class UITaskbar : Fragment() {
    @JvmField var s0: Context? = null
    @JvmField var t0: Int = 0
    @JvmField var u0: MainActivityIn? = null
    @JvmField var v0: SwitchCompat? = null
    @JvmField var w0: UISettingsCardSpinnerCompact? = null
    @JvmField var x0: UISettingsCardSpinnerCompact? = null

    override fun onDestroyView() {
        super.onDestroyView()
        u0 = null
        x0?.setItemSelectListener(null)
        w0?.setItemSelectListener(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.postDelayed(k(4, this), 380L)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_taskbar, container, false)
        val context = view.context
        s0 = context
        u0 = context as? MainActivityIn
        t0 = context.resources.configuration.orientation
        v0 = view.findViewById(R.id.switch_widget_show)
        x0 = view.findViewById(R.id.spinnerSearchAppType)
        w0 = view.findViewById(R.id.spinnerTBarAlignment)
        return view
    }
}
