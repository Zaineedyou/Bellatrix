package com.bellatrixos.app_settings.fragments

import androidx.recyclerview.widget.LinearLayoutManager
import com.bellatrixos.AppExecutors
import com.bellatrixos.app_settings.adapters.InstalledAppsAdapter
import com.bellatrixos.windows.installed_apps.InstalledAppsDB

/** Runnable pemuat daftar aplikasi terpasang. */
class RunnableC0057f(@JvmField val v: UIAppsInstalled, @JvmField val u: Int) : Runnable {
    override fun run() {
        when (u) {
            0 -> {
                if (v.isAdded && v.view != null) {
                    AppExecutors.d().execute(RunnableC0057f(v, 1))
                }
            }
            1 -> {
                val list = InstalledAppsDB.b()
                v.w0 = list
                val context = v.s0 ?: return
                v.v0 = InstalledAppsAdapter(context, list.toMutableList())
                AppExecutors.c().post(RunnableC0057f(v, 2))
            }
            else -> {
                val recycler = v.u0 ?: return
                recycler.layoutManager = LinearLayoutManager(recycler.context, 1, false)
                recycler.adapter = v.v0
                v.v0?.A = C0054c(2, v)
            }
        }
    }
}
