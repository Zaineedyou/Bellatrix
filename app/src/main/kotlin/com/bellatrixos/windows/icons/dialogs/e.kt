package com.bellatrixos.windows.icons.dialogs

import android.content.Intent
import android.net.Uri
import com.bellatrixos.AppExecutors
import com.bellatrixos.startmenu.StartMenuAdapter

/** Runnable sintetis yang memuat daftar paket ikon aplikasi. */
class e(@JvmField val v: DialogAppIcons, @JvmField val u: Int) : Runnable {
    override fun run() {
        if (u == 0) {
            val packages = v.Q0
            if (packages.isEmpty()) {
                if (StartMenuAdapter.G.isEmpty()) {
                    val intent = Intent("android.intent.action.MAIN", null as Uri?).apply {
                        addCategory("android.intent.category.LAUNCHER")
                    }
                    val pm = v.requireContext().packageManager
                    pm.queryIntentActivities(intent, 0).forEach { packages.add(it.activityInfo.packageName) }
                } else {
                    packages.addAll(StartMenuAdapter.G.keys)
                }
            }
            AppExecutors.c().post(e(v, 1))
            return
        }
        val adapter = v.K0 ?: return
        @Suppress("UNCHECKED_CAST")
        val list = adapter.z as MutableList<Any>
        list.clear()
        list.addAll(v.Q0)
        adapter.notifyDataSetChanged()
    }
}
