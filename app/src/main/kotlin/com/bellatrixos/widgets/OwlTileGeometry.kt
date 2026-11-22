package com.bellatrixos.widgets

import com.bellatrixos.ui.home.NativeGrid
import com.bellatrixos.widgets.lang.Lst
import com.bellatrixos.widgets.lang.WNode
import com.bellatrixos.widgets.lang.num
import com.bellatrixos.widgets.runtime.WidgetInstance
import kotlin.math.roundToInt

/**
 * Turns the declarative root of a widget (Box/Flex/Anchor and their width, height, size, fill,
 * offset and anchor props) into the initial pixel rectangle of its tile on Bellatrix's native home.
 *
 * This is only the starting geometry: a later drag/resize is kept in [WidgetLayout] as a free
 * [PixelRect]. Nothing here assigns grid cells, checks neighbours or snaps to them — OWL tiles may
 * overlap and start wherever their file says.
 */
internal object OwlTileGeometry {
    private val containers = setOf("Box", "Anchor", "Flex", "Row", "Column", "Flow")

    /** A grid measured before the first layout pass can be empty; coerceIn(1, 0) throws. */
    private fun fit(
        value: Int,
        limit: Int,
    ): Int = if (limit > 0) value.coerceIn(1, limit) else 1

    fun initialRect(
        spec: WidgetSpec,
        instance: WidgetInstance?,
        grid: NativeGrid,
        density: Float,
    ): PixelRect {
        val root =
            spec.program?.root?.let(::firstContainer)
                ?: return fromCells(spec, grid, density)

        fun value(key: String) =
            instance?.let { inst ->
                root.props[key]?.let { ev -> runCatching { inst.eval(ev, inst.root, "tile $key") }.getOrNull() }
            }

        fun px(key: String): Int? =
            value(key)?.num()?.takeIf { it.isFinite() && it > 0f }?.let { (it * density).roundToInt() }

        val fill = value("fill")
        val (cellsW, cellsH) = spec.size ?: spec.minSize
        var w = px("width") ?: px("size") ?: (cellsW * grid.cellW - grid.gapX).roundToInt()
        var h = px("height") ?: px("size") ?: (cellsH * grid.cellH - grid.gapY).roundToInt()
        when {
            fill?.toString() == "height" -> h = grid.height
            fill?.toString() == "width" -> w = grid.width
            fill?.truthy == true -> {
                w = grid.width
                h = grid.height
            }
        }
        w = fit(w, grid.width)
        h = fit(h, grid.height)

        val gridW = grid.width.coerceAtLeast(1)
        val gridH = grid.height.coerceAtLeast(1)
        val anchor = (value("anchor") ?: value("align"))?.toString().orEmpty()
        var x = when (anchor) {
            "end", "topEnd", "bottomEnd" -> gridW - w
            "center", "top", "bottom" -> (gridW - w) / 2
            else -> 0
        }
        var y = when (anchor) {
            "bottom", "bottomStart", "bottomEnd" -> gridH - h
            "center", "start", "end" -> (gridH - h) / 2
            else -> 0
        }
        val offset = value("offset") as? Lst
        x += offset?.items?.getOrNull(0)?.num()?.takeIf { it.isFinite() }?.let { (it * density).roundToInt() } ?: 0
        y += offset?.items?.getOrNull(1)?.num()?.takeIf { it.isFinite() }?.let { (it * density).roundToInt() } ?: 0
        return PixelRect(x, y, w, h)
    }

    /** A widget with no geometry in its root falls back to its declared cell size, or a small cascade for new files. */
    private fun fromCells(
        spec: WidgetSpec,
        grid: NativeGrid,
        density: Float,
    ): PixelRect {
        val (cellsW, cellsH) = spec.size ?: spec.minSize
        val w = fit((cellsW * grid.cellW - grid.gapX).roundToInt(), grid.width)
        val h = fit((cellsH * grid.cellH - grid.gapY).roundToInt(), grid.height)
        val step = ((spec.order % 8).coerceAtLeast(0) * 16 * density).roundToInt()
        return PixelRect(step, step, w, h)
    }

    private fun firstContainer(nodes: List<WNode>): WNode.Element? {
        for (node in nodes) {
            when (node) {
                is WNode.Element -> if (node.kind in containers) return node
                is WNode.If -> {
                    firstContainer(node.then)?.let { return it }
                    firstContainer(node.orElse)?.let { return it }
                }
                is WNode.For -> {
                    firstContainer(node.body)?.let { return it }
                }
                is WNode.Lets -> {
                    firstContainer(node.children)?.let { return it }
                }
                is WNode.Use -> {
                    firstContainer(node.body)?.let { return it }
                }
                is WNode.Draw -> Unit
            }
        }
        return null
    }
}
