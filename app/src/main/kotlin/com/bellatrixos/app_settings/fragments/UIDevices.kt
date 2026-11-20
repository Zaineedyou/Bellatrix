package com.bellatrixos.app_settings.fragments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bellatrixos.R
import com.bellatrixos.app_settings.UISettings
import com.bellatrixos.widgets.cards.UISettingsCard
import com.bellatrixos.widgets.cards.UISettingsCardSwitch
import com.bellatrixos.windows.drivers.network.Network

/** Halaman perangkat: mouse, bluetooth, printer, dan display. */
class UIDevices : Fragment() {
    @JvmField var A0: UISettingsCard? = null
    @JvmField var s0: Context? = null
    @JvmField var t0: TextView? = null
    @JvmField var u0: UISettings? = null
    @JvmField var v0: Boolean = false
    @JvmField var w0: UISettingsCardSwitch? = null
    lateinit var x0: ActivityResultLauncher<Intent>
    @JvmField var y0: UISettingsCard? = null
    @JvmField var z0: UISettingsCard? = null

    override fun onDestroyView() {
        super.onDestroyView()
        u0 = null
        y0?.setOnCardClickListener(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        u0 = parentFragment as? UISettings
        y0?.setOnCardClickListener {
            u0?.p0(21, true, null)
        }
        t0?.setOnClickListener {
            val context = s0 ?: return@setOnClickListener
            try {
                context.startActivity(Intent("android.settings.BLUETOOTH_SETTINGS"))
            } catch (_: SecurityException) {
                Toast.makeText(context, context.getString(R.string.toast_settings_not_found), 0).show()
            }
        }
        z0?.setOnCardClickListener {
            s0?.startActivity(Intent("android.settings.ACTION_PRINT_SETTINGS"))
        }
        A0?.setOnCardClickListener {
            val context = s0 ?: return@setOnCardClickListener
            try {
                context.startActivity(Intent("android.settings.CAST_SETTINGS"))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, context.getString(R.string.toast_settings_not_found), 0).show()
            }
        }

        w0?.setSwitchChecked(Network.a())
        w0?.setOnSwitchCheckedChangeListener(q(0, this))
        x0 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            C0054c(4, this).onActivityResult(result)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.settings_devices, container, false)
        s0 = view.context
        t0 = view.findViewById(R.id.btn_add_device)
        y0 = view.findViewById(R.id.btn_device_mouse)
        z0 = view.findViewById(R.id.btn_settings_printer)
        A0 = view.findViewById(R.id.btn_settings_display)
        w0 = view.findViewById(R.id.switch_bluetooth_enabled)
        return view
    }
}
