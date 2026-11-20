package com.bellatrixos.linux

import android.content.Context
import com.bellatrixos.storage.LauncherData
import java.io.File

/**
 * What the Arch Linux install left behind. The launcher no longer has Arch, so its rootfs, state and
 * cached download just sit in the app's storage until deleted. That is only done when the user asks
 * ([delete]): the rootfs held their home folder.
 */
class LegacyArch(
    private val context: Context,
    private val filesDir: File,
) {
    val rootfs = File(filesDir, "arch")

    val present get() = rootfs.exists()

    /** Plain names: Arch kept the ones every state file had before there were several systems. */
    private val stateFiles = listOf("rootfs-steps.json", "setup-agents", "updates.json", LauncherData.GUEST_MANIFEST)

    private val gpuKeys = listOf("gpu_wanted", "gpu_checked_build", "gpu_device", "gpu_opengl_on_cpu")

    /** Callers stop guest processes first; Arch isn't the system that runs, so there are none of its. */
    fun delete() {
        runCatching { LauncherData.deleteTree(rootfs) }
        stateFiles.forEach { File(filesDir, it).delete() }
        context.cacheDir.listFiles { f -> f.name.startsWith("ArchLinuxARM-") }?.forEach { it.delete() }
        LauncherData.prefs(context).edit().also { e -> gpuKeys.forEach(e::remove) }.apply()
    }
}
