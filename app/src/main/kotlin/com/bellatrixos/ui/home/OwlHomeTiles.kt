package com.bellatrixos.ui.home

import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.systemGestureExclusion
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bellatrixos.MainActivity
import com.bellatrixos.R
import com.bellatrixos.Runtime
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.context_menu.UIMenuItem
import com.bellatrixos.ui.Mono
import com.bellatrixos.ui.Palette
import com.bellatrixos.ui.TextSize
import com.bellatrixos.widgets.Cell
import com.bellatrixos.widgets.OwlTileGeometry
import com.bellatrixos.widgets.PixelRect
import com.bellatrixos.widgets.WidgetItem
import com.bellatrixos.widgets.editWidgetCommand
import com.bellatrixos.widgets.render.WidgetContent
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeoutOrNull

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

/** Which edges a press grabs. A corner grabs two. [p] is tile-local, never a screen rect. */
private fun tileEdgesAt(
    rect: IntRect,
    p: Offset,
    grab: Int,
    sideGrab: Int,
): TileEdges {
    val inY = p.y > -grab.toFloat() && p.y < rect.height + grab
    val inX = p.x > -grab.toFloat() && p.x < rect.width + grab
    val gx = minOf(sideGrab, rect.width / 3)
    val gy = minOf(grab, rect.height / 3)
    return TileEdges(
        left = inY && p.x > -grab && p.x < gx,
        top = inX && p.y > -grab && p.y < gy,
        right = inY && p.x < rect.width + grab && p.x > rect.width - gx,
        bottom = inX && p.y < rect.height + grab && p.y > rect.height - gy,
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

private tailrec fun Context.findActivity(): MainActivity? =
    when (this) {
        is MainActivity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

/**
 * The real Bellatrix context-menu PopupWindow, not a Compose imitation. Long-pressing a tile and
 * releasing without dragging opens this window with the same frame, icons and animation as the
 * desktop's own menus.
 */
private fun showNativeWidgetMenu(
    context: Context,
    anchor: View,
    rect: PixelRect,
    editLabel: String,
    removeLabel: String,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val activity = context.findActivity() ?: return
    UIContextMenu.f4743f?.dismiss()
    val menu = UIContextMenu(activity)
    menu.f4745d.clear()

    val edit = UIMenuItem(0, 1, R.drawable.menu_ic_edit_20_regular, editLabel)
    edit.f4750f = true
    menu.f4745d.add(edit)
    menu.f4745d.add(UIMenuItem(1, 0, 0, ""))
    menu.f4745d.add(UIMenuItem(0, 2, R.drawable.menu_ic_delete_20_regular, removeLabel))

    menu.c =
        UIContextMenu.OnMenuItemClickListener { _, id ->
            when (id) {
                1 -> onEdit()
                2 -> onRemove()
            }
            menu.dismiss()
        }
    menu.setOnDismissListener { onDismiss() }
    menu.a(0)
    menu.c()

    val x = rect.x.coerceIn(0, (anchor.width - menu.width).coerceAtLeast(0))
    val below = rect.y + rect.h
    val y =
        if (below + menu.height <= anchor.height) {
            below
        } else {
            (rect.y - menu.height).coerceAtLeast(0)
        }
    menu.showAt(anchor, x, y)
}

/**
 * Declarative OWL tiles on Bellatrix's native home.
 *
 * A tile's starting rectangle comes from its root `Box`/`Flex`/`Anchor` props (`width`, `height`,
 * `size`, `fill`, `offset`, `anchor`), not from a grid. Drag moves it anywhere and an edge or corner
 * resizes it freely; tiles may overlap. Holding a tile opens Bellatrix's own popup window with
 * Edit (a Coding Agent on the `.owl` file) and Remove. Geometry is clipped to the native home view,
 * with no visible boundary line.
 */
@Composable
internal fun OwlHomeTiles(
    runtime: Runtime,
    grid: NativeGrid,
) {
    val engine = runtime.widgets
    val layout = engine.layout
    val context = LocalContext.current
    val anchor = LocalView.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val editLabel = stringResource(R.string.home_edit)
    val removeLabel = stringResource(R.string.home_remove)
    var selected by remember { mutableStateOf<String?>(null) }

    val items = engine.specs.filter(engine::onHome).map(::WidgetItem)
    layout.geometry = Triple(grid.cellW, grid.cellH, grid.gapX.toInt())
    layout.pageSize = null
    layout.setGrid(grid.cols, grid.rows)

    val grab = with(LocalDensity.current) { 20.dp.toPx() }.roundToInt()
    val sideGrab = with(LocalDensity.current) { 36.dp.toPx() }.roundToInt()
    val absoluteMin = with(LocalDensity.current) { 56.dp.toPx() }.roundToInt()

    // No visible border: clipToBounds is the hard guarantee that no tile escapes into the taskbar.
    Box(Modifier.fillMaxSize().clipToBounds()) {
        for (item in items) {
            val spec = item.spec
            val instance = engine.instance(spec)
            val raw =
                layout.nativeRect(item.key)
                    ?: if (instance != null) {
                        OwlTileGeometry.initialRect(spec, instance, grid, density.density)
                    } else {
                        legacyRect(layout.cell(item.key), grid)
                            ?: OwlTileGeometry.initialRect(spec, null, grid, density.density)
                    }
            val minW = max(absoluteMin, (item.minSize.first * grid.cellW).roundToInt()).coerceAtMost(grid.width)
            val minH = max(absoluteMin, (item.minSize.second * grid.cellH).roundToInt()).coerceAtMost(grid.height)
            val at = contained(raw, grid, minW, minH)
            key(item.key) {
                val current = rememberUpdatedState(at)
                val isSelected = selected == item.key

                Box(
                    Modifier
                        .offset { IntOffset(at.x, at.y) }
                        .size(width = with(density) { at.w.toDp() }, height = with(density) { at.h.toDp() })
                        .zIndex(if (isSelected) 2f else 0f)
                        .then(if (isSelected) Modifier.systemGestureExclusion() else Modifier)
                        .pointerInput("owl-tile-${item.key}") {
                            awaitEachGesture {
                                val down =
                                    awaitFirstDown(
                                        requireUnconsumed = false,
                                        pass = PointerEventPass.Initial,
                                    )
                                val id = down.id
                                val start = down.position
                                var last = start
                                var movedEarly = false

                                // Hold to select; a move before the timeout is an immediate drag.
                                val longPressed =
                                    withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                        while (true) {
                                            val change =
                                                awaitPointerEvent(PointerEventPass.Initial)
                                                    .changes
                                                    .firstOrNull { it.id == id }
                                                    ?: return@withTimeoutOrNull false
                                            if (!change.pressed) return@withTimeoutOrNull false
                                            last = change.position
                                            if ((last - start).getDistance() > viewConfiguration.touchSlop) {
                                                movedEarly = true
                                                change.consume()
                                                return@withTimeoutOrNull false
                                            }
                                        }
                                        false
                                    } == null

                                if (!longPressed && !movedEarly) {
                                    if (selected == item.key) selected = null else engine.tap(spec)
                                    return@awaitEachGesture
                                }

                                selected = item.key
                                if (longPressed) haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                                val origin = current.value
                                val local = start - Offset(origin.x.toFloat(), origin.y.toFloat())
                                val edges =
                                    tileEdgesAt(
                                        IntRect(0, 0, origin.w, origin.h),
                                        local,
                                        grab,
                                        sideGrab,
                                    )
                                var total = last - start
                                var previous = last
                                var didDrag = movedEarly

                                if (movedEarly) {
                                    val next =
                                        if (edges.any) {
                                            resized(origin, edges, total.x, total.y, grid, minW, minH)
                                        } else {
                                            moved(origin, total.x, total.y, grid)
                                        }
                                    layout.setNativeRect(item.key, next)
                                }

                                while (true) {
                                    val change =
                                        awaitPointerEvent(PointerEventPass.Initial)
                                            .changes
                                            .firstOrNull { it.id == id }
                                            ?: break
                                    if (!change.pressed) break
                                    val delta = change.position - previous
                                    previous = change.position
                                    if (!didDrag && (change.position - start).getDistance() <= viewConfiguration.touchSlop) {
                                        continue
                                    }
                                    didDrag = true
                                    change.consume()
                                    total += delta
                                    val next =
                                        if (edges.any) {
                                            resized(origin, edges, total.x, total.y, grid, minW, minH)
                                        } else {
                                            moved(origin, total.x, total.y, grid)
                                        }
                                    layout.setNativeRect(item.key, next)
                                }

                                if (didDrag) layout.saveNativeRects()
                                if (longPressed && !didDrag) {
                                    showNativeWidgetMenu(
                                        context = context,
                                        anchor = anchor,
                                        rect = current.value,
                                        editLabel = editLabel,
                                        removeLabel = removeLabel,
                                        onEdit = {
                                            val agent = runtime.agents.default
                                            if (agent == null) {
                                                runtime.showCodingAgents()
                                            } else {
                                                runtime.terminal(
                                                    editWidgetCommand(agent.bin, spec),
                                                    "Edit ${spec.name}",
                                                    agent,
                                                )
                                            }
                                        },
                                        onRemove = { engine.removeWidget(spec) },
                                        onDismiss = { selected = null },
                                    )
                                }
                            }
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
                            Box(
                                Modifier
                                    .align(align)
                                    .size(9.dp)
                                    .background(Palette.accent, CircleShape),
                            )
                        }
                    }
                }
            }
        }
    }
}
