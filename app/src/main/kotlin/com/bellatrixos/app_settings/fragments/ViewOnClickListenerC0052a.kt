package com.bellatrixos.app_settings.fragments

import android.content.DialogInterface
import android.content.Intent
import android.view.View
import android.widget.Toast
import com.bellatrixos.R
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.variables.Global
import com.bellatrixos.windows.dialogs.UIDialog
import com.bumptech.glide.Glide

/** Klik bersama: navigasi About, skala tampilan, home settings, dan pembersihan cache. */
class ViewOnClickListenerC0052a(@JvmField val u: Int, @JvmField val v: Any?) : View.OnClickListener {
    override fun onClick(view: View) {
        when (u) {
            0 -> (v as? UIAbout)?.u0?.p0(5, true, null)
            1 -> {
                val scale = v as? UIDisplayScale ?: return
                val text = scale.u0?.text?.toString() ?: return
                val value = text.toFloatOrNull() ?: return
                if (value in 45.0f..135.0f) {
                    val target = value / 100.0f
                    Global.s = 1
                    val context = scale.s0 ?: return
                    if (SharedPrefs.a(context) != target) {
                        SharedPrefs.d(target, t(scale, 1))
                    }
                }
            }
            2 -> {
                val home = v as? UIHome ?: return
                val intent = Intent("android.settings.HOME_SETTINGS")
                if (intent.resolveActivity(home.s0?.packageManager ?: return) != null) {
                    home.startActivity(intent)
                }
            }
            else -> {
                val storage = v as? UIStorage ?: return
                val context = storage.s0 ?: return
                val dialog = UIDialog(
                    context,
                    context.getString(R.string.settings_system_storage),
                    context.getString(R.string.settings_storage_cleanup_subtitle)
                )
                dialog.z = context.getString(R.string.settings_storage_cleanup)
                dialog.A = context.getString(R.string.dialog_actions_cancel)
                dialog.C = DialogInterface.OnClickListener { _, _ ->
                    Glide.get(context).clearMemory()
                    Thread(k(3, storage)).start()
                }
                dialog.setCanceledOnTouchOutside(true)
                dialog.show()
            }
        }
    }
}
