package com.bellatrixos.wayland

/** Keys by name and characters on a US keyboard as Linux evdev codes. Ported from Omalauncher's OWL KeyNames. */
object KeyNames {
    data class Combo(
        val code: Int,
        val held: List<Int>,
    )

    private const val SHIFT = 42

    val modifiers = linkedMapOf("ctrl" to 29, "alt" to 56, "shift" to SHIFT, "super" to 125)
    val latches = listOf("ctrl", "alt", "shift")
    val hows = listOf("once", "locked", "off")

    private val named =
        buildMap {
            put("esc", 1)
            put("backspace", 14)
            put("tab", 15)
            put("enter", 28)
            put("space", 57)
            put("home", 102)
            put("up", 103)
            put("pgup", 104)
            put("left", 105)
            put("right", 106)
            put("end", 107)
            put("down", 108)
            put("pgdn", 109)
            put("insert", 110)
            put("delete", 111)
            for (f in 1..10) put("f$f", 58 + f)
            put("f11", 87)
            put("f12", 88)
        }

    private const val UNSHIFTED = "1234567890-=qwertyuiop[]asdfghjkl;'`\\zxcvbnm,./ "
    private const val SHIFTED = "!@#$%^&*()_+QWERTYUIOP{}ASDFGHJKL:\"~|ZXCVBNM<>? "
    private val CODES =
        intArrayOf(
            2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
            16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,
            30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 43,
            44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 57,
        )

    fun forChar(c: Char): Pair<Int, Boolean>? {
        if (c == '\n') return 28 to false
        if (c == '\t') return 15 to false
        UNSHIFTED.indexOf(c).takeIf { it >= 0 }?.let { return CODES[it] to false }
        SHIFTED.indexOf(c).takeIf { it >= 0 }?.let { return CODES[it] to true }
        return null
    }

    fun combo(text: String): Combo? {
        if (text.isEmpty()) return null
        val parts =
            when {
                text.length == 1 -> listOf(text)
                text.endsWith("++") -> text.dropLast(2).split('+') + "+"
                else -> text.split('+')
            }
        val key = parts.last()
        val held = ArrayList<Int>()
        for (m in parts.dropLast(1)) {
            val code = modifiers[m.lowercase()] ?: return null
            if (code !in held) held += code
        }
        named[key.lowercase()]?.let { return Combo(it, held) }
        if (key.length != 1) return null
        val c = if (held.isNotEmpty() && key[0] in 'A'..'Z') key[0].lowercaseChar() else key[0]
        val (code, shift) = forChar(c) ?: return null
        if (shift && SHIFT !in held) held += SHIFT
        return Combo(code, held)
    }
}
