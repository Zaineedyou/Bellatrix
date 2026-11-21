package com.bellatrixos.taskbar

import android.content.Context
import com.bellatrixos.storage.LauncherData
import com.bellatrixos.theme.Toml
import com.bellatrixos.upgrade.SyncedFile
import com.bellatrixos.widgets.lang.sha256Hex
import java.io.File

/**
 * The taskbar's pins as layout.toml, which Linux sees as
 * ~/.config/bellatrix/taskbar/layout.toml. Pins made on the phone write it,
 * and an agent or the user editing it reorders, adds and removes taskbar apps
 * within a second. Mirrors HomeStore's bidirectional file model. Main thread.
 */
class TaskBarStore(
    context: Context,
) {
    companion object {
        const val FILE = "layout.toml"
        const val ERRORS = "errors.txt"
        const val GUEST_DIR = ".config/bellatrix/taskbar"
        private const val WRITTEN = "taskbar_layout_written"
        private const val HEADER =
            "# Bellatrix taskbar layout. Edit pins to change the taskbar; errors.txt says what was ignored.\n" +
                "# Names are taskbar keys: UIExplorer, UISettings, UITerminal, linux:<desktop id>, or an Android package.\n"
    }

    private val dir = LauncherData.taskbarDir(context)
    private val prefs = LauncherData.prefs(context)
    private var pins: List<TaskBarItemPins> = emptyList()
    private var ready = false
    private var applying = false
    private var onApply: ((List<TaskBarItemPins>) -> Unit)? = null

    /** What errors.txt says, so a terminal or coding agent can read it. */
    val errors get() = synced.errors

    private val synced =
        SyncedFile(File(dir, FILE), File(dir, ERRORS), "the taskbar layout") {
            Toml.write(
                mapOf("version" to 1, "pins" to pins.map { it.a() }),
                HEADER,
            )
        }

    init {
        synced.onWritten = { text -> prefs.edit().putString(WRITTEN, hash(text)).apply() }
        TaskBarDB.onPinsChanged = { changed(it) }
    }

    /** Takes the pins the launcher had, or the file's when someone edited it; called once as the taskbar loads. */
    fun loaded(
        current: List<TaskBarItemPins>,
        apply: (List<TaskBarItemPins>) -> Unit,
    ) {
        if (ready) {
            apply(current)
            return
        }
        onApply = apply
        pins = current
        val text = synced.open()
        ready = true
        when {
            text == null -> {
                synced.create()
                apply(current)
            }
            hash(text) == prefs.getString(WRITTEN, null) -> {
                synced.setProblems(emptyList())
                pins = current
                synced.save()
                apply(current)
            }
            else -> {
                val parsed = parse(text)
                if (parsed != null) {
                    adopt(parsed, apply)
                } else {
                    synced.setProblems(listOf("Couldn't read pins from layout.toml; the taskbar was left as it was."))
                    apply(current)
                }
            }
        }
        synced.start { incoming ->
            val parsed = parse(incoming)
            if (parsed != null) {
                adopt(parsed) { onApply?.invoke(it) }
            } else {
                synced.setProblems(listOf("Couldn't read pins from layout.toml; the taskbar was left as it was."))
            }
        }
    }

    /** A pin was added, removed or reordered from the launcher's own UI. */
    fun changed(list: List<TaskBarItemPins>) {
        if (!ready || applying) return
        pins = list
        synced.save()
    }

    private fun adopt(
        list: List<TaskBarItemPins>,
        apply: (List<TaskBarItemPins>) -> Unit,
    ) {
        pins = list
        applying = true
        TaskBarDB.e(list)
        applying = false
        synced.setProblems(emptyList())
        apply(list)
    }

    /** Blank means no pins; a file that doesn't parse returns null and changes nothing. */
    private fun parse(text: String): List<TaskBarItemPins>? {
        if (text.isBlank()) return emptyList()
        val table = runCatching { Toml.parse(text) }.getOrNull() ?: return null
        val raw = table["pins"] as? List<*> ?: return null
        val names = raw.mapNotNull { it as? String }.filter { it.isNotBlank() }
        return names.distinct().mapIndexed { i, name -> TaskBarItemPins(name, i) }
    }

    private fun hash(text: String) = sha256Hex(text.toByteArray())
}
