package com.bellatrixos.ui.nativeui

import com.bellatrixos.theme.Toml

/**
 * The guest-editable description of Bellatrix's own views. It is not a theme or
 * a second UI: every rule is applied to a real native View already on screen.
 * Kept free of Android types so the JVM tests can read it.
 */
object NativeUiSpec {
    const val FILE = "look.toml"
    const val ERRORS = "errors.txt"
    const val README = "README.md"
    const val GUEST_DIR = ".config/bellatrix/native-ui"

    /** Bump when a rule or property changes meaning. Old files still read; they get a warning, not a rewrite. */
    const val VERSION = 1

    data class Selector(
        val kind: Kind,
        val value: String,
    ) {
        enum class Kind { ID, CLASS, TEXT, ALL }

        fun matches(
            surface: String,
            idName: String?,
            className: String,
            text: String?,
        ): Boolean =
            when (kind) {
                Kind.ALL -> true
                Kind.ID -> idName?.let { wildcard(value, it) } == true
                Kind.CLASS -> {
                    val simple = className.substringAfterLast('.')
                    wildcard(value, className) ||
                        wildcard(value, simple) ||
                        simple.startsWith(value, ignoreCase = true) ||
                        simple.endsWith(value, ignoreCase = true) ||
                        className.endsWith(value, ignoreCase = true)
                }
                Kind.TEXT -> text?.let { wildcard(value, it) } == true
            }

        private fun wildcard(
            pattern: String,
            text: String,
        ): Boolean {
            if (pattern == "*") return true
            val regex =
                buildString {
                    append('^')
                    for (c in pattern) {
                        when (c) {
                            '*' -> append(".*")
                            else -> append(Regex.escape(c.toString()))
                        }
                    }
                    append('$')
                }
            return Regex(regex, RegexOption.IGNORE_CASE).matches(text)
        }
    }

    data class Rule(
        val surfaces: Set<String>,
        val selectors: List<Selector>,
        val properties: Map<String, Any>,
    ) {
        fun appliesTo(surface: String): Boolean = "all" in surfaces || surface in surfaces || surface == "all" && surfaces.isNotEmpty()

        fun matches(
            surface: String,
            idName: String?,
            className: String,
            text: String?,
        ) = appliesTo(surface) && selectors.any { it.matches(surface, idName, className, text) }
    }

    data class Spec(
        val palette: Map<String, String> = emptyMap(),
        val rules: List<Rule> = emptyList(),
    ) {
        companion object {
            val EMPTY = Spec()
        }
    }

    data class Parsed(
        val spec: Spec,
        val problems: List<String> = emptyList(),
    )

    /** An extracted size. Bare numbers are dp; text size defaults to sp in the applier. */
    data class Dim(
        val value: Float,
        val kind: Kind = Kind.DP,
    ) {
        enum class Kind { DP, SP, PX, PERCENT, MATCH, WRAP }

        companion object {
            fun of(value: Any?): Dim? {
                if (value is Number) return Dim(value.toFloat())
                val text = (value as? String)?.trim()?.lowercase() ?: return null
                return when {
                    text == "match" || text == "match-parent" || text == "fill" -> Dim(0f, Kind.MATCH)
                    text == "wrap" || text == "wrap-content" || text == "auto" -> Dim(0f, Kind.WRAP)
                    text.endsWith('%') -> text.dropLast(1).toFloatOrNull()?.let { Dim(it, Kind.PERCENT) }
                    text.endsWith("dp") -> text.dropLast(2).trim().toFloatOrNull()?.let { Dim(it) }
                    text.endsWith("sp") -> text.dropLast(2).trim().toFloatOrNull()?.let { Dim(it, Kind.SP) }
                    text.endsWith("px") -> text.dropLast(2).trim().toFloatOrNull()?.let { Dim(it, Kind.PX) }
                    else -> text.toFloatOrNull()?.let { Dim(it) }
                }
            }
        }
    }

    private val META_KEYS = setOf("surface", "match", "description")

    private val COLOR_KEYS =
        setOf(
            "background",
            "border",
            "text-color",
            "hint-color",
            "icon-color",
            "icon-tint",
        )

    private val DIM_KEYS =
        setOf(
            "width",
            "height",
            "min-width",
            "min-height",
            "padding",
            "margin",
            "radius",
            "border-width",
            "text-size",
            "icon-size",
            "elevation",
            "translation-x",
            "translation-y",
        )

    private val OTHER_KEYS =
        setOf(
            "alpha",
            "rotation",
            "scale-x",
            "scale-y",
            "visibility",
            "text-style",
            "text-align",
            "clip",
            "clickable",
            "focusable",
            "layout-gravity",
            "weight",
        )

    val PROPERTY_KEYS = COLOR_KEYS + DIM_KEYS + OTHER_KEYS

    fun defaults(): String =
        """
        # Bellatrix native UI overrides.
        #
        # This shipped file is neutral: Bellatrix keeps its stock look until an
        # agent edits it. Every [[rule]] changes a real View already on screen,
        # not a replacement screen. README.md lists every surface, selector and
        # property; examples/ has rules for the taskbar and native Settings.
        # Guest path: ~/.config/bellatrix/native-ui/look.toml
        version = 1

        # [palette]
        # surface = "#CC101418"
        # border = "#AA00AAA4"
        # text = "#BDF5EF"

        # [[rule]]
        # surface = ["taskbar"]
        # match = ["id:taskBar"]
        # background = "surface"
        # border = "border"
        # border-width = 1
        # radius = 14
        # padding = [4, 6]

        # [[rule]]
        # surface = ["settings"]
        # match = ["class:UISettingsCard"]
        # background = "#202233"
        # radius = 8
        # padding = [6, 8]
        """.trimIndent() + "\n"

    fun parse(text: String): Parsed {
        if (text.isBlank()) return Parsed(Spec.EMPTY)
        val table =
            try {
                Toml.parse(text)
            } catch (e: Toml.Error) {
                return Parsed(Spec.EMPTY, listOf("look.toml ${e.message}; no native override was applied"))
            }
        val problems = mutableListOf<String>()
        val version =
            (table["version"] as? Number)?.toInt()
                ?: (table["version.level"] as? Number)?.toInt()
                ?: VERSION
        if (version != VERSION) problems += "version $version is not supported; rules were read as version $VERSION"

        val palette = linkedMapOf<String, String>()
        @Suppress("UNCHECKED_CAST")
        val paletteTable = table["palette"] as? Map<*, *>
        if (paletteTable != null) {
            for ((key, value) in paletteTable) {
                val name = key as? String ?: continue
                val color = value as? String
                if (color == null || hex(color) == null) {
                    problems += "palette.$name takes a #rgb, #rrggbb or #aarrggbb color"
                } else {
                    palette[name.lowercase()] = color
                }
            }
        } else if ("palette" in table) {
            problems += "palette must be a table"
        }

        val rawRules: List<*> =
            when (val value = table["rule"]) {
                null -> emptyList<Any?>()
                is List<*> -> value
                else -> {
                    problems += "rule must be a list of [[rule]] tables"
                    emptyList<Any?>()
                }
            }

        val rules = mutableListOf<Rule>()
        rawRules.forEachIndexed { index, item ->
            if (item !is Map<*, *>) {
                problems += "rule $index is not a table"
                return@forEachIndexed
            }
            val surfaces = strings(item["surface"], "rule $index surface", problems).ifEmpty { listOf("all") }.toSet()
            val selectors =
                strings(item["match"], "rule $index match", problems)
                    .ifEmpty { listOf("all") }
                    .mapNotNull { selector(it, index, problems) }
            val properties = linkedMapOf<String, Any>()
            for ((key, value) in item) {
                val name = key as? String ?: continue
                if (name in META_KEYS) continue
                if (name !in PROPERTY_KEYS) {
                    problems += "rule $index: $name is not a native property"
                    continue
                }
                if (name in COLOR_KEYS && color(value, palette) == null) {
                    problems += "rule $index: $name takes a palette name or a hex color"
                    continue
                }
                if (name in DIM_KEYS && name != "padding" && name != "margin" && Dim.of(value) == null) {
                    problems += "rule $index: $name takes a size such as 8, 8dp or 50%"
                    continue
                }
                if ((name == "padding" || name == "margin") && !dims(value).all { it != null }) {
                    problems += "rule $index: $name takes one, two or four sizes"
                    continue
                }
                properties[name] = value ?: continue
            }
            rules += Rule(surfaces, selectors, properties)
        }
        return Parsed(Spec(palette, rules), problems)
    }

    /** Parses one selector token from a script or a file; bad tokens are reported by [parse]. */
    fun selector(token: String): Selector? = selector(token, -1, mutableListOf())

    private fun selector(
        token: String,
        index: Int,
        problems: MutableList<String>,
    ): Selector? {
        val value = token.trim()
        return when {
            value == "all" || value == "*" -> Selector(Selector.Kind.ALL, "*")
            value.startsWith("id:", true) -> Selector(Selector.Kind.ID, value.substring(3).trim())
            value.startsWith("class:", true) -> Selector(Selector.Kind.CLASS, value.substring(6).trim())
            value.startsWith("text:", true) -> Selector(Selector.Kind.TEXT, value.substring(5).trim())
            else -> {
                problems += "rule $index: match \"$token\" must start with id:, class: or text:"
                null
            }
        }
    }

    private fun strings(
        value: Any?,
        what: String,
        problems: MutableList<String>,
    ): List<String> =
        when (value) {
            is String -> listOf(value)
            is List<*> -> value.mapNotNull { it as? String }
            null -> emptyList()
            else -> {
                problems += "$what takes text or an array of text"
                emptyList()
            }
        }

    /** One value for all sides, two for vertical/horizontal, four for left/top/right/bottom. */
    fun dims(value: Any?): List<Dim?> =
        when (value) {
            is String -> value.trim().split(Regex("\\s+")).map { Dim.of(it) }
            is Iterable<*> -> value.map { Dim.of(it) }
            is Array<*> -> value.map { Dim.of(it) }
            else -> listOf(Dim.of(value))
        }

    /** Resolves a palette name or hex string to an ARGB long. */
    fun color(
        value: Any?,
        palette: Map<String, String>,
    ): Long? {
        val raw = (value as? String)?.trim()?.lowercase() ?: return null
        return hex(palette[raw] ?: raw)
    }

    private fun hex(value: String): Long? {
        val raw = value.trim().lowercase()
        if (raw == "none" || raw == "transparent") return 0L
        val text = raw.trimStart('#').lowercase()
        if (text.any { it !in '0'..'9' && it !in 'a'..'f' }) return null
        return when (text.length) {
            3 -> {
                val r = "${text[0]}${text[0]}".toInt(16)
                val g = "${text[1]}${text[1]}".toInt(16)
                val b = "${text[2]}${text[2]}".toInt(16)
                0xff000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
            }
            6 -> "ff$text".toLong(16)
            8 -> text.toLong(16)
            else -> null
        }
    }
}
