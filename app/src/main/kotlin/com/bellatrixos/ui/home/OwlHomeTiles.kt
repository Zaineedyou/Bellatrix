package com.bellatrixos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bellatrixos.R
import com.bellatrixos.Runtime
import com.bellatrixos.ui.Mono
import com.bellatrixos.ui.Palette
import com.bellatrixos.ui.TextSize
import com.bellatrixos.ui.files.MenuPopup
import com.bellatrixos.widgets.Cell
import com.bellatrixos.widgets.OwlTileGeometry
import com.bellatrixos.widgets.PixelRect
import com.bellatrixos.widgets.WidgetItem
import com.bellatrixos.widgets.editWidgetCommand
import com.bellatrixos.widgets.render.WidgetContent
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Geometry of Bellatrix's real desktop area, measured from `R.id.desktop_icons`.
 * The home OWL surface is constrained to the same bounds, so these pixels are already in its coordinates.
 */
internal data class NativeGrid(
    val width: Int,
    val height: Int,
    val cols: Int,
    val rows: Int,
    val cellW: Float,
    val cellH: Float,
    /** Space after a cell: used for OWL's `cols`/`rows` preview globals, not for tile placement. */
    val gapX: Float,
    val gapY: Float,
    val padLeft: Int,
    val padTop: Int,
)

private data class TileEdges(
    val left: Boolean,
    val top: Boolean,
    val right: Boolean,
    val bottom: Boolean,
) {
    val any get() = left || top || right || bottom
}

private val NO_TILE_EDGES = TileEdges(false, false, false, false)

/** Which edges a press grabs. A corner grabs two; the top-right corner belongs to the dropdown button. */
private fun tileEdgesAt(
    rect: IntRect,
    p: Offset,
    grab: Int,
    sideGrab: Int,
): TileEdges {
    val inY = p.y > rect.top - grab && p.y < rect.bottom + grab
    val inX = p.x > rect.left - grab && p.x < rect.right + grab
    val gx = minOf(sideGrab, rect.width / 3)
    val gy = minOf(grab, rect.height / 3)
    return TileEdges(
        left = inY && p.x > rect.left - grab && p.x < rect.left + gx,
        top = inX && p.y > rect.top - grab && p.y < rect.top + gy,
        right = inY && p.x < rect.right + grab && p.x > rect.right - gx,
        bottom = inX && p.y < rect.bottom + grab && p.y > rect.bottom - gy,
    )
}

/** Keeps a free rect inside the native home area. Overlaps are allowed; escaping the border is not. */
private fun contained(
    rect: PixelRect,
    grid: NativeGrid,
    minW: Int,
    minH: Int,
): PixelRect {
    val w = rect.w.coerceIn(minW.coerceAtMost(grid.width), grid.width)
    val h = rect.h.coerceIn(minH.coerceAtMost(grid.height), grid.height)
    return rect.copy(
        x = rect.x.coerceIn(0, grid.width - w),
        y = rect.y.coerceIn(0, grid.height - h),
        w = w,
        h = h,
    )
}

private fun moved(
    start: PixelRect,
    dx: Float,
    dy: Float,
    grid: NativeGrid,
): PixelRect =
    contained(
        start.copy(x = start.x + dx.roundToInt(), y = start.y + dy.roundToInt()),
        grid,
        start.w,
        start.h,
    )

private fun resized(
    start: PixelRect,
    edges: TileEdges,
    dx: Float,
    dy: Float,
    grid: NativeGrid,
    minW: Int,
    minH: Int,
): PixelRect {
    val left = start.x
    val top = start.y
    val right = start.x + start.w
    val bottom = start.y + start.h
    val x0 = if (edges.left) (left + dx).roundToInt().coerceIn(0, right - minW) else left
    val x1 = if (edges.right) (right + dx).roundToInt().coerceIn(x0 + minW, grid.width) else right
    val y0 = if (edges.top) (top + dy).roundToInt().coerceIn(0, bottom - minH) else top
    val y1 = if (edges.bottom) (bottom + dy).roundToInt().coerceIn(y0 + minH, grid.height) else bottom
    return contained(PixelRect(x0, y0, x1 - x0, y1 - y0), grid, minW, minH)
}

private fun legacyRect(
    cell: Cell?,
    grid: NativeGrid,
): PixelRect? =
    cell?.let {
        PixelRect(
            (grid.padLeft + it.x * grid.cellW).roundToInt(),
            (grid.padTop + it.y * grid.cellH).roundToInt(),
            (it.w * grid.cellW - grid.gapX).roundToInt(),
            (it.h * grid.cellH - grid.gapY).roundToInt(),
        )
    }

/**
 * Declarative OWL tiles on Bellatrix's native home.
 *
 * A tile's starting rectangle comes from its root `Box`/`Flex`/`Anchor` props (`width`, `height`,
 * `size`, `fill`, `offset`, `anchor`), not from a grid. After that, a long-press selects it, a drag
 * moves it anywhere, and its edges/corners resize it freely; tiles may overlap. The small ▾ button
 * opens Remove/Edit, with Edit starting a Coding Agent session (or the Coding Agents page when no
 * agent is installed). Geometry is clipped to the native home view, with no visible boundary line.
 */
@Composable
internal fun OwlHomeTiles(
    runtime: Runtime,
    grid: NativeGrid,
) {
    val engine = runtime.widgets
    val layout = engine.layout
    val density = LocalDensity.current
    val densityFloat = density.density
    val haptics = LocalHapticFeedback.current
    var selected by remember { mutableStateOf<String?>(null) }
    var menuFor by remember { mutableStateOf<String?>(null) }

    val items = engine.specs.filter(engine::onHome).map(::WidgetItem)
    layout.geometry = Triple(grid.cellW, grid.cellH, grid.gapX.toInt())
    layout.pageSize = null
    layout.setGrid(grid.cols, grid.rows)

    val grab = with(LocalDensity.current) { 12.dp.toPx() }.roundToInt()
    val sideGrab = with(LocalDensity.current) { 24.dp.toPx() }.roundToInt()
    val buttonPx = with(LocalDensity.current) { 22.dp.toPx() }.roundToInt()
    val absoluteMin = with(LocalDensity.current) { 56.dp.toPx() }.roundToInt()

    // No visible border: clipToBounds is the hard guarantee that no tile escapes into the taskbar.
    Box(Modifier.fillMaxSize().clipToBounds()) {
        for (item in items) {
            val spec = item.spec
            val instance = engine.instance(spec)
            val raw =
                layout.nativeRect(item.key)
                    ?: if (instance != null) {
                        OwlTileGeometry.initialRect(spec, instance, grid, densityFloat)
                    } else {
                        legacyRect(layout.cell(item.key), grid)
                            ?: OwlTileGeometry.initialRect(spec, null, grid, densityFloat)
                    }
            val minW = max(absoluteMin, (item.minSize.first * grid.cellW).roundToInt()).coerceAtMost(grid.width)
            val minH = max(absoluteMin, (item.minSize.second * grid.cellH).roundToInt()).coerceAtMost(grid.height)
            val at = contained(raw, grid, minW, minH)
            key(item.key) {
                val current = rememberUpdatedState(at)
                val isSelected = selected == item.key

                val dragModifier =
                    if (isSelected) {
                        Modifier.pointerInput("owl-edit-${item.key}") {
                            var start = current.value
                            var total = Offset.Zero
                            var edges = NO_TILE_EDGES
                            var ignore = false
                            detectDragGestures(
                                onDragStart = { pos ->
                                    start = current.value
                                    total = Offset.Zero
                                    ignore = pos.x >= start.w - buttonPx && pos.y <= buttonPx
                                    edges =
                                        if (ignore) {
                                            NO_TILE_EDGES
                                        } else {
                                            tileEdgesAt(
                                                IntRect(start.x, start.y, start.x + start.w, start.y + start.h),
                                                pos,
                                                grab,
                                                sideGrab,
                                            )
                                        }
                                },
                                onDrag = { change, amount ->
                                    if (ignore) return@detectDragGestures
                                    change.consume()
                                    total += amount
                                    val next =
                                        if (edges.any) {
                                            resized(start, edges, total.x, total.y, grid, minW, minH)
                                        } else {
                                            moved(start, total.x, total.y, grid)
                                        }
                                    layout.setNativeRect(item.key, next)
                                },
                                onDragEnd = { if (!ignore) layout.saveNativeRects() },
                                onDragCancel = { if (!ignore) layout.saveNativeRects() },
                            )
                        }
                    } else {
                        Modifier.pointerInput("owl-raise-${item.key}") {
                            var start = current.value
                            var total = Offset.Zero
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    start = current.value
                                    total = Offset.Zero
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    total += amount
                                    layout.setNativeRect(item.key, moved(start, total.x, total.y, grid))
                                },
                                onDragEnd = {
                                    layout.saveNativeRects()
                                    selected = null
                                },
                                onDragCancel = {
                                    layout.saveNativeRects()
                                    selected = null
                                },
                            )
                        }
                    }

                Box(
                    Modifier
                        .offset { IntOffset(at.x, at.y) }
                        .size(width = with(density) { at.w.toDp() }, height = with(density) { at.h.toDp() })
                        .zIndex(if (isSelected) 2f else 0f)
                        .then(dragModifier)
                        .pointerInput("owl-tap-${item.key}") {
                            detectTapGestures(
                                onTap = {
                                    if (selected == item.key) selected = null else engine.tap(spec)
                                },
                                onLongPress = {
                                    selected = item.key
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            )
                        },
                ) {
                    if (instance != null) {
                        val cols = (at.w / grid.cellW).roundToInt().coerceAtLeast(1)
                        val rows = (at.h / grid.cellH).roundToInt().coerceAtLeast(1)
                        WidgetContent(instance, Modifier.fillMaxSize(), cells = cols to rows)
                    }
                    spec.error?.let { error ->
                        BasicText(error, style = Mono.copy(color = Palette.red, fontSize = TextSize.Small))
                    }

                    if (isSelected) {
                        Box(Modifier.fillMaxSize().border(1.dp, Palette.accent))
                        for (align in listOf(Alignment.TopStart, Alignment.BottomStart, Alignment.BottomEnd, Alignment.CenterStart, Alignment.CenterEnd, Alignment.BottomCenter)) {
                            Box(Modifier.align(align).size(8.dp).background(Palette.accent))
                        }
                    }

                    // Window-style dropdown: always available, and it never starts a move/resize.
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(22.dp)
                            .background(Palette.card.copy(alpha = 0.82f))
                            .clickable { menuFor = if (menuFor == item.key) null else item.key },
                    ) {
                        BasicText(
                            "▾",
                            Modifier.align(Alignment.Center),
                            style = Mono.copy(color = Palette.foreground, fontSize = TextSize.Small),
                        )
                    }

                    if (menuFor == item.key) {
                        MenuPopup(
                            items =
                                listOf(
                                    stringResource(R.string.home_edit) to {
                                        menuFor = null
                                        val agent = runtime.agents.default
                                        if (agent == null) {
                                            runtime.showCodingAgents()
                                        } else {
                                            runtime.terminal(editWidgetCommand(agent.bin, spec), "Edit ${spec.name}", agent)
                                        }
                                    },
                                    stringResource(R.string.home_remove) to {
                                        menuFor = null
                                        selected = null
                                        engine.removeWidget(spec)
                                    },
                                ),
                            onDismiss = { menuFor = null },
                        )
                    }
                }
            }
        }
    }
}
