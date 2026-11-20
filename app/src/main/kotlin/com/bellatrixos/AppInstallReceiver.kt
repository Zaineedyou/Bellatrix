package com.bellatrixos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Menerima broadcast instal/uninstal paket dan meneruskannya ke callback. */
class AppInstallReceiver(
    @JvmField val f4625a: AppChangeCallback
) : BroadcastReceiver() {

    fun interface AppChangeCallback {
        fun f(str: String, str2: String, z: Boolean)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val data: Uri = intent.data ?: return
        val packageName = data.schemeSpecificPart ?: return
        if (packageName.isEmpty()) return
        f4625a.f(action, packageName, intent.getBooleanExtra("android.intent.extra.REPLACING", false))
    }
}
