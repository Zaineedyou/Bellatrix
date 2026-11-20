package com.bellatrixos.app_settings.fragments

import android.app.AlertDialog
import android.view.View
import android.widget.SimpleAdapter
import androidx.core.content.ContextCompat
import com.bellatrixos.R
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.windows.icons.IconEngine
import com.bellatrixos.windows.icons.IconPack
import com.bellatrixos.windows.icons.dialogs.c
import com.bellatrixos.windows.icons.models.IconPackItem

/** Klik kartu pada halaman Apps (termasuk dialog pemilih paket ikon). */
class ViewOnClickListenerC0056e(@JvmField val v: UIApps, @JvmField val u: Int) : View.OnClickListener {
    override fun onClick(view: View) {
        val apps = v
        when (u) {
            0 -> apps.t0?.p0(41, true, null)
            1 -> if (!apps.C0) {
                apps.C0 = true
                val current = SharedPrefs.f4755a?.getString("system_icon_pack", "system") ?: "system"
                if (current != "system" && IconEngine.g != null) {
                    apps.x0?.text = current
                }
                apps.y0?.setOnClickListener(ViewOnClickListenerC0056e(apps, 4))
            }
            2 -> apps.u0?.c0("UIInstaller", null)
            3 -> apps.t0?.p0(42, true, null)
            else -> {
                if (apps.v0 == null) {
                    val context = apps.s0 ?: return
                    val list = ArrayList<IconPackItem>()
                    list.add(
                        IconPackItem(
                            ContextCompat.getDrawable(context, R.drawable.img_icon_pack_default),
                            "system",
                            "System Default"
                        )
                    )
                    IconPack.a(context, list)
                    apps.v0 = ArrayList<Any?>().apply { addAll(list) }
                }
                val context = apps.s0 ?: return
                val packs = apps.v0 ?: return
                val callback = C0054c(1, apps)
                val builder = AlertDialog.Builder(context, R.style.UIAlertDialog)
                builder.setTitle(context.getString(R.string.settings_app_ic_pack_select))
                val entries = ArrayList<HashMap<String, Any>>()
                for (item in packs) {
                    val pack = item as? IconPackItem ?: continue
                    val map = HashMap<String, Any>()
                    map["icon"] = pack.icon ?: ""
                    map["name"] = pack.label
                    map["packageName"] = pack.packageName
                    entries.add(map)
                }
                val adapter = SimpleAdapter(
                    context,
                    entries,
                    R.layout.ui_dialog_icon_list,
                    arrayOf("icon", "name"),
                    intArrayOf(R.id.icon, R.id.name)
                )
                adapter.viewBinder = c(1)
                builder.setAdapter(adapter, DialogInterfaceOnClickListenerC0055d(entries, 3, callback))
                builder.show()
            }
        }
    }
}
