package com.bellatrixos

import android.app.ActivityManager
import android.app.Application
import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.bellatrixos.adb.AdbBridge
import com.bellatrixos.linux.AGENTS
import com.bellatrixos.linux.Agent
import com.bellatrixos.linux.AgentStore
import com.bellatrixos.linux.AudioBridge
import com.bellatrixos.linux.Bootstrap
import com.bellatrixos.linux.EDIT_COMMAND
import com.bellatrixos.linux.FILES_COMMAND
import com.bellatrixos.linux.VOICE_COMMAND
import com.bellatrixos.linux.Gpu
import com.bellatrixos.linux.LinuxApp
import com.bellatrixos.linux.LinuxDns
import com.bellatrixos.linux.OpenBridge
import com.bellatrixos.linux.PhoneBridge
import com.bellatrixos.linux.Distro
import com.bellatrixos.linux.LegacyArch
import com.bellatrixos.linux.Packages
import com.bellatrixos.linux.Proot
import com.bellatrixos.linux.SystemUpdates
import com.bellatrixos.linux.LauncherFont
import com.bellatrixos.linux.LINUX_AGENT_TAG_PREFIX
import com.bellatrixos.linux.LINUX_APP_TAG_PREFIX
import com.bellatrixos.linux.TerminalFont
import com.bellatrixos.linux.TerminalText
import com.bellatrixos.linux.bundledIcons
import com.bellatrixos.linux.defaultApps
import com.bellatrixos.linux.matches
import com.bellatrixos.linux.run
import com.bellatrixos.linux.shellQuote
import com.bellatrixos.theme.Style
import com.bellatrixos.theme.StyleStore
import com.bellatrixos.theme.ThemeStore
import com.bellatrixos.ui.home.Gestures
import com.bellatrixos.ui.KeyboardBridge
import com.bellatrixos.ui.voice.Speaker
import com.bellatrixos.voice.VoiceSession
import com.bellatrixos.ui.home.TerminalTiles
import com.bellatrixos.ui.WindowManager
import com.bellatrixos.ui.bitmapMask
import com.bellatrixos.ui.Palette
import com.bellatrixos.ui.nativeui.GlobalKeys
import com.bellatrixos.ui.nativeui.NativeUiLook
import com.bellatrixos.ui.nativeui.NativeUiStore
import com.bellatrixos.windows.WindowSize
import com.bellatrixos.ui.decodeImageIcon
import com.bellatrixos.ui.decodeSvgIcon
import com.bellatrixos.upgrade.Provisioner
import com.bellatrixos.wayland.AndroidClipboard
import com.bellatrixos.wayland.GuestImages
import com.bellatrixos.wayland.OutputInfo
import com.bellatrixos.wayland.WaylandServer
import com.bellatrixos.wayland.XdgToplevel
import com.bellatrixos.widgets.Shortcut
import com.bellatrixos.widgets.WidgetEngine
import com.bellatrixos.widgets.WidgetSpec
import com.bellatrixos.widgets.app
import com.bellatrixos.widgets.lang.Program
import com.bellatrixos.widgets.runtime.Dictation
import com.bellatrixos.widgets.runtime.PhoneLocation
import com.bellatrixos.widgets.runtime.PhoneCalendar
import com.bellatrixos.widgets.runtime.NotificationDots
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import com.bellatrixos.apps.AndroidAppCatalog
import com.bellatrixos.apps.AppUsage
import com.bellatrixos.apps.androidAppKey
import com.bellatrixos.apps.androidShortcutKey
import com.bellatrixos.linux.LinuxAppCatalog
import com.bellatrixos.storage.LauncherData
import com.bellatrixos.storage.PhoneFolders
import com.bellatrixos.taskbar.TaskBarStore

/**
 * Process-wide state: the launcher (themes, widgets, Android apps) and the
 * optional Linux system (the rootfs, the Wayland server, the window manager).
 */
class Runtime(
    private val context: Context,
) {
    val proot = Proot(context)
    val wm = WindowManager()
    val server = WaylandServer(proot.waylandSocket.path, wm).also { wm.server = it }
    private val guestImages = GuestImages(context, File(proot.tmpDir, "clipboard"), proot::guestPath)
    val keyboard = KeyboardBridge(wm, guestImages)
    val terminals = TerminalTiles(this)
    val style = StyleStore(context)

    /** Native view overrides from ~/.config/bellatrix/native-ui/look.toml. */
    val nativeUi = NativeUiStore(context)
    val terminalFont = TerminalFont(context, proot)
    val launcherFont = LauncherFont(proot, style, context.resources).also { it.restore() }
    val gestures = Gestures(context, style)
    val linuxApps = LinuxAppCatalog(context, proot)
    val androidApps = AndroidAppCatalog(context)
    val usage = AppUsage(context)
    val themes = ThemeStore(context, proot, style, isLinux = { linux }, onConfigChanged = ::syncGuest)
    private val audio = AudioBridge(proot, context)
    private val openBridge = OpenBridge(context, proot, onOpenFiles = ::openFiles, onOpenVoice = ::openVoice)
    val agents = AgentStore(proot, text = { id, arg -> context.getString(id, arg) }, onChanged = ::syncGuest)
    val packages = Packages(proot)

    /** Arch Linux's old files, if any are still in storage; see [LegacyArch]. */
    private val legacyArch = LegacyArch(context, proot.filesDir)
    var hasLegacyArch by mutableStateOf(legacyArch.present)
        private set
    val legacyArchRoot get() = legacyArch.rootfs

    /** Deletes them, for good: only on the user's say-so. */
    fun deleteLegacyArch() {
        scope.launch(Dispatchers.IO) {
            legacyArch.delete()
            hasLegacyArch = false
        }
    }
    val widgets = WidgetEngine(context, proot, this)

    /** The taskbar's pins as an agent-editable layout.toml, like home's. */
    val taskbar = TaskBarStore(context)

    /** Widgets' speech(...) and android.speech. */
    val dictation = Dictation(context)

    /** Widgets' location("request") and android.location. */
    val location = PhoneLocation(context)

    /** Widgets' android.calendar, calendar("request") and calendar("open"). */
    val calendar = PhoneCalendar(context)

    /** The phone's folders the user let Linux reach: in Files, and through `bellatrix`. */
    val phoneFolders = PhoneFolders(context)

    /** `bellatrix` in the guest (and `phone`, its old name): location, calendar and AirPods for agents, as far as widgets were allowed them; the phone folders. */
    private val phoneBridge by lazy { PhoneBridge(context, proot, location, calendar, { widgets.airpods }, phoneFolders, scope) }

    /** The voice screen's replies, in the phone's voice. */
    val speaker = Speaker(context)

    /** A spoken conversation with a coding agent: the voice screen's (VoiceActivity). */
    private val voiceSession = lazy { VoiceSession(context, proot, agents, speaker) }
    val voice by voiceSession

    init {
        // Not started just for this: a Voice that never ran has nothing to stop.
        agents.release = { agent -> voiceSession.isInitialized() && voice.release(agent.bin) }
    }
    val updates = SystemUpdates(proot, packages, agents)

    /** A newer launcher: on Google Play in the play build, on GitHub releases in the foss build. */
    val launcherUpdates = LauncherUpdates(context)

    /** Shell access for in-place updates, supplied by Shizuku/Sui in this same runtime. */
    val adb = AdbBridge()

    /** Messages from the developer, shown on home. */
    val announcements = com.bellatrixos.net.Announcements(context)
    val gpu = Gpu(context, proot, packages)

    /** The guest's resolv.conf, on the phone's DNS servers while Linux is up. */
    private val dns = LinuxDns(context, proot.rootfs)

    /** The launcher's own provided files (default widgets, their docs); see [Provisioner]. */
    private val launcherFiles =
        Provisioner(proot.filesDir, LauncherData.LAUNCHER_MANIFEST, listOf(widgets::managed, style::managed, nativeUi::managed))

    /** Every launcher-provided file in the guest; synced only while Linux is installed. */
    private val provisioner: Provisioner =
        Provisioner(
            proot.filesDir,
            Distro.active.state(LauncherData.GUEST_MANIFEST),
            listOf(
                { defaultApps(proot.applications) },
                { bundledIcons(context, proot.home) },
                openBridge::managed,
                { phoneBridge.managed() },
                proot::managed,
                agents::managed,
                themes::managed,
                widgets::guestManaged,
            ),
        )
    // Bellatrix's Bootstrap owns the three-argument form: agent selection is passed
    // to install()/queue() below, and provisioning stays with the launcher files.
    val bootstrap = Bootstrap(context, proot, packages)

    /** Its jobs write files and start the guest; one that fails (storage full) mustn't take the home screen down. */
    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main + CoroutineExceptionHandler { _, e -> Log.e(TAG, "background job failed", e) })
    private var started = false
    private var prepared = false

    private val prefs = LauncherData.prefs(context)

    /** Legacy Linux settings and the notification service listen here; the OWL screens collect [bootstrap.state]. */
    @Volatile
    var onStateChanged: () -> Unit = {}

    /** Legacy home's callback when a guest command is attempted before Linux is installed. */
    @Volatile
    var onLinuxUnavailable: () -> Unit = {
        showLinuxSettings()
    }

    /** The home screen's floating-window host; set while home is on screen, cleared as it leaves. */
    @Volatile
    var openFloatingWindow: ((String) -> Unit)? = null

    /** An owl app opened as a Bellatrix window instead of its own full-screen task. */
    @Volatile
    var openOwlAppWindow: ((String) -> Unit)? = null

    /** Files and Voice from the guest port use Bellatrix's own windows, never their old tasks. */
    @Volatile
    var openExplorer: ((String?) -> Unit)? = null
    @Volatile
    var openVoiceWindow: (() -> Unit)? = null

    /** Agents and Linux settings live in Bellatrix's own Settings window, never in an OWL screen. */
    @Volatile
    var openCodingAgents: (() -> Unit)? = null
    @Volatile
    var openLinuxSettings: (() -> Unit)? = null

    /** Licenses, packages and permissions are native pages of that same Settings window. */
    @Volatile
    var openLicenses: (() -> Unit)? = null
    @Volatile
    var openPackages: (() -> Unit)? = null
    @Volatile
    var openPermissions: (() -> Unit)? = null

    /** One hook for every native Settings page; the string is the nav id, or null for Settings itself. */
    @Volatile
    var openNativeSettings: ((String?) -> Unit)? = null

    /** The classic drawer re-reads the guest catalog when an agent or app is installed or removed. */
    @Volatile
    var onAppsChanged: (() -> Unit)? = null

    /** Compatibility with the legacy Linux settings and Terminal fragments. */
    val isLinuxReady get() = linux

    /**
     * How many times, and when last (0: never), a Linux app ended by SIGKILL that we didn't send: Android's
     * phantom process limit (Android 12+), a battery saver or low memory. Shown in settings › linux.
     */
    var kills by mutableStateOf(prefs.getInt(KILLS, 0))
        private set
    var lastKill by mutableStateOf(prefs.getLong(LAST_KILL, 0L))
        private set

    fun clearKills() {
        prefs.edit().remove(KILLS).remove(LAST_KILL).apply()
        kills = 0
        lastKill = 0L
    }

    private fun noteKill() {
        kills += 1
        lastKill = System.currentTimeMillis()
        prefs.edit().putInt(KILLS, kills).putLong(LAST_KILL, lastKill).apply()
    }

    /**
     * The first-run screen is done: from then on the home screen shows, with
     * or without Linux. Installs from before Linux was optional count as done.
     */
    var launcherReady by mutableStateOf(prefs.getBoolean(SETUP_DONE, false) || bootstrap.wanted)
        private set

    /** Linux is installed and its services run: guest commands (like the update check) can go. */
    private var linux = false

    /** The Linux install and removal; one at a time. */
    private var linuxJob: Job? = null

    init {
        // Saved, so removing Linux later doesn't bring the first-run screen back.
        if (launcherReady) prefs.edit().putBoolean(SETUP_DONE, true).apply()
        themes.loadPalette()
        // look.toml edits reach the native views already on screen, not just the next launch.
        NativeUiLook.install(context.applicationContext as Application, nativeUi, taskbar)
        // style.toml is authoritative for foot too, including on first start before any edit event.
        syncTerminalFontFromStyle()
        // An agent or the user edited style.toml; the rest of the style is read where it's drawn.
        style.onExternalChange = { old, new ->
            val changed = new.changed(old)
            if (Style.THEME.key in changed || Style.BACKGROUND.key in changed) scope.launch { themes.followStyle() }
        }
        // foot.ini follows the terminal settings, from the Style menu or the file alike.
        scope.launch {
            snapshotFlow { style.style[Style.TERMINAL_FONT] to style.style[Style.TERMINAL_TEXT] }
                .drop(1)
                .collect { syncTerminalFontFromStyle() }
        }
        scope.launch {
            snapshotFlow { style.style[Style.FONT] }.drop(1).collect { launcherFont.apply() }
        }
        // The recent strip's and home's pinned apps.
        androidApps.iconsFirst = {
            usage.recent.take(20).filter { it.startsWith("a:") }.mapTo(HashSet()) { it.substringAfter(':') } + widgets.shortcuts.items.mapNotNull(::androidKeyOf)
        }
        // Read before start, so the first load draws the pack's icons rather than drawing every icon twice.
        androidApps.iconPack = style.style[Style.ICON_PACK]
        scope.launch {
            snapshotFlow { style.style[Style.ICON_PACK] }.drop(1).collect { androidApps.iconPack = it }
        }
        androidApps.start()
        // Bellatrix's Super bindings inside Linux windows; the home screen handles its own (see MainActivity).
        keyboard.onShortcut = { code ->
            when (code) {
                KeyEvent.KEYCODE_ENTER -> true.also { terminal(null, "Terminal") }
                KeyEvent.KEYCODE_W -> wm.focused?.let { wm.close(it) } != null
                else -> false
            }
        }
        // Legacy Linux settings listen through onStateChanged; OWL screens collect the state flow directly.
        gpu.onChange = { scope.launch { onStateChanged() } }
        packages.onChange = { scope.launch { onStateChanged() } }
        // A terminal shown again after the text size changed catches up, with foot's own zoom keys.
        wm.created = { window -> if (followsTerminalFont(window.appId)) terminalFont.track(window) }
        wm.focusGained = { window -> wm.zoom(window, terminalFont.catchUp(window)) }
        wm.openAppWindow = { handle ->
            val window = wm.windowByHandle(handle)
            val name = window?.let { displayName(it.appId, it.title) }
            // Every guest toplevel is hosted in Bellatrix's own window while home is up: the same
            // frame, taskbar entry, drag/resize and close as the shell terminals. Away from home,
            // where that host is gone, an app keeps its own Android task as before.
            if (openFloatingWindow != null) {
                openFloatingWindow?.invoke(handle)
            } else {
                // Recents uses the activity's manifest label, not the task description's; see the aliases.
                val activity =
                    when (name) {
                        "Terminal" -> "TerminalWindow"
                        "btop" -> "BtopWindow"
                        else -> agentOf(window?.appId.orEmpty(), name)?.windowAlias ?: "LinuxWindowActivity"
                    }
                var flags = Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                // A terminal falling back while home is stopped must not leave a Recents card.
                if (activity == "TerminalWindow") {
                    flags = flags or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                }
                InstantActivity.startInstantly(
                    context,
                    Intent()
                        .setClassName(context, "com.bellatrixos.$activity")
                        .setData(Uri.parse("bellatrix://window/$handle"))
                        .addFlags(flags),
                )
            }
        }
    }

    /** style.toml's terminal values are the source of truth; foot.ini must follow both edits and first start. */
    private fun syncTerminalFontFromStyle() {
        val family = style.style[Style.TERMINAL_FONT]?.toString()?.takeIf { it.isNotBlank() } ?: "JetBrains Mono"
        val em = (style.style[Style.TERMINAL_TEXT] as? Number)?.toFloat() ?: TerminalText.Default.em
        val text = TerminalText.entries.minByOrNull { kotlin.math.abs(it.em - em) } ?: TerminalText.Default
        terminalFont.sync(family, text)
    }

    /** Runs a binding from native-ui/keys.toml through the actions Bellatrix already has. */
    private fun runGlobalAction(action: String): Boolean =
        when (action) {
            "terminal" -> {
                terminal(null, "Terminal")
                true
            }
            "files" -> {
                openFiles()
                true
            }
            "settings" -> {
                showNativeSettings()
                true
            }
            "linux-settings" -> {
                showLinuxSettings()
                true
            }
            "coding-agents" -> {
                showCodingAgents()
                true
            }
            "voice" -> {
                openVoice()
                true
            }
            "close" ->
                wm.focused?.let { focused ->
                    wm.close(focused)
                    true
                } ?: false
            else -> false
        }

    /** The launcher first (it never waits for Linux), then Linux if it's installed or an install was started. */
    fun start() {
        if (started) return
        started = true
        scope.launch { bootstrap.state.collect { onStateChanged() } }
        scope.launch {
            withContext(Dispatchers.IO) {
                launcherFiles.sync()
                agents.load()
            }
            // Linux apps are files on disk: listed now for the recent strip, not after Linux has started.
            if (bootstrap.wanted) scope.launch { linuxApps.refresh() } else linuxApps.listed = true
            // Widgets don't wait for the theme: they draw with the palette, which is already loaded, and the
            // wallpaper decode is the slow part of themes.load().
            widgets.start()
            themes.load()
            style.start()
            // One key dispatcher and one set of window rules, both fed by the guest-editable native-ui files.
            nativeUi.onKeysChange = { bindings -> GlobalKeys.bindings = bindings }
            nativeUi.onWindowRulesChange = { rules -> WindowSize.setRules(rules) }
            GlobalKeys.handler = { action -> runGlobalAction(action) }
            GlobalKeys.bindings = nativeUi.bindings
            WindowSize.setRules(nativeUi.windowRules)
            nativeUi.start()
            if (bootstrap.wanted) runLinux { bootstrap.run() } else bootstrap.run()
        }
    }

    /** The first-run screen is done; the home screen shows from now on. */
    fun finishWelcome() {
        prefs.edit().putBoolean(SETUP_DONE, true).apply()
        launcherReady = true
    }

    /** Called as the home leaves: an app opened elsewhere uses the full-window fallback again. */
    fun clearHomeHooks() {
        openFloatingWindow = null
        openOwlAppWindow = null
        openExplorer = null
        openVoiceWindow = null
        openCodingAgents = null
        openLinuxSettings = null
        openLicenses = null
        openPackages = null
        openPermissions = null
        openNativeSettings = null
        onAppsChanged = null
        onLinuxUnavailable = { showLinuxSettings() }
    }

    /** Legacy settings' install button: no picked agents, the same default install OWL's setup uses. */
    fun install() = runLinux { bootstrap.install() }

    /** Legacy names for the same Linux lifecycle operations the OWL settings use. */
    fun retry() = retryLinux()

    fun cancel() = cancelLinux()

    fun remove() = removeLinux()

    /** Installs Linux in the background, with [agents]; progress shows in [Bootstrap.state] and the notification. */
    fun installLinux(agents: List<Agent>) {
        val target = Distro.chosen
        if (target != Distro.active) {
            // Next to the system that runs: it gets its own folder and state, which only a fresh start picks up.
            // Bellatrix keeps the pending distro in Distro itself; the selected agents are installed after the restart.
            Distro.setNext(proot.filesDir, target)
            restartLauncher(context, proot)
            return
        }
        runLinux { bootstrap.install(agents.map { it.pkg }) }
    }

    /** Runs another installed system from the next start; restarts the launcher now, since all of Linux's state follows the system. */
    fun switchLinux(distro: Distro) {
        if (distro == Distro.active) return
        Distro.setNext(proot.filesDir, distro)
        restartLauncher(context, proot)
    }

    /** Continues a failed install, or a failed update of an installed system. */
    fun retryLinux() = runLinux { bootstrap.run() }

    /** Stops an install that hasn't completed, running or failed, and deletes what it downloaded and unpacked. */
    fun cancelLinux() {
        val install = linuxJob
        if (!bootstrap.cancel()) return
        linuxJob =
            scope.launch {
                install?.cancel()
                withContext(Dispatchers.IO) { proot.killAll() }
                install?.join()
                bootstrap.remove()
                setService(false)
                onStateChanged()
            }
    }

    /** Runs [work] with the foreground service up, so it goes on outside the app; then starts Linux if it's installed. */
    private fun runLinux(work: suspend () -> Unit) {
        if (linuxJob?.isActive == true) return
        linuxJob =
            scope.launch {
                setService(true)
                withContext(Dispatchers.IO) {
                    if (!prepared) {
                        proot.prepare()
                        prepared = true
                    }
                }
                work()
                if (bootstrap.isInstalled) onLinuxReady() else setService(false)
                onStateChanged()
            }
    }

    /**
     * Deletes Linux: its apps, agents, packages and home. Themes, widgets and
     * the home screen stay; widgets that need Linux turn back into placeholders.
     */
    fun removeLinux() {
        if (!bootstrap.isInstalled || linuxJob?.isActive == true) return
        linuxJob =
            scope.launch {
                linux = false
                launcherFont.forget()
                widgets.linuxChanged(false)
                audio.stop()
                withContext(Dispatchers.IO) { proot.killAll() }
                bootstrap.remove()
                withContext(Dispatchers.IO) {
                    updates.forget()
                    agents.load()
                }
                gpu.turnOff()
                linuxApps.clear()
                setService(false)
                onStateChanged()
            }
    }

    /** The foreground service keeps the process, and with it Linux, alive; only while Linux is installed or installing. */
    private fun setService(on: Boolean) {
        // Android 12+ refuses a foreground service started from the background; the work then goes on without it.
        runCatching { LinuxService.set(context, on) }.onFailure { Log.w(TAG, "cannot start the service", it) }
    }

    /** Guest configs follow the theme and the installed agents; only while there is a guest. */
    private var guestSync: Job? = null
    private val guestSyncMutex = Mutex()

    private fun syncGuest() {
        if (!linux) return
        // Theme and agent changes often land together; let the last one write the whole set.
        guestSync?.cancel()
        guestSync =
            scope.launch {
                // A cancelled sync may be stuck in blocking file I/O; still never overlap writes.
                guestSyncMutex.withLock {
                    withContext(Dispatchers.IO) { provisioner.sync() }
                    refreshApps()
                    onAppsChanged?.invoke()
                }
            }
    }

    /** Whether the bridges and the Wayland server are up; they run until the process ends, even if Linux is removed. */
    private var servicesStarted = false

    private suspend fun onLinuxReady() {
        if (linux) return
        if (!servicesStarted) {
            servicesStarted = true
            updateOutput()
            val clipboard = AndroidClipboard(context, guestImages, server::clipboardChanged)
            synchronized(server) { server.selection.system = clipboard }
            context.registerComponentCallbacks(
                object : ComponentCallbacks {
                    override fun onConfigurationChanged(newConfig: Configuration) = updateOutput()

                    @Deprecated("Deprecated in Java")
                    override fun onLowMemory() = Unit
                },
            )
            withContext(Dispatchers.IO) {
                server.start()
                proot.procShim.start()
                openBridge.start()
                phoneBridge.start()
                dns.start()
            }
        } else {
            // Linux installed again: its new home has no foot-font.ini yet.
            terminalFont.write()
        }
        linux = true
        // A font picked from Linux that wasn't remembered at start (or was installed since) loads now.
        scope.launch { launcherFont.apply() }
        // The recent apps strip on home lists Linux apps before the app list is ever opened.
        scope.launch { refreshApps() }
        // Themes picked without Linux are written into the guest now.
        themes.renderGuest()
        withContext(Dispatchers.IO) {
            provisioner.sync()
            audio.start()
            synchronized(server) { server.input.setKeymap(proot.keymap.path) }
            agents.load()
        }
        widgets.linuxChanged(true)
        refreshApps()
        withContext(Dispatchers.IO) { updates.load() }
        scope.launch {
            // Update steps that need the network; the home screen doesn't wait for them.
            bootstrap.runBacklog()
            updates.check()
        }
        // Apps render in software until the GPU passed its check on this system build.
        scope.launch { gpu.recheck() }
    }

    /** Turns GPU acceleration on (installing and checking the driver) or off, for Linux apps started from then on. */
    fun setGpu(on: Boolean) {
        if (!linux) return
        if (on) scope.launch { gpu.turnOn() } else gpu.turnOff()
    }

    /** Whether the Linux system is installed and running, so there are packages to check and update. */
    val linuxInstalled: Boolean get() = linux

    /** Checks the Linux packages and tools for updates now, on the runtime's scope: leaving settings doesn't cancel it. */
    fun checkSystemUpdates() {
        if (linux) scope.launch { updates.check(force = true) }
    }

    /** Checks for updates now, on the runtime's scope: leaving settings doesn't cancel it. */
    fun checkUpdates() {
        checkSystemUpdates()
        launcherUpdates.check(force = true)
    }

    /** Runs every pending update (packages and agents); the drawer then lists what changed. */
    fun updateSystem() {
        scope.launch {
            updates.updateAll()
            refreshApps()
        }
    }

    /**
     * Tells Wayland clients the screen's size and scale; they get it again on
     * rotation, a density change or a new Android font size, which also sizes
     * the terminal font.
     */
    private fun updateOutput() {
        val metrics = context.resources.displayMetrics
        val mmPerPx = 25.4f / metrics.xdpi
        val scale = outputScale()
        server.updateOutput(
            OutputInfo(
                metrics.widthPixels,
                metrics.heightPixels,
                scale,
                (metrics.widthPixels * mmPerPx).toInt(),
                (metrics.heightPixels * mmPerPx).toInt(),
            ),
        )
        terminalFont.update(metrics.density, context.resources.configuration.fontScale, scale, metrics.widthPixels)
    }

    /** wl_output scales are integers, so the density rounds down (2.625 -> 2); [TerminalFont] makes up the rest. */
    private fun outputScale() =
        context.resources.displayMetrics.density
            .toInt()
            .coerceIn(1, 4)

    /** Re-runs update steps that failed, e.g. offline; see [Bootstrap.updates]. */
    fun retryUpdates() {
        if (linux) scope.launch { bootstrap.runBacklog() }
    }

    /**
     * Launcher screens showing now: the home screen and Linux windows. Guest
     * helpers that only matter on screen (the /proc stand-ins) pause at zero.
     */
    private var screens = 0

    fun screenStarted() {
        if (screens++ == 0) proot.procShim.setActive(true)
        // Only checks when the last check is hours old; see SystemUpdates.
        if (linux) scope.launch { updates.check() }
    }

    fun screenStopped() {
        screens = (screens - 1).coerceAtLeast(0)
        if (screens == 0) proot.procShim.setActive(false)
    }

    /** Whether every kind of app has been listed once: Android's, Linux's and owl's. Before, a list of them would be missing some. */
    val appsListed get() = androidApps.listed && linuxApps.listed && widgets.loaded

    /** Rescans .desktop entries; see [LinuxAppCatalog.refresh]. */
    suspend fun refreshApps() {
        if (linux) linuxApps.refresh()
    }

    /** A pinned app's icon as the drawer shows it, or its silhouette; null until loaded, or if it has none. */
    fun shortcutIcon(
        shortcut: Shortcut,
        mono: Boolean,
    ): ImageBitmap? {
        androidKeyOf(shortcut)?.let { return if (mono) androidApps.masks[it] else androidApps.icons[it] }
        shortcut.owl?.let { file -> return widgets.specs.app(file)?.let { owlIcon(it, mono) } }
        val id = shortcut.linux?.id ?: return null
        return if (mono) linuxApps.masks[id] else linuxApps.icons[id]
    }

    /**
     * Owl apps' icons and silhouettes, by their SVG (or image) and the accent they were drawn in: a theme change draws
     * them anew, and drops the ones in the accent before once it has.
     */
    private val owlIcons = mutableStateMapOf<Pair<String, Int>, Pair<ImageBitmap, ImageBitmap>>()

    /** Icons drawn, or being drawn, into [owlIcons]; those with nothing to draw stay here and out of it. */
    private val owlIconsAsked = ConcurrentHashMap.newKeySet<Pair<String, Int>>()

    /**
     * An owl app's icon, or its silhouette; null if it has none. An SVG is drawn with `currentColor` in the theme's
     * accent, an asset's image (a PNG) as it is.
     */
    fun owlIcon(
        spec: WidgetSpec,
        mono: Boolean,
    ): ImageBitmap? {
        val program = spec.program ?: return null
        val key = program.iconKey ?: return null
        val accent = Palette.accent.toArgb()
        val drawn =
            owlIcons[key to accent] ?: run {
                // Drawn off the main thread, as Android's icons are: an SVG is slow to draw, and a theme change drew all of them in one frame.
                if (owlIconsAsked.add(key to accent)) {
                    scope.launch {
                        val icon = withContext(Dispatchers.Default) { drawOwlIcon(program, accent) } ?: return@launch
                        owlIcons[key to accent] = icon
                        owlIcons.keys.removeAll { it.first == key && it.second != accent }
                        owlIconsAsked.removeAll { it.first == key && it.second != accent }
                    }
                }
                // Until then, the one drawn in the accent before.
                owlIcons.entries.firstOrNull { it.key.first == key }?.value
            }
        return if (mono) drawn?.second else drawn?.first
    }

    /** Draws [specs]' icons in the accent now, all at once off the main thread: at start, so home and the recent apps have them from the first frame. */
    suspend fun drawOwlIcons(specs: List<WidgetSpec>) {
        val accent = Palette.accent.toArgb()
        val programs = specs.mapNotNull { it.program }.filter { p -> p.iconKey.let { it != null && owlIconsAsked.add(it to accent) } }
        val drawn = withContext(Dispatchers.Default) { programs.map { p -> async { p.iconKey!! to drawOwlIcon(p, accent) } }.awaitAll() }
        for ((key, icon) in drawn) if (icon != null) owlIcons[key to accent] = icon
    }

    private fun drawOwlIcon(
        program: Program,
        accent: Int,
    ): Pair<ImageBitmap, ImageBitmap>? {
        val size = (52 * context.resources.displayMetrics.density).toInt()
        val image = program.iconImage
        val bmp = if (image != null) decodeImageIcon(image, size) else decodeSvgIcon(program.icon!!, size, accent)
        return bmp?.let { it.asImageBitmap() to bitmapMask(it).asImageBitmap() }
    }

    /**
     * A pinned app's name as the drawer shows it, so it follows the phone's language and the app's updates; the name
     * it was pinned with while its app is gone or not listed yet, and for Linux apps. An owl app's follows its file's name.
     */
    fun shortcutLabel(shortcut: Shortcut) =
        androidKeyOf(shortcut)?.let { androidApps.labels[it] }
            ?: shortcut.owl?.let { widgets.specs.app(it)?.name }
            ?: shortcut.label

    /** Whether a pinned Android app or shortcut has a notification dot. */
    fun hasDot(shortcut: Shortcut) =
        shortcut.component?.let { NotificationDots.has(it.packageName, shortcut.profile) }
            ?: shortcut.androidShortcut?.let { NotificationDots.has(it.packageName, shortcut.profile, it.id) }
            ?: false

    /** A pinned Android app's or shortcut's key in [androidApps]; null for a Linux app. */
    private fun androidKeyOf(shortcut: Shortcut) =
        shortcut.component?.let { androidApps.pinned(it, shortcut.profile)?.key ?: androidAppKey(it, shortcut.profile) }
            ?: shortcut.androidShortcut?.let { androidShortcutKey(it, shortcut.profile) }

    /** Wayland app_id -> name for windows we started, so their Android tasks get a proper label. */
    private val launchedNames = ConcurrentHashMap<String, String>()

    /** Wayland app_id -> the agent running in it, for sessions not named after their agent ("New app"). */
    private val launchedAgents = ConcurrentHashMap<String, Agent>()

    /** Whether a window's foot takes its size from foot.ini, as its own terminals, plain foot and most tiles do. */
    private fun followsTerminalFont(appId: String) =
        when {
            appId == "foot" -> true
            launchedNames.containsKey(appId) -> !terminalFont.hasOwnSize(appId)
            appId.startsWith(TerminalTiles.APP_ID_PREFIX) -> !terminals.hasOwnFont(appId)
            else -> false
        }

    /**
     * Opens [command] (or a shell) in a terminal named [name]. Each gets its own
     * foot app_id, so Recents shows "Grok" or "btop" rather than one "foot" per window.
     * An [agent] session gets the agent's logo and Recents entry whatever it's named.
     */
    fun terminal(
        command: String?,
        name: String,
        agent: Agent? = null,
    ) {
        if (!linux) {
            onLinuxUnavailable()
            return
        }
        // Drawer entries and OWL commands don't carry the Agent; infer it so every launch gets the
        // same service/daemon policy and the right taskbar icon, not a generic terminal one.
        val effectiveAgent = agent ?: inferAgent(command, name)
        // Per agent, so a Claude and a Grok "New app" don't share an app_id.
        val slug = if (effectiveAgent == null || effectiveAgent.name == name) name else "${effectiveAgent.bin} $name"
        val appId = "bellatrix." + slug.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        launchedNames[appId] = name
        if (effectiveAgent != null) launchedAgents[appId] = effectiveAgent
        val script = command?.let { effectiveAgent?.prepared(it) ?: it }
        val args = script?.let { " -e bash -c " + shellQuote(it) }.orEmpty()
        launch("foot --app-id=$appId${terminalFont.optionFor(appId, command)}$args")
    }

    /** Opens a pinned app from the home screen. A Linux one runs its current desktop entry, which an update may have changed. */
    fun open(shortcut: Shortcut) {
        shortcut.linux?.let { pinned -> launch(linuxApps.apps.firstOrNull { it.id == pinned.id } ?: pinned) }
        shortcut.component?.let { c -> androidApps.pinned(c, shortcut.profile)?.let(androidApps::open) ?: androidApps.open(c, shortcut.profile) }
        shortcut.androidShortcut?.let { androidApps.open(it, shortcut.profile) }
        shortcut.owl?.let(::openOwlApp)
    }

    /** The home tile's "open in window" action, for the same kinds a drawer long-press offers. */
    fun openInWindow(shortcut: Shortcut) {
        shortcut.linux?.let { pinned ->
            launch(linuxApps.apps.firstOrNull { it.id == pinned.id } ?: pinned)
            return
        }
        shortcut.owl?.let {
            openOwlAppInWindow(it)
            return
        }
        shortcut.component?.let { c ->
            androidApps.pinned(c, shortcut.profile)?.let { androidApps.openInWindow(it) } ?: androidApps.openInWindow(c, shortcut.profile)
            return
        }
        shortcut.androidShortcut?.let { androidApps.openInWindow(it, shortcut.profile) }
    }

    fun launch(app: LinuxApp) {
        // Our own Files app: open it directly rather than round-tripping through the guest.
        if (app.exec == FILES_COMMAND) return openFiles()
        if (app.exec == EDIT_COMMAND) return edit()
        if (app.exec == VOICE_COMMAND) return openVoice()
        // Cliamp's binary is a downloaded optional step. A missing one used to make foot
        // appear and vanish at once, so finish that step first and never open an empty window.
        if (app.id == "cliamp") {
            scope.launch {
                bootstrap.runBacklog()
                refreshApps()
                onAppsChanged?.invoke()
                terminal(
                    // A Cliamp that stops with an error keeps its window, so the error can be read.
                    "if command -v cliamp >/dev/null 2>&1; then cliamp; s=\$?; " +
                        "if [ \$s -ne 0 ]; then echo \"Cliamp stopped with code \$s; see the lines above.\"; exec bash; fi; " +
                        "else echo 'Cliamp is still not installed. Check Linux settings > process log.'; exec bash; fi",
                    app.name,
                )
            }
            return
        }
        if (app.terminal) return terminal(app.command, app.name)
        // Like an Android icon: an app that already has a window comes back to it. Single-instance
        // apps would otherwise just exit on a second launch, showing nothing. Quickshell apps (App Finder)
        // all share the app_id org.quickshell, so their window title, which is their name, decides.
        wm.appWindows
            .firstOrNull { (it.appId.isNotEmpty() && app.matches(it.appId)) || it.title.equals(app.name, ignoreCase = true) }
            ?.let { existing ->
                // A window already hosted in home has no Android task to bring forward: ask the host to focus it.
                val handle = wm.handleOf(existing)
                if (handle != null && openFloatingWindow != null) {
                    openFloatingWindow?.invoke(handle)
                    return
                }
                if (bringToFront(existing)) return
            }
        launch(app.command)
    }

    /** Moves [window]'s Android task to the front; false if it has none (yet). */
    private fun bringToFront(window: XdgToplevel): Boolean {
        val handle = wm.handleOf(window) ?: return false
        val task =
            context.getSystemService(ActivityManager::class.java).appTasks.firstOrNull {
                runCatching { it.taskInfo?.baseIntent?.data?.lastPathSegment }.getOrNull() == handle
            } ?: return false
        // The task may have gone since it was listed.
        return runCatching { task.moveToFront() }.isSuccess
    }

    /** The name to show for a Linux window: launch name, then .desktop entry, then its title. */
    fun displayName(
        appId: String,
        title: String,
    ): String {
        launchedNames[appId]?.let { return it }
        linuxApps.apps.firstOrNull { it.matches(appId) }?.let { return it.name }
        return title.ifEmpty { appId.ifEmpty { context.getString(R.string.home_linux_window) } }
    }

    /** The agent running in a Linux window: one started for it, or one named after it. */
    fun agentOf(
        appId: String,
        name: String?,
    ): Agent? = launchedAgents[appId] ?: AGENTS.firstOrNull { it.name == name }

    /** Recovers the agent from a drawer command (plain, with arguments or prompt) or an exact window name. */
    private fun inferAgent(
        command: String?,
        name: String,
    ): Agent? {
        if (command.isNullOrBlank()) return null
        val commandBin = command.trimStart().substringBefore(' ')
        return AGENTS.firstOrNull { it.bin == commandBin || it.command == command.trim() || command.trimStart().startsWith(it.bin + " ") }
            ?: AGENTS.firstOrNull { it.name == name }
    }

    /**
     * A floating terminal keeps the pinned Terminal button; a coding agent gets its own taskbar/
     * Recents entry so its logo and name show instead of a second generic Terminal.
     */
    fun floatingWindowTag(handle: String): String {
        val window = wm.windowByHandle(handle) ?: return com.bellatrixos.linux.UILinuxWindow.TAG
        val agent = agentOf(window.appId, displayName(window.appId, window.title))
        if (agent != null) return LINUX_AGENT_TAG_PREFIX + agent.bin
        // A GUI app (Vesktop, a browser...) is its own taskbar entry, not a second "Terminal".
        if (isTerminalWindow(window.appId)) return com.bellatrixos.linux.UILinuxWindow.TAG
        return LINUX_APP_TAG_PREFIX + window.appId.replace(Regex("\\s+"), "_")
    }

    /** Whether a Wayland app_id is a foot terminal: plain foot, one we started with [terminal], or a home tile's. */
    private fun isTerminalWindow(appId: String) =
        appId.isEmpty() || appId == "foot" || launchedNames.containsKey(appId) || appId.startsWith(TerminalTiles.APP_ID_PREFIX)

    /** The drawer icon of the app called [name] (an agent's logo, say); null until loaded, or if it has none. */
    fun iconNamed(name: String): ImageBitmap? = linuxApps.apps.firstOrNull { it.name == name }?.let { linuxApps.icons[it.id] }

    /**
     * Opens Bellatrix's one Settings window. [page] is a native nav id (3 personalize, 4 apps,
     * 14 home, 17 licenses, 71 permissions, 91 packages, …); null opens Settings itself.
     */
    fun showNativeSettings(page: String? = null) {
        val hook = openNativeSettings
        if (hook != null) {
            hook(page)
            return
        }
        val intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (page != null) intent.putExtra(MainActivity.EXTRA_SETTINGS_PAGE, page)
        context.startActivity(intent)
    }

    /**
     * Coding Agents is a standalone page of Bellatrix's Settings (nav id 10). A widget or
     * voice screen asks for it through here instead of opening a ported Compose screen.
     */
    fun showCodingAgents() {
        val hook = openCodingAgents
        if (hook != null) hook() else showNativeSettings("10")
    }

    /** Same for the Linux page (nav id 9), used when a guest command is attempted before install. */
    fun showLinuxSettings() {
        val hook = openLinuxSettings
        if (hook != null) hook() else showNativeSettings("9")
    }

    /** Licenses is a native page of Bellatrix's Settings (nav id 17), never an Omalauncher screen. */
    fun showLicenses() {
        val hook = openLicenses
        if (hook != null) hook() else showNativeSettings("17")
    }

    /** Packages is a native page of Bellatrix's Settings (nav id 91). */
    fun showPackages() {
        val hook = openPackages
        if (hook != null) hook() else showNativeSettings("91")
    }

    /** Permissions is a native page of Bellatrix's Settings (nav id 71). */
    fun showPermissions() {
        val hook = openPermissions
        if (hook != null) hook() else showNativeSettings("71")
    }

    /** Opens the owl app in file [name] in its own task; nothing if it isn't running (removed, or broken). */
    fun openOwlApp(name: String) {
        if (widgets.isApp(name)) InstantActivity.startInstantly(context, OwlAppActivity.intent(context, name))
    }

    /** Opens an owl app in Bellatrix's own floating window while home is up; full screen stays the default tap. */
    fun openOwlAppInWindow(name: String) {
        if (!widgets.isApp(name)) return
        val inHome = openOwlAppWindow
        if (inHome != null) inHome(name) else openOwlApp(name)
    }

    /** Opens voice in a Bellatrix window while home is up; the old task is only a fallback away from home. */
    fun openVoice() {
        val inHome = openVoiceWindow
        if (inHome != null) {
            inHome()
            return
        }
        InstantActivity.startInstantly(context, Intent(context, VoiceActivity::class.java))
    }

    /** Opens the guest path in Bellatrix's Explorer, at [path] if given. */
    fun openFiles(path: String? = null) {
        val inHome = openExplorer
        if (inHome != null) {
            inHome(path)
            return
        }
        InstantActivity.startInstantly(context, Intent(context, FilesActivity::class.java).putExtra(FilesActivity.EXTRA_PATH, path))
    }

    /** Edits the guest [path] in nano, in a terminal window; without a path, an empty nano. */
    fun edit(path: String? = null) {
        if (path.isNullOrEmpty()) return terminal("nano", "Nano")
        terminal("nano ${shellQuote(path)}", path.substringAfterLast('/'))
    }

    /** Starts a command inside the guest; its output goes to logcat. Returns at once; [onExit] runs on the launch thread. */
    fun launch(
        command: String,
        onExit: () -> Unit = {},
    ) {
        if (!linux) {
            onLinuxUnavailable()
            onExit()
            return
        }
        Log.i(TAG, "launch: $command")
        thread(name = "guest-launch", isDaemon = true) {
            val code = runCatching { proot.run("exec $command") { Log.i(TAG, "[${command.take(24)}] $it") } }.getOrElse { -1 }
            Log.i(TAG, "exited $code: $command")
            // 137 is 128 + SIGKILL; not ours (see Proot.killAll) means Android's.
            if (code == EXIT_SIGKILL && System.currentTimeMillis() - proot.killedAllAt > OWN_KILL_WINDOW_MS) noteKill()
            onExit()
        }
    }

    companion object {
        private const val TAG = "runtime"
        private const val SETUP_DONE = "setup_done"
        private const val KILLS = "linux_kills"
        private const val LAST_KILL = "linux_last_kill"
        private const val EXIT_SIGKILL = 137
        private const val OWN_KILL_WINDOW_MS = 15_000L
    }
}
