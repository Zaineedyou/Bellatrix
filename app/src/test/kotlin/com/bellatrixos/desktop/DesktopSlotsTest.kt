package com.bellatrixos.desktop

import org.junit.Assert.assertEquals
import org.junit.Test

class DesktopSlotsTest {
    @Test fun nothingIsDroppedWhenEveryIconFits() {
        assertEquals(emptyList<Int>(), DesktopSlots.blanksToDrop(listOf(false, true, false, true), 4))
    }

    @Test fun earliestBlanksCloseUpUntilTheLastIconFits() {
        // F T T F T F in 4 cells: the last icon is the sixth cell, so two blanks must go.
        assertEquals(listOf(1, 2), DesktopSlots.blanksToDrop(listOf(false, true, true, false, true, false), 4))
    }

    @Test fun iconsThatCannotFitAreLeftAloneWhenNoBlankIsLeft() {
        assertEquals(emptyList<Int>(), DesktopSlots.blanksToDrop(listOf(false, false, false), 2))
    }

    @Test fun blanksAfterTheLastIconNeverCount() {
        assertEquals(emptyList<Int>(), DesktopSlots.blanksToDrop(listOf(false, true, true, true, true), 3))
    }

    @Test fun packedKeepsOrderAndRemovesGaps() {
        assertEquals(listOf("a", "b", "c"), DesktopSlots.packed(listOf("", "a", "", "b", "c", "")) { it.isEmpty() })
    }
}
