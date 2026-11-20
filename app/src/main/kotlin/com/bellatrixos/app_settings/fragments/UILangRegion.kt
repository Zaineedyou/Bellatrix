package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.cards.UISettingsCardSpinner

/** Halaman bahasa dan region. */
class UILangRegion : Fragment() {
    @JvmField var s0: Context? = null
    @JvmField var t0: MainActivityIn? = null
    @JvmField var u0: Boolean = true
    @JvmField var v0: Boolean = true
    @JvmField var w0: Spinner? = null
    @JvmField var x0: Spinner? = null
    @JvmField var y0: UISettingsCardSpinner? = null

    override fun onDestroyView() {
        super.onDestroyView()
        t0 = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.postDelayed(v(this, 0), 380L)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_lang_region, container, false)
        val context = view.context
        s0 = context
        t0 = context as? MainActivityIn
        y0 = view.findViewById(R.id.spinner_language)
        w0 = view.findViewById(R.id.spinner_date_short)
        x0 = view.findViewById(R.id.spinner_time_short)
        return view
    }
}
