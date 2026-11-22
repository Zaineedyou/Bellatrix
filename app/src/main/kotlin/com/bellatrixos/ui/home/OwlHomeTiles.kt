package com.bellatrixos.ui.home

import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
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

/**
 * Only the bottom-right corner resizes. Dragging it left/up shrinks, right/down grows, within the
 * minimum size and the native home bounds. [p] is tile-local.
 */
private fun bottomRightGrabbed(
    rect: PixelRect,
    p: Offset,
    grip: Int,
): Boolean {
    val gx = grip.coerceAtMost(maxOf(1, rect.w / 2))
    val gy = grip.coerceAtMost(maxOf(1, rect.h / 2))
    return p.x >= rect.w - gx && p.y >= rect.h - gy
}

private fun resizedFromBottomRight(
    start: PixelRect,
    dx: Float,
    dy: Float,
    grid: NativeGrid,
    minW: Int,
    minH: Int,
): PixelRect {
    val right = (start.x + start.w + dx).roundToInt().coerceIn(start.x + minW, grid.width)
    val bottom = (start.y + start.h + dy).roundToInt().coerceIn(start.y + minH, grid.height)
    return contained(
        PixelRect(start.x, start.y, right - start.x, bottom - start.y),
        grid,
        minW,
        minH,
    )
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
 * `size`, `fill`, `offset`, `anchor`), not from a grid. Drag the body to move; drag the one bottom-right
 * grip in any direction to resize freely. Tiles may overlap. Holding a tile opens Bellatrix's own
 * native popup window with Edit (a Coding Agent on the `.owl` file) and Remove. Geometry is clipped to
 * the native home view, with no visible boundary line.
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

    val grip = with(LocalDensity.current) { 32.dp.toPx() }.roundToInt()
    val absoluteMin = with(LocalDensity.current) { 56.dp.toPx() }.roundToInt()

    // No visible border: clipToBounds is the hard guarantee that no tile escapes into the taskbar.
    Box(Modifier.fillMaxSize().clipToBounds()) {
        for (item in items) {
            val spec = item.spec
            val instance = engine.instance(spec)
            val saved = layout.nativeRect(item.key)
            val raw =
                saved
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
                // Live drag geometry: read once in the layout modifier, so a move doesn't
                // recompose every tile (or the OWL scene) on every pointer event.
                val live = remember { mutableStateOf<PixelRect?>(null) }
                val isSelected = selected == item.key

                LaunchedEffect(saved) {
                    if (live.value != null && live.value == saved) live.value = null
                }

                Box(
                    Modifier
                        // Size and position are applied in the same layout pass. Two separate state
                        // reads (offset + size) could land on different frames and look like a jerk.
                        .layout { measurable, _ ->
                            val r = live.value ?: current.value
                            val placeable = measurable.measure(Constraints.fixed(r.w, r.h))
                            layout(r.w, r.h) { placeable.place(r.x, r.y) }
                        }
                        .zIndex(if (isSelected) 2f else 0f)
                        .systemGestureExclusion()
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

                                val origin = live.value ?: current.value
                                // down.position is already local to this tile's pointerInput node. Subtracting
                                // the tile's screen origin made every grip miss unless the tile sat at 0,0.
                                val resizing = bottomRightGrabbed(origin, start, grip)
                                var total = last - start
                                var previous = last
                                var didDrag = movedEarly

                                fun apply(dx: Float, dy: Float) {
                                    val next =
                                        if (resizing) {
                                            resizedFromBottomRight(origin, dx, dy, grid, minW, minH)
                                        } else {
                                            moved(origin, dx, dy, grid)
                                        }
                                    live.value = next
                                }

                                if (movedEarly) apply(total.x, total.y)

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
                                    apply(total.x, total.y)
                                }

                                if (didDrag) {
                                    val final = live.value ?: origin
                                    layout.setNativeRect(item.key, final)
                                    layout.saveNativeRects()
                                }
                                if (longPressed && !didDrag && !resizing) {
                                    showNativeWidgetMenu(
                                        context = context,
                                        anchor = anchor,
                                        rect = finalRect(live.value, current.value),
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

                    if (isSelected) Box(Modifier.fillMaxSize().border(1.dp, Palette.accent))

                    // The only grip: bottom-right. Drag it any direction to resize.
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .background(Palette.accent.copy(alpha = if (isSelected) 1f else 0.55f))
                            .border(1.dp, Palette.popup),
                    )
                }
            }
        }
    }
}

private fun finalRect(
    live: PixelRect?,
    saved: PixelRect,
) = live ?: saved
