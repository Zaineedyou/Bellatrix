package com.bellatrixos.ui.settings

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bellatrixos.Runtime
import com.bellatrixos.R
import com.bellatrixos.linux.AGENTS
import com.bellatrixos.linux.Distro
import com.bellatrixos.linux.JobState
import com.bellatrixos.linux.LinuxState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import com.bellatrixos.system.hasPermission
import com.bellatrixos.ui.BracketAction
import com.bellatrixos.ui.JobLog
import com.bellatrixos.ui.Mono
import com.bellatrixos.ui.OverlayPanel
import com.bellatrixos.ui.Palette
import com.bellatrixos.ui.ReadingWidth
import com.bellatrixos.ui.TextSize
import com.bellatrixos.ui.card
import com.bellatrixos.ui.passesKeys
import com.bellatrixos.ui.takesPassedKeys

/**
 * Everything about the optional Linux system in one place: what it adds and
 * an install button, the install's progress, or, once installed, its size
 * and a way to remove it. Every feature that needs Linux opens this.
 */
@Composable
fun LinuxScreen(
    runtime: Runtime,
    onDismiss: () -> Unit,
) {
    val state by runtime.bootstrap.state.collectAsState()
    OverlayPanel(stringResource(R.string.linux_title), onDismiss, contentWidth = ReadingWidth) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
            when (val s = state) {
                LinuxState.Checking -> BasicText(stringResource(R.string.settings_checking), style = Mono.copy(color = Palette.muted))
                LinuxState.NotInstalled -> InstallOffer(runtime)
                is LinuxState.Installing, is LinuxState.Failed -> LinuxProgress(s, runtime.bootstrap.chosenAgents, runtime::retryLinux, runtime::cancelLinux.takeIf { runtime.bootstrap.cancellable })
                LinuxState.Installed -> Installed(runtime)
                LinuxState.Removing -> BasicText(stringResource(R.string.linux_removing), style = Mono.copy(color = Palette.accent))
            }
            LegacyArchCard(runtime)
        }
    }
}

/**
 * Arch Linux's old files, still in the app's storage after the launcher dropped Arch. Deleting them frees
 * the space; it can't be undone, and the old home folder goes with them, so it's the user's to start.
 */
@Composable
private fun LegacyArchCard(runtime: Runtime) {
    if (!runtime.hasLegacyArch) return
    var size by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) { size = withContext(Dispatchers.IO) { diskUsage(runtime.legacyArchRoot) } }
    var confirm by remember { mutableStateOf(false) }
    Spacer(Modifier.height(24.dp))
    Column(Modifier.card(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText("old arch linux data", style = Mono.copy(fontSize = TextSize.Item))
        BasicText(
            "Arch Linux isn't part of the launcher any more, but its files are still here: " + (size?.let(::formatSize) ?: "measuring…") +
                ". Deleting them frees the space and can't be undone: the old home folder (/root) goes with them.",
            style = Mono.copy(color = Palette.muted, fontSize = TextSize.Small),
        )
        if (!confirm) {
            BracketAction("delete", Palette.red) { confirm = true }
        } else {
            Row {
                BracketAction("delete for good", Palette.red, onClick = runtime::deleteLegacyArch)
                BracketAction("cancel", Palette.muted) { confirm = false }
            }
        }
    }
}


/**
 * Starts the Linux install and asks (Android 13+) to post notifications: the foreground
 * service's notification shows the install's progress and that Linux is running.
 * The install doesn't wait for the answer, which may never come back: on the welcome
 * screen, becoming the home app moves home to a new task, away from the dialog.
 */
@Composable
fun rememberLinuxInstall(runtime: Runtime): (List<String>) -> Unit {
    val context = LocalContext.current
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return { bins ->
        runtime.installLinux(AGENTS.filter { it.bin in bins })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            ask.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun InstallOffer(runtime: Runtime) {
    var picked by rememberSaveable { mutableStateOf(listOf(AGENTS.first().bin)) }
    val install = rememberLinuxInstall(runtime)
    BasicText(stringResource(R.string.linux_offer_title), style = Mono.copy(fontSize = TextSize.Large))
    Spacer(Modifier.height(8.dp))
    BasicText(
        stringResource(R.string.linux_offer_body),
        style = Mono.copy(color = Palette.muted, fontSize = TextSize.Small),
    )
    Spacer(Modifier.height(16.dp))
    DistroPicker()
    Spacer(Modifier.height(16.dp))
    // Above the agent list, so it's in reach without scrolling.
    PrimaryButton(if (picked.isEmpty()) stringResource(R.string.linux_install) else pluralStringResource(R.plurals.linux_install_with_agents, picked.size, picked.size), Modifier.takesPassedKeys().passesKeys()) {
        install(picked)
    }
    Spacer(Modifier.height(24.dp))
    AgentPicker(picked) { bin -> picked = if (bin in picked) picked - bin else picked + bin }
    OtherSystems(runtime, canInstall = false)
}

/**
 * The other distributions, each with its own folder and state: one already installed can be switched to,
 * and with [canInstall] the rest install next to the running one. Both restart the launcher.
 */
@Composable
private fun OtherSystems(
    runtime: Runtime,
    canInstall: Boolean,
) {
    val install = rememberLinuxInstall(runtime)
    val filesDir = runtime.proot.filesDir
    val rows = remember { Distro.entries.filter { it != Distro.active }.map { it to Distro.installed(filesDir, it) } }
    val shown = rows.filter { (_, installed) -> installed || canInstall }
    if (shown.isEmpty()) return
    Spacer(Modifier.height(24.dp))
    BasicText("other systems", style = Mono.copy(color = Palette.muted, fontSize = TextSize.Small))
    for ((distro, installed) in shown) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.card(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BasicText(distro.label.lowercase(), style = Mono.copy(fontSize = TextSize.Item))
                BasicText(
                    if (installed) "installed · switching restarts the launcher" else distro.note,
                    style = Mono.copy(color = Palette.muted, fontSize = TextSize.Small),
                )
            }
            if (installed) {
                BracketAction("switch", Palette.accent) { runtime.switchLinux(distro) }
            } else {
                // The same coding agents as the running system; more can be added in settings.
                BracketAction("install", Palette.accent) {
                    Distro.chosen = distro
                    install(runtime.bootstrap.chosenAgents.map { it.bin })
                }
            }
        }
    }
}

@Composable
private fun Installed(runtime: Runtime) {
    var size by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) { size = withContext(Dispatchers.IO) { diskUsage(runtime.proot.rootfs) } }
    var confirm by remember { mutableStateOf(false) }
    Row(
        Modifier.card(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(stringResource(R.string.linux_arch), style = Mono.copy(fontSize = TextSize.Item))
            BasicText(
                stringResource(R.string.linux_installed_size, size?.let(::formatSize) ?: stringResource(R.string.linux_measuring)),
                style = Mono.copy(color = Palette.muted, fontSize = TextSize.Small),
            )
        }
    }
    if (runtime.gpu.supported) {
        Spacer(Modifier.height(8.dp))
        GpuRow(runtime)
    }
    Spacer(Modifier.height(8.dp))
    CpuPinningRow(runtime)
    Spacer(Modifier.height(8.dp))
    KeepAliveRow(runtime)
    OtherSystems(runtime, canInstall = true)
    Spacer(Modifier.height(16.dp))
    // Keys go between remove linux and its cancel as they swap, and wait out the removal for the install it ends with.
    val ask = remember { FocusRequester() }
    val cancel = remember { FocusRequester() }
    if (!confirm) {
        BracketAction(stringResource(R.string.linux_remove), Palette.red, Modifier.focusRequester(ask).passesKeys(cancel)) { confirm = true }
    } else {
        Column(
            Modifier.card(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicText(stringResource(R.string.linux_remove_ask), style = Mono.copy(color = Palette.red))
            BasicText(
                stringResource(R.string.linux_remove_body),
                style = Mono.copy(color = Palette.muted, fontSize = TextSize.Small),
            )
            Row {
                BracketAction(stringResource(R.string.linux_remove_confirm), Palette.red, Modifier.passesKeys(), runtime::removeLinux)
                BracketAction(stringResource(R.string.linux_cancel), Palette.muted, Modifier.focusRequester(cancel).passesKeys(ask)) { confirm = false }
            }
        }
    }
}

/**
 * Why Linux apps die after a while, and what to switch off. Android 12+ kills child processes (signal 9)
 * once the phone has 32 of them, which coding agents reach quickly; battery savers stop the app with the
 * screen off. The limit can only be lifted from Android's developer options (14+) or over adb.
 */
@Composable
private fun KeepAliveRow(runtime: Runtime) {
    val context = LocalContext.current
    val power = remember { context.getSystemService(PowerManager::class.java) }
    var unrestricted by remember { mutableStateOf(power.isIgnoringBatteryOptimizations(context.packageName)) }
    val open =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            unrestricted = power.isIgnoringBatteryOptimizations(context.packageName)
        }
    fun openSettings(action: String) {
        runCatching { open.launch(Intent(action)) }
    }
    val sdk = Build.VERSION.SDK_INT
    val adb =
        if (sdk <= Build.VERSION_CODES.S_V2) {
            "adb shell \"/system/bin/device_config put activity_manager max_phantom_processes 2147483647\"\n" +
                "adb shell settings put global settings_enable_monitor_phantom_procs false"
        } else {
            "adb shell settings put global settings_enable_monitor_phantom_procs false"
        }
    var copied by remember { mutableStateOf(false) }
    Column(Modifier.card(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText("stay alive in the background", style = Mono.copy(fontSize = TextSize.Item))
        if (runtime.kills > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    "Android killed Linux apps ${runtime.kills}× · last " +
                        DateUtils.getRelativeTimeSpanString(runtime.lastKill, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS),
                    Modifier.weight(1f),
                    style = Mono.copy(color = Palette.red, fontSize = TextSize.Small),
                )
                BracketAction("reset", Palette.muted, onClick = runtime::clearKills)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                if (unrestricted) "battery: unrestricted" else "battery: Android may stop Linux when the screen is off",
                Modifier.weight(1f),
                style = Mono.copy(color = if (unrestricted) Palette.green else Palette.muted, fontSize = TextSize.Small),
            )
            if (!unrestricted) BracketAction("allow", Palette.accent) { openSettings(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) }
        }
        if (sdk >= Build.VERSION_CODES.S) {
            BasicText(
                "Android 12+ kills child processes (signal 9) past 32 of them. " +
                    if (sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        "Open developer options and turn on \"Disable child process restrictions\"."
                    } else {
                        "Lifting it needs a computer: run these over adb (USB or wireless debugging)."
                    },
                style = Mono.copy(color = Palette.muted, fontSize = TextSize.Small),
            )
            Row {
                if (sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    BracketAction("developer options", Palette.accent) { openSettings(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) }
                }
                BracketAction(if (copied) "copied" else "copy adb command", Palette.accent) {
                    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("adb", adb))
                    copied = true
                }
            }
        }
    }
}

/** Opt-in GPU acceleration for Linux apps; see [com.bellatrixos.linux.Gpu]. */
@Composable
private fun GpuRow(runtime: Runtime) {
    val gpu = runtime.gpu
    val snapshot by gpu.uiState.collectAsState()
    val state = snapshot.state
    val status =
        when {
            state is JobState.Working -> stringResource(R.string.linux_job_working, stringResource(state.action))
            snapshot.active && snapshot.openGlOnCpu -> stringResource(R.string.linux_gpu_on_vulkan_only, snapshot.device ?: "GPU")
            snapshot.active -> stringResource(R.string.linux_gpu_on, snapshot.device ?: "GPU")
            snapshot.wanted && state is JobState.Failed -> stringResource(R.string.linux_gpu_off_failed)
            snapshot.wanted -> stringResource(R.string.linux_gpu_waiting)
            state is JobState.Failed -> stringResource(R.string.linux_gpu_failed)
            else -> stringResource(R.string.linux_gpu_offer, snapshot.driverLabel)
        }
    Column(Modifier.card()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BasicText(stringResource(R.string.linux_gpu_title), style = Mono.copy(fontSize = TextSize.Item))
                BasicText(
                    status,
                    style = Mono.copy(color = if (snapshot.active) Palette.green else Palette.muted, fontSize = TextSize.Small),
                )
            }
            // Keys on one wait out its job, for the one it ends with.
            val passed = Modifier.takesPassedKeys().passesKeys()
            when {
                state is JobState.Working -> {}
                snapshot.wanted -> {
                    if (state is JobState.Failed) BracketAction(stringResource(R.string.linux_retry), Palette.accent, passed) { runtime.setGpu(true) }
                    BracketAction(stringResource(R.string.linux_gpu_turn_off), Palette.red, passed) { runtime.setGpu(false) }
                }
                else -> BracketAction(stringResource(R.string.linux_gpu_turn_on), Palette.accent, passed) { runtime.setGpu(true) }
            }
        }
        JobLog(state)
    }
}

/** The switch for proot's CPU pinning; see [com.bellatrixos.linux.Proot.cpuPinning]. */
@Composable
private fun CpuPinningRow(runtime: Runtime) {
    val proot = runtime.proot
    val on by proot.cpuPinningState.collectAsState()
    Row(Modifier.card(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(stringResource(R.string.linux_cpu_pinning_title), style = Mono.copy(fontSize = TextSize.Item))
            BasicText(
                stringResource(if (on) R.string.linux_cpu_pinning_on else R.string.linux_cpu_pinning_off),
                style = Mono.copy(color = if (on) Palette.green else Palette.muted, fontSize = TextSize.Small),
            )
        }
        val passed = Modifier.takesPassedKeys().passesKeys()
        if (on) {
            BracketAction(stringResource(R.string.linux_gpu_turn_off), Palette.red, passed) { proot.changeCpuPinning(false) }
        } else {
            BracketAction(stringResource(R.string.linux_gpu_turn_on), Palette.accent, passed) { proot.changeCpuPinning(true) }
        }
    }
}

/** Bytes in [dir], without following symlinks (the rootfs is full of absolute guest links). */
private fun diskUsage(dir: File): Long {
    var total = 0L
    runCatching {
        Files.walkFileTree(
            dir.toPath(),
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    if (attrs.isRegularFile) total += attrs.size()
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(
                    file: Path,
                    exc: IOException,
                ) = FileVisitResult.CONTINUE
            },
        )
    }
    return total
}

private fun formatSize(bytes: Long): String =
    if (bytes >= 1L shl 30) "%.1f GB".format(bytes / (1L shl 30).toDouble()) else "${bytes shr 20} MB"
