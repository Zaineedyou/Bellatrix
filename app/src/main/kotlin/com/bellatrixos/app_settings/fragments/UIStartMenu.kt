package com.bellatrixos.app_settings.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.cards.UISettingsCardSwitch

/** Halaman Start Menu. */
class UIStartMenu : Fragment() {
    @JvmField var A0: UISettingsCardSwitch? = null
    @JvmField var s0: RadioGroup? = null
    @JvmField var t0: MainActivityIn? = null
    @JvmField var u0: RadioButton? = null
    @JvmField var v0: RadioButton? = null
    @JvmField var w0: RadioButton? = null
    @JvmField var x0: ImageView? = null
    @JvmField var y0: ImageView? = null
    @JvmField var z0: ImageView? = null

    override fun onDestroyView() {
        super.onDestroyView()
        t0 = null
        s0?.setOnCheckedChangeListener(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.postDelayed(k(2, this), 380L)
    }

    fun a0(i2: Int) {
        val width = (i2.toDouble() * 0.28).toInt()
        val drawable: Drawable = x0?.drawable ?: return
        val height = ((drawable.intrinsicHeight / drawable.intrinsicWidth.toDouble()) * width).toInt()
        adjustParams(width, height)
    }

    private fun adjustParams(width: Int, height: Int) {
        x0?.update(width, height)
        y0?.update(width, height)
        z0?.update(width, height)
        u0?.update(width, -1)
        v0?.update(width, -1)
        w0?.update(width, -1)
    }

    private fun View.update(width: Int, height: Int) {
        val params = layoutParams ?: return
        params.width = width
        if (height >= 0) params.height = height
        layoutParams = params
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_startmenu, container, false)
        t0 = view.context as? MainActivityIn
        x0 = view.findViewById(R.id.img_1)
        y0 = view.findViewById(R.id.img_2)
        z0 = view.findViewById(R.id.img_3)
        s0 = view.findViewById(R.id.radio_group)
        u0 = view.findViewById(R.id.radio_button_1)
        v0 = view.findViewById(R.id.radio_button_2)
        w0 = view.findViewById(R.id.radio_button_3)
        A0 = view.findViewById(R.id.switch_autofocus_input)
        val saved = arguments
        if (saved != null) {
            a0(saved.getInt("param"))
        } else {
            view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    a0(view.width)
                }
            })
        }
        return view
    }
}
