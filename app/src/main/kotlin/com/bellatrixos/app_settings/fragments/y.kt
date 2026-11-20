package com.bellatrixos.app_settings.fragments

import com.bellatrixos.windows.installed_apps.IntentSites

/** Runnable pengecekan hasil pemasangan situs/web app. */
class y(@JvmField val v: UIPCUpdate, @JvmField val u: Int) : Runnable {
    override fun run() {
        when (u) {
            0 -> {
                val context = v.s0 ?: return
                if (v.isAdded) IntentSites.c(context, y(v, 1))
            }
            else -> v.b0(false)
        }
    }
}
