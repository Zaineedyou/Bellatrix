package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.app_settings.UISettings
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.cards.UISettingsCard
import com.bellatrixos.windows.config.Dates
import java.util.Locale

/** Halaman waktu dan bahasa. */
class UITimeLang : Fragment() {
    @JvmField var A0: UISettingsCard? = null
    @JvmField var s0: Context? = null
    @JvmField var t0: UISettings? = null
    @JvmField var u0: RelativeLayout? = null
    @JvmField var v0: TextView? = null
    @JvmField var w0: TextView? = null
    @JvmField var x0: TextView? = null
    @JvmField var y0: UISettingsCard? = null
    @JvmField var z0: UISettingsCard? = null

    override fun onDestroyView() {
        super.onDestroyView()
        t0 = null
        y0?.setOnCardClickListener(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = s0 ?: return
        val orientation = context.resources.configuration.orientation
        t0 = parentFragment as? UISettings
        view.postDelayed(k(5, this), 380L)
        v0?.text = Dates.a(Global.u, "EEEE, MMMM d, yyyy")
        if (orientation == 2) {
            w0?.text = Locale.getDefault().displayCountry
        } else {
            u0?.visibility = View.GONE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_time_lang, container, false)
        s0 = view.context
        w0 = view.findViewById(R.id.txt_region)
        x0 = view.findViewById(R.id.txt_time_zone)
        v0 = view.findViewById(R.id.txt_clock_footer)
        y0 = view.findViewById(R.id.btn_settings_date_time)
        z0 = view.findViewById(R.id.btn_lang_region)
        A0 = view.findViewById(R.id.btn_typing)
        u0 = view.findViewById(R.id.txt_region_content)
        return view
    }
}
