package com.bellatrixos.app_settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.runtime.snapshotFlow
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bellatrixos.App
import com.bellatrixos.R
import com.bellatrixos.Runtime
import com.bellatrixos.linux.AGENTS
import com.bellatrixos.linux.Agent
import com.bellatrixos.linux.ALL_AGENTS_JOB
import com.bellatrixos.linux.JobState
import com.bellatrixos.linux.LinuxState
import com.bellatrixos.widgets.cards.UISettingsCardCollapse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Coding Agents as one Bellatrix Settings card, like every other Settings page. There is no agent
 * picker: the card installs all missing agents together, in parallel, and its own expanded area
 * shows the combined output. No Compose screen and no nested Linux page.
 */
class UICodingAgents : Fragment() {
    private lateinit var runtime: Runtime
    private lateinit var card: UISettingsCardCollapse
    private lateinit var logText: TextView
    private var updates: Job? = null
    private var current: JobState? = null
    private var installedCount = 0
    private var ready = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.settings_coding_agents, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        runtime = (requireContext().applicationContext as App).runtime
        card = view.findViewById(R.id.coding_agents)
        logText = view.findViewById(R.id.coding_agents_log_text)
        card.setOnCardClickListener { installOrOpen() }

        updates =
            viewLifecycleOwner.lifecycleScope.launch {
                combine(
                    runtime.bootstrap.state,
                    snapshotFlow { runtime.agents.installed },
                    snapshotFlow { runtime.agents.states[ALL_AGENTS_JOB] },
                ) { linux, installed, state -> Triple(linux, installed, state) }
                    .collect { (linux, installed, state) -> render(linux, installed, state) }
            }
    }

    override fun onDestroyView() {
        updates?.cancel()
        updates = null
        super.onDestroyView()
    }

    private fun render(
        linux: LinuxState,
        installed: List<Agent>,
        state: JobState?,
    ) {
        if (!::card.isInitialized) return
        ready = linux is LinuxState.Installed && runtime.isLinuxReady
        installedCount = installed.size
        current = state
        val working = state is JobState.Working
        card.findViewById<TextView>(R.id.title_ui_card)?.text = getString(R.string.agents_title)
        card.findViewById<TextView>(R.id.subtitle_ui_card)?.text =
            when {
                !ready -> getString(R.string.settings_needs_linux)
                working -> getString(R.string.linux_job_working, getString((state as JobState.Working).action))
                state is JobState.Failed -> getString(R.string.home_job_failed)
                installedCount == AGENTS.size -> getString(R.string.agents_installed_count, installedCount, AGENTS.size)
                else -> getString(R.string.agents_install_all)
            }
        card.u.isEnabled = ready && !working
        card.u.alpha = if (ready && !working) 1.0f else 0.6f
        updateLog(state)
    }

    private fun installOrOpen() {
        if (!ready || current is JobState.Working) return
        if (installedCount < AGENTS.size) {
            viewLifecycleOwner.lifecycleScope.launch { runtime.agents.installAll() }
        } else {
            runtime.agents.default?.let { runtime.terminal(it.command, it.name, it) }
        }
    }

    /** The output lives in the same card as the action, so there is no log box at the bottom. */
    private fun updateLog(state: JobState?) {
        val lines =
            when (state) {
                is JobState.Working ->
                    listOf(getString(R.string.linux_job_working, getString(state.action))) +
                        state.log.takeLast(10)
                is JobState.Failed ->
                    listOf(getString(R.string.home_job_failed)) + state.log.takeLast(20)
                else -> emptyList()
            }
        logText.text = lines.joinToString("\n")
        if (lines.isNotEmpty() != card.x) setExpanded(lines.isNotEmpty())
    }

    private fun setExpanded(expanded: Boolean) {
        card.x = expanded
        card.v.visibility = if (expanded) View.VISIBLE else View.GONE
        card.v.layoutParams.height =
            if (expanded) ViewGroup.LayoutParams.WRAP_CONTENT else 1
        card.w.rotation = if (expanded) 180f else 0f
    }
}
