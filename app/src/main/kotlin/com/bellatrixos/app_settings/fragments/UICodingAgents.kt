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
import com.bellatrixos.linux.ALL_AGENTS_JOB
import com.bellatrixos.linux.JobState
import com.bellatrixos.linux.LinuxState
import com.bellatrixos.widgets.cards.UISettingsCard
import com.bellatrixos.widgets.cards.UISettingsCardCollapse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Coding Agents as one Bellatrix Settings page. The cards are the usual UISettingsCard views; the
 * single collapse card installs every missing agent in parallel and keeps the combined log inside
 * itself. The old "default agent" card is gone because installed agents live in the drawer now.
 */
class UICodingAgents : Fragment() {
    private lateinit var runtime: Runtime
    private lateinit var action: UISettingsCardCollapse
    private lateinit var list: LinearLayout
    private lateinit var logText: TextView
    private var updates: Job? = null
    private var allState: JobState? = null
    private var ready = false
    private var installed = emptyList<Agent>()
    private var states = emptyMap<String, JobState>()

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
        action = view.findViewById(R.id.coding_agents)
        list = view.findViewById(R.id.coding_agents_list)
        logText = view.findViewById(R.id.coding_agents_log_text)
        action.setOnCardClickListener { installAll() }

        updates =
            viewLifecycleOwner.lifecycleScope.launch {
                combine(
                    runtime.bootstrap.state,
                    snapshotFlow { runtime.agents.installed },
                    snapshotFlow { runtime.agents.states.toMap() },
                ) { linux, installed, states -> Triple(linux, installed, states) }
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
        if (!::action.isInitialized) return
        ready = linux is LinuxState.Installed && runtime.isLinuxReady
        this.installed = installed
        this.states = states
        allState = states[ALL_AGENTS_JOB]
        val working = allState is JobState.Working
        action.findViewById<TextView>(R.id.title_ui_card)?.text = getString(R.string.agents_title)
        action.findViewById<TextView>(R.id.subtitle_ui_card)?.text =
            when {
                !ready -> getString(R.string.settings_needs_linux)
                working -> getString(R.string.linux_job_working, getString((allState as JobState.Working).action))
                allState is JobState.Failed -> getString(R.string.home_job_failed)
                installed.size == AGENTS.size -> getString(R.string.agents_installed_count, installed.size, AGENTS.size)
                else -> getString(R.string.agents_install_all)
            }
        action.u.isEnabled = ready && !working
        action.u.alpha = if (ready && !working) 1.0f else 0.6f
        updateLog()
        renderList()
    }

    private fun renderList() {
        list.removeAllViews()
        for (agent in AGENTS) {
            val state = states[agent.bin] ?: JobState.Idle
            val isInstalled = installed.any { it.bin == agent.bin }
            list.addView(agentCard(agent, isInstalled, state))
            if (isInstalled) list.addView(removeCard(agent, state))
        }
    }

    private fun agentCard(
        agent: Agent,
        installed: Boolean,
        state: JobState,
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

    private fun installAll() {
        if (ready && allState !is JobState.Working) {
            viewLifecycleOwner.lifecycleScope.launch { runtime.agents.installAll() }
        }
    }

    private fun open(agent: Agent) {
        runtime.terminal(agent.command, agent.name, agent)
    }

    /** The output stays in the same card as the action, so no log box is separated at the bottom. */
    private fun updateLog() {
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
            } + (
                allState?.let { state ->
                    when (state) {
                        is JobState.Working ->
                            listOf(getString(R.string.linux_job_working, getString(state.action))) +
                                state.log.takeLast(8)
                        is JobState.Failed ->
                            listOf(getString(R.string.home_job_failed)) + state.log.takeLast(12)
                        else -> emptyList()
                    }
                } ?: emptyList()
                )
        logText.text = lines.takeLast(40).joinToString("\n")
        if (lines.isNotEmpty() != action.x) setExpanded(lines.isNotEmpty())
    }

    private fun setExpanded(expanded: Boolean) {
        action.x = expanded
        action.v.visibility = if (expanded) View.VISIBLE else View.GONE
        action.v.layoutParams.height =
            if (expanded) ViewGroup.LayoutParams.WRAP_CONTENT else 1
        action.w.rotation = if (expanded) 180f else 0f
    }
}
