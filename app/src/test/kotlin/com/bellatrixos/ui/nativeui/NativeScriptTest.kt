package com.bellatrixos.ui.nativeui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeScriptTest {
    @Test fun shippedScriptCompilesAndIsNeutral() {
        val source = NativeScript.defaults()
        assertEquals(emptyList<String>(), NativeScript.check(source))
        assertFalse(source.lines().any { it.trimStart().startsWith("nativeui:") })
    }

    @Test fun brokenScriptIsReportedWithoutThrowing() {
        val problems = NativeScript.check("nativeui:onClick(")
        assertEquals(1, problems.size)
        assertTrue(problems.single().contains("ui.lua"))
    }

    @Test fun blankScriptIsAllowed() {
        assertEquals(emptyList<String>(), NativeScript.check(""))
    }

    @Test fun selectorsAreSharedWithLookToml() {
        assertNotNull(NativeUiSpec.selector("id:taskBar"))
        assertNotNull(NativeUiSpec.selector("class:UISettingsCard"))
        assertNotNull(NativeUiSpec.selector("text:Wi*"))
        assertNotNull(NativeUiSpec.selector("all"))
        assertEquals(null, NativeUiSpec.selector("nope"))
    }
}
