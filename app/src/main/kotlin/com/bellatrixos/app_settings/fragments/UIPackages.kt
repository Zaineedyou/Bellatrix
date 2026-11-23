package com.bellatrixos.app_settings.fragments

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.snapshotFlow
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bellatrixos.App
import com.bellatrixos.R
import com.bellatrixos.Runtime
import com.bellatrixos.linux.JobState
import com.bellatrixos.linux.Package
import com.bellatrixos.widgets.cards.UISettingsCard
import com.bellatrixos.widgets.cards.UISettingsCardCollapse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * xbps packages as a Bellatrix Settings page: a native search field and one native collapse card per
 * package, with install/remove and that package's job log inside it. Replaces the Omalauncher-style
 * packages screen.
 */
class UIPackages : Fragment() {
    private lateinit var runtime: Runtime
    private lateinit var query: EditText
    private lateinit var status: TextView
    private lateinit var list: LinearLayout
    private var searchJob: Job? = null
    private var refreshJob: Job? = null
    private var progressJob: Job? = null
    private val confirming = HashSet<String>()
    private var lastQuery = ""
    private var searching = false
    private var shown = emptyList<Package>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.settings_packages, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        runtime = (requireContext().applicationContext as App).runtime
        query = view.findViewById(R.id.packages_query)
        status = view.findViewById(R.id.packages_status)
        list = view.findViewById(R.id.packages_list)

        query.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) = Unit

                override fun afterTextChanged(s: Editable?) = load(s?.toString().orEmpty())
            },
        )

        // Installs/removes change what's installed; the tick redraws rows as a job's log grows.
        refreshJob =
            viewLifecycleOwner.lifecycleScope.launch {
                snapshotFlow { runtime.packages.revision.intValue }.collect { load(lastQuery) }
            }
        progressJob =
            viewLifecycleOwner.lifecycleScope.launch {
                snapshotFlow { runtime.packages.progress.intValue }.collect { render() }
            }
    }

    override fun onDestroyView() {
        searchJob?.cancel()
        refreshJob?.cancel()
        progressJob?.cancel()
        super.onDestroyView()
    }

    private fun load(text: String) {
        searchJob?.cancel()
        searchJob =
            viewLifecycleOwner.lifecycleScope.launch {
                val q = text.trim()
                if (q.isNotEmpty()) delay(350) // debounce typing
                lastQuery = q
                searching = true
                renderStatus()
                shown = if (q.isEmpty()) runtime.packages.installed() else runtime.packages.search(q)
                searching = false
                render()
            }
    }

    private fun render() {
        if (!::list.isInitialized) return
        renderStatus()
        list.removeAllViews()
        for (pkg in shown) list.addView(packageCard(pkg))
    }

    private fun renderStatus() {
        status.text =
            when {
                searching -> getString(if (lastQuery.isBlank()) R.string.packages_loading else R.string.packages_searching)
                lastQuery.isBlank() -> getString(R.string.packages_installed, shown.size)
                shown.isEmpty() -> getString(R.string.packages_none)
                else -> resources.getQuantityString(R.plurals.packages_results, shown.size, shown.size)
            }
    }

    private fun packageCard(pkg: Package): UISettingsCardCollapse {
        val context = requireContext()
        val state = runtime.packages.stateOf(pkg.name)
        val card = UISettingsCardCollapse(context)
        card.layoutParams = cardParams()
        card.findViewById<ImageView>(R.id.img_ui_card)?.setImageResource(R.drawable.img_app_terminal)
        card.findViewById<TextView>(R.id.title_ui_card)?.text = pkg.name
        card.findViewById<TextView>(R.id.subtitle_ui_card)?.text =
            when {
                state is JobState.Working -> getString(R.string.linux_job_working, getString(state.action))
                state is JobState.Failed -> getString(R.string.home_job_failed)
                else -> pkg.version.ifEmpty { pkg.repo.orEmpty() }
            }

        val body =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                val pad = (12 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
            }
        if (pkg.description.isNotEmpty()) {
            body.addView(
                TextView(context).apply {
                    text = pkg.description
                    textSize = 12f
                    maxLines = 2
                    setTextColor(resources.getColor(R.color.text_muted, null))
                    layoutParams = cardParams()
                },
            )
        }

        val action = actionFor(pkg, state)
        if (action != null) {
            val button = UISettingsCard(context)
            button.layoutParams = cardParams(2)
            button.setImage(action.icon)
            button.setTitle(getString(action.label))
            button.setSubtitle(null)
            button.setOnCardClickListener { action.click() }
            body.addView(button)
        }
        val lines =
            when (state) {
                is JobState.Working -> state.log
                is JobState.Failed -> listOf(getString(R.string.home_job_failed)) + state.log
                JobState.Idle -> emptyList()
            }
        for (line in lines.takeLast(8)) {
            body.addView(
                TextView(context).apply {
                    text = line
                    typeface = Typeface.MONOSPACE
                    textSize = 11f
                    setTextColor(resources.getColor(R.color.text_muted, null))
                    layoutParams = cardParams()
                },
            )
        }
        card.v.addView(body)
        card.isEnabled = state !is JobState.Working
        return card
    }

    private class Action(
        val label: Int,
        val icon: Int,
        val click: () -> Unit,
    )

    /** Removal takes a second tap: it also removes dependencies nothing else uses. */
    private fun actionFor(
        pkg: Package,
        state: JobState,
    ): Action? =
        when {
            state !is JobState.Idle -> null
            pkg.installed && pkg.name in runtime.packages.core -> null
            pkg.installed && pkg.name in confirming ->
                Action(R.string.packages_confirm, R.drawable.ic_delete_26_regular) {
                    confirming.remove(pkg.name)
                    viewLifecycleOwner.lifecycleScope.launch {
                        runtime.packages.remove(pkg.name)
                        runtime.refreshApps()
                    }
                }
            pkg.installed ->
                Action(R.string.packages_remove, R.drawable.ic_delete_26_regular) {
                    confirming.add(pkg.name)
                    render()
                }
            else ->
                Action(R.string.packages_install, R.drawable.img_nav_update_20) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        runtime.packages.install(pkg)
                        runtime.refreshApps()
                    }
                }
        }

    private fun cardParams(topMarginDp: Int = 0) =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = (topMarginDp * resources.displayMetrics.density).toInt() }
}
