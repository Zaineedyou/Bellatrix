package com.bellatrixos.widgets

import com.bellatrixos.ui.home.NativeGrid
import com.bellatrixos.widgets.lang.Program
import com.bellatrixos.widgets.lang.WNode
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/** Regression for the SDK 33 relaunch crash: tiles must not see an unmeasured (0x0) desktop. */
class OwlTileGeometryTest {
    @Test
    fun anUnmeasuredGridDoesNotThrow() {
        val program =
            Program(
                name = "zero",
                meta = emptyMap(),
                level = 1,
                globals = emptyList(),
                root = listOf(WNode.Element("Box", emptyMap(), emptyMap(), emptyMap(), emptyList(), 0)),
            )
        val spec = WidgetSpec(File("zero.owl"), "zero", 1, 0, program, size = 2 to 2)
        val grid =
            NativeGrid(
                width = 0,
                height = 0,
                cols = 1,
                rows = 1,
                cellW = 0f,
                cellH = 0f,
                gapX = 0f,
                gapY = 0f,
                padLeft = 0,
                padTop = 0,
            )
        assertEquals(PixelRect(0, 0, 1, 1), OwlTileGeometry.initialRect(spec, null, grid, 1f))
    }
}
