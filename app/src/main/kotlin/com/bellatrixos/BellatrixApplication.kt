package com.bellatrixos

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import com.bellatrixos.linux.LinuxRuntime
import com.bellatrixos.linux.Proot
import com.bellatrixos.upgrade.ManagedFiles
import com.bellatrixos.widgets.WidgetEngine
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

private const val TAG = "BellatrixCrash"

/**
 * Application global yang memasang handler crash.
 *
 * Saat ada exception yang tidak tertangkap, log error langsung disalin ke clipboard
 * sebelum proses mati, jadi bisa ditempel tanpa harus lewat logcat PC.
 */
class BellatrixApplication : App() {

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(CrashClipboardHandler(this, previous))
        // Owns proot, Wayland and the GPU state for the whole process. Starting is idempotent and
        // never blocks: MainActivity only wires the "Linux isn't installed" callback on top of it.
        LinuxRuntime.get(this).start()
        // Seed the guest-side widget plumbing (AGENTS.md, owl-check, owl-send) without starting
        // the whole OWL engine. The proot binds already point these files at filesDir.
        runCatching {
            val proot = Proot(this)
            ManagedFiles(proot.filesDir, File(proot.filesDir, "owl-guest-managed.json"))
                .sync(WidgetEngine.guestManaged(this, proot))
        }.onFailure { Log.w(TAG, "could not seed guest widget files", it) }
    }

    private class CrashClipboardHandler(
        private val app: Application,
        private val previous: Thread.UncaughtExceptionHandler?
    ) : Thread.UncaughtExceptionHandler {

        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            val text = try {
                buildLog(throwable, thread)
            } catch (_: Throwable) {
                Log.getStackTraceString(throwable)
            }

            try {
                saveCopy(text)
            } catch (_: Throwable) {
            }

            copyToClipboard(text)

            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }

        private fun buildLog(throwable: Throwable, thread: Thread): String {
            val sb = StringBuilder()
            sb.append("Bellatrix crash | SDK ").append(Build.VERSION.SDK_INT)
                .append(" | ").append(Build.MODEL)
                .append(" | thread ").append(thread.name).append('\n')
            sb.append(Log.getStackTraceString(throwable)).append('\n')
            sb.append("--- logcat ---\n")

            try {
                val process = java.lang.Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "300"))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var count = 0
                while (count < 300) {
                    val line = reader.readLine() ?: break
                    sb.append(line).append('\n')
                    count++
                }
                reader.close()
                process.destroy()
            } catch (e: Throwable) {
                sb.append("(logcat unavailable: ").append(e.message).append(")\n")
            }

            return sb.toString()
        }

        private fun saveCopy(text: String) {
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targets = arrayOf(
                File(publicDir, "Bellatrix-crash.txt"),
                File(app.filesDir, "Bellatrix-crash.txt")
            )
            for (target in targets) {
                try {
                    target.parentFile?.mkdirs()
                    target.writeText(text)
                    return
                } catch (_: Throwable) {
                }
            }
        }

        private fun copyToClipboard(text: String) {
            val copy = Runnable {
                try {
                    val manager = app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    manager?.setPrimaryClip(ClipData.newPlainText("Bellatrix crash log", text))
                    Log.i(TAG, "Crash log copied to clipboard (${text.length} chars)")
                } catch (e: Throwable) {
                    Log.w(TAG, "Failed to copy crash log", e)
                }
            }

            if (Looper.myLooper() == Looper.getMainLooper()) {
                copy.run()
                return
            }

            val latch = CountDownLatch(1)
            Handler(Looper.getMainLooper()).post {
                try {
                    copy.run()
                } finally {
                    latch.countDown()
                }
            }
            try {
                latch.await(1500, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
            }
        }
    }
}
