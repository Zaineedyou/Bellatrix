package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.cards.UISettingsCardProgressBar
import com.bellatrixos.widgets.cards.UISettingsCardSpinner

/** Halaman mouse. */
class UIMouse : Fragment() {
    @JvmField var s0: View? = null
    @JvmField var t0: Context? = null
    @JvmField var u0: MainActivityIn? = null
    @JvmField var v0: UISettingsCardSpinner? = null
    @JvmField var w0: UISettingsCardProgressBar? = null

    override fun onDestroyView() {
        super.onDestroyView()
        u0 = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val root = s0 ?: return
        root.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val context = t0 ?: return
                val pointerSpeed = try {
                    Settings.System.getInt(context.contentResolver, "pointer_speed", 0)
                } catch (_: Settings.SettingNotFoundException) {
                    0
                }
                w0?.setProgressMax(14)
                w0?.setProgressValue(pointerSpeed + 7)
                v0?.setArray(
                    arrayOf(
                        context.getString(R.string.settings_mouse_click_single),
                        context.getString(R.string.settings_mouse_click_double)
                    )
                )
                v0?.setSelection(if (Global.p != 0) 1 else 0)
                v0?.setItemSelectListener(C0054c(6, this@UIMouse))
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_mouse, container, false)
        s0 = view
        val context = view.context
        t0 = context
        u0 = context as? MainActivityIn
        v0 = view.findViewById(R.id.spinnerClickBehavior)
        w0 = view.findViewById(R.id.seekBarPointerSpeed)
        return view
    }
}
