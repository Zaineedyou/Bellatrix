package com.bellatrixos.linux

import android.app.ActivityManager
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import android.view.KeyEvent
import com.bellatrixos.LinuxService
import com.bellatrixos.LinuxWindowActivity
import com.bellatrixos.R
import com.bellatrixos.wayland.AndroidClipboard
import com.bellatrixos.wayland.GuestImages
import com.bellatrixos.wayland.KeyboardBridge
import com.bellatrixos.wayland.LinuxWindowManager
import com.bellatrixos.wayland.OutputInfo
import com.bellatrixos.wayland.WaylandServer
import com.bellatrixos.wayland.XdgToplevel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * Process-wide state for Bellatrix's Linux system: proot, the Wayland compositor,
 * the window manager, the keybar bridge and the GPU settings. Ported from
 * Omalauncher's `Runtime`, with the launcher's themes, widgets and agents left
 * out: only what Terminal, Proot and GPU acceleration need is kept.
 */
class LinuxRuntime private constructor(
    private val context: Context,
) {
    val proot = Proot(context)
    val wm = LinuxWindowManager()
    val server = WaylandServer(proot.waylandSocket.path, wm).also { wm.server = it }
    private val guestImages = GuestImages(context, File(proot.tmpDir, "clipboard"), proot::guestPath)
    val keyboard = KeyboardBridge(wm, guestImages)
    val packages = Packages(proot)
    val bootstrap = Bootstrap(context, proot, packages)
    val gpu = Gpu(context, proot, packages)
    val terminalFont = TerminalFont(context, proot)
    private val dns = LinuxDns(context, proot.rootfs)

    /** Called when a guest command is attempted before Linux is installed. The host app shows its Linux settings. */
    @Volatile
    var onLinuxUnavailable: () -> Unit = {}

    /** Invoked when the install state changes, so Bellatrix's settings can redraw. */
    @Volatile
    var onStateChanged: () -> Unit = {}

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main +
                CoroutineExceptionHandler { _, e -> Log.e(TAG, "background job failed", e) },
        )

    private val launchedNames = ConcurrentHashMap<String, String>()
    private val prefs = context.getSharedPreferences("bellatrix_linux", Context.MODE_PRIVATE)

    @Volatile
    private var started = false

    @Volatile
    private var prepared = false

    @Volatile
    private var linux = false

    @Volatile
    private var servicesStarted = false

    private var linuxJob: Job? = null
    private var screens = 0

    /** Linux is installed, its services run and guest commands can go. */
    val isLinuxReady get() = linux

    init {
        gpu.onChange = { scope.launch { onStateChanged() } }
        packages.onChange = { scope.launch { onStateChanged() } }
        wm.openAppWindow = { handle -> openWindow(handle) }
        wm.created = { window -> if (followsTerminalFont(window.appId)) terminalFont.track(window) }
        wm.focusGained = { window -> wm.zoom(window, terminalFont.catchUp(window)) }
        keyboard.onShortcut = { code ->
            when (code) {
                KeyEvent.KEYCODE_ENTER -> true.also { terminal(null, "Terminal") }
                KeyEvent.KEYCODE_W -> wm.focused?.let { wm.close(it) } != null
                else -> false
            }
        }
    }

    /** Began once from the launcher's lifecycle; never waits for Linux and never blocks the UI. */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            withContext(Dispatchers.IO) { proot.prepare() }
            prepared = true
            if (bootstrap.wanted) {
                runLinux { bootstrap.run() }
            } else {
                // foot.ini follows Android's font size and display density even before Linux exists.
                updateOutput()
            }
        }
    }

    /** Starts an install. The chosen distro is the only one this port supports. */
    fun install() {
        bootstrap.choose()
        runLinux { bootstrap.install() }
    }

    /** Continues a failed install or update. */
    fun retry() = runLinux { bootstrap.run() }

    /** Cancels an install that hasn't completed and deletes what it downloaded and unpacked. */
    fun cancel() {
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

    /** Deletes Linux and everything the guest wrote. Bellatrix's own settings stay. */
    fun remove() {
        if (!bootstrap.isInstalled || linuxJob?.isActive == true) return
        linuxJob =
            scope.launch {
                linux = false
                withContext(Dispatchers.IO) { proot.killAll() }
                bootstrap.remove()
                gpu.turnOff()
                setService(false)
                onStateChanged()
            }
    }

    /** Turns GPU acceleration on (installing and checking the driver) or off. */
    fun setGpu(on: Boolean) {
        if (!linux) {
            onLinuxUnavailable()
            return
        }
        if (on) scope.launch { gpu.turnOn() } else gpu.turnOff()
    }

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

    private fun setService(on: Boolean) =
        runCatching { LinuxService.set(context, on) }
            .onFailure { Log.w(TAG, "cannot start the service", it) }

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
                dns.start()
            }
        } else {
            terminalFont.write()
        }
        linux = true
        withContext(Dispatchers.IO) {
            synchronized(server) { server.input.setKeymap(proot.keymap.path) }
        }
        terminalFont.write()
        scope.launch { bootstrap.runBacklog() }
        scope.launch { gpu.recheck() }
    }

    /** Tells Wayland clients the screen's size and scale, and sizes the terminal font. */
    private fun updateOutput() {
        val metrics = context.resources.displayMetrics
        val mmPerPx = 25.4f / metrics.xdpi
        val scale =
            metrics.density
                .toInt()
                .coerceIn(1, 4)
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

    fun screenStarted() {
        if (screens++ == 0) proot.procShim.setActive(true)
    }

    fun screenStopped() {
        screens = (screens - 1).coerceAtLeast(0)
        if (screens == 0) proot.procShim.setActive(false)
    }

    /** Opens [command] in a terminal named [name], with its own foot app_id and Recents entry. */
    fun terminal(
        command: String?,
        name: String,
    ) {
        if (!linux) {
            onLinuxUnavailable()
            return
        }
        val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        val appId = "omarchy.$slug"
        launchedNames[appId] = name
        val args = command?.let { " -e bash -c " + shellQuote(it) }.orEmpty()
        launch("foot --app-id=$appId${terminalFont.optionFor(appId, command)}$args")
    }

    /** The label to show for a Linux window: the launch name, then its title. */
    fun displayName(
        appId: String,
        title: String,
    ): String = launchedNames[appId] ?: title.ifEmpty { appId.ifEmpty { context.getString(R.string.linux_window_default) } }

    private fun followsTerminalFont(appId: String) =
        when {
            appId == "foot" -> true
            launchedNames.containsKey(appId) -> !terminalFont.hasOwnSize(appId)
            else -> false
        }

    /**
     * Starts a command inside the guest. Output goes to logcat, and returns at
     * once; SIGKILL not sent by us is counted as an Android phantom-process kill.
     */
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
            if (code == 137 && System.currentTimeMillis() - proot.killedAllAt > 15_000L) noteKill()
            onExit()
        }
    }

    private fun noteKill() {
        val kills = prefs.getInt(KILLS, 0) + 1
        val at = System.currentTimeMillis()
        prefs.edit().putInt(KILLS, kills).putLong(LAST_KILL, at).apply()
    }

    val kills get() = prefs.getInt(KILLS, 0)

    val lastKill get() = prefs.getLong(LAST_KILL, 0L)

    fun clearKills() = prefs.edit().remove(KILLS).remove(LAST_KILL).apply()

    private fun openWindow(handle: String) {
        val window = wm.windowByHandle(handle) ?: return
        val name = displayName(window.appId, window.title)
        val activity =
            when (name) {
                "Terminal" -> "TerminalWindow"
                "btop" -> "BtopWindow"
                else -> "LinuxWindowActivity"
            }
        runCatching {
            context.startActivity(
                Intent()
                    .setClassName(context, "com.bellatrixos.$activity")
                    .setData(Uri.parse("omarchy://window/$handle"))
                    .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
                ),
            )
        }.onFailure { Log.w(TAG, "cannot open $name", it) }
    }

    /** Brings an existing window's Android task forward; false if it has none (yet). */
    fun bringToFront(window: XdgToplevel): Boolean {
        val handle = wm.handleOf(window) ?: return false
        val task =
            context.getSystemService(ActivityManager::class.java).appTasks.firstOrNull {
                runCatching { it.taskInfo?.baseIntent?.data?.lastPathSegment }.getOrNull() == handle
            } ?: return false
        return runCatching { task.moveToFront() }.isSuccess
    }

    companion object {
        private const val TAG = "linux-runtime"
        private const val KILLS = "linux_kills"
        private const val LAST_KILL = "linux_last_kill"

        @Volatile
        private var instance: LinuxRuntime? = null

        fun get(context: Context): LinuxRuntime =
            instance ?: synchronized(this) {
                instance ?: LinuxRuntime(context.applicationContext).also { instance = it }
            }
    }
}
