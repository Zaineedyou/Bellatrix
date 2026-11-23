package com.bellatrixos.adb

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import rikka.shizuku.Shizuku
import java.util.concurrent.atomic.AtomicBoolean

/** How much shell privilege Shizuku/Sui has given this app. */
enum class ShellStatus {
    CHECKING,
    UNAVAILABLE,
    PERMISSION_REQUIRED,
    READY_ROOT,
    READY_ADB,
}

/**
 * The app-side half of shell access. It lives in the existing [com.bellatrixos.Runtime];
 * Shizuku/Sui only supplies a binder, and the actual work runs in [AdbShellService].
 */
class AdbBridge {
    var status by mutableStateOf(ShellStatus.CHECKING)
        private set

    val ready: Boolean
        get() = status == ShellStatus.READY_ROOT || status == ShellStatus.READY_ADB

    private val onBinderReceived = Shizuku.OnBinderReceivedListener { refresh() }
    private val onBinderDead = Shizuku.OnBinderDeadListener { status = ShellStatus.UNAVAILABLE }
    private val onPermissionResult =
        Shizuku.OnRequestPermissionResultListener { _, result ->
            if (result == PackageManager.PERMISSION_GRANTED) {
                refresh()
            } else {
                status = ShellStatus.PERMISSION_REQUIRED
            }
        }

    init {
        runCatching {
            Shizuku.addBinderReceivedListenerSticky(onBinderReceived)
            Shizuku.addBinderDeadListener(onBinderDead)
            Shizuku.addRequestPermissionResultListener(onPermissionResult)
        }.onFailure { Log.w(TAG, "could not register shell listeners", it) }
        refresh()
    }

    /** Re-reads the binder and permission; safe to call from the update page. */
    fun refresh() {
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            status = ShellStatus.UNAVAILABLE
            return
        }
        val granted =
            runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }
                .getOrDefault(false)
        if (!granted) {
            status = ShellStatus.PERMISSION_REQUIRED
            return
        }
        val uid = runCatching { Shizuku.getUid() }.getOrDefault(-1)
        status = if (uid == 0) ShellStatus.READY_ROOT else ShellStatus.READY_ADB
    }

    /** Shows Shizuku's own permission dialog; the result arrives through [onPermissionResult]. */
    fun requestPermission() {
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            status = ShellStatus.UNAVAILABLE
            return
        }
        runCatching { Shizuku.requestPermission(REQUEST_CODE) }
            .onFailure { Log.w(TAG, "could not request shell permission", it) }
    }

    /** With no Shizuku binder yet, send the user to a manager that can start one. */
    fun openManager(context: Context) {
        for (packageName in MANAGERS) {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: continue
            runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            return
        }
    }

    /** Calls the privileged service off the main thread, then returns 0 on success. */
    fun installApk(
        context: Context,
        apkPath: String,
        onResult: (Int) -> Unit,
    ) {
        if (!ready) {
            onResult(-1)
            return
        }
        val args =
            Shizuku.UserServiceArgs(ComponentName(context.packageName, AdbShellService::class.java.name))
                .daemon(false)
                .processNameSuffix("adb")
                .version(1)
        val done = AtomicBoolean(false)
        val main = Handler(Looper.getMainLooper())
        lateinit var connection: ServiceConnection

        fun finish(code: Int, unbind: Boolean) {
            if (!done.compareAndSet(false, true)) return
            main.post {
                onResult(code)
                if (unbind) runCatching { Shizuku.unbindUserService(args, connection, true) }
            }
        }

        connection =
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    if (binder == null) {
                        finish(-1, true)
                        return
                    }
                    Thread {
                        val code =
                            runCatching { IAdbShellService.Stub.asInterface(binder).installApk(apkPath) }
                                .getOrDefault(-1)
                        finish(code, true)
                    }.start()
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    finish(-1, false)
                }
            }
        try {
            Shizuku.bindUserService(args, connection)
        } catch (t: Throwable) {
            Log.w(TAG, "could not start the shell service", t)
            finish(-1, false)
        }
    }

    private companion object {
        const val TAG = "AdbBridge"
        const val REQUEST_CODE = 41773
        val MANAGERS =
            listOf(
                "moe.shizuku.privileged.api",
                "moe.shizuku.manager",
                "com.rikka.sui",
            )
    }
}
