package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import com.bellatrixos.R
import com.bellatrixos.widgets.UINotification
import com.bellatrixos.windows.drivers.Sounds

/** Klik tombol suara pada pengaturan suara. */
class C(@JvmField val u: Int) : View.OnClickListener {
    override fun onClick(view: View) {
        when (u) {
            0 -> {
                val context = view.context
                try {
                    context.startActivity(Intent("android.settings.SOUND_SETTINGS"))
                } catch (_: SecurityException) {
                    Toast.makeText(context, context.getString(R.string.toast_settings_not_found), 0).show()
                }
            }
            1 -> UINotification.b?.dismiss()
            else -> Sounds.a(0)
        }
    }
}
