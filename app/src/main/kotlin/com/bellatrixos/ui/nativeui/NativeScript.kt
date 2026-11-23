package com.bellatrixos.ui.nativeui

import org.luaj.vm2.LuaError
import org.luaj.vm2.lib.jse.JsePlatform

/**
 * The Lua half of the native UI files. It stays free of Android references so
 * JVM tests can compile shipped and edited scripts. Execution and the native
 * API live in [NativeUiApi]; this object only owns the file contract.
 */
object NativeScript {
    const val FILE = "ui.lua"
    const val VERSION = 1

    /** Neutral on first install: no rule or hook is active until an agent edits this file. */
    fun defaults(): String =
        """
        -- Bellatrix native UI script.
        --
        -- This runs inside Bellatrix's one App.runtime. It only touches native
        -- Views already on screen; it does not start another launcher, Activity,
        -- home, drawer or Settings window. look.toml owns declarative style,
        -- this file owns behaviour hooks and actions. README.md lists the API.
        -- Guest path: ~/.config/bellatrix/native-ui/ui.lua
        version = 1

        -- Active examples are in examples/taskbar.lua and examples/settings.lua.
        -- Copy what you need here, or call the API directly. For example:
        --
        -- nativeui:onGesture("id:taskBar", "taskbar", function(view, action, x, y)
        --     if action == "down" then view:alpha(0.85)
        --     elseif action == "up" or action == "cancel" then view:alpha(1.0) end
        -- end)
        --
        -- nativeui:taskbar("pin", "UISettings")
        -- nativeui:settings("1")
        -- nativeui:setBackground("class:UISettingsCard", "settings", "#202233", 8)
        """.trimIndent() + "\n"

    /** Compiles without executing, so no native API is needed. */
    fun check(source: String): List<String> {
        if (source.isBlank()) return emptyList()
        return try {
            val globals = JsePlatform.standardGlobals()
            globals.load(source, FILE)
            emptyList()
        } catch (e: LuaError) {
            listOf("ui.lua ${e.message ?: "could not be compiled"}")
        } catch (e: Exception) {
            listOf("ui.lua ${e.message ?: "could not be read"}")
        }
    }
}
