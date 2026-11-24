package com.bellatrixos.ui.nativeui

import android.view.KeyEvent

/**
 * The one process-wide key dispatcher. Each Bellatrix activity offers a key to
 * this object first; a binding that matches is consumed before the view, the
 * terminal or Android sees it. A bare key is never a binding, so typing is safe.
 */
object GlobalKeys {
    @Volatile
    var bindings: List<NativeKeysSpec.Binding> = emptyList()

    /** Set by the one Runtime; returns false when the action wasn't available. */
    @Volatile
    var handler: (String) -> Boolean = { false }

    fun dispatch(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return false
        val combo = comboOf(event) ?: return false
        val action = bindings.firstOrNull { it.combo.equals(combo, ignoreCase = true) }?.action ?: return false
        return handler(action)
    }

    /** The chord an Android key event stands for, or null when it isn't one. */
    fun comboOf(event: KeyEvent): String? {
        val key = nameOf(event.keyCode) ?: return null
        val modifiers = ArrayList<String>(4)
        if (event.isMetaPressed) modifiers += "Super"
        if (event.isCtrlPressed) modifiers += "Ctrl"
        if (event.isAltPressed) modifiers += "Alt"
        if (event.isShiftPressed) modifiers += "Shift"
        if (modifiers.isEmpty()) return null
        modifiers += key
        return modifiers.joinToString("+")
    }

    private fun nameOf(code: Int): String? =
        when (code) {
            in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> ('A'.code + code - KeyEvent.KEYCODE_A).toChar().toString()
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> (code - KeyEvent.KEYCODE_0).toString()
            in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 -> "F" + (code - KeyEvent.KEYCODE_F1 + 1)
            KeyEvent.KEYCODE_ESCAPE -> "Esc"
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> "Enter"
            KeyEvent.KEYCODE_TAB -> "Tab"
            KeyEvent.KEYCODE_SPACE -> "Space"
            KeyEvent.KEYCODE_DPAD_UP -> "Up"
            KeyEvent.KEYCODE_DPAD_DOWN -> "Down"
            KeyEvent.KEYCODE_DPAD_LEFT -> "Left"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "Right"
            else -> null
        }
}
