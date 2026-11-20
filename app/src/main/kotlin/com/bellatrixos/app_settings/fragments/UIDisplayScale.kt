package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.interfaces.MainActivityIn

/** Layar skala tampilan kustom. */
class UIDisplayScale : Fragment() {
    @JvmField var s0: Context? = null
    @JvmField var t0: Int = 0
    @JvmField var u0: EditText? = null
    @JvmField var v0: MainActivityIn? = null
    @JvmField var w0: RelativeLayout? = null
    @JvmField var x0: ImageButton? = null

    override fun onDestroyView() {
        super.onDestroyView()
        v0 = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.postDelayed(t(this, 0), 380L)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_display_scale, container, false)
        val context = view.context
        s0 = context
        v0 = context as? MainActivityIn
        t0 = context.resources.configuration.orientation
        w0 = view.findViewById(R.id.scale_ui_card)
        u0 = view.findViewById(R.id.btn_download)
        x0 = view.findViewById(R.id.scale_custom_btn_confirm)
        return view
    }
}
