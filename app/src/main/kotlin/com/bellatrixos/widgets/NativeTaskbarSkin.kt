package com.bellatrixos.widgets

import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.bellatrixos.R
import com.bellatrixos.Runtime
import com.bellatrixos.widgets.lang.Builtins
import com.bellatrixos.widgets.lang.Str
import com.bellatrixos.widgets.lang.Value
import com.bellatrixos.widgets.lang.WNode
import com.bellatrixos.widgets.lang.num
import java.util.WeakHashMap
import kotlin.math.roundToInt

/**
 * Applies `taskbar.owl` to the native taskbar views themselves.
 *
 * There is no second taskbar and no overlay View: this reads the compiled panel's root `Box` and changes the
 * real taskbar's background, corners, border, padding, item gaps and icon tint. A transparent file restores
 * Bellatrix's shipped taskbar exactly. The home Compose surface watches the file list and calls [apply], so
 * saving `taskbar.owl` updates the native bar within a second through the one [App.runtime].
 */
object NativeTaskbarSkin {
    private class Original(
        val background: Drawable?,
        val padding: IntArray,
        val margins: Map<View, Int>,
        val filters: Map<ImageView, ColorFilter?>,
    )

    private val originals = WeakHashMap<ViewGroup, Original>()

    fun apply(
        activity: AppCompatActivity,
        runtime: Runtime,
    ) {
        val bar = activity.findViewById<ViewGroup>(R.id.taskBar) ?: return
        val list = activity.findViewById<LinearLayout>(R.id.taskBarList)
        val buttons = taskbarButtons(bar)
        val original =
            originals.getOrPut(bar) {
                Original(
                    background = bar.background,
                    padding = intArrayOf(bar.paddingLeft, bar.paddingTop, bar.paddingRight, bar.paddingBottom),
                    margins =
                        list?.let { l ->
                            (0 until l.childCount).mapNotNull { i ->
                                val child = l.getChildAt(i)
                                (child.layoutParams as? LinearLayout.LayoutParams)?.let { child to it.marginStart }
                            }.toMap()
                        } ?: emptyMap(),
                    filters = buttons.associate { it.u to it.u.colorFilter },
                )
            }

        val spec = runtime.widgets.specs.firstOrNull { it.file.name == WidgetEngine.TASKBAR_BAR }
        val program = spec?.program
        val instance = program?.let { runtime.widgets.barInstance(WidgetEngine.TASKBAR_BAR) }
        val root =
            program?.screen
                ?.asSequence()
                ?.elements()
                ?.firstOrNull { it.kind == "Box" }

        if (program == null || instance == null || root == null) {
            restore(bar, list, buttons, original)
            return
        }

        val bg = valueOf(program, instance, root, "background")
        val color = bg?.let { Builtins.argb(it, instance) }
        val transparent = bg == null || bg is Str && bg.v.equals("transparent", true) || color == 0 || color == null
        if (transparent) {
            bar.background = original.background
        } else {
            val radius = valueOf(program, instance, root, "radius")?.num()?.toFloat() ?: 0f
            val strokeColor = valueOf(program, instance, root, "borderColor")?.let { Builtins.argb(it, instance) }
            val strokeWidth = valueOf(program, instance, root, "border")?.num()?.toFloat() ?: 0f
            bar.background =
                GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(color)
                    if (radius > 0f) cornerRadii = floatArrayOf(radius, radius, radius, radius, radius, radius, radius, radius)
                    if (strokeWidth > 0f && strokeColor != null) setStroke(strokeWidth.roundToInt(), strokeColor)
                }
        }

        val pad = valueOf(program, instance, root, "padding")?.num()?.roundToInt() ?: 0
        bar.setPadding(
            original.padding[0] + pad,
            original.padding[1] + pad,
            original.padding[2] + pad,
            original.padding[3] + pad,
        )

        // `gap` belongs to Flex/Row/Column, not Box, so read it from the first visible layout child.
        val gap =
            program.screen
                ?.asSequence()
                ?.elements()
                ?.firstOrNull { node -> node.kind in setOf("Flex", "Row", "Column") && valueOf(program, instance, node, "visible")?.truthy != false }
                ?.let { node -> valueOf(program, instance, node, "gap")?.num()?.roundToInt() }
                ?: 0
        if (list != null) {
            for (i in 0 until list.childCount) {
                val child = list.getChildAt(i)
                val params = child.layoutParams as? LinearLayout.LayoutParams ?: continue
                // With no OWL gap, leave every native margin alone; with one, keep the first item's margin.
                params.marginStart =
                    if (gap > 0 && i > 0) gap else original.margins[child] ?: params.marginStart
                child.layoutParams = params
            }
        }

        // A visible Text node's color tints the native taskbar icons; a hidden one leaves them alone.
        val iconColor =
            program.screen
                ?.asSequence()
                ?.elements()
                ?.firstOrNull { node -> node.kind == "Text" && valueOf(program, instance, node, "visible")?.truthy != false }
                ?.let { node -> valueOf(program, instance, node, "color")?.let { Builtins.argb(it, instance) } }
        for (button in buttons) {
            if (iconColor == null) button.u.colorFilter = original.filters[button.u]
            else button.u.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        }
    }

    private fun restore(
        bar: ViewGroup,
        list: LinearLayout?,
        buttons: List<TaskbarBtn>,
        original: Original,
    ) {
        bar.background = original.background
        bar.setPadding(original.padding[0], original.padding[1], original.padding[2], original.padding[3])
        if (list != null) {
            for (i in 0 until list.childCount) {
                val child = list.getChildAt(i)
                val params = child.layoutParams as? LinearLayout.LayoutParams ?: continue
                params.marginStart = original.margins[child] ?: 0
                child.layoutParams = params
            }
        }
        for (button in buttons) button.u.colorFilter = original.filters[button.u]
    }

    private fun valueOf(
        program: com.bellatrixos.widgets.lang.Program,
        instance: WidgetInstance,
        node: WNode.Element,
        key: String,
    ): Value? =
        node.props[key]?.let { ev -> runCatching { program.eval(ev, instance.root) }.getOrNull() }

    private fun WNode.elements(): Sequence<WNode.Element> =
        when (this) {
            is WNode.Element -> sequenceOf(this) + children.asSequence().elements()
            is WNode.If -> (then.asSequence() + orElse.asSequence()).elements()
            is WNode.For -> body.asSequence().elements()
            is WNode.Lets -> children.asSequence().elements()
            is WNode.Use -> body.asSequence().elements()
            is WNode.Draw -> children?.asSequence()?.elements() ?: emptySequence()
        }

    private fun Sequence<WNode>.elements(): Sequence<WNode.Element> = flatMap { it.elements() }

    private fun taskbarButtons(root: View): List<TaskbarBtn> {
        val out = ArrayList<TaskbarBtn>()
        fun walk(view: View) {
            if (view is TaskbarBtn) out.add(view)
            if (view is ViewGroup) for (i in 0 until view.childCount) walk(view.getChildAt(i))
        }
        walk(root)
        return out
    }
}
