package com.bellatrixos.linux

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import com.bellatrixos.storage.PhoneStorage
import com.bellatrixos.upgrade.Managed
import com.bellatrixos.upgrade.Policy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.concurrent.thread

private const val FAST_LANE = "/usr/local/lib/libfastpath.so"

/**
 * Paths and process launching for the proot'd Linux rootfs.
 *
 * Ported 1:1 from Omalauncher's `linux.Proot`: the proot command
 * line, the environment, the binds, the temporary-directory handling and the
 * process bookkeeping are unchanged. The launcher-specific binds for widgets,
 * style and home point at Bellatrix's OWL directories under ~/.config/bellatrix.
 */
class Proot(
    private val context: Context,
) {
    private val nativeDir = File(context.applicationInfo.nativeLibraryDir)
    val filesDir: File = context.filesDir
    private val active = Distro.load(filesDir)
    val rootfs = File(filesDir, active.dir)
    val tmpDir = File(filesDir, "tmp")

    /** Bound to XDG_RUNTIME_DIR inside the guest; holds the Wayland socket and the bridges' FIFOs. */
    val runtimeDir = File(filesDir, "xdg")
    val waylandSocket = File(runtimeDir, "wayland-0")
    val keymap = File(filesDir, "keymap.xkb")

    /** The guest user's home, /root. */
    val home = File(rootfs, "root")

    /**
     * Where proot keeps the files behind hard links, which Android denies apps.
     */
    private val linkStore = File(rootfs, "var/lib/proot/l2s")
    val applications = File(home, ".local/share/applications")
    val launcherConfig = File(home, ".config/bellatrix")

    /** A listable stand-in for /sys. */
    private val sysDir = File(filesDir, "sys")

    /** Stand-ins for /proc files Android hides from apps. */
    val procShim = ProcShim(File(filesDir, "proc"))

    /** How guest apps render OpenGL and Vulkan: in software, or on the GPU once [Gpu] turns it on. */
    @Volatile
    var graphicsEnv: List<String> = SOFTWARE_ENV

    private val prefs = context.getSharedPreferences("bellatrix_linux", Context.MODE_PRIVATE)

    private val _cpuPinningState = MutableStateFlow(prefs.getBoolean(CPU_PINNING, true))

    /** Observable CPU pinning for the Compose settings row; the plain field below stays for existing callers. */
    val cpuPinningState: StateFlow<Boolean> = _cpuPinningState.asStateFlow()

    /** proot keeps programs making many syscalls on one fast CPU (built into the pinned proot). */
    @Volatile
    var cpuPinning: Boolean = _cpuPinningState.value
        private set

    fun changeCpuPinning(on: Boolean) {
        cpuPinning = on
        _cpuPinningState.value = on
        prefs.edit().putBoolean(CPU_PINNING, on).apply()
    }

    /** Android's Chinese, Japanese and Korean font, which the guest has none of. */
    private val cjkFont = File("/system/fonts/NotoSansCJK-Regular.ttc")

    /** proot's freestanding fast path, built by scripts/fetch-proot.sh when available. */
    private val fastLaneSource = File(nativeDir, "libfastpath.so")
    private val fastLane = File(rootfs, FAST_LANE.removePrefix("/"))

    /** Guest-visible copies of the launcher's data, at Bellatrix's exact paths. */
    private val dataBinds =
        listOf(
            GUEST_HOME + "/.config/bellatrix/widgets" to File(filesDir, "widgets"),
            GUEST_HOME + "/.config/bellatrix/style" to File(filesDir, "style"),
            GUEST_HOME + "/.config/bellatrix/native-ui" to File(filesDir, "native-ui"),
            GUEST_HOME + "/.config/bellatrix/home" to File(filesDir, "home"),
            GUEST_HOME + "/.config/bellatrix/taskbar" to File(filesDir, "taskbar"),
            GUEST_HOME + "/.config/bellatrix/themes" to File(filesDir, "themes"),
            GUEST_HOME + "/.config/bellatrix/backgrounds" to File(filesDir, "backgrounds"),
            GUEST_HOME + "/.local/share/bellatrix/themes" to File(filesDir, "bellatrix-themes"),
        )

    private val fixedBinds =
        listOf("/tmp" to tmpDir, "/dev/shm" to tmpDir, GUEST_RUNTIME_DIR to runtimeDir) + dataBinds

    /** Whether Android's shared storage can be bound into the guest whole. */
    fun storageAccess(): Boolean {
        if (!File(PRIMARY_STORAGE).isDirectory) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * With "All files access": the phone's own storage as /sdcard, and all of Android's /storage.
     * Without it: what PhoneStorage shows, the files Linux made there itself, so `bellatrix
     * ls/get/put/pick` and /sdcard paths keep working under scoped storage.
     */
    private val storageBinds: List<Pair<String, File>>
        get() =
            when {
                storageAccess() -> listOf(GUEST_SDCARD to File(PRIMARY_STORAGE), GUEST_STORAGE to File(STORAGE_ROOT))
                PhoneStorage.supported() -> listOf(PhoneStorage.GUEST to PhoneStorage.root)
                else -> emptyList()
            }

    /** Host directories bound over guest paths by [command]. */
    private val binds: List<Pair<String, File>>
        get() = fixedBinds + storageBinds

    /** Where an absolute guest path lives on the Android side, following the binds. */
    fun hostFile(guestPath: String): File {
        for ((guest, host) in binds) {
            if (guestPath == guest || guestPath.startsWith("$guest/")) return File(host, guestPath.removePrefix(guest).removePrefix("/"))
        }
        return File(rootfs, guestPath.removePrefix("/"))
    }

    /** Where the guest can see: the rootfs and the binds, as real paths. */
    private val visible get() = (listOf(rootfs) + binds.map { it.second }).map { it.canonicalFile }

    /** Whether [real], a canonical host file, is one the guest can see. */
    fun guestVisible(real: File) = visible.any { real.startsWith(it) }

    /** The guest's files as it sees them; see [GuestFs]. */
    val guestFs by lazy { GuestFs(::hostFile, ::guestVisible) }

    /** The empty directory in the rootfs that the phone's storage is bound over. */
    private val storageMount get() = File(rootfs, PhoneStorage.GUEST.removePrefix("/"))

    /** The guest path of a host file, the reverse of [hostFile]. */
    fun guestPath(host: File): String? {
        for ((guest, dir) in binds) {
            if (host.startsWith(dir)) return (guest + "/" + host.relativeTo(dir).path).trimEnd('/')
        }
        return if (host.startsWith(rootfs)) "/" + host.relativeTo(rootfs).path else null
    }

    /**
     * Resets what only lives as long as our process and writes the host-side
     * stand-ins every guest command needs. Call once, before any guest runs.
     */
    fun prepare() {
        discardTmp()
        deleteTree(runtimeDir)
        runtimeDir.mkdirs()
        // proot skips a bind whose host directory is missing; empty mount points also make
        // the bound directories show up in listings of their parents inside the guest.
        for ((guest, host) in dataBinds) {
            host.mkdirs()
            if (rootfs.isDirectory) File(rootfs, guest.removePrefix("/")).mkdirs()
        }
        // /sdcard must exist in / even without full storage access; Android 10 has no scoped path at all.
        if (rootfs.isDirectory) storageMount.let { if (PhoneStorage.supported()) it.mkdirs() else it.delete() }
        if (rootfs.isDirectory && storageAccess()) {
            for (guest in listOf(GUEST_SDCARD, GUEST_STORAGE)) File(rootfs, guest.removePrefix("/")).mkdirs()
        }
        File(rootfs, "var/lib/xbps/lock").delete()
        if (rootfs.isDirectory) {
            val stub = File(rootfs, GUEST_CJK_FONT.removePrefix("/"))
            if (cjkFont.isFile) {
                stub.parentFile?.mkdirs()
                stub.createNewFile()
            } else {
                stub.delete()
            }
            installManagedFiles()
        }
        for (sub in listOf("devices/system/cpu", "class", "block", "bus", "dev", "fs", "kernel")) File(sysDir, sub).mkdirs()
        File(rootfs, "usr/bin/bwrap").takeIf { it.isFile && it.length() == 0L }?.delete()
        File(filesDir, "bwrap-shim").delete()
    }

    /** Owns the launcher's guest-side shims; called from [prepare] before any guest runs. */
    private fun installManagedFiles() {
        val bwrap = File(rootfs, "usr/local/bin/bwrap")
        runCatching {
            bwrap.parentFile?.mkdirs()
            context.assets.open("bwrap-shim.sh").use { input -> bwrap.outputStream().use { input.copyTo(it) } }
            bwrap.setExecutable(true, false)
        }.onFailure { Log.w("proot", "can't install bwrap shim", it) }
        if (fastLaneSource.isFile) {
            runCatching {
                fastLane.parentFile?.mkdirs()
                fastLaneSource.copyTo(fastLane, overwrite = true)
                fastLane.setExecutable(true, false)
            }.onFailure { Log.w("proot", "can't install fastpath", it) }
        }
    }

    private val discardedTmp = File(filesDir, "tmp-discarded")

    /** Starts the guest with an empty /tmp, deleting the old one in the background. */
    private fun discardTmp() {
        discardedTmp.mkdirs()
        if (tmpDir.exists() && !tmpDir.renameTo(File(discardedTmp, System.nanoTime().toString()))) deleteTree(tmpDir)
        tmpDir.mkdirs()
        thread(name = "tmp-discard", isDaemon = true) {
            runCatching { deleteTree(discardedTmp) }.onFailure { Log.w("proot", "old /tmp", it) }
        }
    }

    private fun command(script: String): List<String> =
        listOf(
            File(nativeDir, "libproot.so").path,
            "--kill-on-exit",
            "--link2symlink",
            "-0",
            "-r",
            rootfs.path,
        ) + procShim.binds.flatMap { (guest, host) -> listOf("-b", "${host.path}:$guest") } +
            listOf(
                "-b", "/dev",
                "-b", "/proc",
                "-b", "${sysDir.path}:/sys",
                "-b", "/sys/devices/system/cpu",
            ) + (if (cjkFont.isFile) listOf("-b", "${cjkFont.path}:$GUEST_CJK_FONT") else emptyList()) +
            binds.flatMap { (guest, host) -> listOf("-b", "${host.path}:$guest") } +
            listOf(
                "-w", GUEST_HOME,
                "/usr/bin/env", "-i",
                "HOME=$GUEST_HOME",
                "USER=root",
                "LANG=C.UTF-8",
                "TZ=${java.util.TimeZone.getDefault().id}",
                "TERM=xterm-256color",
                "PATH=/usr/local/sbin:/usr/local/bin:/usr/bin:/usr/sbin:/root/.local/bin:/root/.local/share/mise/shims",
                "TAR_OPTIONS=--no-same-permissions",
                "MISE_NPM_PACKAGE_MANAGER=npm",
                "DISABLE_AUTOUPDATER=1",
                "OPENCODE_DISABLE_AUTOUPDATE=1",
                "GROK_DISABLE_AUTOUPDATER=1",
                "CLAUDE_CODE_HARBOR_KITE=0",
                "BROWSER=xdg-open",
                "EDITOR=micro",
                "VISUAL=micro",
                "XDG_RUNTIME_DIR=$GUEST_RUNTIME_DIR",
                "WAYLAND_DISPLAY=${waylandSocket.name}",
                "XDG_SESSION_TYPE=wayland",
                "GDK_BACKEND=wayland",
                "QT_QPA_PLATFORM=wayland",
                "MOZ_ENABLE_WAYLAND=1",
                "SDL_VIDEODRIVER=wayland",
            ) + graphicsEnv +
            (if (cpuPinning && fastLane.isFile) listOf("LD_PRELOAD=$FAST_LANE") else emptyList()) +
            listOf("/bin/sh", "-c", script)

    /** When [killAll] last ran. */
    @Volatile
    var killedAllAt = 0L
        private set

    /** Kills every proot we started and every guest process. */
    fun killAll() {
        killedAllAt = System.currentTimeMillis()
        val proot = File(nativeDir, "libproot.so").path
        val loader = File(nativeDir, "libproot-loader.so").path
        for (dir in File("/proc").listFiles().orEmpty()) {
            val pid = dir.name.toIntOrNull() ?: continue
            val argv0 = runCatching { File(dir, "cmdline").readBytes().takeWhile { it != 0.toByte() }.toByteArray().decodeToString() }.getOrNull()
            val exe = runCatching { android.system.Os.readlink(File(dir, "exe").path) }.getOrNull()
            if (argv0 == proot || exe == loader) runCatching { android.system.Os.kill(pid, android.system.OsConstants.SIGKILL) }
        }
    }

    /** A guest `sh -c [script]`, stderr merged into stdout. Needs [prepare] to have run. */
    fun process(script: String): ProcessBuilder =
        ProcessBuilder(command(script)).apply {
            environment().apply {
                clear()
                put("PROOT_LOADER", File(nativeDir, "libproot-loader.so").path)
                put("PROOT_TMP_DIR", tmpDir.path)
                put("LD_LIBRARY_PATH", nativeDir.path)
                if (rootfs.isDirectory && (linkStore.isDirectory || linkStore.mkdirs())) put("PROOT_L2S_DIR", linkStore.canonicalPath)
                if (!cpuPinning) {
                    put("PROOT_HOME_CPU", "off")
                    put("PROOT_FASTPATH", "off")
                }
            }
            redirectErrorStream(true)
        }

    /** Files this launcher owns inside the guest, so provisioning can keep them current. */
    fun managed(): List<Managed> =
        listOf(Managed.asset(context, "bwrap-shim.sh", File(rootfs, "usr/local/bin/bwrap"), Policy.Owned, executable = true)) +
            if (fastLaneSource.isFile) listOf(Managed(fastLane, Policy.Owned) { fastLaneSource.readBytes() }) else emptyList()

    /** The host file for an absolute guest path, following the guest's own symlinks and binds. */
    fun guestFile(guestPath: String): File? = runCatching { guestFs.host(guestPath) }.getOrNull()

    companion object {
        private const val CPU_PINNING = "proot_cpu_pinning"

        const val GUEST_HOME = "/root"
        const val GUEST_RUNTIME_DIR = "/run/user/0"
        const val GUEST_SDCARD = "/sdcard"
        const val GUEST_STORAGE = "/storage"

        private const val PRIMARY_STORAGE = "/storage/emulated/0"
        private const val STORAGE_ROOT = "/storage"
        private const val GUEST_CJK_FONT = "/usr/share/fonts/android/NotoSansCJK-Regular.ttc"
    }
}
