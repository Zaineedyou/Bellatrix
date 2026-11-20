package com.bellatrixos

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process
import com.bellatrixos.linux.Proot

/**
 * Ends this process and starts the launcher again. Guest processes go first: a proot outlives the app's
 * process and would keep running on the old system. The relaunch is also scheduled, in case Android doesn't
 * bring the home app back on its own.
 */
fun restartLauncher(
    context: Context,
    proot: Proot,
) {
    runCatching { proot.killAll() }
    val intent =
        (
            context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(context.packageName)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    runCatching { context.getSystemService(AlarmManager::class.java).set(AlarmManager.RTC, System.currentTimeMillis() + 600, pending) }
    Process.killProcess(Process.myPid())
}
