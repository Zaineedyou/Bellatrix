package com.bellatrixos.linux

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** A rootfs to download: where from, what to call it in the cache, its checksum when the distro publishes one. */
data class RootfsSource(
    val url: String,
    val file: String,
    val sha256: String?,
    val xz: Boolean,
)

/**
 * The Linux the launcher installs: Void Linux (glibc, xbps). Kept as a list so a system has its own
 * folder and state and another could be added next to it; today there is one. Everything that would
 * differ between systems lives here: where the rootfs comes from, and the commands of the package manager.
 *
 * Ported 1:1 from Omalauncher `dev.omalauncher.linux.Distro`; the only change is that Compose's
 * `mutableStateOf` is replaced by a plain volatile field (Bellatrix does not depend on Compose).
 */
enum class Distro(
    val id: String,
    val label: String,
    val note: String,
) {
    Void("void", "Void Linux", "xbps, glibc · terminal apps, coding agents, GPU drivers"),
    ;

    /** The package manager's name, for messages and for the `requires { ... }` key of widgets. */
    val manager get() = "xbps"

    /** How to install a package, for messages. */
    val installHint get() = "xbps-install -S"

    /** What the packages screen says about installing. */
    val packagesNote get() = "Void Linux packages via xbps. Installing also updates the system, so it can take a while."

    /** The rootfs folder under the app's files. */
    val dir get() = id

    /** [name] for this distro's own copy of a state file: `-id` goes before the extension. */
    fun state(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot < 0) "$name-$id" else name.substring(0, dot) + "-$id" + name.substring(dot)
    }

    /** Where the guest's tarball is found now. Void's server is asked for the current file name and checksum. */
    fun resolveRootfs(): RootfsSource {
        val sums = fetchText("$VOID_BASE/sha256sum.txt")
        val name =
            Regex("void-aarch64-ROOTFS-[0-9]+\\.tar\\.xz").find(sums)?.value
                ?: error("couldn't find Void's rootfs on its server")
        val quoted = Regex.escape(name)
        // Both `hash  file` and BSD's `SHA256 (file) = hash`.
        val sum =
            Regex("([0-9a-f]{64})\\s+\\*?$quoted").find(sums)?.groupValues?.get(1)
                ?: Regex("$quoted\\)?\\s*=\\s*([0-9a-f]{64})").find(sums)?.groupValues?.get(1)
        return RootfsSource("$VOID_BASE/$name", name, sum, xz = true)
    }

    /**
     * Installs the terminal and the tools the launcher's scripts lean on. The first list must install;
     * the second goes one package at a time, so a name this distro spells differently is skipped (and
     * listed in the log) instead of failing the whole setup.
     */
    fun installScript(): String =
        "xbps-install -Suy xbps && xbps-install -Suy && xbps-install -y $VOID_CORE && " +
            "for p in $VOID_EXTRAS; do xbps-install -y \"\$p\" || echo \"skipped \$p\"; done; $LAVAPIPE_LINK; true"

    /**
     * JetBrains Mono (the terminals' default font) and the Nerd Font symbols, downloaded.
     */
    fun fontsScript(): String =
        "d=/usr/local/share/fonts/omalauncher; mkdir -p \$d && cd \$d && " +
            "get() { f=\$1; shift; for u in \"\$@\"; do curl -fsSL --connect-timeout 10 --max-time 60 -o \"\$f.tmp\" \"\$u\" && " +
            "mv \"\$f.tmp\" \"\$f\" && return 0; done; rm -f \"\$f.tmp\"; return 1; }; " +
            "{ fc-list 2>/dev/null | grep -qi 'JetBrains Mono' || { get JetBrainsMono.ttf '$JETBRAINS_MONO_URL' '$JETBRAINS_MONO_MIRROR' && " +
            "{ get JetBrainsMono-Italic.ttf '$JETBRAINS_MONO_ITALIC_URL' '$JETBRAINS_MONO_ITALIC_MIRROR' || true; }; }; } && " +
            "{ fc-list 2>/dev/null | grep -qi 'Symbols Nerd Font' || " +
            "{ get nerd.tar.xz '$NERD_SYMBOLS_URL'; r=\$?; [ \$r -eq 0 ] && tar -xJf nerd.tar.xz --wildcards '*SymbolsNerdFont*-Regular.ttf'; r=\$?; " +
            "rm -f nerd.tar.xz; [ \$r -eq 0 ]; }; } && " +
            "{ fc-cache -f \$d 2>/dev/null; true; }"

    /** The loader, Mesa's tools and the libraries the pinned GPU blobs and the patched Mesa link against. */
    fun gpuPackagesScript(): String =
        "{ xbps-install -Suy xbps && xbps-install -Suy && for p in $VOID_GPU_PACKAGES; do xbps-install -y \"\$p\" || echo \"skipped \$p\"; done; " +
            "ldconfig 2>/dev/null; $LAVAPIPE_LINK; }"

    /** What the launcher relies on, so the packages screen doesn't offer to remove it. */
    fun corePackages(): Set<String> = (VOID_CORE + " " + VOID_EXTRAS).split(' ').toSet() + setOf("base-system", "base-minimal", "xbps", "glibc")

    /** Installs [packages], after a full upgrade. */
    fun installCommand(packages: Collection<String>): String =
        "xbps-install -Suy xbps && xbps-install -Suy ${packages.joinToString(" ") { shellQuote(it) }} && xbps-remove -yO"

    /** Removes [name] and the dependencies nothing else needs. */
    fun removeCommand(name: String): String = "xbps-remove -Ry ${shellQuote(name)}"

    /** Prints `pkg <name>` for each of [quoted] (shell-quoted names) that isn't installed. */
    fun missingScript(quoted: String): String =
        "for p in $quoted; do xbps-query \"\$p\" >/dev/null 2>&1 || echo \"pkg \$p\"; done; "

    /** Pending updates: `#packages`, then `name old -> new` lines. */
    fun checkScript(): String =
        "if xbps-install -S >/dev/null 2>&1; then echo '#packages'; " +
            "xbps-install -un 2>/dev/null | awk '$2==\"update\"{print $1 \" -> new\"}'; " +
            "else echo '#packages-error'; fi"

    fun updateScript(): String = "xbps-install -Suy xbps && xbps-install -Suy && xbps-remove -yO"

    companion object {
        /** What an install started now uses. */
        @Volatile
        var chosen: Distro = Void

        /** The distro this process runs. Fixed at start by [load]. */
        @Volatile
        var active: Distro = Void
            private set

        fun load(filesDir: File): Distro = read(File(filesDir, POINTER)).also { active = it }

        /** Which distro the next start runs; the one running keeps going until the launcher restarts. */
        fun setNext(filesDir: File, distro: Distro) {
            File(filesDir, POINTER).writeText(distro.id)
        }

        /** Whether [distro] has an install, started or done, in [filesDir]. */
        fun installed(filesDir: File, distro: Distro): Boolean =
            File(filesDir, distro.state("setup-agents")).exists() ||
                runCatching { File(filesDir, distro.state("rootfs-steps.json")).readText().replace(" ", "").contains("\"setup\":true") }
                    .getOrDefault(false)

        /** After [removed] was deleted: the next start runs another installed distro if there is one, else the default. */
        fun afterRemoval(filesDir: File, removed: Distro) {
            val other = entries.firstOrNull { it != removed && installed(filesDir, it) }
            if (other != null) setNext(filesDir, other) else File(filesDir, POINTER).delete()
        }

        private const val POINTER = "setup-distro"

        /** The distro recorded in [file]; Void when there is none, or when it names one that's gone. */
        fun read(file: File): Distro =
            runCatching { file.readText().trim() }.getOrNull()?.let { id -> entries.find { it.id == id } } ?: Void

        /** Cache file prefixes of every distro's tarball, to clean up after a removal. */
        val tarballPrefixes = listOf("void-aarch64-")

        /** Lavapipe's manifest is named `lvp_icd.aarch64.json` on Void; software rendering looks for `lvp_icd.json`. */
        const val LAVAPIPE_LINK =
            "f=\$(ls /usr/share/vulkan/icd.d/lvp_icd.*.json 2>/dev/null | head -n 1); " +
                "[ -z \"\$f\" ] || ln -sf \"\$f\" /usr/share/vulkan/icd.d/lvp_icd.json"

        private const val VOID_BASE = "https://repo-default.voidlinux.org/live/current"

        /** Google Fonts' copy of JetBrains Mono, variable fonts: all weights in one file, upright and italic. */
        private const val JETBRAINS_MONO_URL = "https://raw.githubusercontent.com/google/fonts/main/ofl/jetbrainsmono/JetBrainsMono%5Bwght%5D.ttf"
        private const val JETBRAINS_MONO_ITALIC_URL =
            "https://raw.githubusercontent.com/google/fonts/main/ofl/jetbrainsmono/JetBrainsMono-Italic%5Bwght%5D.ttf"

        /** The same files through jsDelivr, for networks where raw.githubusercontent.com is blocked or slow. */
        private const val JETBRAINS_MONO_MIRROR = "https://cdn.jsdelivr.net/gh/google/fonts@main/ofl/jetbrainsmono/JetBrainsMono%5Bwght%5D.ttf"
        private const val JETBRAINS_MONO_ITALIC_MIRROR =
            "https://cdn.jsdelivr.net/gh/google/fonts@main/ofl/jetbrainsmono/JetBrainsMono-Italic%5Bwght%5D.ttf"

        /** Nerd Fonts' symbols-only family: icons for terminal tools, nothing else. */
        private const val NERD_SYMBOLS_URL = "https://github.com/ryanoasis/nerd-fonts/releases/latest/download/NerdFontsSymbolsOnly.tar.xz"

        private const val VOID_CORE = "bash coreutils grep sed gawk findutils curl wget ca-certificates tar xz git less fontconfig"
        private const val VOID_EXTRAS =
            "foot foot-terminfo dejavu-fonts-ttf btop fastfetch neovim starship eza bat fd ripgrep " +
                "fzf zoxide tmux gum jq tealdear man-db dua-cli inxi whois bash-completion pulseaudio micro libxkbcommon-tools xkeyboard-config " +
                "vulkan-loader Vulkan-Tools mesa-dri mesa-vulkan-lavapipe"
        private const val VOID_GPU_PACKAGES =
            "mesa mesa-dri vulkan-loader Vulkan-Tools mesa-demos libglvnd mesa-vulkan-lavapipe libdrm wayland libX11 libxcb " +
                "libxshmfence expat zlib libzstd zstd elfutils libelf xcb-util-keysyms SPIRV-Tools SPIRV-Tools-devel libstdc++ libgcc"

        private fun fetchText(url: String): String {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            try {
                check(conn.responseCode == 200) { "HTTP ${conn.responseCode} for $url" }
                return conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }
    }
}
