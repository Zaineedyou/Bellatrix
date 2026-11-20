package com.bellatrixos.linux

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.bellatrixos.R
import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.io.FileNotFoundException

/**
 * Opt-in GPU acceleration for guest apps.
 *
 * Ported 1:1 from Bellatrix's `dev.omalauncher.linux.Gpu`. Bellatrix does not use
 * Compose, so the four observable properties call [onChange] instead of using
 * `mutableStateOf`; the state machine, persistence, probe, rollback and the
 * generated scripts are otherwise unchanged.
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

    /** Notified whenever a setting or job state changes, so the settings screen can redraw. */
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

    /** What changes under the driver: the kernel and Android's own GPU driver come with system updates. */
    private val build get() = Build.FINGERPRINT + " " + System.getProperty("os.version")

    private fun changed() {
        onChange?.invoke()
    }

    /** Called when Linux becomes ready. */
    suspend fun recheck() {
        val driver = spec ?: return
        if (!wanted || active || state is JobState.Working) return
        if (!proot.hostFile(driver.driverPath).isFile) return turnOff()
        // The APK carries a different Mesa than the guest has, or none was installed yet.
        val mesaCurrent = runCatching { proot.hostFile(GpuDrivers.MESA_ZINK_STAMP).readText().trim() }
            .getOrNull() == GpuDrivers.MESA_ZINK_TAR_SHA256
        if (mesaCurrent && prefs.getString(CHECKED_BUILD, null) == build) {
            if (!proot.hostFile(driver.icdPath).isFile) {
                retireLegacyManifest(driver)
                if (!run(R.string.linux_job_installing, GpuDrivers.manifestScript(driver))) {
                    proot.graphicsEnv = SOFTWARE_ENV
                    conceal(driver)
                    return
                }
            }
            activate(driver)
            return
        }
        val ok = if (mesaCurrent) {
            Log.i(TAG, "system changed since the last GPU check, checking again")
            run(R.string.linux_job_checking, GpuDrivers.probeScript(driver), checksOpenGl = true)
        } else {
            Log.i(TAG, "the patched Mesa changed, installing it and checking again")
            stageMesaBundle()
            run(
                R.string.linux_job_installing,
                GpuDrivers.mesaZinkScript() + " && " + GpuDrivers.probeScript(driver),
                checksOpenGl = true,
            )
        }
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
        prefs.edit().putBoolean(WANTED, true).apply()
        wanted = true
        changed()
        stageMesaBundle()
        retireLegacyManifest(driver)
        val script = GpuDrivers.installScript(driver) + " && " + GpuDrivers.probeScript(driver)
        if (run(R.string.linux_job_installing, script, packageManager.lock, checksOpenGl = true)) {
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
        lock: Mutex? = null,
        checksOpenGl: Boolean = false,
    ): Boolean {
        val (ok, tail) = proot.trackedJob(action, script, lock) { state = it; changed() }
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
