package com.bellatrixos.desktop

/**
 * Home's icons are one list read down the first column, then down the next. When the phone turns and the
 * screen has fewer cells, this says which empty cells to close up so every icon still lands on screen.
 */
object DesktopSlots {
    /**
     * Indices of the blank cells to remove, earliest first, so the last real icon fits inside [capacity] cells.
     * Empty when it already fits, or when there are not enough blanks to close the gap.
     */
    @JvmStatic
    fun blanksToDrop(
        blank: List<Boolean>,
        capacity: Int,
    ): List<Int> {
        val lastReal = blank.indexOfLast { !it }
        var overflow = lastReal + 1 - capacity
        if (overflow <= 0) return emptyList()
        val drop = ArrayList<Int>()
        for (i in 0 until lastReal) {
            if (overflow == 0) break
            if (blank[i]) {
                drop.add(i)
                overflow--
            }
        }
        return drop
    }

    /** Drops the leading blank cells and every gap, keeping the icons in order: the layout that fills from the top left. */
    @JvmStatic
    fun <T> packed(
        items: List<T>,
        isBlank: (T) -> Boolean,
    ): List<T> = items.filterNot(isBlank)
}
