package com.bellatrixos

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Resources
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bellatrixos.linux.LinuxRuntime
import com.bellatrixos.linux.LinuxState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Keeps proot and the Wayland compositor alive while Linux is installed or installing. */
class LinuxService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watch: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun getResources(): Resources = application?.resources ?: super.getResources()

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, getString(R.string.linux_notification_channel), NotificationManager.IMPORTANCE_LOW),
            )
        }
        val state = LinuxRuntime.get(this).bootstrap.state
        val notification = notification(state.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            // Bellatrix's minSdk is older than Omalauncher's; use the two-argument call there.
            @Suppress("DEPRECATION")
            startForeground(1, notification)
        }
        pending = (pending - 1).coerceAtLeast(0)
        if (!wanted) {
            if (pending == 0) stopSelf()
            return START_NOT_STICKY
        }
        if (watch == null) {
            watch =
                scope.launch {
                    state.map(::text).distinctUntilChanged().collect { nm.notify(1, build(it)) }
                }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun notification(state: LinuxState) = build(text(state))

    private fun text(state: LinuxState): String =
        when (state) {
            is LinuxState.Installing ->
                state.progress?.let {
                    getString(R.string.linux_notification_installing_percent, (it * 100).toInt(), getString(state.stage.label))
                } ?: getString(R.string.linux_notification_installing, getString(state.stage.label))
            is LinuxState.Failed -> getString(R.string.linux_notification_failed)
            LinuxState.Removing -> getString(R.string.linux_notification_removing)
            else -> getString(R.string.linux_notification_running)
        }

    private fun build(text: String): Notification =
        NotificationCompat
            .Builder(this, CHANNEL)
            .setContentTitle(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    companion object {
        private const val CHANNEL = "session"

        // Main thread only, like onStartCommand.
        private var wanted = false

        /** startForegroundService() calls whose onStartCommand hasn't run yet. */
        private var pending = 0

        /**
         * Android kills the app when a foreground service started with startForegroundService() is
         * stopped before it calls startForeground(); leave such an early stop to onStartCommand.
         */
        fun set(
            context: Context,
            on: Boolean,
        ) {
            wanted = on
            val intent = Intent(context, LinuxService::class.java)
            if (on) {
                ContextCompat.startForegroundService(context, intent)
                pending++
            } else if (pending == 0) {
                context.stopService(intent)
            }
        }
    }
}
