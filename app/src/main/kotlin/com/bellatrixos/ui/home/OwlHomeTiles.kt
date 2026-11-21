package com.bellatrixos.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.bellatrixos.Runtime
import com.bellatrixos.ui.Mono
import com.bellatrixos.ui.Palette
import com.bellatrixos.ui.TextSize
import com.bellatrixos.widgets.OwlBars
import com.bellatrixos.widgets.WidgetItem
import com.bellatrixos.widgets.render.WidgetContent

/**
 * Geometry of Bellatrix's real desktop grid, measured from `R.id.desktop_icons` rather than guessed.
 * The home OWL surface is constrained to the same bounds, so these pixels are already in its coordinates.
 */
internal data class NativeGrid(
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

/**
 * Oma-style declarative tiles on the native home grid.
 *
 * These are ordinary `widget`/`face` files from the one [App.runtime]. [WidgetLayout] still decides the cell
 * (first free spot, or the least crowded one when the page is full), but the pixels come from the native
 * RecyclerView's columns, so a tile sits exactly where Bellatrix's desktop icons sit. The tile is drawn on
 * top of the native grid; the rest of the surface stays transparent and lets native icons and gestures through.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun OwlHomeTiles(
    runtime: Runtime,
    grid: NativeGrid,
) {
    val engine = runtime.widgets
    val density = LocalDensity.current

    // HomeGrid does the same in the Compose launcher: place once per geometry change, then draw from the cells.
    val items = engine.specs.filter(engine::onHome).map(::WidgetItem)
    val layout = engine.layout
    layout.geometry = Triple(grid.cellW, grid.cellH, grid.gapX.toInt())
    layout.pageSize = null
    layout.place(items, grid.cols, grid.rows)

    for (item in items) {
        val at = layout.cell(item.key) ?: continue
        val spec = item.spec
        val instance = engine.instance(spec)
        key(item.key) {
            val x = grid.padLeft + at.x * grid.cellW
            val y = grid.padTop + at.y * grid.cellH
            val w = (at.w * grid.cellW - grid.gapX).coerceAtLeast(1f)
            val h = (at.h * grid.cellH - grid.gapY).coerceAtLeast(1f)
            Box(
                Modifier
                    .offset { IntOffset(x.toInt(), y.toInt()) }
                    .size(width = with(density) { w.toDp() }, height = with(density) { h.toDp() })
                    .combinedClickable(
                        onClick = { engine.tap(spec) },
                        onLongClick = { runtime.edit(OwlBars.guestPath(spec.file.name)) },
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
            }
        }
    }
}
