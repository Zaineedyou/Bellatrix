package com.bellatrixos.windows

import android.app.Activity
import android.content.Intent
import com.bellatrixos.UIFragment
import com.bellatrixos.taskbar.TaskBarAdapter
import com.bellatrixos.taskbar.TaskBarDB
import com.bellatrixos.windows.config.LogonController

/** Runnable sintetis untuk logon, restart, dan sinkronisasi taskbar. */
class a(
    @JvmField val v: Any,
    @JvmField val u: Int,
    @JvmField val w: Any
) : Runnable {
    override fun run() {
        when (u) {
            0 -> finishIfNeeded { it.finishAffinity() }
            1 -> finishIfNeeded { activity ->
                val intent = Intent(activity, activity.javaClass)
                intent.addFlags(335609856)
                activity.finish()
                activity.startActivity(intent)
            }
            else -> {
                val adapter = v as? TaskBarAdapter ?: return
                val fragment = w as? UIFragment ?: return
                val tag = fragment.tag?.substringBefore("::::")
                val index = TaskBarDB.a(tag ?: "")
                if (index == -1) return
                adapter.g(tag ?: "", true, false)
                if (index in adapter.D.indices) adapter.A = index
            }
        }
    }

    private fun finishIfNeeded(action: (Activity) -> Unit) {
        val controller = (v as? Array<*>)?.getOrNull(0) as? LogonController
        if (controller == null || controller.f4890d) return
        val activity = w as? Activity ?: return
        if (!activity.isFinishing && !activity.isDestroyed) action(activity)
    }
}
