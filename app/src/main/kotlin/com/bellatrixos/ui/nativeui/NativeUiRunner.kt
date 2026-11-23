package com.bellatrixos.ui.nativeui

import org.luaj.vm2.LuaError
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform

/**
 * Executes ui.lua inside this process and clears the previous script's rules
 * and hooks first, so a deleted or broken script cannot leave stale behaviour.
 */
class NativeUiRunner(
    private val api: NativeUiApi,
    private val report: (String) -> Unit,
) {
    fun run(source: String): List<String> {
        NativeUiLook.clearScript()
        NativeUiLook.onScriptError = report
        if (source.isBlank()) return emptyList()
        val problems = NativeScript.check(source).toMutableList()
        if (problems.isNotEmpty()) return problems
        val globals = JsePlatform.standardGlobals()
        globals.set("nativeui", CoerceJavaToLua.coerce(api))
        try {
            globals.load(source, NativeScript.FILE).call()
        } catch (e: LuaError) {
            problems += "ui.lua ${e.message ?: "failed while running"}"
            NativeUiLook.clearScript()
        } catch (e: Exception) {
            problems += "ui.lua ${e.message ?: "failed while running"}"
            NativeUiLook.clearScript()
        }
        return problems
    }
}
