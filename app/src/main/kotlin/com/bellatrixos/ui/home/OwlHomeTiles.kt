package com.bellatrixos.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bellatrixos.Runtime
import com.bellatrixos.ui.Mono
import com.bellatrixos.ui.Palette
import com.bellatrixos.ui.TextSize
import com.bellatrixos.widgets.Cell
import com.bellatrixos.widgets.OwlBars
import com.bellatrixos.widgets.WidgetItem
import com.bellatrixos.widgets.render.WidgetContent
import kotlin.math.roundToInt

/**
 * Geometry of Bellatrix's real desktop grid, measured from `R.id.desktop_icons`.
 * The home OWL surface is constrained to the same bounds, so these pixels are already in its coordinates.
 */
internal data class NativeGrid(
    val width: Int,
    val height: Int,
    val cols: Int,
    val rows: Int,
    val cellW: Float,
    val cellH: Float,
    /** Space after a cell: a native icon is narrower than the column GridAutofillLayoutManager gives it. */
    val gapX: Float,
    val gapY: Float,
    val padLeft: Int,
    val padTop: Int,
)

/** Never let a saved cell (from a rotation or another screen) put a tile outside the native home area. */
private fun fit(
    cell: Cell,
    item: WidgetItem,
    grid: NativeGrid,
): Cell {
    val x = cell.x.coerceIn(0, (grid.cols - 1).coerceAtLeast(0))
    val y = cell.y.coerceIn(0, (grid.rows - 1).coerceAtLeast(0))
    val w = if (item.resizable) cell.w.coerceIn(1, grid.cols - x) else 1
    val h = if (item.resizable) cell.h.coerceIn(1, grid.rows - y) else 1
    return if (cell.x == x && cell.y == y && cell.w == w && cell.h == h) cell else Cell(x, y, w, h)
}

/**
 * Oma-style declarative tiles on the native home grid, clipped to the real desktop bounds.
 *
 * These are ordinary `widget`/`face` files from the one [App.runtime]. [WidgetLayout] still decides the cell
 * (first free spot, or the least crowded one when the page is full), but the pixels come from the native
 * RecyclerView's columns. A long-press selects a tile; its bottom-right handle resizes it in whole cells,
 * and its top-right button edits the `.owl` file. The surface is clipped to the native home view, so no
 * tile can be dragged or restored outside the home border or over the taskbar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun OwlHomeTiles(
    runtime: Runtime,
    grid: NativeGrid,
) {
    val engine = runtime.widgets
    val density = LocalDensity.current
    var selected by remember { mutableStateOf<String?>(null) }

    // HomeGrid does the same in the Compose launcher: place once per geometry change, then draw from the cells.
    val items = engine.specs.filter(engine::onHome).map(::WidgetItem)
    val layout = engine.layout
    layout.geometry = Triple(grid.cellW, grid.cellH, grid.gapX.toInt())
    layout.pageSize = null
    layout.place(items, grid.cols, grid.rows)

    // The border makes the home boundary explicit; clipToBounds is the hard guarantee that no tile escapes it.
    Box(Modifier.fillMaxSize().clipToBounds().border(1.dp, Palette.border)) {
        for (item in items) {
            val raw = layout.cell(item.key) ?: continue
            val at = fit(raw, item, grid)
            val spec = item.spec
            val instance = engine.instance(spec)
            val minW = item.minSize.first.coerceAtMost(grid.cols)
            val minH = item.minSize.second.coerceAtMost(grid.rows)
            val taken = items.filter { it.key != item.key }.mapNotNull { other ->
                layout.cell(other.key)?.let { fit(it, other, grid) }
            }
            key(item.key) {
                val x = grid.padLeft + at.x * grid.cellW
                val y = grid.padTop + at.y * grid.cellH
                val w =
                    (at.w * grid.cellW - grid.gapX)
                        .coerceAtLeast(1f)
                        .coerceAtMost((grid.width - x).coerceAtLeast(1f))
                val h =
                    (at.h * grid.cellH - grid.gapY)
                        .coerceAtLeast(1f)
                        .coerceAtMost((grid.height - y).coerceAtLeast(1f))
                Box(
                    Modifier
                        .offset { IntOffset(x.toInt(), y.toInt()) }
                        .size(width = with(density) { w.toDp() }, height = with(density) { h.toDp() })
                        .combinedClickable(
                            onClick = {
                                if (selected == item.key) selected = null else engine.tap(spec)
                            },
                            onLongClick = { selected = item.key },
                        ),
                ) {
                    if (instance != null) {
                        WidgetContent(instance, Modifier.fillMaxSize(), cells = at.w to at.h)
                    }
                    spec.error?.let { error ->
                        BasicText(
                            error,
                            style = Mono.copy(color = Palette.red, fontSize = TextSize.Small),
                        )
                    }

                    if (selected == item.key) {
                        Box(Modifier.fillMaxSize().border(2.dp, Palette.accent))

                        // A small edit button keeps the old long-press action available without stealing the resize gesture.
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(Palette.card)
                                .combinedClickable(onClick = { runtime.edit(OwlBars.guestPath(spec.file.name)) }),
                        ) {
                            BasicText(
                                "✎",
                                Modifier.align(Alignment.Center),
                                style = Mono.copy(color = Palette.foreground, fontSize = TextSize.Small),
                            )
                        }

                        if (item.resizable) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(28.dp)
                                    .background(Palette.accent)
                                    .pointerInput("${item.key}:${at.x}:${at.y}:${at.w}:${at.h}") {
                                        var totalX = 0f
                                        var totalY = 0f
                                        var start = at
                                        detectDragGestures(
                                            onDragStart = {
                                                totalX = 0f
                                                totalY = 0f
                                                start = at
                                            },
                                            onDrag = { change, drag ->
                                                change.consume()
                                                totalX += drag.x
                                                totalY += drag.y
                                                val nw =
                                                    (start.w + totalX / grid.cellW).roundToInt()
                                                        .coerceIn(minW, grid.cols - start.x)
                                                val nh =
                                                    (start.h + totalY / grid.cellH).roundToInt()
                                                        .coerceIn(minH, grid.rows - start.y)
                                                val next = Cell(start.x, start.y, nw, nh)
                                                // Same rule as HomeGrid: a resize that covers another tile is refused.
                                                if (taken.none { it.overlaps(next) }) layout.set(item.key, next)
                                            },
                                            onDragEnd = { layout.save() },
                                            onDragCancel = { layout.save() },
                                        )
                                    },
                            ) {
                                BasicText(
                                    "⇲",
                                    Modifier.align(Alignment.Center),
                                    style = Mono.copy(color = Palette.foreground, fontSize = TextSize.Small),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
