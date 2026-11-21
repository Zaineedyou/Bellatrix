package com.bellatrixos.app_settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
 * Coding Agents as one Bellatrix Settings page: one normal collapse card per agent. Interactive,
 * install, remove and that agent's own log are all inside its dropdown, so nothing is split into
 * separate cards. The former "install all" card is gone.
 */
class UICodingAgents : Fragment() {
    private lateinit var runtime: Runtime
    private lateinit var list: LinearLayout
    private var updates: Job? = null
    private val cards = LinkedHashMap<String, AgentViews>()
    private val manualExpanded = HashSet<String>()
    private var ready = false
    private var installed = emptyList<Agent>()
    private var states = emptyMap<String, JobState>()

    private class AgentViews(
        val card: UISettingsCardCollapse,
        val interactive: UISettingsCard,
        val install: UISettingsCard,
        val remove: UISettingsCard,
        val log: TextView,
    )

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
        list = view.findViewById(R.id.coding_agents_list)
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
        cards.clear()
        super.onDestroyView()
    }

    private fun render(
        linux: LinuxState,
        installed: List<Agent>,
        states: Map<String, JobState>,
    ) {
        if (!::list.isInitialized) return
        ready = linux is LinuxState.Installed && runtime.isLinuxReady
        this.installed = installed
        this.states = states
        if (cards.isEmpty()) buildCards()
        AGENTS.forEach(::updateCard)
    }

    private fun buildCards() {
        list.removeAllViews()
        for (agent in AGENTS) {
            val views = agentCard(agent)
            cards[agent.bin] = views
            list.addView(views.card)
        }
    }

    private fun agentCard(agent: Agent): AgentViews {
        val card = UISettingsCardCollapse(requireContext())
        card.layoutParams = cardParams()
        val icon =
            resources.getIdentifier(
                "agent_${agent.bin.replace('-', '_')}",
                "drawable",
                requireContext().packageName,
            )
        card.findViewById<ImageView>(R.id.img_ui_card)?.setImageResource(if (icon != 0) icon else R.drawable.img_app_terminal)
        card.findViewById<TextView>(R.id.title_ui_card)?.text = agent.name
        card.findViewById<TextView>(R.id.subtitle_ui_card)?.text = getString(R.string.agents_install)
        // The header only opens the dropdown; the actions live inside it.
        card.setOnCardClickListener { manualExpanded(agent.bin, card.x) }

        val body =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                val pad = (12 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
            }
        val interactive = actionCard(R.drawable.img_app_terminal, R.string.agents_interactive) { open(agent) }
        val install = actionCard(R.drawable.img_app_installer, R.string.agents_install) { install(agent) }
        val remove = actionCard(R.drawable.app_title_ic_close_16, R.string.agents_remove) { remove(agent) }
        val logLabel =
            TextView(requireContext()).apply {
                text = getString(R.string.agents_log)
                textSize = 12f
                setTextColor(resources.getColor(R.color.text_muted, null))
                layoutParams = cardParams(2)
            }
        val log =
            TextView(requireContext()).apply {
                text = getString(R.string.linux_process_log_empty)
                typeface = android.graphics.Typeface.MONOSPACE
                textSize = 12f
                setTextColor(resources.getColor(R.color.text_muted, null))
                layoutParams = cardParams(0)
            }
        body.addView(interactive)
        body.addView(install)
        body.addView(remove)
        body.addView(logLabel)
        body.addView(log)
        card.v.addView(body)
        return AgentViews(card, interactive, install, remove, log)
    }

    private fun actionCard(
        icon: Int,
        title: Int,
        click: () -> Unit,
    ): UISettingsCard {
        val card = UISettingsCard(requireContext())
        card.layoutParams = cardParams(2)
        card.setImage(icon)
        card.setTitle(getString(title))
        card.setSubtitle(null)
        card.setOnCardClickListener { click() }
        return card
    }

    private fun updateCard(agent: Agent) {
        val views = cards[agent.bin] ?: return
        val state = states[agent.bin] ?: JobState.Idle
        val isInstalled = installed.any { it.bin == agent.bin }
        views.card.findViewById<TextView>(R.id.subtitle_ui_card)?.text =
            when {
                !ready -> getString(R.string.settings_needs_linux)
                state is JobState.Working -> getString(R.string.linux_job_working, getString(state.action))
                state is JobState.Failed -> getString(R.string.home_job_failed)
                isInstalled -> getString(R.string.agents_installed)
                else -> getString(R.string.agents_install)
            }
        views.interactive.isEnabled = ready && isInstalled && state !is JobState.Working
        views.install.isEnabled = ready && !isInstalled && state !is JobState.Working
        views.remove.isEnabled = ready && isInstalled && state !is JobState.Working
        views.log.text =
            when (state) {
                is JobState.Working ->
                    (listOf(getString(R.string.linux_job_working, getString(state.action))) + state.log.takeLast(8)).joinToString("\n")
                is JobState.Failed ->
                    (listOf(getString(R.string.home_job_failed)) + state.log.takeLast(12)).joinToString("\n")
                else -> getString(R.string.linux_process_log_empty)
            }
        val expanded = state is JobState.Working || state is JobState.Failed || manualExpanded.contains(agent.bin)
        if (expanded != views.card.x) setExpanded(views.card, expanded)
    }

    private fun manualExpanded(
        bin: String,
        expanded: Boolean,
    ) {
        if (expanded) manualExpanded.add(bin) else manualExpanded.remove(bin)
    }

    private fun setExpanded(
        card: UISettingsCardCollapse,
        expanded: Boolean,
    ) {
        card.x = expanded
        card.v.visibility = if (expanded) View.VISIBLE else View.GONE
        // Keep WRAP_CONTENT even when hidden: the collapse animation measures this view on expand.
        card.v.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        card.w.rotation = if (expanded) 180f else 0f
        card.u.setBackgroundResource(
            if (expanded) R.drawable.ui_settings_card_header_clickable else R.drawable.ui_settings_card_clickable,
        )
    }

    private fun cardParams(topMarginDp: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = (topMarginDp * resources.displayMetrics.density).toInt() }

    private fun install(agent: Agent) {
        if (ready && (states[agent.bin] !is JobState.Working)) {
            viewLifecycleOwner.lifecycleScope.launch { runtime.agents.install(agent) }
        }
    }

    private fun remove(agent: Agent) {
        if (ready && (states[agent.bin] !is JobState.Working)) {
            viewLifecycleOwner.lifecycleScope.launch { runtime.agents.remove(agent) }
        }
    }

    private fun open(agent: Agent) {
        runtime.terminal(agent.command, agent.name, agent)
    }
}
