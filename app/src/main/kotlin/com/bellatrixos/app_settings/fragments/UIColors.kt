package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R
import com.bellatrixos.app_settings.adapters.ColorPickerAdapter
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.cards.UISettingsCardSpinner
import com.bellatrixos.widgets.cards.UISettingsCardSwitch
import com.bellatrixos.widgets.cards.UISettingsCardSwitchCompact

/** Halaman warna aksen. */
class UIColors : Fragment() {
    @JvmField var A0: GridLayoutManager? = null
    @JvmField var B0: UISettingsCardSpinner? = null
    @JvmField var C0: UISettingsCardSwitch? = null
    @JvmField var D0: UISettingsCardSwitchCompact? = null
    @JvmField var s0: Context? = null
    @JvmField var t0: Int = 0
    @JvmField var u0: Animation? = null
    @JvmField var v0: List<String>? = null
    @JvmField var w0: RecyclerView? = null
    @JvmField var x0: MainActivityIn? = null
    @JvmField var y0: ColorPickerAdapter? = null
    @JvmField var z0: ImageView? = null

    override fun onDestroyView() {
        super.onDestroyView()
        x0 = null
        y0?.A = null
        y0 = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        z0?.clipToOutline = true
        z0?.setImageBitmap(x0?.G())
        view.postDelayed(l(this, 0), 380L)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_colors, container, false)
        val context = view.context
        s0 = context
        x0 = context as? MainActivityIn
        t0 = (container ?: view).resources.configuration.orientation
        w0 = view.findViewById(R.id.rv_color_palette)
        D0 = view.findViewById(R.id.switch_ui_accent)
        C0 = view.findViewById(R.id.switch_ui_blur)
        B0 = view.findViewById(R.id.spinner_app_scheme)
        v0 = listOf(
            "#FFB900", "#FF8C00", "#F7630C", "#CA5010", "#DA3B01", "#EF6950", "#D13438",
            "#FF4343", "#E74856", "#E81123", "#EA005E", "#C30052", "#E3008C", "#BF0077",
            "#C239B3", "#9A0089", "#0078D4", "#0063B1", "#8E8CD8", "#6B69D6", "#8764B8",
            "#744DA9", "#B146C2", "#881798", "#0099BC", "#2D7D9A", "#00B7C3", "#038387",
            "#00B294", "#018574", "#00CC6A", "#10893E", "#7A7574", "#5D5A58", "#68768A",
            "#515C6B", "#567C73", "#486860", "#498205", "#107C10", "#767676", "#4C4A48",
            "#69797E", "#4A5459", "#647C64", "#525E54", "#847545", "#7E735F"
        )
        z0 = view.findViewById(R.id.desktop_wallpaper)
        w0?.minimumHeight = context.resources.getDimensionPixelSize(R.dimen.ui_46dp) * (if (t0 == 2) 6 else 8)
        return view
    }
}
