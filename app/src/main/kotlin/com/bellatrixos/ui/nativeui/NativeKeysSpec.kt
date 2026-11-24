package com.bellatrixos.ui.nativeui

import com.bellatrixos.theme.Toml

/**
 * The guest-editable global keybindings, read from ~/.config/bellatrix/keys.toml.
 *
 * These are Bellatrix keys on the home screen and in its windows, not an
 * Android accessibility service: a key outside Bellatrix stays with that app.
 * Kept free of Android types so the JVM tests can read it.
 */
object NativeKeysSpec {
    const val FILE = "keys.toml"
    const val VERSION = 1

    data class Binding(
        val combo: String,
        val action: String,
    )

    data class Parsed(
        val bindings: List<Binding> = emptyList(),
        val problems: List<String> = emptyList(),
    )

    /** Actions the launcher can run from a key. Each maps to an existing Runtime call. */
    val ACTIONS =
        listOf(
            "terminal",
            "files",
            "settings",
            "linux-settings",
            "coding-agents",
            "voice",
            "close",
        )

    private val MODIFIERS = setOf("super", "meta", "ctrl", "control", "alt", "shift")
    private val NAMED_KEYS =
        setOf("esc", "escape", "enter", "tab", "space", "up", "down", "left", "right") +
            (1..12).map { "f$it" }

    fun defaults(): String =
        """
        # Bellatrix global keys.
        #
        # A chord belongs to the launcher on home and in Bellatrix windows;
        # a bare key is left alone so typing never triggers an action.
        # Guest path: ~/.config/bellatrix/native-ui/keys.toml
        version = 1

        # [[keybind]]
        # combo = "Super+T"
        # action = "terminal"

        # Actions: ${ACTIONS.joinToString(", ")}
        """.trimIndent() + "\n"

    fun parse(text: String): Parsed {
        if (text.isBlank()) return Parsed()
        val table =
            try {
                Toml.parse(text)
            } catch (e: Toml.Error) {
                return Parsed(problems = listOf("keys.toml ${e.message}; no key was bound"))
            }
        val problems = mutableListOf<String>()
        val version = (table["version"] as? Number)?.toInt() ?: VERSION
        if (version != VERSION) problems += "version $version is not supported; keys were read as version $VERSION"

        val raw: List<*> =
            when (val value = table["keybind"]) {
                null -> emptyList<Any?>()
                is List<*> -> value
                else -> {
                    problems += "keybind must be a list of [[keybind]] tables"
                    emptyList<Any?>()
                }
            }

        val bindings = mutableListOf<Binding>()
        raw.forEachIndexed { index, item ->
            if (item !is Map<*, *>) {
                problems += "keybind $index is not a table"
                return@forEachIndexed
            }
            val combo = item["combo"] as? String
            val action = item["action"] as? String
            if (combo.isNullOrBlank() || !validCombo(combo)) {
                problems += "keybind $index needs a chord such as Super+Shift+T"
                return@forEachIndexed
            }
            if (action.isNullOrBlank() || action !in ACTIONS) {
                problems += "keybind $index needs one of these actions: ${ACTIONS.joinToString(", ")}"
                return@forEachIndexed
            }
            val named = normalize(combo)
            if (bindings.any { it.combo.equals(named, ignoreCase = true) }) {
                problems += "keybind $index: $combo is already bound"
                return@forEachIndexed
            }
            bindings += Binding(named, action)
        }
        return Parsed(bindings, problems)
    }

    /** The spelling a key event produces, so "ctrl+alt+t" and "Ctrl+Alt+T" bind the same chord. */
    fun normalize(text: String): String =
        text.split('+').map { it.trim() }.filter { it.isNotEmpty() }.joinToString("+") { part ->
            when (part.lowercase()) {
                "super", "meta" -> "Super"
                "ctrl", "control" -> "Ctrl"
                "alt" -> "Alt"
                "shift" -> "Shift"
                "esc", "escape" -> "Esc"
                else -> if (part.length == 1) part.uppercase() else part
            }
        }

    /** One modifier or more, then a letter, digit or named key. */
    fun validCombo(text: String): Boolean {
        val parts = text.split('+').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size !in 2..5) return false
        if (parts.dropLast(1).any { it.lowercase() !in MODIFIERS }) return false
        val key = parts.last().lowercase()
        return key.length == 1 && key[0].isLetterOrDigit() || key in NAMED_KEYS
    }
}
