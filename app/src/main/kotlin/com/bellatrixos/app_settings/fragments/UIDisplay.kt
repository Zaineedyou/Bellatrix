package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import com.bellatrixos.R
import com.bellatrixos.app_settings.UISettings
import com.bellatrixos.app_settings.utils.SFragment
import com.bellatrixos.app_settings.utils.b
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.cards.UISettingsCardSpinner
import com.bellatrixos.widgets.cards.UISettingsCardSpinnerText
import com.bellatrixos.widgets.cards.UISettingsCardSwitch

/** Halaman tampilan. Runnable `g` case 2 di-inline dari helper media3. */
class UIDisplay : SFragment() {
    @JvmField var A0: UISettingsCardSpinner? = null
    @JvmField var B0: UISettingsCardSwitch? = null
    @JvmField var C0: UISettingsCardSpinner? = null
    @JvmField var D0: UISettingsCardSpinner? = null
    @JvmField var t0: Context? = null
    @JvmField var u0: Int = 0
    @JvmField var v0: TextView? = null
    @JvmField var w0: UISettings? = null
    @JvmField var x0: MainActivityIn? = null
    @JvmField var y0: UISettingsCardSwitch? = null
    @JvmField var z0: UISettingsCardSpinnerText? = null

    override fun onDestroyView() {
        super.onDestroyView()
        x0 = null
        w0 = null
        A0?.setOnCardClickListener(null)
        y0?.setOnSwitchCheckedChangeListener(null)
        C0?.setItemSelectedListener(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        w0 = parentFragment as? UISettings
        val compact = u0 == 1
        if (compact) {
            y0?.visibility = View.GONE
            C0?.visibility = View.GONE
            v0?.visibility = View.GONE
        } else {
            z0?.visibility = View.GONE
        }
        view.postDelayed(displaySetupRunnable(compact), 380L)
    }

    fun a0(f2: Float) {
        Global.s = 1
        val context = t0 ?: return
        if (SharedPrefs.a(context) != f2) {
            SharedPrefs.d(f2, k(1, this))
        }
    }

    private fun displaySetupRunnable(compact: Boolean) = Runnable {
        if (!isAdded || view == null) return@Runnable
        val context = t0 ?: return@Runnable
        val prefs = SharedPrefs.f4755a ?: return@Runnable
        val mounted = context.resources.getInteger(R.integer.settings_display_mounted_device)

        val scaleArray = when (mounted) {
            0 -> arrayOf(
                context.getString(R.string.settings_display_scale_100),
                context.getString(R.string.settings_display_scale_75_recommended),
                context.getString(R.string.settings_display_scale_60),
                context.getString(R.string.settings_display_scale_custom)
            )
            1 -> arrayOf(
                context.getString(R.string.settings_display_scale_100_recommended),
                "80%",
                context.getString(R.string.settings_display_scale_60),
                context.getString(R.string.settings_display_scale_custom)
            )
            else -> arrayOf(
                context.getString(R.string.settings_display_scale_100),
                context.getString(R.string.settings_display_scale_75),
                context.getString(R.string.settings_display_scale_60),
                context.getString(R.string.settings_display_scale_custom)
            )
        }
        A0?.setArray(scaleArray)

        if (u0 == 2) {
            val smallDevice = mounted == 0
            val scale = SharedPrefs.a(context)
            A0?.setSelection(
                when {
                    scale == 1.0f -> 0
                    (scale == 0.75f && smallDevice) || (scale == 0.8f && !smallDevice) -> 1
                    scale == 0.6f -> 2
                    else -> 3
                }
            )
            A0?.setItemSelectListener(object : UISettingsCardSpinner.SpinnerCallback {
                override fun c(i2: Int) {
                    when (i2) {
                        0 -> a0(1.0f)
                        1 -> a0(if (smallDevice) 0.75f else 0.8f)
                        2 -> a0(0.6f)
                        3 -> w0?.p0(111, true, null)
                    }
                }
            })
        } else {
            A0?.setSelection(0)
            A0?.setItemSelectListener(object : UISettingsCardSpinner.SpinnerCallback {
                override fun c(i2: Int) {
                    Toast.makeText(context, getString(R.string.toast_supports_only_landscape), 0).show()
                }
            })
        }
        A0?.setOnCardClickListener {
            if (u0 == 2) {
                w0?.p0(111, true, null)
            } else {
                Toast.makeText(context, getString(R.string.toast_supports_only_landscape), 0).show()
            }
        }

        val orientation = prefs.getInt("system_display_orientation", 1)
        var selection = 2
        if (orientation != 0) {
            when {
                orientation == 1 -> selection = 1
                orientation == 3 -> {
                    B0?.setSwitchChecked(true)
                    selection = 1
                }
            }
        } else {
            B0?.isEnabled = false
            selection = 0
        }
        D0?.setArray(R.array.settings_display_orientation_types)
        D0?.setSelection(selection)
        val main = x0
        D0?.setItemSelectListener(object : UISettingsCardSpinner.SpinnerCallback {
            override fun c(i2: Int) {
                when (i2) {
                    0 -> {
                        prefs.edit().putInt("system_display_orientation", 0).apply()
                        main?.Q(-1)
                        B0?.isEnabled = false
                    }
                    1 -> {
                        SharedPrefs.c(1, b(main, 2))
                        B0?.isEnabled = true
                    }
                    2 -> SharedPrefs.c(2, b(main, 3))
                }
            }
        })
        B0?.setOnSwitchCheckedChangeListener { _: CompoundButton?, checked: Boolean ->
            if (checked) {
                SharedPrefs.c(3, b(main, 0))
            } else {
                SharedPrefs.c(1, b(main, 1))
            }
        }

        if (compact) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val point = Point()
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay?.getRealSize(point)
            z0?.setSpinnerText("${point.x} ⛌ ${point.y}")
            B0?.isEnabled = false
        } else {
            // JANGAN reset B0 di sini: state switch sudah diset dari pref di atas, dan
            // setSwitchChecked() memicu listener sehingga pref kunci rotasi tertimpa ke OFF.
            C0?.setArray(R.array.settings_display_statusbar_types)
            C0?.setSelection(prefs.getInt("device_statusbar_type", 0))
            C0?.setItemSelectListener(object : UISettingsCardSpinner.SpinnerCallback {
                override fun c(i2: Int) {
                    try {
                        prefs.edit().putInt("device_statusbar_type", i2).apply()
                    } catch (_: Exception) {
                    } finally {
                        x0?.n()
                    }
                }
            })
            y0?.setSwitchChecked(prefs.getInt("device_navigation_type", 1) == 0)
            y0?.setOnSwitchCheckedChangeListener(q(1, this@UIDisplay))
        }

        s0?.run()
        s0 = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_display, container, false)
        val context = view.context
        t0 = context
        x0 = context as? MainActivityIn
        u0 = context.resources.configuration.orientation
        v0 = view.findViewById(R.id.txt_navigation)
        A0 = view.findViewById(R.id.scale_spinner)
        D0 = view.findViewById(R.id.orientation_spinner)
        B0 = view.findViewById(R.id.orientation_lock_switch)
        z0 = view.findViewById(R.id.btn_screen_res)
        C0 = view.findViewById(R.id.spinner_statusbar_type)
        y0 = view.findViewById(R.id.switch_hide_navigation)
        return view
    }
}
