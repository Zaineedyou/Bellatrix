package com.bellatrixos.app_settings.fragments

import android.content.Context
import android.content.Intent
import android.view.View
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.variables.Global
import com.bellatrixos.variables.AppLinks
import com.bellatrixos.windows.installed_apps.IntentSites

/** Klik tombol pada halaman pembaruan aplikasi. */
class w(@JvmField val v: UIPCUpdate, @JvmField val u: Int) : View.OnClickListener {
    override fun onClick(view: View) {
        when (u) {
            // Cek pembaruan: tampilkan animasi sebentar lalu buka halaman Release di repo.
            0 -> {
                if (Global.B != 1) {
                    v.b0(true)
                    v.a0(UIPCUpdate.STATUS.u)
                    AppExecutors.c().postDelayed(y(v, 0), 1500L)
                }
            }
            // Beri Star: buka repo GitHub.
            1 -> {
                val context = v.s0 ?: return
                IntentSites.b(context, AppLinks.REPO_URL)
            }
            // Bagikan aplikasi: tautan ke repo GitHub.
            2 -> {
                val context: Context = v.s0 ?: return
                val intent = Intent().apply {
                    action = "android.intent.action.SEND"
                    putExtra("android.intent.extra.TEXT", context.getString(R.string.toast_share_msg) + ": " + AppLinks.REPO_URL)
                    type = "text/plain"
                }
                context.startActivity(intent)
            }
            // Tombol unduh / lihat isi pembaruan: halaman Release.
            else -> {
                val context = v.s0 ?: return
                IntentSites.c(context, y(v, 1))
            }
        }
    }
}
