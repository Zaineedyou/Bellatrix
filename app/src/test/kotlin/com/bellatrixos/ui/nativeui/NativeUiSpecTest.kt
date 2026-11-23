package com.bellatrixos.ui.nativeui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeUiSpecTest {
    @Test fun shippedDefaultsParseWithoutProblems() {
        val parsed = NativeUiSpec.parse(NativeUiSpec.defaults())
        assertEquals(emptyList<String>(), parsed.problems)
        assertEquals(4, parsed.spec.rules.size)
        assertEquals("#CC001811", parsed.spec.palette["surface"])
    }

    @Test fun readsRulesAsNativeViewSelectors() {
        val parsed =
            NativeUiSpec.parse(
                """
                version = 1
                [palette]
                panel = "#101418cc"
                [[rule]]
                surface = ["taskbar", "settings"]
                match = ["id:taskBarList", "class:UISettingsCard*"]
                background = "panel"
                radius = 12
                padding = [8, 6]
                [[rule]]
                surface = "all"
                match = "text:Wi*"
                text-color = "#fff"
                """.trimIndent(),
            )
        assertEquals(emptyList<String>(), parsed.problems)
        val spec = parsed.spec
        assertEquals(2, spec.rules.size)
        assertEquals(setOf("taskbar", "settings"), spec.rules[0].surfaces)
        assertTrue(spec.rules[0].matches("taskbar", "taskBarList", "LinearLayout", null))
        assertTrue(spec.rules[0].matches("settings", null, "UISettingsCardSwitch", null))
        assertFalse(spec.rules[0].matches("home", "taskBarList", "LinearLayout", null))
        assertTrue(spec.rules[1].matches("window", null, "TextView", "Wi-Fi"))
        assertEquals(0x101418ccL, NativeUiSpec.color("panel", spec.palette))
    }

    @Test fun reportsBadSelectorsColorsAndPropertiesWithoutLosingGoodRules() {
        val parsed =
            NativeUiSpec.parse(
                """
                [[rule]]
                match = ["id:ok", "nope"]
                background = "missing"
                radius = "soft"
                bogus = 1
                [[rule]]
                surface = "home"
                match = "all"
                background = "#fff"
                """.trimIndent(),
            )
        assertEquals(1, parsed.spec.rules.size)
        assertTrue(parsed.problems.any { "match \"nope\"" in it })
        assertTrue(parsed.problems.any { "background takes a palette name" in it })
        assertTrue(parsed.problems.any { "radius takes a size" in it })
        assertTrue(parsed.problems.any { "bogus is not a native property" in it })
    }

    @Test fun sizesUseUnitsAndShorthand() {
        assertEquals(NativeUiSpec.Dim(8f, NativeUiSpec.Dim.Kind.DP), NativeUiSpec.Dim.of(8.0))
        assertEquals(NativeUiSpec.Dim(14f, NativeUiSpec.Dim.Kind.SP), NativeUiSpec.Dim.of("14sp"))
        assertEquals(NativeUiSpec.Dim(50f, NativeUiSpec.Dim.Kind.PERCENT), NativeUiSpec.Dim.of("50%"))
        assertEquals(NativeUiSpec.Dim(0f, NativeUiSpec.Dim.Kind.MATCH), NativeUiSpec.Dim.of("match"))
        assertEquals(
            listOf(NativeUiSpec.Dim(4f), NativeUiSpec.Dim(8f)),
            NativeUiSpec.dims("4 8"),
        )
        assertEquals(
            listOf(NativeUiSpec.Dim(1f), NativeUiSpec.Dim(2f), NativeUiSpec.Dim(3f), NativeUiSpec.Dim(4f)),
            NativeUiSpec.dims("1 2 3 4"),
        )
    }

    @Test fun reportsUnreadableTomlWithoutApplyingAnything() {
        val parsed = NativeUiSpec.parse("[[rule]\nmatch = 1")
        assertTrue(parsed.spec.rules.isEmpty())
        assertTrue(parsed.problems.single().contains("line 1"))
    }
}
