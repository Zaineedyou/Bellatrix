package com.bellatrixos.theme

import java.io.File
import java.nio.file.Files

/**
 * A Bellatrix theme: a directory of colors, images and optional guest config
 * fragments under ~/.config/bellatrix/themes. Agents and the user can add or
 * change one by writing files; the launcher rescans it. There is no upstream
 * theme source and no catalog: the theme is Bellatrix's own.
 */
private val IMAGE_EXTENSIONS = listOf("png", "jpg", "jpeg", "webp")
private val NUMBERED = Regex("^[0-9]+-")
private val THEME_NAME = Regex("^[a-z0-9_][a-z0-9._+-]*$")

internal fun titleOf(name: String) = name.split('-').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

/** Symlinks in a theme are skipped: one made in the guest points at a guest path, which means something else here. */
private fun File.isLink() = Files.isSymbolicLink(toPath())

class BellatrixTheme(
    val name: String,
    /** Built-in first, then the user's own, which overrides it file by file. */
    val dirs: List<File>,
    val colors: Map<String, String>,
    /** The user's directory of it, in ~/.config/bellatrix/themes, if there is one. */
    val own: File? = null,
    /** The user's extra backgrounds for this theme, ~/.config/bellatrix/backgrounds/<name>. */
    private val extraBackgrounds: File? = null,
) {
    val title get() = titleOf(name)
    val isLight get() = colors["mode"] == "light"

    /** A preview image of any case and type, the user's over the built-in's, else the first background. */
    val preview: File by lazy {
        dirs.asReversed().firstNotNullOfOrNull { dir ->
            dir
                .listFiles { f -> f.nameWithoutExtension.equals("preview", ignoreCase = true) && f.extension.lowercase() in IMAGE_EXTENSIONS && f.isFile && !f.isLink() }
                ?.minByOrNull { IMAGE_EXTENSIONS.indexOf(it.extension.lowercase()) }
        } ?: backgrounds().firstOrNull() ?: File(dirs.last(), "preview.png")
    }

    /** The extra backgrounds first, then the theme's own. */
    fun backgrounds(): List<File> {
        val merged = LinkedHashMap<String, File>()
        for (dir in dirs) images(File(dir, "backgrounds")).forEach { merged[it.name] = it }
        return images(extraBackgrounds) + merged.values.sortedBy { it.name }
    }

    /** The background a style names; a renumbered copy still counts. */
    fun backgroundNamed(name: String): File? {
        val all = backgrounds()
        all.firstOrNull { it.name == name }?.let { return it }
        val rest = name.replaceFirst(NUMBERED, "").takeIf { it != name } ?: return null
        return all.singleOrNull { it.name.replaceFirst(NUMBERED, "") == rest }
    }

    /** Whether [file] is one of the user's extra backgrounds, which may be removed. */
    fun isExtraBackground(file: File) = extraBackgrounds != null && file.parentFile == extraBackgrounds

    /**
     * Files Linux gets in ~/.local/state/bellatrix/current/theme, by name:
     * top-level files from each directory, without images and dotfiles.
     */
    fun guestFiles(): Map<String, File> {
        val files = LinkedHashMap<String, File>()
        for (dir in dirs) {
            for (f in dir.listFiles().orEmpty().sortedBy { it.name }) {
                if (!f.isFile || f.isLink() || f.extension.lowercase() in IMAGE_EXTENSIONS || f.name.startsWith('.')) continue
                files[f.name] = f
            }
        }
        return files
    }

    private fun images(dir: File?): List<File> =
        dir
            ?.listFiles { f -> f.extension.lowercase() in com.bellatrixos.theme.BACKGROUND_EXTENSIONS && f.isFile && !f.isLink() }
            .orEmpty()
            .sortedBy { it.name }
}

/**
 * Where Bellatrix themes are: [builtIn] shipped with the launcher, [user]
 * (~/.config/bellatrix/themes), and per-theme extra backgrounds. All are files
 * an agent can change; a broken theme is skipped and reported.
 */
class ThemeDirs(
    val builtIn: File,
    val user: File,
    val backgrounds: File,
) {
    fun scan(onError: (String, Throwable) -> Unit = { _, _ -> }): List<BellatrixTheme> =
        (names(builtIn) + names(user))
            .distinct()
            .mapNotNull { name -> runCatching { load(name) }.onFailure { onError(name, it) }.getOrNull() }
            .sortedBy { it.name }

    fun load(name: String): BellatrixTheme? {
        if (!isValidName(name)) return null
        val dirs = listOf(File(builtIn, name), File(user, name)).filter { it.isDirectory && !it.isLink() }
        val colors = colors(dirs) ?: return null
        val own = File(user, name).takeIf { it in dirs }
        return BellatrixTheme(name, dirs, colors, own = own, extraBackgrounds = File(backgrounds, name))
    }

    private fun colors(dirs: List<File>): Map<String, String>? = dirs.asReversed().firstNotNullOfOrNull { colorsOf(it) }

    fun hasColors(name: String) = listOf(builtIn, user).any { File(it, "$name/colors.toml").isFile || File(it, "$name/alacritty.toml").isFile }

    private fun names(dir: File) = dir.listFiles { f -> f.isDirectory && !f.isLink() }.orEmpty().map { it.name }

    companion object {
        fun isValidName(name: String) = THEME_NAME.matches(name)

        /** [dir]'s own palette, from colors.toml or (older themes) alacritty.toml. */
        fun colorsOf(dir: File): Map<String, String>? {
            val toml = File(dir, "colors.toml")
            if (toml.isFile && !toml.isLink()) return ThemeColors.load(toml)
            val alacritty = File(dir, "alacritty.toml")
            if (alacritty.isFile && !alacritty.isLink()) return ThemeColors.fromAlacritty(alacritty.readText())?.let(ThemeColors::resolve)
            return null
        }
    }
}
