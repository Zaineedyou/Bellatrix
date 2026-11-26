package com.bellatrixos.linux

import java.io.File

/** Key a guest `.desktop` entry carries in Bellatrix's existing drawer, before its desktop id. */
const val LINUX_APP_PREFIX = "linux:"

/**
 * Window tag (and so taskbar/Recents key) of a guest GUI app such as Vesktop, followed by its Wayland app_id. Like a
 * coding agent's [LINUX_AGENT_TAG_PREFIX], it keeps the app off the pinned Terminal button: Terminal opens and focuses
 * only terminals, so an app and a terminal can be open together.
 */
const val LINUX_APP_TAG_PREFIX = "LinuxApp."

data class LinuxApp(
    val name: String,
    /** The command, without the terminal wrapper for terminal apps. */
    val exec: String,
    val terminal: Boolean,
    /** Desktop file id (foot.desktop -> "foot"); GUI apps usually use it as their Wayland app_id. */
    val id: String,
    val wmClass: String?,
    /** Icon= value: a theme icon name or an absolute guest path. */
    val icon: String? = null,
    /** Path= from the desktop entry: the command runs in this guest directory. */
    val dir: String? = null,
) {
    /**
     * The shell command to run: the entry's working directory, then its Exec. Electron/Chromium apps need to be
     * told to use this compositor's Wayland socket; without it Vesktop starts an X11 window that has nowhere to go.
     */
    val command: String
        get() {
            val flags = mutableListOf<String>()
            if ("--ozone-platform" !in exec) flags += listOf("--ozone-platform=wayland", "--enable-features=UseOzonePlatform")
            if ("--no-sandbox" !in exec) flags += "--no-sandbox"
            val run = if (terminal || !electronLike || flags.isEmpty()) exec else "$exec ${flags.joinToString(" ")}"
            return if (dir.isNullOrBlank()) run else "cd ${guestQuote(dir)} && $run"
        }

    /** Known Electron shells; based on the desktop id, WM class, label and Exec so a renamed Vesktop still matches. */
    val electronLike: Boolean
        get() =
            sequenceOf(id, wmClass.orEmpty(), name, exec)
                .map { it.lowercase() }
                .any { value -> ELECTRON_NAMES.any { it in value } }
}

private val ELECTRON_NAMES =
    listOf("vesktop", "vencord", "discord", "webcord", "armcord", "electron", "chromium", "google-chrome", "brave-browser")

/** Quotes a guest path so spaces and quotes in an Exec or Path cannot become shell syntax. */
fun guestQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

/**
 * Whether a window with Wayland [appId] belongs to this app: by desktop id, StartupWMClass, or a segment of a
 * reverse-DNS id. Electron apps such as Vesktop use `io.github.Vesktop.Vesktop` for a `vesktop.desktop` entry;
 * matching only the old "tail after the last dot" missed those windows.
 */
fun LinuxApp.matches(appId: String): Boolean {
    val id = appId.lowercase()
    val own = this.id.lowercase()
    return own == id ||
        wmClass?.lowercase() == id ||
        own.substringAfterLast('.') == id ||
        id.split('.', '-').any { it == own }
}

private val FIELD_CODE = Regex("%[fFuUdDnNickvm]")

/**
 * Reads launchable entries from every guest application dir Bellatrix knows. A user entry replaces the system one
 * with the same file name (XDG), so a NoDisplay=true file in ~/.local/share/applications hides a package's entry.
 * AppImages in the guest's Applications, Downloads and /opt are offered too: they have no .desktop file until
 * their own runtime writes one, which is why an app like Vesktop could be installed and still absent from the drawer.
 */
fun loadLinuxApps(proot: Proot): List<LinuxApp> {
    val root = proot.rootfs
    val dirs =
        listOf(
            proot.applications,
            File(root, "usr/local/share/applications"),
            File(root, "usr/share/applications"),
            File(root, "var/lib/flatpak/exports/share/applications"),
            File(proot.home, ".local/share/flatpak/exports/share/applications"),
            File(proot.home, "Applications"),
        )
    val entries =
        dirs
            .flatMap { it.listFiles { f -> f.extension.equals("desktop", ignoreCase = true) }.orEmpty().toList() }
            .distinctBy { it.name }
            .mapNotNull { runCatching { parseDesktopEntry(it) }.getOrNull() }
    val appImages =
        listOf(File(proot.home, "Applications"), File(proot.home, "Downloads"), File(root, "opt"))
            .flatMap { it.walkTopDown().maxDepth(2).toList() }
            .filter { it.isFile && it.extension.equals("appimage", ignoreCase = true) }
            .map { appImageEntry(it, root) }
    return (entries + appImages)
        // The same app can ship a .desktop file and an AppImage; show one tile, preferring the desktop entry.
        .distinctBy { it.family }
        .sortedBy { it.name.lowercase() }
}

/** Groups a desktop entry and its `Vesktop-x86_64.AppImage` under one drawer tile, ignoring the arch suffix. */
internal val LinuxApp.family: String
    get() =
        name.lowercase()
            .replace(Regex("[-_ ]?(x86[-_]?64|x64|aarch64|arm64).*"), "")
            .replace(Regex("[^a-z0-9]+"), "")
            .ifBlank { id.lowercase().replace(Regex("[^a-z0-9]+"), "") }

/** Parses one guest `.desktop` file. Kept visible to the JVM tests for the entries installers actually write. */
internal fun parseDesktopEntry(file: File): LinuxApp? {
    var inEntry = false
    val fields = mutableMapOf<String, String>()
    file.forEachLine { raw ->
        val line = raw.trim()
        if (line.startsWith("[")) {
            inEntry = line.equals("[Desktop Entry]", ignoreCase = true)
        } else if (inEntry && '=' in line) {
            val key = line.substringBefore('=').trim()
            fields.putIfAbsent(key, line.substringAfter('=').trim())
        }
    }
    if (fields["Type"] != "Application") return null
    if (fields["NoDisplay"] == "true" || fields["Hidden"] == "true") return null
    val name = fields["Name"]?.takeIf { it.isNotBlank() } ?: return null
    val exec = fields["Exec"]?.replace(FIELD_CODE, "")?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return LinuxApp(
        name = name,
        exec = exec,
        terminal = fields["Terminal"] == "true",
        id = file.nameWithoutExtension,
        wmClass = fields["StartupWMClass"]?.ifBlank { null },
        icon = fields["Icon"]?.ifBlank { null },
        dir = fields["Path"]?.ifBlank { null },
    )
}

/**
 * An AppImage is a single executable file. In proot there is no FUSE mount, so ask the bundled runtime to extract
 * and run it; the id is the file name, which is also what most Electron AppImages report as their Wayland app_id.
 */
internal fun appImageEntry(
    file: File,
    rootfs: File,
): LinuxApp {
    val name = file.nameWithoutExtension
    // The scan sees host paths; proot's shell sees the same files under / inside the guest.
    val guestPath = "/" + file.relativeTo(rootfs).path.replace(File.separatorChar, '/')
    return LinuxApp(
        name = name,
        exec = "${guestQuote(guestPath)} --appimage-extract-and-run",
        terminal = false,
        id = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-'),
        wmClass = null,
        icon = null,
        dir = guestPath.substringBeforeLast('/').ifEmpty { "/" },
    )
}

private val ICON_SIZES = listOf("256x256", "192x192", "128x128", "96x96", "72x72", "64x64", "48x48")

/**
 * Finds an icon file for [icon] in the guest: an absolute path, or a hicolor/pixmaps
 * lookup by name, largest PNG first, then SVG (many apps only ship hicolor/scalable).
 */
fun resolveLinuxIcon(
    rootfs: File,
    icon: String,
): File? {
    if (icon.startsWith("/")) return File(rootfs, icon.removePrefix("/")).takeIf { it.isFile && it.extension in ICON_TYPES }
    val candidates =
        ICON_SIZES.map { "usr/share/icons/hicolor/$it/apps/$icon.png" } +
            "usr/share/pixmaps/$icon.png" +
            "root/.local/share/icons/hicolor/256x256/apps/$icon.png" +
            "usr/share/icons/hicolor/scalable/apps/$icon.svg" +
            "usr/share/pixmaps/$icon.svg" +
            "root/.local/share/icons/hicolor/scalable/apps/$icon.svg"
    return candidates.map { File(rootfs, it) }.firstOrNull { it.isFile }
}

private val ICON_TYPES = setOf("png", "svg")
