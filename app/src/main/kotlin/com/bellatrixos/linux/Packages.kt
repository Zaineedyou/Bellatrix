package com.bellatrixos.linux

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableIntStateOf
import com.bellatrixos.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class Package(
    val name: String,
    val version: String,
    val description: String,
    /** The repo it comes from; null for the installed list. */
    val repo: String?,
    val installed: Boolean,
)

/** Installs and removes guest packages from the settings screen, one package manager run at a time. */
class Packages(
    private val proot: Proot,
) {
    private val distro = Distro.active

    /** The package manager holds a lock on its database; everything that runs it takes this first. */
    val lock = Mutex()

    /** Per-package install/remove progress. */
    val states = ConcurrentHashMap<String, JobState>()

    /** Bumped after installs and removals so lists reload. */
    val revision = mutableIntStateOf(0)
        private set

    /** Notified when a package job starts, progresses or ends. */
    var onChange: (() -> Unit)? = null

    /** What the launcher relies on, so the packages screen doesn't offer to remove it. */
    val core: Set<String> = distro.corePackages()

    suspend fun search(query: String): List<Package> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return parseXbpsSearch(output("xbps-query -Rs -- ${shellQuote(q)}"), q.lowercase())
    }

    suspend fun installed(): List<Package> =
        parseXbpsInstalled(output("xbps-query -m; echo '#all'; xbps-query -l"))

    /** A full upgrade first (never a partial one), then the package. */
    suspend fun install(pkg: Package) = run(pkg.name, R.string.linux_job_installing, "installing", distro.installCommand(listOf(pkg.name)))

    /** Removes the package and the dependencies nothing else needs. */
    suspend fun remove(name: String) = run(name, R.string.linux_job_removing, "removing", distro.removeCommand(name))

    private suspend fun output(script: String): String =
        withContext(Dispatchers.IO) {
            runCatching {
                val out = StringBuilder()
                val code = proot.run(script) { out.append(it).append('\n') }
                if (code == 0) out.toString() else ""
            }.getOrElse {
                Log.w(TAG, "package query failed", it)
                ""
            }
        }

    private suspend fun run(
        name: String,
        @StringRes action: Int,
        verb: String,
        script: String,
    ): Boolean {
        val (ok, _) = proot.trackedJob(action, script, lock) {
            if (it == JobState.Idle) states.remove(name) else states[name] = it
            onChange?.invoke()
        }
        Log.i(TAG, "$verb $name: ${if (ok) "ok" else "failed"}")
        revision.intValue++
        onChange?.invoke()
        return ok
    }

    companion object {
        private const val TAG = "packages"

        /** Short queries match thousands of packages. */
        private const val MAX_RESULTS = 100

        private val XBPS_LIST = Regex("""\[(.)]\s+(\S+)\s*(.*)""")
        private val XBPS_INSTALLED = Regex("""ii\s+(\S+)\s*(.*)""")
        private val XBPS_PKGVER = Regex("""(.+)-[^-]+_[0-9]+""")

        /** `bash-5.2.37_1` is `bash`; a bare name stays. */
        private fun xbpsName(s: String) = XBPS_PKGVER.matchEntire(s)?.groupValues?.get(1) ?: s

        private fun rank(
            lower: String,
            list: List<Package>,
        ) = list
            .distinctBy { it.name }
            .sortedWith(compareBy<Package>({ it.name != lower }, { !it.name.startsWith(lower) }, { lower !in it.name }, { it.name }))
            .take(MAX_RESULTS)

        internal fun parseXbpsSearch(
            out: String,
            lower: String,
        ): List<Package> =
            rank(
                lower,
                out.lines().mapNotNull { line ->
                    val m = XBPS_LIST.matchEntire(line.trim()) ?: return@mapNotNull null
                    val pkgver = m.groupValues[2]
                    Package(
                        name = pkgver.substringBeforeLast('-'),
                        version = pkgver.substringAfterLast('-'),
                        description = m.groupValues[3],
                        repo = "void",
                        installed = m.groupValues[1] == "*",
                    )
                },
            )

        internal fun parseXbpsInstalled(out: String): List<Package> {
            val manual = out.substringBefore("#all").lines().map { xbpsName(it.trim()) }.filter { it.isNotEmpty() }.toSet()
            return out
                .substringAfter("#all")
                .lines()
                .mapNotNull { line ->
                    val m = XBPS_INSTALLED.matchEntire(line.trim()) ?: return@mapNotNull null
                    val pkgver = m.groupValues[1]
                    val name = pkgver.substringBeforeLast('-')
                    if (name !in manual) return@mapNotNull null
                    Package(name, pkgver.substringAfterLast('-'), m.groupValues[2], null, installed = true)
                }.sortedBy { it.name }
        }
    }
}
