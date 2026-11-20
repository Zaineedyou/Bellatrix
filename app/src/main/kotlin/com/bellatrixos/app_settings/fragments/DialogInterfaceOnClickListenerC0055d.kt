package com.bellatrixos.app_settings.fragments

import android.content.DialogInterface
import android.widget.TextView
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.desktop.DesktopItem
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.windows.dialogs.UIDialog
import com.bellatrixos.windows.icons.dialogs.DialogIconPacks

/** Klik tombol dialog bersama (nama pengguna, desktop, dan paket ikon). */
class DialogInterfaceOnClickListenerC0055d(
    @JvmField val v: Any?,
    @JvmField val u: Int,
    @JvmField val w: Any?
) : DialogInterface.OnClickListener {
    override fun onClick(dialog: DialogInterface, i2: Int) {
        when (u) {
            0 -> {
                val accounts = v as? UIAccounts ?: return
                val name = (w as? UIDialog)?.w?.text?.toString() ?: return
                accounts.z0?.text = name
                accounts.A0?.text = name
                @Suppress("UNCHECKED_CAST")
                (accounts.v0 as? Any)?.let { settings ->
                    try {
                        val field = settings.javaClass.getField("J0")
                        (field.get(settings) as? TextView)?.text = name
                    } catch (_: Exception) {
                    }
                }
                accounts.w0?.r(name)
                SharedPrefs.f4755a?.edit()?.putString("system_user_name", name)?.apply()
            }
            1 -> (v as? MainActivityIn)?.m0(w as? DesktopItem)
            2 -> (v as? MainActivityIn)?.D(w as? String ?: return)
            else -> {
                @Suppress("UNCHECKED_CAST")
                val list = v as? List<Map<String, Any>> ?: return
                if (i2 < 0 || i2 >= list.size) return
                val entry = list[i2]
                val label = entry["name"] as? String ?: return
                val packageName = entry["packageName"] as? String ?: return
                (w as? DialogIconPacks.IconPackSelectionListener)?.d(label, packageName)
            }
        }
    }
}
