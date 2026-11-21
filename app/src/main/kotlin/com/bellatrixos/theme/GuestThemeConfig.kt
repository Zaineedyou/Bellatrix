package com.bellatrixos.theme

import android.system.Os
import com.bellatrixos.storage.LauncherData
import com.bellatrixos.linux.Proot
import com.bellatrixos.linux.TerminalFont
import com.bellatrixos.linux.run
import com.bellatrixos.upgrade.Managed
import com.bellatrixos.upgrade.Policy
import com.bellatrixos.upgrade.writeAtomically
import java.io.File

/**
 * Writes a Bellatrix theme into the guest: the theme's own config fragments
 * plus the launcher's terminal/agent defaults in its colors. All paths are
 * Bellatrix's; everything is seeded so an agent may change it afterwards.
 */
class GuestThemeConfig(
    private val proot: Proot,
    private val themesDir: File,
) {
    private val home = proot.home
    private val currentDir = File(home, ".local/state/bellatrix/current")
    @Volatile private var footColorSection: String? = null

    /** Renders [theme] into current/theme, which foot, btop and the rest include. */
    fun render(theme: BellatrixTheme) {
        val themeDir = File(currentDir, "theme")
        LauncherData.deleteTree(themeDir)
        themeDir.mkdirs()
        theme.guestFiles().forEach { (name, file) -> file.copyTo(File(themeDir, name)) }
        File(currentDir, "theme.name").writeText(theme.name + "\n")
        // A theme may provide its own terminal colors; otherwise generate them from its palette.
        if (!File(themeDir, "foot.ini").isFile) File(themeDir, "foot.ini").writeText(footTheme(theme.colors, footColorSection()))
        if (!File(themeDir, "btop.theme").isFile) File(themeDir, "btop.theme").writeText(btopTheme(theme.colors))
        symlink("${Proot.GUEST_HOME}/.local/state/bellatrix/current/theme/btop.theme", File(home, ".config/btop/themes/current.theme"))
        val agentTheme = File(themeDir, if (File(home, ".claude").isDirectory) "claude.json" else "pi.json")
        if (agentTheme.isFile) {
            copyTheme(agentTheme, ".claude/themes/bellatrix.json")
            copyTheme(agentTheme, ".pi/agent/themes/bellatrix-system.json")
        }
    }

    private fun copyTheme(src: File, to: String) {
        if (src.isFile) writeAtomically(File(home, to), src.readBytes())
    }

    fun linkBackground(file: File) {
        proot.guestPath(file)?.let { symlink(it, File(currentDir, "background")) }
    }

    private fun symlink(target: String, link: File) {
        link.parentFile?.mkdirs()
        link.delete()
        runCatching { Os.symlink(target, link.path) }
    }

    /** Configs the launcher owns in the guest. */
    fun managed(theme: BellatrixTheme?): List<Managed> {
        val files = ArrayList<Managed>()
        fun seed(path: String, content: () -> String) = files.add(Managed.text(File(home, path), Policy.Seeded, content = content))

        seed(".config/foot/foot.ini") { FOOT_INI }
        seed(".bashrc") { BASHRC }
        seed(".pi/agent/settings.json") { "{\n  \"theme\": \"bellatrix-system\"\n}\n" }
        seed(".config/opencode/opencode.json") { OPENCODE_JSON }
        seed(".config/opencode/tui.json") { "{\n  \"theme\": \"system\"\n}\n" }
        files.add(Managed.text(File(proot.rootfs, "usr/local/bin/btop"), Policy.Owned, executable = true) { BTOP })
        if (theme != null) {
            val dark = !theme.isLight
            val accent = theme.colors["accent"] ?: theme.colors["blue"] ?: "#7aa2f7"
            for (gtk in listOf("gtk-3.0", "gtk-4.0")) {
                seed(".config/$gtk/settings.ini") {
                    "[Settings]\ngtk-application-prefer-dark-theme=${if (dark) 1 else 0}\ngtk-theme-name=Adwaita${if (dark) "-dark" else ""}\n"
                }
                seed(".config/$gtk/gtk.css") {
                    "@define-color accent_color $accent;\n@define-color accent_bg_color $accent;\n@define-color theme_selected_bg_color $accent;\n"
                }
            }
        }
        return files
    }

    /**
     * foot 1.26 renamed `[colors]` to `[colors-dark]`; newer builds reject the old
     * name outright. Ask the installed binary instead of guessing a version.
     */
    private fun footColorSection(): String {
        footColorSection?.let { return it }
        val version = StringBuilder()
        runCatching { proot.run("foot --version 2>/dev/null") { version.append(it).append('\n') } }
        val numbers = Regex("""foot version:\s*(\d+)\.(\d+)""").find(version.toString())?.destructured
        val modern = numbers?.let { (major, minor) ->
            val ma = major.toIntOrNull() ?: 0
            val mi = minor.toIntOrNull() ?: 0
            ma > 1 || ma == 1 && mi >= 26
        } ?: false
        val section = if (modern) "[colors-dark]" else "[colors]"
        footColorSection = section
        return section
    }

    /** Live-recolors running foot terminals with OSC sequences. Blocking. */
    fun recolorTerminals(theme: BellatrixTheme) {
        val c = footColors(theme.colors)
        val osc =
            buildString {
                fun emit(code: String, key: String) = c[key]?.let { append("\u001b]$code;$it\u0007") }
                emit("10", "foreground")
                emit("11", "background")
                emit("12", "cursor")
                emit("17", "selection_background")
                emit("19", "selection_foreground")
                for (i in 0..15) c["color$i"]?.let { append("\u001b]4;$i;$it\u0007") }
            }
        proot.hostFile("/tmp/bellatrix-osc").writeText(osc)
        val script =
            "for p in \$(pgrep -x foot); do for c in \$(pgrep -P \$p); do " +
                "t=\$(readlink /proc/\$c/fd/1); case \$t in /dev/pts/*) cat /tmp/bellatrix-osc > \$t;; esac; done; done; " +
                "pkill -SIGUSR2 btop; true"
        runCatching { proot.run(script) }
    }

    private companion object {
        fun footColors(c: Map<String, String>): Map<String, String> {
            val accent = c["accent"] ?: c["blue"] ?: return c
            val background = c["background"] ?: return c
            return c + mapOf("selection_background" to accent, "selection_foreground" to background)
        }

        fun footTheme(
            c: Map<String, String>,
            section: String,
        ): String {
            val colors = footColors(c)
            // foot's color values are RRGGBB without a leading '#', and its palette
            // keys are regular0..7 / bright0..7 rather than color0..15.
            fun hex(value: String) = value.removePrefix("#")
            return buildString {
                append(section).append('\n')
                colors["foreground"]?.let { append("foreground=${hex(it)}\n") }
                colors["background"]?.let { append("background=${hex(it)}\n") }
                colors["selection_foreground"]?.let { append("selection-foreground=${hex(it)}\n") }
                colors["selection_background"]?.let { append("selection-background=${hex(it)}\n") }
                for (i in 0..15) {
                    colors["color$i"]?.let { value ->
                        val key = if (i < 8) "regular$i" else "bright${i - 8}"
                        append("$key=${hex(value)}\n")
                    }
                }
            }
        }

        fun btopTheme(c: Map<String, String>): String =
            buildString {
                append("[theme]\n")
                c["background"]?.let { append("main_bg = $it\n") }
                c["foreground"]?.let { append("main_fg = $it\n") }
                c["accent"]?.let { append("selected_fg = $it\n") }
                c["muted"]?.let { append("title_bg = $it\n") }
            }

        val FOOT_INI =
            """
            [main]
            include=~/.local/state/bellatrix/current/theme/foot.ini
            term=xterm-256color
            include=${TerminalFont.PATH}
            pad=8x8
            workers=0
            selection-target=both

            [scrollback]
            lines=10000
            multiplier=7.0

            [cursor]
            style=block
            blink=no

            [key-bindings]
            clipboard-copy=Control+Insert Control+Shift+c XF86Copy
            primary-paste=none
            clipboard-paste=Shift+Insert Control+Shift+v XF86Paste
            """.trimIndent() + "\n"

        val BTOP =
            """
            #!/bin/sh
            # bellatrix: btop with fewer boxes in a narrow terminal (see GuestThemeConfig.kt)
            [ ${'$'}# -eq 0 ] || exec /usr/bin/btop "${'$'}@"
            cols=${'$'}(stty size 2>/dev/null | cut -d' ' -f2)
            cols=${'$'}{cols:-80}
            [ "${'$'}cols" -ge 80 ] && exec /usr/bin/btop
            [ "${'$'}cols" -ge 60 ] && exec /usr/bin/btop -p 1
            conf=${'$'}{XDG_CONFIG_HOME:-${'$'}HOME/.config}/btop/btop.conf
            narrow=/tmp/bellatrix-btop-narrow.conf
            { grep -v -e '^shown_boxes' -e '^presets' "${'$'}conf" 2>/dev/null; echo 'shown_boxes = "proc"'; echo 'presets = "proc:0:default"'; } > "${'$'}narrow"
            exec /usr/bin/btop -c "${'$'}narrow"
            """.trimIndent() + "\n"

        val OPENCODE_JSON =
            """
            {
              "${'$'}schema": "https://opencode.ai/config.json",
              "theme": "system",
              "autoupdate": false
            }
            """.trimIndent() + "\n"

        val BASHRC =
            """
            export BELLATRIX_PATH=${Proot.GUEST_HOME}/.local/share/bellatrix
            export EDITOR=bellatrix-edit VISUAL=bellatrix-edit
            """.trimIndent() + "\n"
    }
}
