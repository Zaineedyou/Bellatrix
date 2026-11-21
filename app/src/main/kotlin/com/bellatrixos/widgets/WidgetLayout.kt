package com.bellatrixos.widgets

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import com.bellatrixos.upgrade.Versioned
import com.bellatrixos.upgrade.mapEntries
import org.json.JSONObject

/** A widget's free-form rectangle on Bellatrix's native home, in device pixels. */
data class PixelRect(
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
)

/** A widget's place on its page's grid, in cells. */
data class Cell(
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
) {
    val right get() = x + w
    val bottom get() = y + h

    fun overlaps(o: Cell) = x < o.right && o.x < right && y < o.bottom && o.y < bottom

    /**
     * This cell, placed on a grid of [from] columns, moved onto one of [to]: its left edge and width scale down
     * to whole cells, and it stays at least [minWidth] wide, or the whole width, by moving left.
     */
    fun rescaled(
        from: Int,
        to: Int,
        minWidth: Int = 1,
    ): Cell {
        if (from == to) return this
        val x = x * to / from
        // A tile in layout.toml can start past its own columns, so x can be at or past the new edge.
        val w = maxOf((w * to / from).coerceIn(1, (to - x).coerceAtLeast(1)), minWidth.coerceAtMost(to))
        return copy(x = x.coerceAtMost(to - w), w = w)
    }
}

/**
 * Deprecated: Bellatrix home is hard-coded to one page, so no face page exists. Kept so old saved data and callers
 * still compile; every tile, faces included, is on page 1.
 */
const val FACE_PAGE = 0

/** Deprecated: always false on Bellatrix's one home page. */
@Suppress("UNUSED_PARAMETER")
fun isFacePage(page: Int) = false

/** The home grid's columns before they could be set: a saved layout that doesn't say has this many. */
const val CLASSIC_COLUMNS = 6

/**
 * A screen home has a layout of its own: the phone's, a foldable's cover screen, where a layout made
 * for the inner screen would squeeze every tile into a third of its height, or a Fold's unfolded inner
 * screen, where it would stretch them over twice the width. [key] names it in saved data and
 * layout.toml's `[cover]` and `[unfolded]` sections; [MAIN] is the top of the file, as before there were
 * two. [ownColumns]: the screen's grid has a column count of its own (home.unfolded-columns) rather than
 * home.columns.
 */
enum class Screen(
    val key: String,
    val ownColumns: Boolean = false,
) {
    MAIN("main"),
    COVER("cover"),
    UNFOLDED("unfolded", ownColumns = true),
    ;

    /**
     * Every screen has Bellatrix's one hard-coded home page. [page] is kept for old saved layouts and ignored.
     */
    @Suppress("UNUSED_PARAMETER")
    fun fromMain(page: Int) = 1
}

/**
 * Where tiles start on an unfolded screen the first time it shows: the same one home page, with each of [main]'s
 * cells (key, old page number, cell) rescaled from [mainColumns] to the unfolded grid's [columns]. Faces are tiles
 * with their own cells now. A tile that would land on one already there loses its cell (null), so the page places
 * it afresh at the least crowded spot. The old page number is ignored.
 */
@Suppress("UNUSED_PARAMETER")
fun seedUnfolded(
    main: List<Triple<String, Int, Cell?>>,
    mainColumns: Int,
    columns: Int,
    minWidth: (String) -> Int,
): Map<String, Pair<Int, Cell?>> {
    val kept = ArrayList<Cell>()
    return main.associate { (key, _, cell) ->
        val placed =
            cell
                ?.rescaled(mainColumns, columns, minWidth(key))
                ?.takeIf { c -> kept.none { it.overlaps(c) } }
                ?.also { kept += it }
        key to (1 to placed)
    }
}

/**
 * Where each widget and pinned app sits on the home grid, keyed by file name
 * (or shortcut key), and the page a widget was moved to on the phone; one set
 * per [Screen], of which [screen] is the one home shows and every call here
 * reads and writes unless it says otherwise. Kept apart
 * from the widget files, so rearranging never rewrites the files agents own:
 * [HomeStore] writes it all to layout.toml, which agents may edit instead.
 */
class WidgetLayout(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("widget_grid", Context.MODE_PRIVATE)
    private val cellsOf = Screen.entries.associateWith { mutableStateMapOf<String, Cell>() }
    /** Free pixel geometry set by dragging on the native home; separate from layout.toml's agent-owned cells. */
    private val nativeRectsOf = mutableStateMapOf<String, PixelRect>()
    /** Read while compiling widgets, off the main thread. */
    private val pagesOf = Screen.entries.associateWith { java.util.concurrent.ConcurrentHashMap<String, Int>() }

    /** The screen home is on; see [show]. State, so what lists home's items by page follows it. */
    var screen: Screen by mutableStateOf(Screen.MAIN)
        private set

    /**
     * Counts page changes. The pages aren't snapshot state (they're read while compiling, off the main thread), so what
     * derives an app's page on another screen than the main one from them reads this to follow them; see [Shortcuts.onGrid].
     */
    var pageStamp by mutableIntStateOf(0)
        private set

    private val cells get() = cellsOf.getValue(screen)
    private val pages get() = pagesOf.getValue(screen)

    /** Whether [screen] has a layout of its own yet: a cell or a page chosen for it. Without one, home lays its tiles out afresh there. */
    fun has(screen: Screen) = cellsOf.getValue(screen).isNotEmpty() || pagesOf.getValue(screen).isNotEmpty()

    /**
     * Home moves to [screen]: its cells and pages are the ones read and written from here on. On a screen without a
     * layout of its own yet, every tile is on the page it has on the main screen (see [chosenPage]) and has no cell: its
     * grid lays them out afresh at their own size, and what doesn't fit moves on to the next page there.
     */
    fun show(screen: Screen) {
        if (screen == this.screen) return
        this.screen = screen
        pageStamp++
    }

    /** Forgets [screen]'s layout; shown, it starts over from the main screen's pages as in [show]. */
    fun reset(screen: Screen) {
        cellsOf.getValue(screen).clear()
        pagesOf.getValue(screen).clear()
        pageStamp++
        save()
    }

    /**
     * Columns of the home grid the cells are on, each screen's: home.columns, or for a screen with [Screen.ownColumns]
     * its own. Saved with them, so [regrid] can move them onto another.
     */
    private val columnsBy = Screen.entries.filter { it.ownColumns }.associateWith { mutableIntStateOf(CLASSIC_COLUMNS) }
    private var mainColumns by mutableIntStateOf(CLASSIC_COLUMNS)

    /** Columns of the grid of [screen]: its own, or home.columns. */
    fun columnsOf(screen: Screen) = columnsBy[screen]?.intValue ?: mainColumns

    /** Columns of the grid home shows, [screen]'s. */
    val columns get() = columnsOf(screen)

    init {
        (Versioned.read(prefs.getString(LAYOUT, null), VERSION) as? JSONObject)?.let { data ->
            mainColumns = savedColumns(data, Screen.MAIN)
            for ((screen, c) in columnsBy) c.intValue = savedColumns(data, screen)
            load(data)
        }
        (Versioned.read(prefs.getString(NATIVE_RECTS, null), NATIVE_RECTS_VERSION) as? JSONObject)?.let { data ->
            for (key in data.keys()) {
                data.optJSONObject(key)?.let { r ->
                    nativeRectsOf[key] = PixelRect(r.optInt("x"), r.optInt("y"), r.optInt("w", 1), r.optInt("h", 1))
                }
            }
        }
    }

    private fun suffix(screen: Screen) = if (screen == Screen.MAIN) "" else "_${screen.key}"

    /** The columns saved for [screen]'s grid; one saved before it had its own has the main grid's. */
    private fun savedColumns(
        data: JSONObject,
        screen: Screen,
    ): Int {
        val main = data.optInt("columns", CLASSIC_COLUMNS).takeIf { it > 0 } ?: CLASSIC_COLUMNS
        return if (screen.ownColumns) data.optInt("columns${suffix(screen)}", main).takeIf { it > 0 } ?: main else main
    }

    private fun load(data: JSONObject) {
        // The main screen's are "cells" and "pages", as saved before there were two screens; the others' carry the screen's key.
        for (screen in Screen.entries) {
            val suffix = suffix(screen)
            data.optJSONObject("cells$suffix")?.mapEntries { _, c -> Cell(c.getInt("x"), c.getInt("y"), c.getInt("w"), c.getInt("h")) }?.let(cellsOf.getValue(screen)::putAll)
            data.optJSONObject("pages$suffix")?.let { p -> for (k in p.keys()) pagesOf.getValue(screen)[k] = p.optInt(k, 1) }
        }
    }

    /**
     * Gives [screen], which has no layout of its own yet, the pages and cells in [seed] (key to page and cell, null
     * where its page places it afresh); see [seedUnfolded].
     */
    fun seed(
        screen: Screen,
        seed: Map<String, Pair<Int, Cell?>>,
    ) {
        val cells = cellsOf.getValue(screen)
        val pages = pagesOf.getValue(screen)
        for ((key, at) in seed) {
            pages[key] = at.first
            at.second?.let { cells[key] = it }
        }
        pageStamp++
        save()
    }

    /**
     * Moves every cell onto a grid of [to] columns (see [Cell.rescaled]); a widget stays as wide as its
     * [HomeItem.minSize] allows. A tile that lands on one before it on its page in [tiles] (each page's, in the
     * order they keep their spots) loses its cell, so its page places it afresh in its first free spot, or moves
     * it to a page with room. The other screens' cells move onto the new grid too; their pages aren't laid out now, so
     * which of them then overlap is settled when that screen shows.
     */
    fun regrid(
        to: Int,
        tiles: List<List<HomeItem>>,
        screen: Screen = Screen.MAIN,
    ) {
        val from = columnsOf(screen)
        if (to == from) return
        val minWidth = tiles.flatten().associate { it.key to it.minSize.first }
        // The screens on this grid: one with its own columns alone, else every one without.
        val on = Screen.entries.filter { if (screen.ownColumns) it == screen else !it.ownColumns }
        for (s in on) {
            val screenCells = cellsOf.getValue(s)
            for ((key, c) in screenCells.toMap()) screenCells[key] = c.rescaled(from, to, minWidth[key] ?: 1)
        }
        columnsBy[screen]?.let { it.intValue = to } ?: run { mainColumns = to }
        val cells = cellsOf.getValue(screen)
        for (items in tiles) {
            val kept = ArrayList<Cell>()
            for (item in items) {
                val c = cells[item.key]?.let { if (item.resizable) it else it.copy(w = 1, h = 1) } ?: continue
                if (kept.any { it.overlaps(c) }) cells.remove(item.key) else kept += c
            }
        }
        save()
    }

    /** Always page 1 when [name] has a saved page; on [screen], the one shown unless said. */
    fun page(
        name: String,
        screen: Screen = this.screen,
    ): Int? = pagesOf.getValue(screen)[name]?.let { 1 }

    /**
     * Bellatrix has one page: every saved page number reads as 1 (see [Screen.fromMain]). Kept so old layout files
     * still load; it never puts a tile on a second page.
     */
    fun chosenPage(
        name: String,
        screen: Screen = this.screen,
    ): Int? =
        if (pagesOf.getValue(screen).containsKey(name) || (screen != Screen.MAIN && pagesOf.getValue(Screen.MAIN).containsKey(name))) 1 else null

    /** The page [name] is on on [screen]: as [chosenPage], else its file's [filePage] as it is on that screen (see [Screen.fromMain]). */
    @Suppress("UNUSED_PARAMETER")
    fun pageOn(
        name: String,
        filePage: Int,
        screen: Screen = this.screen,
    ): Int = 1

    fun setPage(
        name: String,
        page: Int,
        screen: Screen = this.screen,
    ) {
        // Hard-coded single page: every home item lands on Bellatrix's one page, whatever a file or layout says.
        pagesOf.getValue(screen)[name] = 1
        pageStamp++
        save()
    }

    fun cell(
        name: String,
        screen: Screen = this.screen,
    ) = cellsOf.getValue(screen)[name]

    /** The free pixel rectangle a tile was dragged to on native home, if it has one. */
    fun nativeRect(name: String) = nativeRectsOf[name]

    /** Live update while dragging native home tiles; [saveNativeRects] persists. Equal values don't recompose. */
    fun setNativeRect(
        name: String,
        rect: PixelRect,
    ) {
        if (nativeRectsOf[name] != rect) nativeRectsOf[name] = rect
    }

    fun clearNativeRect(name: String) = nativeRectsOf.remove(name)

    /** Persists only the free geometry; it must never rewrite layout.toml or the agent-owned grid cells. */
    fun saveNativeRects() {
        val data = JSONObject()
        for ((name, rect) in nativeRectsOf) {
            data.put(name, JSONObject().put("x", rect.x).put("y", rect.y).put("w", rect.w).put("h", rect.h))
        }
        prefs.edit().putString(NATIVE_RECTS, Versioned.write(NATIVE_RECTS_VERSION, data)).apply()
    }

    /**
     * Sets the measured native grid for previews without assigning cells. The native home places its tiles with
     * declarative Box/Flex/Anchor geometry and free pixel drags, not with these cells; layout.toml keeps them for
     * agents and the classic grid.
     */
    fun setGrid(
        cols: Int,
        rows: Int,
    ) {
        grid = cols to rows
    }

    /** Live update while dragging; [save] persists. An unchanged cell isn't written, so pointer moves within it don't recompose the page. */
    fun set(
        name: String,
        cell: Cell,
        screen: Screen = this.screen,
    ) {
        val cells = cellsOf.getValue(screen)
        if (cells[name] != cell) cells[name] = cell
    }

    /**
     * Rows the home grid had in portrait on a window of [size] (its short and long side, so both
     * orientations share it). Landscape turns that grid on its side, so every tile keeps its cell.
     */
    fun portraitRows(size: String) = prefs.getInt(PORTRAIT_ROWS + size, 0).takeIf { it > 0 }

    fun setPortraitRows(
        size: String,
        rows: Int,
    ) {
        if (portraitRows(size) != rows) prefs.edit().putInt(PORTRAIT_ROWS + size, rows).apply()
    }

    /** The grid last laid out on screen, columns and rows; every page shares it. */
    var grid: Pair<Int, Int>? = null
        private set

    /**
     * Home is hard-coded to one page: no face takes a page over. Face files are ordinary tiles on that page,
     * so every item keeps its own cell and the classic Bellatrix grid stays visible.
     */
    @Suppress("UNUSED_PARAMETER")
    fun owner(items: List<HomeItem>): HomeItem? = null

    /**
     * Gives widgets that have no cell yet one at their declared size, in the
     * first free spot. Saved cells are never rewritten here: a temporarily
     * small page (keyboard, rotation) must not destroy the user's layout.
     *
     * Bellatrix home is hard-coded to one page, so nothing is homeless: every item gets a cell. When the grid
     * is full, the newcomer uses the spot that overlaps existing tiles least, and is drawn last so it is
     * the one on top. Never returns anything; callers keep the old signature for compatibility.
     */
    fun place(
        items: List<HomeItem>,
        cols: Int,
        rows: Int,
    ): List<HomeItem> {
        grid = cols to rows
        var changed = false
        for (item in items) {
            if (cells[item.key] != null) continue
            cells[item.key] = freeSpot(item, items, cols, rows) ?: crowdSpot(item, items, cols, rows)
            changed = true
        }
        if (changed) save()
        return emptyList()
    }

    /**
     * Gives [item] a spot at its size among [others]. Items without a cell yet get theirs first, so the
     * page's own tiles keep their spots and the newcomer takes what's left. Home has only one page, so
     * a full page falls back to the least crowded spot instead of sending the item to another page.
     */
    fun claim(
        item: HomeItem,
        others: List<HomeItem>,
    ): Boolean {
        val (cols, rows) = grid ?: return false
        val rest = others.filter { it.key != item.key }
        place(rest, cols, rows)
        cells[item.key] = freeSpot(item, others, cols, rows) ?: crowdSpot(item, others, cols, rows)
        save()
        return true
    }

    /** First spot where [item] fits at its size beside the placed [items]; null when the page is full. */
    private fun freeSpot(
        item: HomeItem,
        items: List<HomeItem>,
        cols: Int,
        rows: Int,
    ): Cell? {
        // Shortcuts always show as 1x1, whatever an old saved cell says.
        val others = items.mapNotNull { other -> if (other.key == item.key) null else cells[other.key]?.let { if (other.resizable) it else it.copy(w = 1, h = 1) } }
        val (pw, ph) = item.size ?: (cols / 2 to 3)
        val w = pw.coerceIn(1, cols)
        val h = ph.coerceIn(1, rows)
        for (y in 0..rows - h) for (x in 0..cols - w) {
            val c = Cell(x, y, w, h)
            if (others.none { it.overlaps(c) }) return c
        }
        return null
    }

    /** A spot on the one home page even when it is full: the one covering the fewest existing cells. */
    private fun crowdSpot(
        item: HomeItem,
        items: List<HomeItem>,
        cols: Int,
        rows: Int,
    ): Cell {
        val (pw, ph) = item.size ?: (cols / 2 to 3)
        val w = pw.coerceIn(1, cols)
        val h = ph.coerceIn(1, rows)
        val occupied = items.mapNotNull { other -> if (other.key == item.key) null else cells[other.key] }
        var best = Cell(0, 0, w, h)
        var fewest = Int.MAX_VALUE
        for (y in 0..rows - h) for (x in 0..cols - w) {
            val c = Cell(x, y, w, h)
            val covered = occupied.sumOf { c.overlapWith(it) }
            if (covered < fewest) {
                best = c
                fewest = covered
            }
            if (covered == 0) return c
        }
        return best
    }

    private fun Cell.overlapWith(other: Cell): Int =
        maxOf(0, minOf(right, other.right) - maxOf(x, other.x)) *
            maxOf(0, minOf(bottom, other.bottom) - maxOf(y, other.y))

    /** Takes [name]'s cell, so its page places it afresh in its first free spot. */
    fun clearCell(
        name: String,
        screen: Screen = this.screen,
    ) {
        cellsOf.getValue(screen).remove(name)
    }

    /** Forgets everything about [name] on every screen: its cells, pages and free native-home rectangle. */
    fun remove(name: String) {
        for (c in cellsOf.values) c.remove(name)
        for (p in pagesOf.values) p.remove(name)
        nativeRectsOf.remove(name)
        saveNativeRects()
        pageStamp++
        save()
    }

    /** The home grid's cell width and height on screen and the gap between tiles, in pixels, as last laid out. */
    @Volatile var geometry: Triple<Float, Float, Int>? = null

    /** A home page's width and height in pixels, as last laid out: what a face fills. */
    @Volatile var pageSize: Pair<Int, Int>? = null

    /** Whether the home grid was last laid out on its side (landscape): a tile's columns run down the screen. */
    @Volatile var turned = false

    /** A tile's size in pixels at [cells]; before the home grid was laid out, an estimate from the screen width. */
    fun tilePx(
        cells: Pair<Int, Int>,
        metrics: android.util.DisplayMetrics,
    ): Pair<Int, Int> {
        val (cellW, cellH, gap) =
            geometry ?: run {
                val gap = Math.round(6 * metrics.density)
                val cell = (metrics.widthPixels - gap) / columns.toFloat()
                Triple(cell, cell, gap)
            }
        val (across, down) = if (turned) cells.second to cells.first else cells
        return (across * cellW).toInt() - gap to (down * cellH).toInt() - gap
    }

    /** A face's size in pixels; before home was laid out, an estimate: the screen less the status bar, strip and bar. */
    fun pagePx(metrics: android.util.DisplayMetrics) = pageSize ?: (metrics.widthPixels to (metrics.heightPixels * 0.8f).toInt())

    fun save() {
        val data = JSONObject().put("columns", mainColumns)
        for ((screen, c) in columnsBy) data.put("columns${suffix(screen)}", c.intValue)
        for (screen in Screen.entries) {
            val suffix = suffix(screen)
            val json = JSONObject()
            for ((name, c) in cellsOf.getValue(screen)) json.put(name, JSONObject().put("x", c.x).put("y", c.y).put("w", c.w).put("h", c.h))
            data.put("cells$suffix", json).put("pages$suffix", JSONObject(pagesOf.getValue(screen) as Map<*, *>))
        }
        prefs.edit().putString(LAYOUT, Versioned.write(VERSION, data)).apply()
        onSave()
    }

    /** Called after every save; [HomeStore] writes layout.toml then. */
    var onSave: () -> Unit = {}

    private companion object {
        const val LAYOUT = "layout"
        const val VERSION = 1
        const val NATIVE_RECTS = "native_tile_rects"
        const val NATIVE_RECTS_VERSION = 1
        const val PORTRAIT_ROWS = "portrait_rows_"
    }
}
