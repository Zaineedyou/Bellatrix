package com.bellatrixos.widgets

import org.junit.Assert.assertEquals
import org.junit.Test

/** The data model behind home's workspaces: page numbers survive a screen change and a regrid. */
class HomePagesTest {
    @Test fun aPageNumberIsTheSameOnEveryScreen() {
        assertEquals(1, Screen.fromMain(1))
        assertEquals(3, Screen.fromMain(3))
        assertEquals(1, Screen.fromMain(0))
    }

    @Test fun seedingTheUnfoldedGridKeepsEachTilesPage() {
        val main: List<Triple<String, Int, Cell?>> =
            listOf(
                Triple("first.owl", 1, Cell(0, 0, 3, 3)),
                Triple("second.owl", 2, Cell(0, 0, 3, 3)),
                Triple("third.owl", 2, Cell(3, 0, 3, 3)),
            )
        val seed =
            seedUnfolded(
                main,
                mainColumns = 6,
                columns = 12,
                minWidth = { 1 },
            )
        assertEquals(1, seed.getValue("first.owl").first)
        assertEquals(2, seed.getValue("second.owl").first)
        assertEquals(2, seed.getValue("third.owl").first)
        // Two tiles on page 2 keep different cells: overlap is only checked among the page's own tiles.
        assertEquals(Cell(0, 0, 6, 3), seed.getValue("second.owl").second)
        assertEquals(Cell(6, 0, 6, 3), seed.getValue("third.owl").second)
    }
}
