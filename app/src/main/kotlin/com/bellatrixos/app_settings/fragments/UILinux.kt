package com.bellatrixos.app_settings.fragments

import android.graphics.Color
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bellatrixos.R
import com.bellatrixos.linux.JobState
import com.bellatrixos.linux.LinuxRuntime
import com.bellatrixos.linux.LinuxState
import com.bellatrixos.linux.Stage
import com.bellatrixos.widgets.cards.UISettingsCard
import com.bellatrixos.widgets.cards.UISettingsCardCollapse
import com.bellatrixos.widgets.cards.UISettingsCardProgressBar
import kotlinx.coroutines.launch

/** Settings page for the ported Linux system: install, GPU, terminal and removal. */
class UILinux : Fragment() {
    private lateinit var root: View
    private lateinit var runtime: LinuxRuntime
    private lateinit var status: TextView
    private lateinit var progress: UISettingsCardProgressBar
    private lateinit var stages: TextView
    private lateinit var progressNote: TextView
    private lateinit var install: UISettingsCard
    private lateinit var retry: UISettingsCard
    private lateinit var cancel: UISettingsCard
    private lateinit var installLog: UISettingsCardCollapse
    private lateinit var installLogText: TextView
    private lateinit var gpu: UISettingsCard
    private lateinit var gpuLog: UISettingsCardCollapse
    private lateinit var gpuLogText: TextView
    private lateinit var terminal: UISettingsCard
    private lateinit var remove: UISettingsCard
    private lateinit var processLogText: TextView

    private var previousStateChanged: (() -> Unit)? = null
    private var installLogExpandedByFailure = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.settings_linux, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        root = view
        runtime = LinuxRuntime.get(requireContext())

        status = view.findViewById(R.id.linux_status)
        progress = view.findViewById(R.id.linux_progress)
        stages = view.findViewById(R.id.linux_stages)
        progressNote = view.findViewById(R.id.linux_progress_note)
        install = view.findViewById(R.id.linux_install)
        retry = view.findViewById(R.id.linux_retry)
        cancel = view.findViewById(R.id.linux_cancel)
        installLog = view.findViewById(R.id.linux_install_log)
        installLogText = view.findViewById(R.id.linux_install_log_text)
        gpu = view.findViewById(R.id.linux_gpu)
        gpuLog = view.findViewById(R.id.linux_gpu_log)
        gpuLogText = view.findViewById(R.id.linux_gpu_log_text)
        terminal = view.findViewById(R.id.linux_terminal)
        remove = view.findViewById(R.id.linux_remove)
        processLogText = view.findViewById(R.id.linux_process_log_text)
        processLogText.movementMethod = ScrollingMovementMethod()

        install.setOnCardClickListener { runtime.install() }
        retry.setOnCardClickListener { runtime.retry() }
        cancel.setOnCardClickListener { runtime.cancel() }
        gpu.setOnCardClickListener {
            // A failed check clears `wanted`, so a tap on the card is a retry, like Omalauncher.
            runtime.setGpu(!runtime.gpu.wanted)
        }
        terminal.setOnCardClickListener { runtime.terminal(null, "Terminal") }
        remove.setOnCardClickListener { runtime.remove() }
        installLog.setOnCardClickListener { updateLogs() }
        gpuLog.setOnCardClickListener { updateLogs() }

        previousStateChanged = runtime.onStateChanged
        runtime.onStateChanged = ::update

        viewLifecycleOwner.lifecycleScope.launch { runtime.bootstrap.state.collect { update() } }
        update()
    }

    override fun onDestroyView() {
        if (::runtime.isInitialized) {
            runtime.onStateChanged = previousStateChanged ?: {}
        }
        super.onDestroyView()
    }

    private fun update() {
        if (!::root.isInitialized) return
        val state = runtime.bootstrap.state.value
        val working = state as? LinuxState.Installing
        val failed = state as? LinuxState.Failed

        status.text =
            when (state) {
                LinuxState.Checking -> getString(R.string.linux_job_checking)
                LinuxState.NotInstalled -> getString(R.string.linux_status_not_installed)
                LinuxState.Installed -> getString(R.string.linux_status_installed)
                LinuxState.Removing -> getString(R.string.linux_job_removing)
                is LinuxState.Installing ->
                    working!!.progress?.let {
                        getString(R.string.linux_notification_installing_percent, (it * 100).toInt(), getString(working.stage.label))
                    } ?: getString(R.string.linux_notification_installing, getString(working.stage.label))
                is LinuxState.Failed -> getString(R.string.linux_progress_stopped)
            }

        updateProgress(working)
        updateStages(working, failed)

        install.isEnabled = state !is LinuxState.Installing && state !is LinuxState.Installed
        retry.isEnabled = failed != null
        cancel.isEnabled = working != null
        remove.isEnabled = state is LinuxState.Installed
        terminal.isEnabled = state is LinuxState.Installed && runtime.isLinuxReady

        updateGpu()
        updateLogs()
        updateProcessLog()
    }

    /** Shows the current stage's own percentage and detail line, like Omalauncher's StageRow. */
    private fun updateProgress(working: LinuxState.Installing?) {
        if (working == null) {
            progress.visibility = View.GONE
            return
        }
        progress.visibility = View.VISIBLE
        progress.setTitle(getString(working.stage.label))
        progress.setSubtitle(working.step)
        val fraction = working.progress
        if (fraction == null) {
            progress.u.visibility = View.GONE
            progress.y.visibility = View.GONE
        } else {
            progress.u.visibility = View.VISIBLE
            progress.y.visibility = View.VISIBLE
            val percent = (fraction.coerceIn(0f, 1f) * 100).toInt()
            progress.setProgressMax(100)
            progress.u.progress = percent
            progress.y.text = getString(R.string.linux_progress_percent, percent)
        }
    }

    /** The done/running/failed/pending checklist from Omalauncher's setup screen. */
    private fun updateStages(
        working: LinuxState.Installing?,
        failed: LinuxState.Failed?,
    ) {
        val current = working?.stage ?: failed?.stage
        if (current == null) {
            stages.visibility = View.GONE
            progressNote.visibility = View.GONE
            return
        }
        stages.visibility = View.VISIBLE
        progressNote.visibility = if (failed == null) View.VISIBLE else View.GONE
        stages.text =
            buildString {
                for (stage in Stage.values()) {
                    val mark =
                        when {
                            stage.ordinal < current.ordinal -> "✓"
                            stage == current && failed != null -> "✗"
                            stage == current -> "›"
                            else -> "·"
                        }
                    append(mark).append(' ').append(getString(stage.label))
                    if (stage != Stage.values().last()) append('\n')
                }
            }
    }

    private fun updateGpu() {
        val gpuState = runtime.gpu.state
        val device = runtime.gpu.device ?: "GPU"
        gpu.setSubtitle(
            when {
                gpuState is JobState.Working -> getString(R.string.linux_job_working, getString(gpuState.action))
                runtime.gpu.active && runtime.gpu.openGlOnCpu -> getString(R.string.linux_gpu_on_vulkan_only, device)
                runtime.gpu.active -> getString(R.string.linux_gpu_on, device)
                runtime.gpu.wanted && gpuState is JobState.Failed -> getString(R.string.linux_gpu_off_failed)
                runtime.gpu.wanted -> getString(R.string.linux_gpu_waiting)
                gpuState is JobState.Failed -> getString(R.string.linux_gpu_failed)
                !runtime.gpu.supported -> getString(R.string.linux_gpu_unsupported)
                else -> getString(R.string.linux_gpu_offer, runtime.gpu.driverLabel)
            },
        )
        val ready = runtime.bootstrap.state.value is LinuxState.Installed && runtime.isLinuxReady
        gpu.isEnabled = ready && runtime.gpu.supported && gpuState !is JobState.Working

        val gpuLines =
            when (gpuState) {
                is JobState.Working -> gpuState.log
                is JobState.Failed -> listOf(getString(R.string.home_job_failed)) + gpuState.log
                JobState.Idle -> emptyList()
            }
        setLog(gpuLog, gpuLogText, gpuLines)
    }

    private fun updateLogs() {
        val state = runtime.bootstrap.state.value
        if (state !is LinuxState.Failed) installLogExpandedByFailure = false
        val lines =
            when (state) {
                is LinuxState.Installing -> state.log
                is LinuxState.Failed -> state.log
                else -> emptyList()
            }
        setLog(installLog, installLogText, lines)
        if (state is LinuxState.Failed && lines.isNotEmpty() && !installLogExpandedByFailure) {
            expand(installLog)
            installLogExpandedByFailure = true
        }
    }

    /** Bottom terminal box: live output of the Linux install/process job. */
    private fun updateProcessLog() {
        val state = runtime.bootstrap.state.value
        val rendered =
            when (state) {
                is LinuxState.Installing -> {
                    val percent = state.progress?.let { " ${(it * 100).toInt()}%" }.orEmpty()
                    val header = "› ${getString(state.stage.label)}$percent"
                    // report() already appended the current step to the log; don't print it twice.
                    val tail =
                        if (state.log.lastOrNull() == state.step) state.log.dropLast(1) else state.log
                    listOf(header) + tail.takeLast(199).map { "$ $it" }
                }
                is LinuxState.Failed ->
                    listOf("✗ ${getString(state.stage.label)}: ${state.message}") +
                        state.log.takeLast(199).map { "$ $it" }
                else -> emptyList()
            }
        if (rendered.isEmpty()) {
            processLogText.text = getString(R.string.linux_process_log_empty)
            processLogText.setTextColor(Color.parseColor("#7A9B80"))
            return
        }
        processLogText.setTextColor(Color.parseColor("#D6FFD9"))
        processLogText.text = rendered.joinToString("\n")
        processLogText.post { processLogText.scrollTo(0, processLogText.lineCount * processLogText.lineHeight) }
    }

    private fun setLog(
        card: UISettingsCardCollapse,
        text: TextView,
        lines: List<String>,
    ) {
        card.visibility = if (lines.isEmpty()) View.GONE else View.VISIBLE
        text.text = lines.takeLast(if (card.x) 40 else 3).joinToString("\n")
        card.findViewById<TextView>(R.id.subtitle_ui_card)?.text =
            getString(if (card.x) R.string.linux_output_less else R.string.linux_output_more)
    }

    private fun expand(card: UISettingsCardCollapse) {
        card.x = true
        card.v.visibility = View.VISIBLE
        card.v.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        card.w.rotation = 180f
    }
}
