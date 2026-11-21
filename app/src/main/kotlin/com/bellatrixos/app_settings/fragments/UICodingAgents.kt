package com.bellatrixos.app_settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.snapshotFlow
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bellatrixos.App
import com.bellatrixos.R
import com.bellatrixos.Runtime
import com.bellatrixos.linux.AGENTS
import com.bellatrixos.linux.Agent
import com.bellatrixos.linux.JobState
import com.bellatrixos.linux.LinuxState
import com.bellatrixos.widgets.cards.UISettingsCard
import com.bellatrixos.widgets.cards.UISettingsCardCollapse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Coding Agents as a standalone Bellatrix Settings page. Every row is a UISettingsCard, so the
 * install/remove/open controls use the same views as the rest of launcher Settings. No Compose
 * screen and no nested Linux page.
 */
class UICodingAgents : Fragment() {
    private lateinit var runtime: Runtime
    private lateinit var status: UISettingsCard
    private lateinit var list: LinearLayout
    private lateinit var log: UISettingsCardCollapse
    private lateinit var logText: TextView
    private var updates: Job? = null
    private var logExpanded = false

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
        status = view.findViewById(R.id.coding_agents_default)
        list = view.findViewById(R.id.coding_agents_list)
        log = view.findViewById(R.id.coding_agents_log)
        logText = view.findViewById(R.id.coding_agents_log_text)

        updates =
            viewLifecycleOwner.lifecycleScope.launch {
                combine(
                    runtime.bootstrap.state,
                    snapshotFlow { runtime.agents.installed },
                    snapshotFlow { AGENTS.map { it.bin to runtime.agents.states[it.bin] } },
                ) { linux, installed, states ->
                    Triple(linux, installed, states.mapNotNull { (bin, state) -> state?.let { bin to it } }.toMap())
                }
                    .collect { (linux, installed, states) -> render(linux, installed, states) }
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
        states: Map<String, JobState>,
    ) {
        if (!::status.isInitialized) return
        val ready = linux is LinuxState.Installed && runtime.isLinuxReady
        val default = runtime.agents.default

        status.setTitle(
            if (default != null) {
                getString(R.string.settings_agents_default, default.name)
            } else {
                getString(R.string.agents_title)
            },
        )
        status.setSubtitle(
            when {
                !ready -> getString(R.string.settings_needs_linux)
                default != null -> getString(R.string.agents_open)
                else -> getString(R.string.settings_agents_install)
            },
        )
        status.isEnabled = ready && default != null
        status.setOnCardClickListener { if (default != null && ready) open(default) }

        list.removeAllViews()
        for (agent in AGENTS) {
            val state = states[agent.bin] ?: JobState.Idle
            val isInstalled = installed.any { it.bin == agent.bin }
            list.addView(agentCard(agent, isInstalled, state, ready))
            if (isInstalled) list.addView(removeCard(agent, state, ready))
        }
        updateLog(states)
    }

    private fun agentCard(
        agent: Agent,
        installed: Boolean,
        state: JobState,
        ready: Boolean,
    ): UISettingsCard {
        val card = UISettingsCard(requireContext())
        card.layoutParams = cardParams()
        val icon =
            resources.getIdentifier(
                "agent_${agent.bin.replace('-', '_')}",
                "drawable",
                requireContext().packageName,
            )
        card.setImage(if (icon != 0) icon else R.drawable.img_app_terminal)
        card.setTitle(agent.name)
        card.setSubtitle(
            when {
                state is JobState.Working -> getString(R.string.linux_job_working, getString(state.action))
                state is JobState.Failed -> getString(R.string.home_job_failed)
                installed -> getString(R.string.agents_open)
                else -> getString(R.string.agents_install)
            },
        )
        card.isEnabled = ready && state !is JobState.Working
        card.setOnCardClickListener {
            if (!ready || state is JobState.Working) return@setOnCardClickListener
            if (installed) {
                open(agent)
            } else {
                viewLifecycleOwner.lifecycleScope.launch { runtime.agents.install(agent) }
            }
        }
        return card
    }

    private fun removeCard(
        agent: Agent,
        state: JobState,
        ready: Boolean,
    ): UISettingsCard {
        val card = UISettingsCard(requireContext())
        card.layoutParams = cardParams()
        card.setImage(R.drawable.app_title_ic_close_16)
        card.setTitle(getString(R.string.agents_remove))
        card.setSubtitle(agent.name)
        card.isEnabled = ready && state !is JobState.Working
        card.setOnCardClickListener {
            if (ready && state !is JobState.Working) {
                viewLifecycleOwner.lifecycleScope.launch { runtime.agents.remove(agent) }
            }
        }
        return card
    }

    private fun cardParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = (4 * resources.displayMetrics.density).toInt() }

    private fun open(agent: Agent) {
        runtime.terminal(agent.command, agent.name, agent)
    }

    /** One Bellatrix collapse card carries the job output for every agent row. */
    private fun updateLog(states: Map<String, JobState>) {
        val lines =
            AGENTS.flatMap { agent ->
                when (val state = states[agent.bin] ?: JobState.Idle) {
                    is JobState.Working ->
                        listOf("${agent.name}: ${getString(R.string.linux_job_working, getString(state.action))}") +
                            state.log.takeLast(4)
                    is JobState.Failed ->
                        listOf("${agent.name}: ${getString(R.string.home_job_failed)}") +
                            state.log.takeLast(8)
                    else -> emptyList()
                }
            }
        log.visibility = if (lines.isEmpty()) View.GONE else View.VISIBLE
        logText.text = lines.takeLast(if (log.x) 80 else 8).joinToString("\n")
        log.findViewById<TextView>(R.id.subtitle_ui_card)?.text =
            getString(if (log.x) R.string.linux_output_less else R.string.linux_output_more)

        val failed = states.values.any { it is JobState.Failed }
        if (!failed) logExpanded = false
        if (failed && lines.isNotEmpty() && !logExpanded) {
            log.x = true
            log.v.visibility = View.VISIBLE
            log.v.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            log.w.rotation = 180f
            logExpanded = true
        }
    }
}
