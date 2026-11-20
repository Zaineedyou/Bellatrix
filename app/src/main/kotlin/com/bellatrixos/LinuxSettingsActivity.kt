package com.bellatrixos

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bellatrixos.linux.LinuxRuntime
import com.bellatrixos.linux.LinuxState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Small settings screen for the ported Linux system: install, GPU, terminal and removal. */
class LinuxSettingsActivity : AppCompatActivity() {
    private lateinit var runtime: LinuxRuntime
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watch: Job? = null

    private lateinit var status: TextView
    private lateinit var install: Button
    private lateinit var retry: Button
    private lateinit var cancel: Button
    private lateinit var gpu: Button
    private lateinit var terminal: Button
    private lateinit var remove: Button

    private var previousUnavailable: (() -> Unit)? = null
    private var previousStateChanged: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtime = LinuxRuntime.get(this)
        setContentView(R.layout.activity_linux_settings)
        status = findViewById(R.id.linux_status)
        install = findViewById(R.id.linux_install)
        retry = findViewById(R.id.linux_retry)
        cancel = findViewById(R.id.linux_cancel)
        gpu = findViewById(R.id.linux_gpu)
        terminal = findViewById(R.id.linux_terminal)
        remove = findViewById(R.id.linux_remove)

        install.setOnClickListener { runtime.install() }
        retry.setOnClickListener { runtime.retry() }
        cancel.setOnClickListener { runtime.cancel() }
        remove.setOnClickListener { runtime.remove() }
        gpu.setOnClickListener { runtime.setGpu(!runtime.gpu.wanted) }
        terminal.setOnClickListener { runtime.terminal(null, "Terminal") }
        previousUnavailable = runtime.onLinuxUnavailable
        runtime.onLinuxUnavailable = { }
    }

    override fun onStart() {
        super.onStart()
        previousStateChanged = runtime.onStateChanged
        runtime.onStateChanged = ::update
        watch = scope.launch { runtime.bootstrap.state.collect { update() } }
        update()
    }

    override fun onStop() {
        watch?.cancel()
        watch = null
        runtime.onStateChanged = previousStateChanged ?: {}
        runtime.onLinuxUnavailable = previousUnavailable ?: {}
        super.onStop()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
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
        gpu.text =
            if (!runtime.gpu.supported) {
                getString(R.string.linux_gpu_title)
            } else {
                "${getString(R.string.linux_gpu_title)}: ${if (runtime.gpu.wanted) getString(R.string.linux_gpu_on, runtime.gpu.driverLabel) else getString(R.string.linux_gpu_off_failed)}"
            }
    }
}
