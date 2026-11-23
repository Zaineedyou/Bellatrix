package com.bellatrixos.ui.nativeui

import android.content.Context
import com.bellatrixos.storage.LauncherData
import com.bellatrixos.upgrade.Managed
import com.bellatrixos.upgrade.Policy
import com.bellatrixos.upgrade.SyncedFile
import java.io.File

/**
 * Reads look.toml from the launcher data Linux sees as
 * ~/.config/bellatrix/native-ui. Agents and the user own the file: the launcher
 * only writes a first copy, and never saves over a file it could not read.
 * Main thread.
 */
class NativeUiStore(
    context: Context,
) {
    private val dir = LauncherData.nativeUiDir(context)

    private val synced =
        SyncedFile(File(dir, NativeUiSpec.FILE), File(dir, NativeUiSpec.ERRORS), "the native UI") {
            NativeUiSpec.defaults()
        }

    var spec: NativeUiSpec.Spec = NativeUiSpec.Spec.EMPTY
        private set

    val errors get() = synced.errors

    /** Called after an agent or the user edits the file; the applier restyles the views already on screen. */
    var onExternalChange: (NativeUiSpec.Spec) -> Unit = {}

    init {
        val text = synced.open()
        if (text == null) {
            read(NativeUiSpec.defaults())
            synced.create()
        } else {
            read(text)
        }
    }

    /** The README is owned so agents always get the selector list that matches this launcher. */
    fun managed(): List<Managed> =
        listOf(
            Managed.asset(context, "native-ui/${NativeUiSpec.README}", File(dir, NativeUiSpec.README), Policy.Owned),
        )

    fun start() {
        synced.start { text ->
            read(text)
            if (text.isEmpty() && !synced.exists) synced.create()
            onExternalChange(spec)
        }
    }

    private fun read(text: String) {
        val parsed = NativeUiSpec.parse(text)
        synced.readable = parsed.problems.isEmpty() || parsed.spec.rules.isNotEmpty()
        synced.setProblems(parsed.problems)
        spec = parsed.spec
    }
}
