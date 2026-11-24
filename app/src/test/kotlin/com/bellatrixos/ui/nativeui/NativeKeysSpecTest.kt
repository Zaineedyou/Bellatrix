package com.bellatrixos.ui.nativeui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeKeysSpecTest {
    @Test fun shippedDefaultsBindNothing() {
        val parsed = NativeKeysSpec.parse(NativeKeysSpec.defaults())
        assertEquals(emptyList<String>(), parsed.problems)
        assertTrue(parsed.bindings.isEmpty())
    }

    @Test fun readsChordsAndNormalizesTheirSpelling() {
        val parsed =
            NativeKeysSpec.parse(
                """
                version = 1
                [[keybind]]
                combo = "ctrl+shift+t"
                action = "terminal"
                [[keybind]]
                combo = "Super+F"
                action = "files"
                """.trimIndent(),
            )
        assertEquals(emptyList<String>(), parsed.problems)
        assertEquals(listOf(NativeKeysSpec.Binding("Ctrl+Shift+T", "terminal"), NativeKeysSpec.Binding("Super+F", "files")), parsed.bindings)
    }

    @Test fun bareKeysAndUnknownActionsAreRejected() {
        val parsed =
            NativeKeysSpec.parse(
                """
                [[keybind]]
                combo = "T"
                action = "terminal"
                [[keybind]]
                combo = "Super+Q"
                action = "quit"
                [[keybind]]
                combo = "Super+T"
                action = "terminal"
                """.trimIndent(),
            )
        assertEquals(1, parsed.bindings.size)
        assertEquals("Super+T", parsed.bindings[0].combo)
        assertEquals(2, parsed.problems.size)
    }

    @Test fun validComboNeedsAModifierAndAKey() {
        assertFalse(NativeKeysSpec.validCombo("T"))
        assertTrue(NativeKeysSpec.validCombo("Super+Shift+T"))
        assertTrue(NativeKeysSpec.validCombo("Alt+F4"))
        assertTrue(NativeKeysSpec.validCombo("Ctrl+Esc"))
        assertFalse(NativeKeysSpec.validCombo("Super+Bogus"))
        assertFalse(NativeKeysSpec.validCombo("Super+Alt"))
    }
}
