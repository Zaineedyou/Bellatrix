package com.bellatrixos.linux

import android.content.Context
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.util.Log
import androidx.annotation.StringRes
import com.bellatrixos.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPInputStream
import kotlin.concurrent.thread

private const val TAG = "bootstrap"
private const val NETWORK_TRIES = 3
private const val FORK_CHECK_SECONDS = 30L
private const val TARBALL_SPACE = 900_000_000L
private const val SYSTEM_SPACE = 2_100_000_000L
private const val CLIAMP_VERSION = "v2.3.0"
private const val CLIAMP_URL = "https://github.com/bjarneo/cliamp/releases/download/$CLIAMP_VERSION/cliamp-linux-arm64"
private const val CLIAMP_SHA256 = "827b4aa879feecab33ea621dc1460c60ffa765c210b5046d679ce5a4073d2b32"

private class NoRetry(message: String, cause: Throwable? = null) : IOException(message, cause)

private fun Throwable.outOfSpace() =
    generateSequence(this) { it.cause }.any {
        (it is ErrnoException && it.errno == OsConstants.ENOSPC) || it.message?.contains("ENOSPC") == true
    }

private fun gigabytes(bytes: Long) = "%.1f GB".format(bytes / 1e9)

private class CountingInputStream(source: InputStream) : FilterInputStream(source) {
    var count = 0L
        private set

    override fun read(): Int = super.read().also { if (it >= 0) count++ }

    override fun read(
        b: ByteArray,
        off: Int,
        len: Int,
    ): Int = super.read(b, off, len).also { if (it > 0) count += it }
}

/** Idempotent step journal, replacing Bellatrix's upgrade StepJournal. */
private class StepJournal(private val file: File) {
    private val ids = HashSet<String>()

    @Volatile
    var setupComplete = false
        private set

    init {
        val lines = runCatching { file.readLines() }.getOrDefault(emptyList())
        setupComplete = lines.firstOrNull() == "setup"
        ids += lines.drop(1).filter { it.isNotEmpty() }
    }

    operator fun contains(id: String) = synchronized(this) { id in ids }

    @Synchronized
    fun mark(id: String) {
        ids += id
        save()
    }

    @Synchronized
    fun completeSetup() {
        setupComplete = true
        save()
    }

    @Synchronized
    fun reset() {
        ids.clear()
        setupComplete = false
        save()
    }

    private fun save() {
        file.parentFile?.mkdirs()
        writeAtomically(file, ((if (setupComplete) listOf("setup") else emptyList()) + ids.sorted()).joinToString("\n", postfix = "\n").toByteArray())
    }
}

/** The phases of a fresh install, in order. */
enum class Stage(
    @StringRes val label: Int,
) {
    Download(R.string.linux_stage_download),
    Extract(R.string.linux_stage_extract),
    Configure(R.string.linux_stage_configure),
    Packages(R.string.linux_stage_packages),
}

sealed interface LinuxState {
    data object Checking : LinuxState
    data object NotInstalled : LinuxState

    data class Installing(
        val stage: Stage,
        val step: String,
        val progress: Float?,
        val log: List<String>,
    ) : LinuxState

    data class Failed(
        val stage: Stage,
        val message: String,
        val log: List<String>,
    ) : LinuxState

    data object Installed : LinuxState
    data object Removing : LinuxState
}

private class Step(
    val id: String,
    val stage: Stage,
    @StringRes val label: Int,
    val network: Boolean,
    val optional: Boolean = false,
    val isDone: (() -> Boolean)? = null,
    val run: suspend () -> Unit,
)

/**
 * Sets up the Void Linux rootfs and keeps it current. Ported 1:1 from
 * Bellatrix's Bootstrap for the parts a terminal needs; the coding-agent
 * steps are intentionally not part of this port.
 */
class Bootstrap(
    private val context: Context,
    private val proot: Proot,
    private val packages: Packages,
) {
    private val _state = MutableStateFlow<LinuxState>(LinuxState.Checking)
    val state = _state.asStateFlow()

    val isInstalled get() = _state.value == LinuxState.Installed

    /** An install was started (and may still need to finish), or one finished. */
    val wanted get() = journal.setupComplete || choiceFile.exists()

    private val _updates = MutableStateFlow<JobState>(JobState.Idle)
    val updates = _updates.asStateFlow()

    private val log = OutputTail(200)
    val distro: Distro get() = Distro.active

    init {
        Distro.chosen = distro
    }

    private val journal = StepJournal(File(proot.filesDir, distro.state("rootfs-steps.json")))

    private val choiceFile get() = File(proot.filesDir, distro.state("setup-agents"))

    /** Kept for parity: the file's existence marks a started install. */
    fun choose(picked: List<String> = emptyList()) {
        proot.filesDir.mkdirs()
        choiceFile.writeText(picked.joinToString("") { it + "\n" })
    }

    /** The coding agents picked for setup, in the order they were listed. */
    val chosenAgents: List<Agent>
        get() = runCatching { choiceFile.readLines() }.getOrDefault(emptyList()).mapNotNull { bin -> AGENTS.find { it.bin == bin } }

    private var stage = Stage.Download
    private var step = ""

    private val steps get() =
        listOf(
            Step("rootfs", Stage.Download, R.string.linux_step_download, network = true) { installRootfs() },
            Step("etc-2", Stage.Configure, R.string.linux_step_configure, network = false) { configure() },
            // foot aborts if either include is missing. Omarchy's theme isn't part of
            // this port, so seed both targets. The new id also migrates installs whose
            // foot.ini still points at the old state path.
            Step(
                "foot-includes-2",
                Stage.Configure,
                R.string.linux_step_foot,
                network = false,
                isDone = {
                    File(proot.rootfs, "root/.config/foot/foot.ini").readTextOrNull() == FOOT_INI &&
                        File(proot.home, ".local/state/omarchy/current/theme/foot.ini").isFile &&
                        File(proot.home, TerminalFont.PATH.removePrefix("~/")).isFile
                },
            ) { ensureFootIncludes() },
            Step("forks-1", Stage.Configure, R.string.linux_step_forks, network = false, isDone = { journal.setupComplete }) { checkForks() },
            Step("packages-${distro.id}-1", Stage.Packages, R.string.linux_step_packages, network = true) { guest(distro.installScript()) },
            Step("keymap-1", Stage.Packages, R.string.linux_step_keymap, network = false, isDone = { proot.keymap.exists() }) { compileKeymap() },
            Step("fonts-1", Stage.Packages, R.string.linux_step_fonts, network = true, optional = true) {
                guest("timeout 400 sh -c ${shellQuote(distro.fontsScript())}")
            },
            Step(
                "cliamp-$CLIAMP_VERSION",
                Stage.Packages,
                R.string.linux_step_cliamp,
                network = true,
                optional = true,
                // A failed optional download isn't marked done, but a binary deleted later
                // must be fetched again as well: the drawer only lists Cliamp if it runs.
                isDone = { File(proot.rootfs, "usr/local/bin/cliamp").isFile },
            ) { installCliamp() },
        )

    private fun Step.done() = isDone?.invoke() ?: (id in journal)

    private suspend fun Step.attempt() {
        repeat(if (network) NETWORK_TRIES - 1 else 0) { n ->
            try {
                return run()
            } catch (e: Exception) {
                if (e is NoRetry || e is CancellationException) throw e
                log.add(text(R.string.linux_log_retrying, "${e.message}", n + 2, NETWORK_TRIES))
                delay(5_000L * (n + 1))
            }
        }
        run()
    }

    suspend fun run() =
        withContext(Dispatchers.IO) {
            try {
                when {
                    journal.setupComplete -> update()
                    !choiceFile.exists() -> {
                        _state.value = LinuxState.NotInstalled
                        if (proot.rootfs.exists()) runCatching { deleteTree(proot.rootfs) }.onFailure { Log.e(TAG, "leftover rootfs", it) }
                        return@withContext
                    }
                    else -> setup()
                }
                _state.value = LinuxState.Installed
            } catch (e: Throwable) {
                if (cancelled) return@withContext
                Log.e(TAG, "setup failed", e)
                val message = e.message ?: e.toString()
                log.add(message)
                _state.value = LinuxState.Failed(stage, message, log.toList())
            }
        }

    @Volatile
    private var cancelled = false

    @Volatile
    private var connection: HttpURLConnection? = null

    val cancellable get() = !journal.setupComplete && !cancelled

    fun cancel(): Boolean {
        if (!cancellable) return false
        cancelled = true
        connection?.disconnect()
        return true
    }

    private fun checkCancelled() {
        if (cancelled) throw CancellationException("cancelled")
    }

    suspend fun install(picked: List<String> = emptyList()) {
        cancelled = false
        choose(picked)
        run()
    }

    /** Publishes a setup failure that happened outside [run], e.g. while preparing proot. */
    fun fail(e: Throwable) {
        if (cancelled) return
        Log.e(TAG, "setup failed before a step", e)
        val message = e.message ?: e.toString()
        log.add(message)
        _state.value = LinuxState.Failed(stage, message, log.toList())
    }

    suspend fun remove() =
        withContext(Dispatchers.IO) {
            _state.value = LinuxState.Removing
            backlog = emptyList()
            packages.lock.withLock {
                try {
                    deleteTree(proot.rootfs)
                } catch (e: Exception) {
                    Log.e(TAG, "rootfs not fully deleted", e)
                }
                journal.reset()
                choiceFile.delete()
                Distro.afterRemoval(proot.filesDir, distro)
                proot.keymap.delete()
                context.cacheDir.listFiles { f -> Distro.tarballPrefixes.any { f.name.startsWith(it) } }?.forEach { it.delete() }
                log.clear()
                cancelled = false
            }
            _state.value = LinuxState.NotInstalled
        }

    private suspend fun setup() {
        for (step in steps) {
            if (step.done()) continue
            checkCancelled()
            report(step.stage, text(step.label))
            try {
                step.attempt()
                journal.mark(step.id)
            } catch (e: Exception) {
                if (!step.optional || e is CancellationException) throw e
                log.add(text(R.string.linux_log_step_failed, text(step.label), "${e.message}"))
            }
        }
        checkCancelled()
        journal.completeSetup()
    }

    private suspend fun update() {
        val pending = steps.filterNot { it.done() }
        val blocking = pending.takeWhile { !it.network }
        for (step in blocking) {
            report(step.stage, text(step.label))
            step.run()
            journal.mark(step.id)
        }
        backlog = pending.drop(blocking.size)
    }

    private var backlog: List<Step> = emptyList()
    private val backlogRunning = AtomicBoolean(false)

    /**
     * Runs queued optional steps. A caller that taps Cliamp while the first install is
     * still running waits for it instead of briefly showing "not installed".
     */
    suspend fun runBacklog() {
        while (!backlogRunning.compareAndSet(false, true)) delay(100)
        try {
            runSteps()
        } finally {
            backlogRunning.set(false)
        }
    }

    private suspend fun runSteps() =
        withContext(Dispatchers.IO) {
            val todo = backlog.filterNot { it.done() }
            if (todo.isEmpty()) return@withContext
            for (step in todo) {
                _updates.value = JobState.Working(step.label, emptyList())
                try {
                    packages.lock.withLock { step.attempt() }
                    journal.mark(step.id)
                } catch (e: Throwable) {
                    Log.w(TAG, "update step ${step.id} failed", e)
                    _updates.value = JobState.Failed(log.toList().takeLast(12) + (e.message ?: e.toString()))
                    return@withContext
                }
            }
            _updates.value = JobState.Idle
        }

    private fun report(
        stage: Stage,
        step: String,
        progress: Float? = null,
    ) {
        this.stage = stage
        this.step = step
        if (journal.setupComplete && _state.value == LinuxState.Installed) return
        // Keep a live, terminal-like history of every step so the process log
        // updates while download/unpack are running, not only on failure.
        if (log.lastOrNull() != step) log.add(step)
        _state.value = LinuxState.Installing(stage, step, progress, log.toList())
    }

    private fun installRootfs() {
        check(!journal.setupComplete) { "refusing to replace a set-up system" }
        deleteTree(proot.rootfs)
        val source = distro.resolveRootfs()
        val tarball =
            try {
                obtainTarball(source)
            } catch (e: Exception) {
                throw if (e.outOfSpace()) outOfSpace(e) else e
            }
        try {
            extract(tarball, source.xz)
        } catch (e: Exception) {
            if (e is NoRetry || e is CancellationException || e.outOfSpace()) {
                runCatching { deleteTree(proot.rootfs) }
                throw if (e is NoRetry || e is CancellationException) e else outOfSpace(e)
            }
            if (tarball.parentFile == context.cacheDir) tarball.delete()
            throw e
        }
        tarball.delete()
    }

    private fun requireSpace(bytes: Long) {
        val free = proot.filesDir.usableSpace
        if (free < bytes) throw NoRetry(text(R.string.linux_error_no_space, gigabytes(bytes), gigabytes(free)))
    }

    private fun outOfSpace(e: Exception) = NoRetry(text(R.string.linux_error_out_of_space, gigabytes(TARBALL_SPACE + SYSTEM_SPACE)), e)

    private fun openHttp(
        url: String,
        connectTimeout: Int = 15_000,
        readTimeout: Int = 30_000,
    ): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = connectTimeout
        conn.readTimeout = readTimeout
        conn.instanceFollowRedirects = true
        return conn
    }

    private fun obtainTarball(source: RootfsSource): File {
        val external = context.getExternalFilesDir(null)?.let { File(it, source.file) }
        if (external != null && external.exists()) return external
        val target = File(context.cacheDir, source.file)
        if (target.exists()) return target
        val partial = File(context.cacheDir, "${source.file}.part")
        val validator = File(context.cacheDir, "${source.file}.version")
        requireSpace(TARBALL_SPACE - (if (partial.exists()) partial.length() else 0L) + SYSTEM_SPACE)
        report(Stage.Download, text(R.string.linux_step_download), 0f)
        val conn = openHttp(source.url)
        connection = conn
        try {
            checkCancelled()
            download(conn, partial, validator)
        } finally {
            connection = null
            conn.disconnect()
        }
        checkCancelled()
        check(partial.renameTo(target)) { text(R.string.linux_error_move_download) }
        validator.delete()
        source.sha256?.let { verify(target, it) }
        return target
    }

    private fun verify(
        file: File,
        expected: String,
    ) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        if (digest.digest().hex() != expected.lowercase()) {
            file.delete()
            error("the download doesn't match its checksum; retry")
        }
    }

    private fun download(
        conn: HttpURLConnection,
        partial: File,
        validator: File,
    ) {
        val have = if (partial.exists() && validator.exists()) partial.length() else 0L
        if (have > 0) {
            conn.setRequestProperty("Range", "bytes=$have-")
            conn.setRequestProperty("If-Range", validator.readText())
        }
        val resumed = conn.responseCode == HttpURLConnection.HTTP_PARTIAL
        if (conn.responseCode !in 200..299) error(text(R.string.linux_error_http, conn.responseCode))
        val start = if (resumed) have else 0L
        val total = conn.contentLengthLong.let { if (it > 0) it + start else -1L }
        (conn.getHeaderField("ETag") ?: conn.getHeaderField("Last-Modified"))?.let(validator::writeText) ?: validator.delete()
        conn.inputStream.use { input ->
            FileOutputStream(partial, resumed).use { out ->
                val buf = ByteArray(1 shl 16)
                var done = start
                var lastReport = 0L
                while (true) {
                    checkCancelled()
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    done += n
                    if (done - lastReport > 4 shl 20) {
                        lastReport = done
                        report(
                            Stage.Download,
                            if (total > 0) text(R.string.linux_step_downloaded, done shr 20, total shr 20) else text(R.string.linux_step_downloaded_unknown, done shr 20),
                            if (total > 0) done.toFloat() / total else null,
                        )
                    }
                }
            }
        }
        check(total <= 0 || partial.length() == total) { text(R.string.linux_error_cut_short) }
    }

    private fun extract(
        tarball: File,
        xz: Boolean,
    ) {
        report(Stage.Extract, text(R.string.linux_step_extract_start), 0f)
        val root = proot.rootfs
        deleteTree(root)
        requireSpace(SYSTEM_SPACE)
        root.mkdirs()
        journal.reset()
        val total = tarball.length()
        val counter = CountingInputStream(tarball.inputStream())
        val raw = BufferedInputStream(counter, 1 shl 16)
        val decompressed =
            try {
                if (xz) XZCompressorInputStream(raw) else GZIPInputStream(raw, 1 shl 16)
            } catch (e: Throwable) {
                throw NoRetry("can't open the Linux archive (${e.message ?: e.javaClass.simpleName})", e)
            }
        TarArchiveInputStream(decompressed).use { tar ->
            var count = 0
            var lastReport = counter.count
            while (true) {
                checkCancelled()
                val entry = try {
                    tar.nextEntry ?: break
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    throw NoRetry("tar archive error: ${e.message ?: e.javaClass.simpleName}", e)
                }
                val name = entry.name.removePrefix("./").trimEnd('/')
                if (name.isEmpty() || skipEntry(name)) continue
                val file = File(root, name)
                runCatching {
                    file.parentFile?.mkdirs()
                    when {
                        entry.isDirectory -> file.mkdirs()
                        entry.isSymbolicLink -> {
                            file.delete()
                            runCatching { Os.symlink(entry.linkName, file.path) }.onFailure {
                                log.add("$name: symlink skipped (${it.message ?: it.javaClass.simpleName})")
                            }
                        }
                        entry.isLink -> {
                            val target = File(root, entry.linkName.removePrefix("./"))
                            if (target.exists()) target.copyTo(file, overwrite = true)
                            else file.createNewFile()
                        }
                        entry.isFile -> file.outputStream().use { tar.copyTo(it) }
                        else -> Unit
                    }
                    if (!entry.isSymbolicLink) runCatching { Os.chmod(file.path, entry.mode and 0x1ff or 0x180) }
                }.onFailure { e ->
                    log.add("$name: ${e.message ?: e.javaClass.simpleName}")
                }
                count++
                // Report by compressed bytes too: a single large file can take a long
                // time and must not look like the unpack froze at 0%.
                if (count % 100 == 0 || counter.count - lastReport >= (4L shl 20)) {
                    lastReport = counter.count
                    report(
                        Stage.Extract,
                        context.resources.getQuantityString(R.plurals.linux_step_extracted, count, count),
                        counter.count.toFloat() / total,
                    )
                }
            }
        }
        report(Stage.Extract, text(R.string.linux_step_extract_start), 1f)
    }

    private fun skipEntry(name: String) =
        name == "boot" ||
            name.startsWith("boot/") ||
            name.startsWith("usr/lib/firmware/") ||
            name.startsWith("usr/lib/modules/") ||
            name.startsWith("dev/")

    private fun configure() {
        val root = proot.rootfs
        File(root, "etc/resolv.conf").apply {
            delete()
            writeText(LinuxDns.resolvConf(emptyList(), null))
        }
        File(root, "etc/hosts").writeText("127.0.0.1 localhost\n::1 localhost\n")
        File(root, "etc/locale.conf").writeText("LANG=C.UTF-8\n")
        // foot.ini: Bellatrix's phone-sized defaults, with our font include.
        File(root, "root/.config/foot/foot.ini").apply {
            parentFile?.mkdirs()
            writeText(FOOT_INI)
        }
        ensureFootIncludes()
    }

    /** Write FOOT_INI and create both files it includes; foot exits when an include is absent. */
    private fun ensureFootIncludes() {
        File(proot.rootfs, "root/.config/foot/foot.ini").apply {
            parentFile?.mkdirs()
            writeText(FOOT_INI)
        }
        val theme = File(proot.home, ".local/state/omarchy/current/theme/foot.ini")
        val font = File(proot.home, TerminalFont.PATH.removePrefix("~/"))
        theme.parentFile?.mkdirs()
        if (!theme.isFile) theme.writeText("# Bellatrix: no Omarchy theme is installed.\n")
        font.parentFile?.mkdirs()
        if (!font.isFile) font.writeText("[main]\nfont=JetBrains Mono:size=8.0\n")
    }

    private fun File.readTextOrNull(): String? = takeIf { it.isFile }?.readText()

    private fun compileKeymap() {
        guest(
            "xkbcli compile-keymap --layout us > /tmp/keymap.xkb || " +
                "/usr/lib/xkbcommon/xkbcli-compile-keymap --layout us > /tmp/keymap.xkb || " +
                "/usr/libexec/xkbcommon/xkbcli-compile-keymap --layout us > /tmp/keymap.xkb",
        )
        writeAtomically(proot.keymap, proot.hostFile("/tmp/keymap.xkb").readBytes() + 0)
    }

    private fun installCliamp() {
        val target = File(proot.rootfs, "usr/local/bin/cliamp")
        val tmp = File(target.parentFile, ".cliamp.download")
        val digest = MessageDigest.getInstance("SHA-256")
        val conn = openHttp(CLIAMP_URL, 10_000, 20_000)
        try {
            conn.useCaches = false
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode} for $CLIAMP_URL")
            target.parentFile?.mkdirs()
            DigestInputStream(conn.inputStream, digest).use { input ->
                tmp.outputStream().use { out ->
                    input.copyTo(out, 1 shl 16)
                    out.fd.sync()
                }
            }
        } finally {
            conn.disconnect()
        }
        if (digest.digest().hex() != CLIAMP_SHA256) {
            tmp.delete()
            error(text(R.string.linux_error_cliamp_mismatch, CLIAMP_VERSION))
        }
        tmp.setExecutable(true, false)
        if (!tmp.renameTo(target)) {
            tmp.delete()
            error(text(R.string.linux_error_cant_replace, target))
        }
    }

    private fun checkForks() {
        val process = proot.process("echo forks | cat").start()
        val output = StringBuffer()
        val reader = thread(isDaemon = true) { runCatching { process.inputStream.bufferedReader().forEachLine { output.appendLine(it) } } }
        if (process.waitFor(FORK_CHECK_SECONDS, TimeUnit.SECONDS)) {
            reader.join(1_000)
            output.lines().forEach(log::add)
            check(process.exitValue() == 0 && "forks" in output) { text(R.string.linux_error_cant_run, process.exitValue()) }
            return
        }
        process.destroyForcibly()
        proot.killAll()
        log.add("a guest command that starts another one didn't finish in $FORK_CHECK_SECONDS s (kernel ${System.getProperty("os.version")})")
        error(text(R.string.linux_error_kernel))
    }

    private fun text(
        @StringRes id: Int,
        vararg args: Any,
    ) = context.getString(id, *args)

    private fun guest(script: String) {
        log.add("$ $script")
        val code = proot.run(script) { line -> if (log.add(line)) report(stage, step) }
        check(code == 0) { text(R.string.linux_error_command, script.take(60), code) }
    }

    companion object {
        val FOOT_INI =
            """
            [main]
            include=~/.local/state/omarchy/current/theme/foot.ini
            term=xterm-256color
            include=${TerminalFont.PATH}
            pad=8x8
            workers=0
            selection-target=both

            [scrollback]
            lines=10000
            multiplier=7.0

            [cursor]
            style=block
            blink=no

            [key-bindings]
            clipboard-copy=Control+Insert Control+Shift+c XF86Copy
            primary-paste=none
            clipboard-paste=Shift+Insert Control+Shift+v XF86Paste
            """.trimIndent() + "\n"
    }
}
