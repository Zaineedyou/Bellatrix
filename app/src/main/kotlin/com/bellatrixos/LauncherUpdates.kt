package com.bellatrixos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.bellatrixos.adb.AdbBridge
import com.bellatrixos.net.httpGet
import com.bellatrixos.net.openHttp
import com.bellatrixos.system.installerOf
import com.bellatrixos.system.openUrl
import com.bellatrixos.variables.AppLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import org.json.JSONArray
import java.io.File
import java.io.IOException

/**
 * A newer release of this fork on GitHub Releases, for both Composable and Bellatrix settings.
 * It only asks when settings opens or its Check button is used, never in the background. The
 * native update page shows the release notes and can install its APK from inside the launcher or
 * hand the release URL to Chrome.
 */
class LauncherUpdates(
    private val context: Context,
) {
    data class Info(
        val version: String,
        val page: String,
        val changelog: String,
        val apk: String,
    )

    private val fromFdroid = installerOf(context) in FDROID_CLIENTS
    private val scope = MainScope()
    private var checkedAt = 0L

    var available by mutableStateOf(false)
        private set

    var checking by mutableStateOf(false)
        private set

    /** Never: the install is offered from the launcher's own update page. */
    val failed = false

    /** The newer release's version, like "2.9.18". */
    var version by mutableStateOf<String?>(null)
        private set

    var changelog by mutableStateOf<String?>(null)
        private set

    var apk by mutableStateOf<String?>(null)
        private set

    /** The newest GitHub release data, or null when this build is current. */
    var info by mutableStateOf<Info?>(null)
        private set

    var installing by mutableStateOf(false)
        private set

    var lastError by mutableStateOf<String?>(null)
        private set

    /** Asks this fork's GitHub unless it was asked within the hour; [force] asks anyway. */
    fun check(
        force: Boolean = false,
        onChecked: ((Info?) -> Unit)? = null,
    ) {
        if (!CHECK_UPSTREAM || fromFdroid || checking || (!force && System.currentTimeMillis() - checkedAt < CHECK_EVERY)) {
            // The native page is showing progress; it must always get its callback back.
            onChecked?.let { callback -> scope.launch(Dispatchers.Main) { callback(info) } }
            return
        }
        checking = true
        lastError = null
        scope.launch {
            var result: Info? = null
            try {
                val json = withContext(Dispatchers.IO) { fetchReleases() }
                result = newerRelease(json, BuildConfig.VERSION_NAME)
                publish(result)
                checkedAt = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.w(TAG, "checking GitHub for a release failed", e)
                lastError = context.getString(R.string.settings_update_dialog_failed)
                publish(null)
            } finally {
                checking = false
                withContext(Dispatchers.Main) { onChecked?.invoke(result) }
            }
        }
    }

    private fun publish(release: Info?) {
        info = release
        version = release?.version
        changelog = release?.changelog
        apk = release?.apk.takeUnless { it.isNullOrBlank() }
        available = release != null
    }

    /** Opens the GitHub release so Chrome (or any browser) can download and install the APK. */
    fun openRelease(targetContext: Context = context) {
        val target = info?.page?.takeUnless { it.isBlank() } ?: AppLinks.RELEASES_URL
        if (!targetContext.openUrl(target)) Log.w(TAG, "no app opens $target")
    }

    /** Downloads this fork's APK and hands it to Android's installer without leaving the launcher. */
    fun install(
        targetContext: Context = context,
        onFinished: (() -> Unit)? = null,
    ) {
        val release = info
        if (release == null) {
            onFinished?.invoke()
            return
        }
        if (release.apk.isBlank()) {
            openRelease(targetContext)
            onFinished?.invoke()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !targetContext.packageManager.canRequestPackageInstalls()) {
            val permission =
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${targetContext.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { targetContext.startActivity(permission) }
            onFinished?.invoke()
            return
        }
        if (installing) {
            onFinished?.invoke()
            return
        }
        installing = true
        lastError = null
        scope.launch {
            try {
                val file =
                    withContext(Dispatchers.IO) {
                        val bytes = httpGet(release.apk, connectTimeout = 15_000, readTimeout = 60_000)
                        val dir = File(targetContext.cacheDir, "updates").apply { mkdirs() }
                        File(dir, "Bellatrix-${release.version}.apk").apply { writeBytes(bytes) }
                    }
                val uri = FileProvider.getUriForFile(targetContext, "${targetContext.packageName}.provider", file)
                val intent =
                    Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                withContext(Dispatchers.Main) { targetContext.startActivity(intent) }
            } catch (e: Exception) {
                Log.w(TAG, "installing the launcher update failed", e)
                lastError = targetContext.getString(R.string.settings_update_dialog_install_failed)
            } finally {
                withContext(Dispatchers.Main) {
                    installing = false
                    onFinished?.invoke()
                }
            }
        }
    }

    /**
     * Installs the release through the shell privilege already granted to Bellatrix by
     * Shizuku/Sui. USB debugging, Android 11+ wireless debugging and root are routes to
     * that same privilege; there is no second launcher or settings surface here.
     */
    fun installWithShell(
        targetContext: Context = context,
        onFinished: ((Boolean) -> Unit)? = null,
    ) {
        val release = info
        if (release == null) {
            onFinished?.invoke(false)
            return
        }
        if (release.apk.isBlank()) {
            openRelease(targetContext)
            onFinished?.invoke(false)
            return
        }
        if (installing) return
        val adb = (targetContext.applicationContext as? App)?.runtime?.adb
        if (adb == null || !adb.ready) {
            lastError = targetContext.getString(R.string.adb_shell_status_unavailable)
            onFinished?.invoke(false)
            return
        }
        installing = true
        lastError = null
        scope.launch {
            try {
                val apk =
                    withContext(Dispatchers.IO) {
                        val bytes = httpGet(release.apk, connectTimeout = 15_000, readTimeout = 60_000)
                        // External app storage is readable by the shell user service; app cache is not.
                        val dir =
                            targetContext.getExternalFilesDir("updates")
                                ?: File(targetContext.cacheDir, "updates").also { runCatching { it.setReadable(true, false) } }
                        dir.mkdirs()
                        File(dir, "Bellatrix-${release.version}.apk").apply {
                            writeBytes(bytes)
                            runCatching { setReadable(true, false) }
                        }
                    }
                val code = withContext(Dispatchers.IO) { adb.installAwait(targetContext, apk.absolutePath) }
                if (code != 0) lastError = targetContext.getString(R.string.adb_shell_install_failed)
            } catch (e: Exception) {
                Log.w(TAG, "shell install of the launcher update failed", e)
                lastError = targetContext.getString(R.string.adb_shell_install_failed)
            } finally {
                withContext(Dispatchers.Main) {
                    installing = false
                    onFinished?.invoke(lastError == null)
                }
            }
        }
    }

    private suspend fun AdbBridge.installAwait(
        context: Context,
        path: String,
    ): Int =
        suspendCoroutine { continuation -> installApk(context, path) { continuation.resume(it) } }

    @Suppress("UNUSED_PARAMETER")
    fun start(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        openRelease()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onResult(resultCode: Int) {}

    private fun fetchReleases(): String {
        val connection = openHttp(RELEASES, 15_000, 30_000)
        try {
            connection.setRequestProperty("User-Agent", "Bellatrix-Launcher")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            if (connection.responseCode != 200) throw IOException("HTTP ${connection.responseCode} for $RELEASES")
            return connection.inputStream.use { String(it.readBytes(), Charsets.UTF_8) }
        } finally {
            connection.disconnect()
        }
    }

    internal companion object {
        const val TAG = "LauncherUpdates"
        const val CHECK_UPSTREAM = true
        const val RELEASES = "https://api.github.com/repos/Zaineedyou/Bellatrix/releases?per_page=20"
        const val CHECK_EVERY = 60 * 60_000L

        /** F-Droid and the clients of its repositories. */
        val FDROID_CLIENTS = setOf("org.fdroid.fdroid", "org.fdroid.basic", "com.looker.droidify", "com.machiav3lli.fdroid")

        private val TAG_VERSION = Regex("""v(\d+(?:\.\d+)*)""")

        /**
         * The newest release in GitHub's releases [json] that's newer than [current], with its
         * version, page, changelog and APK asset. Only this fork's tags (v1.5.0, …) count; drafts,
         * pre-releases and unrelated tags do not.
         */
        fun newerRelease(
            json: String,
            current: String,
        ): Info? {
            val releases = JSONArray(json)
            val versions =
                (0 until releases.length()).mapNotNull { i ->
                    val release = releases.getJSONObject(i)
                    if (release.optBoolean("draft") || release.optBoolean("prerelease")) return@mapNotNull null
                    val version = TAG_VERSION.matchEntire(release.optString("tag_name"))?.groupValues?.get(1) ?: return@mapNotNull null
                    val assets = release.optJSONArray("assets")
                    var apk = ""
                    if (assets != null) {
                        for (j in 0 until assets.length()) {
                            val asset = assets.getJSONObject(j)
                            val name = asset.optString("name")
                            if (!name.endsWith(".apk", ignoreCase = true)) continue
                            apk = asset.optString("browser_download_url")
                            if (name.startsWith("Bellatrix", ignoreCase = true)) break
                        }
                    }
                    Info(
                        version = version,
                        page = release.optString("html_url"),
                        changelog = release.optString("body"),
                        apk = apk,
                    )
                }
            return versions.filter { compareVersions(it.version, current) > 0 }.maxWithOrNull { a, b -> compareVersions(a.version, b.version) }
        }

        /** Compares dotted versions part by part as numbers: 1.10.0 is after 1.9.2; a missing part is 0. */
        fun compareVersions(
            a: String,
            b: String,
        ): Int {
            val x = a.split('.').map { it.toIntOrNull() ?: 0 }
            val y = b.split('.').map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(x.size, y.size)) {
                val c = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
                if (c != 0) return c
            }
            return 0
        }
    }
}
