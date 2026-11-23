package com.bellatrixos.ui.nativeui

import android.content.Context
import com.bellatrixos.storage.LauncherData
import com.bellatrixos.upgrade.Managed
import com.bellatrixos.upgrade.Policy
import com.bellatrixos.upgrade.SyncedFile
import com.bellatrixos.widgets.runtime.watchFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Reads the two guest-editable native UI files from the launcher data Linux
 * sees as ~/.config/bellatrix/native-ui. Agents and the user own them: the
 * launcher only writes first copies, and never saves over a file it could not
 * read. Both files report into the one errors.txt beside them. Main thread.
 */
class NativeUiStore(
    private val context: Context,
) {
    private val dir = LauncherData.nativeUiDir(context)
    private val scriptFile = File(dir, NativeScript.FILE)

    private val look =
        SyncedFile(File(dir, NativeUiSpec.FILE), File(dir, NativeUiSpec.ERRORS), "the native UI") {
            NativeUiSpec.defaults()
        }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var scriptJob: Job? = null
    private var runner: NativeUiRunner? = null
    private var lookProblems = emptyList<String>()
    private var scriptProblems = emptyList<String>()
    private val hookProblems = LinkedHashSet<String>()

    var spec: NativeUiSpec.Spec = NativeUiSpec.Spec.EMPTY
        private set

    val errors get() = look.errors

    /** Called after an agent or the user edits either file; the applier uses the views already on screen. */
    var onExternalChange: (NativeUiSpec.Spec) -> Unit = {}

    init {
        val text = look.open()
        if (text == null) {
            read(NativeUiSpec.defaults())
            look.create()
        } else {
            read(text)
        }
        if (!scriptFile.isFile) {
            runCatching {
                scriptFile.parentFile?.mkdirs()
                scriptFile.writeText(NativeScript.defaults())
            }.onFailure { error ->
                scriptProblems = listOf("ui.lua could not be created: ${error.message ?: "write failed"}")
                syncProblems()
            }
        }
    }

    /** The Lua API is installed by NativeUiLook before start(), so the first script run can use it. */
    fun attachApi(api: NativeUiApi) {
        runner =
            NativeUiRunner(api) { problem ->
                hookProblems += problem
                syncProblems()
            }
    }

    /** README is owned so agents always get the API that matches this launcher; examples are seeds. */
    fun managed(): List<Managed> =
        listOf(
            Managed.asset(context, "native-ui/${NativeUiSpec.README}", File(dir, NativeUiSpec.README), Policy.Owned),
            Managed.asset(context, "native-ui/examples/taskbar.lua", File(dir, "examples/taskbar.lua"), Policy.Seeded),
            Managed.asset(context, "native-ui/examples/settings.lua", File(dir, "examples/settings.lua"), Policy.Seeded),
        )

    fun start() {
        look.start { text ->
            read(text)
            if (text.isEmpty() && !look.exists) look.create()
            onExternalChange(spec)
        }
        if (scriptJob != null) return
        scriptJob =
            scope.launch {
                watchFile(scriptFile).collect { text -> runScript(text) }
            }
    }

    private fun read(text: String) {
        val parsed = NativeUiSpec.parse(text)
        look.readable = parsed.problems.isEmpty() || parsed.spec.rules.isNotEmpty()
        lookProblems = parsed.problems
        spec = parsed.spec
        syncProblems()
    }

    private fun runScript(text: String) {
        hookProblems.clear()
        scriptProblems = runner?.run(text) ?: NativeScript.check(text)
        syncProblems()
    }

    private fun syncProblems() {
        look.setProblems(lookProblems + scriptProblems + hookProblems.toList())
    }
}
