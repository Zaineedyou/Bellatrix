package com.bellatrixos.app_settings.fragments

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import com.bellatrixos.LauncherActivity
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.storage.LauncherData
import com.bellatrixos.variables.Global

/** Check listener bersama untuk pengaturan perangkat, tampilan, home dan taskbar. */
class q(@JvmField val f4720a: Int, @JvmField val b: Fragment) : CompoundButton.OnCheckedChangeListener {
    override fun onCheckedChanged(buttonView: CompoundButton, z: Boolean) {
        when (f4720a) {
            0 -> {
                val fragment = b as? UIDevices ?: return
                if (!fragment.v0) {
                    fragment.v0 = true
                    val intent = Intent("android.settings.BLUETOOTH_SETTINGS")
                    val context = fragment.s0 ?: return
                    if (intent.resolveActivity(context.packageManager) == null) {
                        fragment.v0 = false
                    } else {
                        fragment.x0?.launch(intent)
                    }
                }
            }
            1 -> {
                val fragment = b as? UIDisplay ?: return
                SharedPrefs.f4755a?.edit()?.putInt("device_navigation_type", if (!z) 1 else 0)?.apply()
                fragment.x0?.n()
            }
            2 -> {
                val fragment = b as? UIHome ?: return
                val context = fragment.s0 ?: return
                val component = ComponentName(context, LauncherActivity::class.java)
                if (!z) {
                    context.packageManager.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    fragment.t0?.isEnabled = false
                } else {
                    context.packageManager.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    fragment.t0?.isEnabled = true
                }
            }
            3 -> {
                val fragment = b as? UIHome ?: return
                val context = fragment.s0 ?: return
                LauncherData.setOwlHome(context, z)
                // The home is now inside MainActivity; redraw it with (or without) the OWL layout at once.
                (context as? Activity)?.recreate()
            }
            else -> {
                val fragment = b as? UITaskbar ?: return
                Global.o = z
                fragment.u0?.j0(z)
            }
        }
    }
}
