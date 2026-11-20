package com.bellatrixos.app_settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bellatrixos.R
import com.bellatrixos.linux.LinuxRuntime
import com.bellatrixos.linux.LinuxState
import com.bellatrixos.widgets.cards.UISettingsCard
import kotlinx.coroutines.launch

/** Settings page for the ported Linux system: install, GPU, terminal and removal. */
class UILinux : Fragment() {
    private lateinit var runtime: LinuxRuntime
    private lateinit var status: TextView
    private lateinit var install: UISettingsCard
    private lateinit var retry: UISettingsCard
    private lateinit var cancel: UISettingsCard
    private lateinit var gpu: UISettingsCard
    private lateinit var terminal: UISettingsCard
    private lateinit var remove: UISettingsCard

    private var previousUnavailable: (() -> Unit)? = null
    private var previousStateChanged: (() -> Unit)? = null

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
        runtime = LinuxRuntime.get(requireContext())

        status = view.findViewById(R.id.linux_status)
        install = view.findViewById(R.id.linux_install)
        retry = view.findViewById(R.id.linux_retry)
        cancel = view.findViewById(R.id.linux_cancel)
        gpu = view.findViewById(R.id.linux_gpu)
        terminal = view.findViewById(R.id.linux_terminal)
        remove = view.findViewById(R.id.linux_remove)

        install.setOnCardClickListener { runtime.install() }
        retry.setOnCardClickListener { runtime.retry() }
        cancel.setOnCardClickListener { runtime.cancel() }
        gpu.setOnCardClickListener { runtime.setGpu(!runtime.gpu.wanted) }
        terminal.setOnCardClickListener { runtime.terminal(null, "Terminal") }
        remove.setOnCardClickListener { runtime.remove() }

        previousUnavailable = runtime.onLinuxUnavailable
        runtime.onLinuxUnavailable = {}
        previousStateChanged = runtime.onStateChanged
        runtime.onStateChanged = ::update

        viewLifecycleOwner.lifecycleScope.launch { runtime.bootstrap.state.collect { update() } }
        update()
    }

    override fun onDestroyView() {
        if (::runtime.isInitialized) {
            runtime.onStateChanged = previousStateChanged ?: {}
            runtime.onLinuxUnavailable = previousUnavailable ?: {}
        }
        super.onDestroyView()
    }

    private fun update() {
        if (!::status.isInitialized) return
        val state = runtime.bootstrap.state.value
        status.text =
            when (state) {
                LinuxState.Checking -> getString(R.string.linux_job_checking)
                LinuxState.NotInstalled -> getString(R.string.linux_status_not_installed)
                LinuxState.Installed -> getString(R.string.linux_status_installed)
                LinuxState.Removing -> getString(R.string.linux_job_removing)
                is LinuxState.Installing ->
                    state.progress?.let {
                        getString(R.string.linux_notification_installing_percent, (it * 100).toInt(), getString(state.stage.label))
                    } ?: getString(R.string.linux_notification_installing, getString(state.stage.label))
                is LinuxState.Failed -> getString(R.string.linux_notification_failed)
            }

        install.isEnabled = state !is LinuxState.Installing && state !is LinuxState.Installed
        retry.isEnabled = state is LinuxState.Failed
        cancel.isEnabled = state is LinuxState.Installing
        remove.isEnabled = state is LinuxState.Installed
        terminal.isEnabled = state is LinuxState.Installed
        gpu.isEnabled = state is LinuxState.Installed && runtime.gpu.supported
        gpu.setSubtitle(
            if (!runtime.gpu.supported) {
                getString(R.string.linux_gpu_failed)
            } else if (runtime.gpu.wanted) {
                getString(R.string.linux_gpu_on, runtime.gpu.driverLabel)
            } else {
                getString(R.string.linux_gpu_off_failed)
            },
        )
    }
}
