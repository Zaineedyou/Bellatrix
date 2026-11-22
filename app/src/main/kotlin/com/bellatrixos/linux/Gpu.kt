package com.bellatrixos.linux

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.bellatrixos.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.atomic.AtomicInteger

/** Everything the settings row needs to redraw while a GPU job is running. */
data class GpuUiState(
    val supported: Boolean,
    val driverLabel: String,
    val wanted: Boolean,
    val active: Boolean,
    val openGlOnCpu: Boolean,
    val device: String?,
    /** Job progress (including the log) lives in [state]. */
    val state: JobState,
)

/**
 * Opt-in GPU acceleration for guest apps.
 *
 * Ported 1:1 from Omalauncher's `linux.Gpu`: the state machine, persistence, probe, rollback and the
 * generated scripts are otherwise unchanged. [uiState] is what Compose observes; the plain properties
 * and [onChange] remain for the legacy settings fragment.
 */
class Gpu(
    private val context: Context,
    private val proot: Proot,
    private val packageManager: Packages,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bellatrix_linux", Context.MODE_PRIVATE)

    private val spec: GpuDriver? = gpuKind(
        File("/dev/mali0").exists(),
        File("/dev/kgsl-3d0").exists(),
        runCatching { File("/sys/class/kgsl/kgsl-3d0/gpu_model").readText() }.getOrNull(),
    )?.let(GpuDrivers::of)

    /** Notified whenever a setting or job state changes, so the legacy settings screen can redraw. */
    var onChange: (() -> Unit)? = null

    val supported: Boolean get() = spec != null
    val driverLabel: String get() = spec?.label ?: ""

    /** The user turned it on; it may still be inactive until [recheck] passes. */
    var wanted: Boolean = prefs.getBoolean(WANTED, false)
        private set

    /** Linux apps started from now on render on the GPU. */
    var active: Boolean = false
        private set

    var openGlOnCpu: Boolean = prefs.getBoolean(GL_ON_CPU, false)
        private set

    /** The GPU the last check found, e.g. "Mali-G715 MC7". */
    var device: String? = prefs.getString(DEVICE, null)
        private set

    var state: JobState = JobState.Idle
        private set

    private val _uiState =
        MutableStateFlow(
            GpuUiState(supported, driverLabel, wanted, active, openGlOnCpu, device, state),
        )

    /** Observable mirror of the fields above, so the Compose Linux screen redraws during a job. */
    val uiState: StateFlow<GpuUiState> = _uiState.asStateFlow()

    /** Incremented by [turnOff] (and each new [turnOn]) so an old job cannot complete later. */
    private val generation = AtomicInteger(0)

    /** What changes under the driver: the kernel and Android's own GPU driver come with system updates. */
    private val build get() = Build.FINGERPRINT + " " + System.getProperty("os.version")

    private fun changed() {
        _uiState.value = GpuUiState(supported, driverLabel, wanted, active, openGlOnCpu, device, state)
        onChange?.invoke()
    }

    /** Called when Linux becomes ready. */
    suspend fun recheck() {
        val driver = spec ?: return
        if (!wanted || active || state is JobState.Working) return
        val token = generation.get()
        if (!proot.hostFile(driver.driverPath).isFile) return turnOff()
        // The APK carries a different Mesa than the guest has, or none was installed yet.
        val mesaCurrent = runCatching { proot.hostFile(GpuDrivers.MESA_ZINK_STAMP).readText().trim() }
            .getOrNull() == GpuDrivers.MESA_ZINK_TAR_SHA256
        if (mesaCurrent && prefs.getString(CHECKED_BUILD, null) == build) {
            if (!proot.hostFile(driver.icdPath).isFile) {
                retireLegacyManifest(driver)
                if (!run(R.string.linux_job_installing, GpuDrivers.manifestScript(driver), token)) {
                    if (token != generation.get()) return
                    proot.graphicsEnv = SOFTWARE_ENV
                    conceal(driver)
                    changed()
                    return
                }
            }
            if (token != generation.get()) return
            activate(driver)
            changed()
            return
        }
        val ok = if (mesaCurrent) {
            Log.i(TAG, "system changed since the last GPU check, checking again")
            run(R.string.linux_job_checking, GpuDrivers.probeScript(driver), token, checksOpenGl = true)
        } else {
            Log.i(TAG, "the patched Mesa changed, installing it and checking again")
            stageMesaBundle()
            run(
                R.string.linux_job_installing,
                GpuDrivers.mesaZinkScript() + " && " + GpuDrivers.probeScript(driver),
                token,
                checksOpenGl = true,
            )
        }
        if (token != generation.get()) return
        if (ok) {
            activate(driver)
        } else {
            proot.graphicsEnv = SOFTWARE_ENV
            conceal(driver)
        }
        changed()
    }

    /** Installs the driver and Mesa, then checks them; turns on only if the check passes. */
    suspend fun turnOn() {
        val driver = spec ?: return
        if (state is JobState.Working) return
        val token = generation.incrementAndGet()
        prefs.edit().putBoolean(WANTED, true).apply()
        wanted = true
        changed()
        stageMesaBundle()
        retireLegacyManifest(driver)
        val script = GpuDrivers.installScript(driver) + " && " + GpuDrivers.probeScript(driver)
        val ok = run(R.string.linux_job_installing, script, token, packageManager.lock, checksOpenGl = true)
        // A turnOff() while the job was running already reset everything: don't undo it.
        if (token != generation.get()) return
        if (ok) {
            activate(driver)
        } else {
            prefs.edit().putBoolean(WANTED, false).apply()
            wanted = false
            active = false
            proot.graphicsEnv = SOFTWARE_ENV
            conceal(driver)
        }
        changed()
    }

    /** Back to software rendering for apps started from now on. */
    fun turnOff() {
        generation.incrementAndGet()
        prefs.edit().putBoolean(WANTED, false).remove(CHECKED_BUILD).remove(GL_ON_CPU).apply()
        wanted = false
        active = false
        openGlOnCpu = false
        state = JobState.Idle
        proot.graphicsEnv = SOFTWARE_ENV
        spec?.let(::conceal)
        changed()
    }

    private fun activate(driver: GpuDriver) {
        active = true
        proot.graphicsEnv = if (openGlOnCpu) driver.vulkanOnlyEnv else driver.env
    }

    /** Drops the manifest so a later app cannot load the driver by the default search path. */
    private fun conceal(driver: GpuDriver) {
        proot.hostFile(driver.icdPath).delete()
        retireLegacyManifest(driver)
    }

    private fun retireLegacyManifest(driver: GpuDriver) =
        GpuDrivers.retireLegacyManifest(driver, proot.hostFile(GpuDrivers.LEGACY_MALI_ICD))

    /** Copies the patched Mesa tar from the APK into the guest. */
    private fun stageMesaBundle() {
        val dest = proot.hostFile(GpuDrivers.MESA_ZINK_BUNDLE)
        val input = try {
            context.assets.open("mesa-zink/mesa-zink.tar.xz")
        } catch (_: FileNotFoundException) {
            return
        }
        dest.parentFile?.mkdirs()
        input.use { src -> dest.outputStream().use { src.copyTo(it) } }
    }

    private suspend fun run(
        action: Int,
        script: String,
        token: Int,
        lock: Mutex? = null,
        checksOpenGl: Boolean = false,
    ): Boolean {
        val (ok, tail) =
            proot.trackedJob(action, script, lock) { job ->
                // Ignore progress from a turnOff()'d job: it must not resurrect a Working state.
                if (generation.get() == token) {
                    state = job
                    changed()
                }
            }
        if (generation.get() != token) return ok
        val lines = tail.toList()
        val name = lines.firstNotNullOfOrNull { DEVICE_NAME.find(it)?.groupValues?.get(1) }
        Log.i(TAG, "${context.getString(action)}: ${if (ok) "ok, $name" else "failed"}")
        if (ok) {
            device = name
            prefs.edit().putString(CHECKED_BUILD, build).putString(DEVICE, name).apply()
            if (checksOpenGl) {
                openGlOnCpu = lines.any { GpuDrivers.OPENGL_ON_CPU in it }
                prefs.edit().putBoolean(GL_ON_CPU, openGlOnCpu).apply()
            }
        }
        return ok
    }

    companion object {
        private const val TAG = "gpu"
        private val WANTED get() = Distro.active.state("gpu_wanted")
        private val CHECKED_BUILD get() = Distro.active.state("gpu_checked_build")
        private val DEVICE get() = Distro.active.state("gpu_device")
        private val GL_ON_CPU get() = Distro.active.state("gpu_opengl_on_cpu")

        private val DEVICE_NAME = Regex("""deviceName\s*=\s*(.+)""")
    }
}
