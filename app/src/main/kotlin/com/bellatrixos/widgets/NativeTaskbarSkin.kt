package com.bellatrixos.widgets

import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.bellatrixos.R
import com.bellatrixos.Runtime
import com.bellatrixos.widgets.lang.Builtins
import com.bellatrixos.widgets.lang.Lst
import com.bellatrixos.widgets.lang.Str
import com.bellatrixos.widgets.lang.Value
import com.bellatrixos.widgets.lang.WNode
import com.bellatrixos.widgets.lang.num
import com.bellatrixos.widgets.runtime.WidgetInstance
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
        /** Start margins of the list's children, by index: no View is kept here. */
        val margins: IntArray,
        /** Color filters of the buttons, by index. */
        val filters: List<ColorFilter?>,
    )

    /** An OWL dp length, clamped the way the renderer clamps its insets. */
    private const val MAX_DP = 4000.0

    /**
     * Kept on the bar itself, not in a static map: a value holding the Views it
     * came from kept a dead activity's whole view tree reachable through its key.
     */
    private fun originalOf(
        bar: ViewGroup,
        list: LinearLayout?,
        buttons: List<TaskbarBtn>,
    ): Original {
        (bar.getTag(R.id.taskBar) as? Original)?.let { return it }
        return Original(
            background = bar.background,
            padding = intArrayOf(bar.paddingLeft, bar.paddingTop, bar.paddingRight, bar.paddingBottom),
            margins =
                list?.let { l ->
                    IntArray(l.childCount) { i -> (l.getChildAt(i).layoutParams as? LinearLayout.LayoutParams)?.marginStart ?: 0 }
                } ?: IntArray(0),
            filters = buttons.map { it.u.colorFilter },
        ).also { bar.setTag(R.id.taskBar, it) }
    }

    /** OWL lengths are dp; the native taskbar takes px. */
    private fun px(
        activity: AppCompatActivity,
        dp: Double?,
    ): Int {
        // Same rule as the renderer's inset(): negative and NaN are 0, large positive values clamp.
        val value = if (dp == null || dp.isNaN()) 0.0 else if (dp >= 0) dp.coerceAtMost(MAX_DP) else 0.0
        if (value == 0.0) return 0
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), activity.resources.displayMetrics).roundToInt()
    }

    /** The same [h,v] and [l,t,r,b] padding the renderer accepts, converted to px. */
    private fun padding(
        activity: AppCompatActivity,
        instance: WidgetInstance,
        root: WNode.Element,
    ): IntArray {
        val v = valueOf(instance, root, "padding")
        val items = (v as? Lst)?.items?.map { it.num() ?: 0.0 }
        val sides =
            when {
                v?.num() != null -> DoubleArray(4) { v.num()!! }
                items?.size == 2 -> doubleArrayOf(items[0], items[1], items[0], items[1])
                items?.size == 4 -> doubleArrayOf(items[0], items[1], items[2], items[3])
                else -> return IntArray(4)
            }
        return IntArray(4) { px(activity, sides[it]) }
    }

    fun apply(
        activity: AppCompatActivity,
        runtime: Runtime,
    ) {
        val bar = activity.findViewById<ViewGroup>(R.id.taskBar) ?: return
        val list = activity.findViewById<LinearLayout>(R.id.taskBarList)
        val buttons = taskbarButtons(bar)
        val original = originalOf(bar, list, buttons)

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

        val bg = valueOf(instance, root, "background")
        val color = bg?.let { Builtins.argb(it, instance) }
        val transparent = bg == null || bg is Str && bg.v.equals("transparent", true) || color == 0 || color == null
        if (transparent) {
            bar.background = original.background
        } else {
            val radius = px(activity, valueOf(instance, root, "radius")?.num()).toFloat()
            val strokeColor = valueOf(instance, root, "borderColor")?.let { Builtins.argb(it, instance) }
            val strokeWidth = px(activity, valueOf(instance, root, "border")?.num())
            bar.background =
                GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(color)
                    if (radius > 0f) cornerRadii = floatArrayOf(radius, radius, radius, radius, radius, radius, radius, radius)
                    if (strokeWidth > 0 && strokeColor != null) setStroke(strokeWidth, strokeColor)
                }
        }

        val pad = padding(activity, instance, root)
        bar.setPadding(
            original.padding[0] + pad[0],
            original.padding[1] + pad[1],
            original.padding[2] + pad[2],
            original.padding[3] + pad[3],
        )

        // `gap` belongs to Flex/Row/Column, not Box, so read it from the first visible layout child.
        val gap =
            program.screen
                ?.asSequence()
                ?.elements()
                ?.firstOrNull { node -> node.kind in setOf("Flex", "Row", "Column") && valueOf(instance, node, "visible")?.truthy != false }
                ?.let { node -> px(activity, valueOf(instance, node, "gap")?.num()) }
                ?: 0
        if (list != null) {
            for (i in 0 until list.childCount) {
                val child = list.getChildAt(i)
                val params = child.layoutParams as? LinearLayout.LayoutParams ?: continue
                // With no OWL gap, leave every native margin alone; with one, keep the first item's margin.
                params.marginStart =
                    if (gap > 0 && i > 0) gap else original.margins.getOrElse(i) { params.marginStart }
                child.layoutParams = params
            }
        }

        // A visible Text node's color tints the native taskbar icons; a hidden one leaves them alone.
        val iconColor =
            program.screen
                ?.asSequence()
                ?.elements()
                ?.firstOrNull { node -> node.kind == "Text" && valueOf(instance, node, "visible")?.truthy != false }
                ?.let { node -> valueOf(instance, node, "color")?.let { Builtins.argb(it, instance) } }
        for ((i, button) in buttons.withIndex()) {
            if (iconColor == null) button.u.colorFilter = original.filters.getOrNull(i)
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
                params.marginStart = original.margins.getOrElse(i) { params.marginStart }
                child.layoutParams = params
            }
        }
        for ((i, button) in buttons.withIndex()) button.u.colorFilter = original.filters.getOrNull(i)
    }

    private fun valueOf(
        instance: WidgetInstance,
        node: WNode.Element,
        key: String,
    ): Value? =
        node.props[key]?.let { ev -> runCatching { instance.eval(ev, instance.root, "taskbar $key") }.getOrNull() }

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
