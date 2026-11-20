package com.bellatrixos.linux

import androidx.annotation.StringRes
import com.bellatrixos.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Installs [packages] with a full sync, never a partial upgrade. */
fun pacmanInstall(
    packages: Collection<String>,
    refresh: Boolean = false,
) = "pacman -Sy${if (refresh) "y" else ""}u --noconfirm --needed ${packages.joinToString(" ") { shellQuote(it) }} && $CLEAN_PACMAN_CACHE"

/** `pacman -Scc --noconfirm` answers its prompt with No, so delete directly. */
const val CLEAN_PACMAN_CACHE = "rm -rf /var/cache/pacman/pkg/*"

/** Progress of a guest job started from the UI. */
sealed interface JobState {
    data object Idle : JobState

    data class Working(
        @StringRes val action: Int,
        val log: List<String>,
    ) : JobState

    data class Failed(
        val log: List<String>,
    ) : JobState
}

/**
 * The last [max] lines of a command's output, with progress bars kept at their latest state.
 */
class OutputTail(
    private val max: Int = 200,
) {
    private val lines = ArrayDeque<String>()
    private var dropped = 0
    private var frame: IntRange? = null
    private var inFrame = false
    private var redrawing = false

    /** Adds [raw]; false if it was blank. */
    fun add(raw: String): Boolean {
        val redraw = raw.endsWith('\r')
        val drawn = raw.trimEnd('\r').substringAfterLast('\r')
        val text = drawn.trim()
        if (text.isEmpty()) {
            if (!redraw) redrawing = false
            return false
        }
        if (redrawing) {
            lines.removeLastOrNull()
            frame = frame?.let { it.first..minOf(it.last, end - 1) }?.takeUnless { it.isEmpty() }
        }
        redrawing = redraw
        when {
            PROGRESS_BAR.containsMatchIn(text) -> {
                frame?.let { removeRange(it) }
                frame = end..end
                inFrame = true
            }
            inFrame && drawn.first().isWhitespace() -> frame = frame?.let { it.first..end }
            else -> inFrame = false
        }
        lines.addLast(text)
        while (lines.size > max) {
            lines.removeFirst()
            dropped++
        }
        return true
    }

    private val end get() = dropped + lines.size

    private fun removeRange(range: IntRange) {
        val from = (range.first - dropped).coerceAtLeast(0)
        val to = (range.last - dropped).coerceAtMost(lines.size - 1)
        for (i in to downTo from) lines.removeAt(i)
    }

    fun toList() = lines.toList()

    fun lastOrNull() = lines.lastOrNull()

    fun clear() {
        lines.clear()
        dropped = 0
        frame = null
        inFrame = false
        redrawing = false
    }

    override fun toString() = lines.joinToString("\n")

    private companion object {
        val PROGRESS_BAR = Regex("[░▒▓█▏▎▍▌▋▊▉━]{4,}")
    }
}

/** Runs [script] in the guest to completion, blocking, feeding each output line to [onLine]. */
fun Proot.run(
    script: String,
    onLine: (String) -> Unit = {},
): Int {
    val process = process(script).start()
    process.inputStream.bufferedReader().use { forEachOutputLine(it, onLine) }
    return process.waitFor()
}

/** Splits [reader] at newlines, as they come; a carriage return ends a line too but stays on it. */
internal fun forEachOutputLine(
    reader: java.io.Reader,
    onLine: (String) -> Unit,
) {
    val line = StringBuilder()
    while (true) {
        val c = reader.read()
        if (c < 0) break
        when (c.toChar()) {
            '\n' -> onLine(line.toString())
            '\r' -> onLine(line.append('\r').toString())
            else -> {
                line.append(c.toChar())
                continue
            }
        }
        line.clear()
    }
    if (line.isNotEmpty()) onLine(line.toString())
}

/** Like [run], reporting the tail of the output to [report] as it comes. */
fun Proot.runJob(
    script: String,
    tail: OutputTail = OutputTail(),
    report: (List<String>) -> Unit = {},
): Pair<Boolean, OutputTail> {
    val ok =
        runCatching { run(script) { if (tail.add(it)) report(tail.toList()) } == 0 }
            .getOrElse {
                tail.add(it.message ?: it.toString())
                false
            }
    return ok to tail
}

/** Runs [script] as a job the UI shows, publishing progress to [publish]. */
suspend fun Proot.trackedJob(
    @StringRes action: Int,
    script: String,
    lock: Mutex? = null,
    publish: (JobState) -> Unit,
): Pair<Boolean, OutputTail> {
    if (lock != null) publish(JobState.Working(R.string.linux_job_waiting, emptyList()))
    val job: suspend () -> Pair<Boolean, OutputTail> = {
        withContext(Dispatchers.IO) {
            publish(JobState.Working(action, emptyList()))
            runJob(script) { publish(JobState.Working(action, it)) }.also { (ok, tail) ->
                publish(if (ok) JobState.Idle else JobState.Failed(tail.toList()))
            }
        }
    }
    return if (lock != null) lock.withLock { job() } else job()
}
