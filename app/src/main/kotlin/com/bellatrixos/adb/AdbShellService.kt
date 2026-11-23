package com.bellatrixos.adb

import android.util.Log
import java.io.File

/**
 * Shizuku/Sui user service: this code runs as the shell (UID 2000) or root (UID 0),
 * so it can call the package manager directly. USB debugging, Android 11+ wireless
 * debugging and a rooted shell all reach this same service through Shizuku/Sui.
 */
class AdbShellService : IAdbShellService.Stub() {
    override fun destroy() {
        // Shizuku calls this when the connection is removed. Nothing is held open.
    }

    override fun installApk(apkPath: String): Int {
        val staged = File("/data/local/tmp/bellatrix-update.apk")
        return try {
            val source = File(apkPath)
            if (!source.isFile || source.length() <= 0L) {
                Log.w(TAG, "update APK is missing: $apkPath")
                return -1
            }
            if (staged.exists() && !staged.delete()) {
                Log.w(TAG, "could not remove the staged update APK")
                return -1
            }
            source.inputStream().use { input -> staged.outputStream().use { input.copyTo(it) } }
            staged.setReadable(true, false)

            val process =
                ProcessBuilder("pm", "install", "-r", staged.absolutePath)
                    .redirectErrorStream(true)
                    .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val code = process.waitFor()
            if (code != 0) Log.w(TAG, "pm install failed ($code): ${output.take(2000)}")
            code
        } catch (t: Throwable) {
            Log.w(TAG, "shell install failed", t)
            -1
        } finally {
            runCatching { staged.delete() }
        }
    }

    private companion object {
        const val TAG = "AdbShellService"
    }
}
