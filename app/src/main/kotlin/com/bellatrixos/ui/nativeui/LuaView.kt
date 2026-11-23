package com.bellatrixos.ui.nativeui

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.luaj.vm2.LuaFunction

/**
 * One existing Bellatrix View as a Lua handle. Every method edits that View
 * in place; it never creates a replacement panel or a second renderer.
 */
class LuaView(
    private val view: View,
    private val surfaceName: String,
) {
    /** The underlying native View, for Lua code that needs an adapter row. */
    fun raw(): View = view

    fun id(): String = NativeUiLook.idOf(view) ?: ""

    fun surface(): String = surfaceName

    fun className(): String = view::class.java.name

    fun text(): String = (view as? TextView)?.text?.toString() ?: ""

    fun setText(value: String) {
        require(view is TextView) { "view is not a TextView" }
        NativeUiLook.rememberDirect(view)
        view.text = value
    }

    fun setHint(value: String) {
        require(view is TextView) { "view is not a TextView" }
        NativeUiLook.rememberDirect(view)
        view.hint = value
    }

    @JvmOverloads
    fun background(
        color: String,
        radius: Double = 0.0,
    ) {
        val argb =
            NativeUiSpec.color(color, emptyMap())
                ?: throw IllegalArgumentException("background takes a #rgb, #rrggbb or #aarrggbb color")
        NativeUiLook.rememberDirect(view)
        val drawable = GradientDrawable()
        drawable.setColor(argb.toInt())
        if (radius > 0) drawable.cornerRadius = radius.toFloat()
        view.background = drawable
        view.backgroundTintList = null
    }

    fun border(
        color: String,
        width: Int,
    ) {
        val argb =
            NativeUiSpec.color(color, emptyMap())
                ?: throw IllegalArgumentException("border takes a hex color")
        NativeUiLook.rememberDirect(view)
        val drawable =
            (view.background as? GradientDrawable) ?: GradientDrawable().also { view.background = it }
        drawable.setStroke(width, argb.toInt())
    }

    fun padding(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        NativeUiLook.rememberDirect(view)
        view.setPadding(left, top, right, bottom)
    }

    fun width(px: Int) {
        NativeUiLook.rememberDirect(view)
        val params = view.layoutParams ?: return
        params.width = px
        view.layoutParams = params
    }

    fun height(px: Int) {
        NativeUiLook.rememberDirect(view)
        val params = view.layoutParams ?: return
        params.height = px
        view.layoutParams = params
    }

    fun visibility(value: String) {
        NativeUiLook.rememberDirect(view)
        view.visibility =
            when (value.lowercase()) {
                "invisible" -> View.INVISIBLE
                "gone" -> View.GONE
                else -> View.VISIBLE
            }
    }

    fun alpha(value: Double) {
        NativeUiLook.rememberDirect(view)
        view.alpha = value.coerceIn(0.0, 1.0).toFloat()
    }

    fun iconSize(px: Int) {
        require(view is ImageView) { "view is not an ImageView" }
        NativeUiLook.rememberDirect(view)
        val params = view.layoutParams ?: return
        params.width = px
        params.height = px
        view.layoutParams = params
    }

    fun tint(color: String) {
        require(view is ImageView) { "view is not an ImageView" }
        val argb =
            NativeUiSpec.color(color, emptyMap())
                ?: throw IllegalArgumentException("tint takes a hex color")
        NativeUiLook.rememberDirect(view)
        view.imageTintList = null
        view.setColorFilter(argb.toInt(), android.graphics.PorterDuff.Mode.SRC_IN)
    }

    fun onClick(fn: LuaFunction) = NativeUiLook.setHook("click", selector(), surfaceName, fn)

    fun onLongClick(fn: LuaFunction) = NativeUiLook.setHook("long-click", selector(), surfaceName, fn)

    fun onGesture(fn: LuaFunction) = NativeUiLook.setHook("gesture", selector(), surfaceName, fn)

    private fun selector(): String = id().takeIf { it.isNotEmpty() }?.let { "id:$it" } ?: "class:${view::class.java.name}"
}
