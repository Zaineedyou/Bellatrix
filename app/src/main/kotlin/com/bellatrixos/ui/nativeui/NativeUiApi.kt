package com.bellatrixos.ui.nativeui

import android.content.Context
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.bellatrixos.taskbar.TaskBarStore
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * The `nativeui` global exposed to ~/.config/bellatrix/native-ui/ui.lua.
 * Every call lands on an existing Bellatrix View, adapter, activity or store;
 * none of them starts a parallel launcher or a replacement screen.
 */
class NativeUiApi(
    private val context: Context,
    private val taskBar: TaskBarStore,
) {
    private val events = setOf("click", "long-click", "gesture")

    /** Adds a native rule from a Lua table, using the same properties as look.toml. */
    fun style(
        match: String,
        surface: String,
        props: LuaTable,
    ) {
        val properties = linkedMapOf<String, Any>()
        for (key in props.keys()) {
            val name = key.tojstring() ?: continue
            require(name in NativeUiSpec.PROPERTY_KEYS) { "$name is not a native property" }
            val value = toValue(props.get(key)) ?: continue
            properties[name] = value
        }
        val rule =
            NativeUiSpec.Rule(
                setOf(surface),
                listOf(requireSelector(match)),
                properties,
            )
        NativeUiLook.addScriptRule(rule)
    }

    /** Returns Lua handles for every real View that matches, in current view order. */
    fun views(
        match: String,
        surface: String,
    ): LuaValue {
        val selector = requireSelector(match)
        val handles =
            NativeUiLook.findViews(selector, surface).map { view ->
                CoerceJavaToLua.coerce(LuaView(view, NativeUiLook.surfaceOfView(view)))
            }
        return LuaValue.listOf(handles.toTypedArray())
    }

    fun view(
        match: String,
        surface: String,
    ): LuaValue = views(match, surface).get(1)

    fun on(
        event: String,
        match: String,
        surface: String,
        fn: LuaFunction,
    ) {
        val name = event.lowercase()
        require(name in events) { "event must be click, long-click or gesture" }
        NativeUiLook.setHook(name, requireSelector(match), surface, fn)
    }

    fun onClick(
        match: String,
        surface: String,
        fn: LuaFunction,
    ) = on("click", match, surface, fn)

    fun onLongClick(
        match: String,
        surface: String,
        fn: LuaFunction,
    ) = on("long-click", match, surface, fn)

    fun onGesture(
        match: String,
        surface: String,
        fn: LuaFunction,
    ) = on("gesture", match, surface, fn)

    fun setText(
        match: String,
        surface: String,
        text: String,
    ) = each(match, surface) { it.setText(text) }

    fun setHint(
        match: String,
        surface: String,
        hint: String,
    ) = each(match, surface) { it.setHint(hint) }

    @JvmOverloads
    fun setBackground(
        match: String,
        surface: String,
        color: String,
        radius: Double = 0.0,
    ) = each(match, surface) { it.background(color, radius) }

    fun setPadding(
        match: String,
        surface: String,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) = each(match, surface) { it.padding(left, top, right, bottom) }

    fun setVisibility(
        match: String,
        surface: String,
        value: String,
    ) = each(match, surface) { it.visibility(value) }

    fun adapter(
        match: String,
        surface: String,
    ): LuaAdapter {
        val host =
            NativeUiLook.findViews(requireSelector(match), surface)
                .firstOrNull { it is android.widget.AdapterView<*> || it is androidx.recyclerview.widget.RecyclerView }
                ?: throw IllegalArgumentException("no native adapter view matched \"$match\" on $surface")
        return LuaAdapter(host)
    }

    /** action: pin, unpin, refresh, or launch a native window key. */
    @JvmOverloads
    fun taskbar(
        action: String,
        arg: String = "",
    ) {
        when (action.lowercase()) {
            "pin" -> taskBar.addPin(arg)
            "unpin" -> taskBar.removePin(arg)
            "refresh" -> taskBar.refresh()
            "launch" -> launch(arg)
            else -> throw IllegalArgumentException("taskbar action must be pin, unpin, refresh or launch")
        }
    }

    /** Opens any existing Bellatrix window key through the launcher's own window manager. */
    @JvmOverloads
    fun launch(
        key: String,
        data: String = "",
    ) {
        NativeUiLook.whenMainReady { activity -> activity.c0(key, data.takeIf { it.isNotBlank() }) }
    }

    /** Opens an existing Bellatrix Settings window; no new Settings Activity is created. */
    @JvmOverloads
    fun settings(page: String = "") {
        NativeUiLook.whenMainReady { activity -> activity.c0("UISettings", page.takeIf { it.isNotBlank() }) }
    }

    /** action: title, background, or height on the current native window decoration. */
    fun window(
        action: String,
        value: String,
    ) {
        when (action.lowercase()) {
            "title" -> {
                val label =
                    NativeUiLook.findViews(requireSelector("class:TextView"), "window").firstOrNull()
                        ?: return
                require(label is TextView) { "window title is not a TextView" }
                NativeUiLook.rememberDirect(label)
                label.text = value
            }
            "background" -> setBackground("id:title_bar", "window", value)
            "height" -> {
                val px = value.toIntOrNull() ?: throw IllegalArgumentException("window height takes pixels")
                NativeUiLook.findViews(requireSelector("id:title_bar"), "window").forEach { title ->
                    NativeUiLook.rememberDirect(title)
                    val params = title.layoutParams ?: return@forEach
                    params.height = px
                    title.layoutParams = params
                }
            }
            else -> throw IllegalArgumentException("window action must be title, background or height")
        }
    }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    fun log(message: String) {
        Log.i("NativeUi", message)
    }

    fun pins(): LuaValue =
        LuaValue.listOf(taskBar.pins().map { LuaValue.valueOf(it) }.toTypedArray())

    private fun each(
        match: String,
        surface: String,
        block: (LuaView) -> Unit,
    ) {
        val selector = requireSelector(match)
        NativeUiLook.findViews(selector, surface).forEach { block(LuaView(it, NativeUiLook.surfaceOfView(it))) }
    }

    private fun requireSelector(match: String) =
        NativeUiSpec.selector(match)
            ?: throw IllegalArgumentException("match must start with id:, class: or text: (or be all)")

    /** Lua tables can be lists (padding, margin) or maps (style properties). */
    private fun toValue(value: LuaValue): Any? =
        when {
            value.isnil() -> null
            value.isboolean() -> value.toboolean()
            value.isnumber() ->
                if (value.todouble() == value.toint().toDouble()) value.toint() else value.todouble()
            value.istable() -> {
                val table = value.checktable()
                val list = mutableListOf<Any>()
                var i = 1
                while (true) {
                    val item = table.get(i)
                    if (item.isnil()) break
                    toValue(item)?.let { list += it }
                    i++
                }
                if (list.isNotEmpty()) {
                    list
                } else {
                    val map = linkedMapOf<String, Any>()
                    var key = LuaValue.NIL
                    while (true) {
                        val pair = table.next(key)
                        key = pair.arg1()
                        if (key.isnil()) break
                        val name = key.tojstring() ?: continue
                        toValue(pair.arg(2))?.let { map[name] = it }
                    }
                    map
                }
            }
            else -> value.tojstring()
        }
}
