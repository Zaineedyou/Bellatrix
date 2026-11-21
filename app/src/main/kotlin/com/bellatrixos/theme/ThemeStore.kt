package com.bellatrixos.theme

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bellatrixos.storage.LauncherData
import com.bellatrixos.R
import com.bellatrixos.linux.Proot
import com.bellatrixos.ui.Palette
import com.bellatrixos.ui.decodeForSystemWallpaper
import com.bellatrixos.upgrade.appStamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.CopyOnWriteArraySet

private const val TAG = "themes"

/** A theme's colors bundled with the app, for picking a theme before a local one is added. */
class BundledTheme(
    val name: String,
    val colors: Map<String, String>,
) {
    val title get() = titleOf(name)
}

/**
 * Bellatrix themes: local files under ~/.config/bellatrix/themes, shipped
 * defaults in the app's own theme dir. There is no upstream release, catalog
 * or installer: a coding agent or the user writes theme files, and the launcher
 * picks them up. Applying one recolors the launcher, swaps the wallpaper and,
 * when Linux is installed, writes the guest's themed configs.
 */
class ThemeStore(
    private val context: Context,
    proot: Proot,
    /** Where the picked theme and background are saved. */
    private val style: StyleStore,
    /** Whether Linux is installed; without it, nothing is written into the guest. */
    private val isLinux: () -> Boolean,
    /** Called when the guest configs depending on the theme changed. */
    private val onConfigChanged: () -> Unit,
) {
    private val dirs = themeDirs(context)
    private val guest = GuestThemeConfig(proot, dirs.builtIn)
    private val prefs = prefs(context)

    var themes by mutableStateOf<List<BellatrixTheme>>(emptyList())
        private set
    var current by mutableStateOf<BellatrixTheme?>(null)
        private set
    var background by mutableStateOf<File?>(null)
        private set

    init {
        // The shipped default lives in the app's own theme dir, which agents can read and copy.
        provisionBuiltIn()
    }

    /** The shipped palette, written once so the default theme is a real local theme. */
    private fun provisionBuiltIn() {
        val dir = File(dirs.builtIn, DEFAULT_THEME)
        val file = File(dir, "colors.toml")
        if (file.isFile) return
        runCatching {
            dir.mkdirs()
            file.writeText(BUILTIN_COLORS)
        }.onFailure { Log.w(TAG, "couldn't write the shipped theme", it) }
    }

    /** Every theme's colors, from the app's bundled assets and local theme files. */
    val bundled: List<BundledTheme> by lazy {
        val assets =
            context.assets.list("themes").orEmpty().map { file ->
                val text = context.assets.open("themes/$file").bufferedReader().use { it.readText() }
                BundledTheme(file.removeSuffix(".toml"), ThemeColors.resolve(ThemeColors.parse(text)))
            }
        val local = scan().map { BundledTheme(it.name, it.colors) }
        (assets + local).distinctBy { it.name }
    }

    /** The themes to pick from: bundled, with a local override where there is one, then local-only themes. */
    val choices: List<BundledTheme> by derivedStateOf {
        val own = themes.filter { it.own != null }.associateBy { it.name }
        bundled.map { b -> own[b.name]?.let { BundledTheme(it.name, it.colors) } ?: b } +
            own.values.filter { t -> bundled.none { it.name == t.name } }.map { BundledTheme(it.name, it.colors) }
    }

    /*
     * style.toml is the one record of the theme and background. What the user
     * does here ([pick], [apply], [setBackground]) changes the style, then shows
     * it; showing never writes it, so following an edit can't overwrite it.
     */

    /** The theme picked, applied or not. */
    val picked get() = style.style[Style.THEME] ?: DEFAULT_THEME

    /** Recolors the launcher right away and remembers the choice. */
    fun pick(theme: BundledTheme) {
        Palette.apply(theme.colors)
        style.update { it.with(Style.THEME, theme.name).with(Style.BACKGROUND, null) }
    }

    /** Shows the theme and background the style names, once it's on disk. */
    suspend fun applyPicked(): Boolean {
        val theme = themes.firstOrNull { it.name == picked } ?: return false
        val backgrounds = theme.backgrounds()
        val bgName = style.style[Style.BACKGROUND]
        val bg = bgName?.let(theme::backgroundNamed)
        if (bgName != null && bg == null) style.report("background: \"$bgName\" isn't one of ${theme.name}'s backgrounds; showing its first")
        val target = bg ?: backgrounds.firstOrNull()
        when {
            theme.name != current?.name || theme.colors != current?.colors -> show(theme, target)
            target != background -> showBackground(target)
        }
        return true
    }

    /** The picked theme's colors, read before the first frame. */
    fun loadPalette() {
        runCatching { ThemeColors.resolve(ThemeColors.parse(context.assets.open("themes/$picked.toml").bufferedReader().use { it.readText() })) }
            .recoverCatching { checkNotNull(dirs.load(picked)).colors }
            .recoverCatching { ThemeColors.resolve(ThemeColors.parse(BUILTIN_COLORS)) }
            .onSuccess { Palette.apply(it) }
    }

    private var liveWallpaperPackage by mutableStateOf<String?>(null)

    val liveWallpaperActive get() = liveWallpaperPackage == context.packageName
    val foreignLiveWallpaper get() = liveWallpaperPackage != null && !liveWallpaperActive
    var systemWallpaperVersion by mutableStateOf(0)
        private set

    suspend fun refreshLiveWallpaper() {
        liveWallpaperPackage = WallpaperManager.getInstance(context).wallpaperInfo?.packageName
    }

    /** Opens Android's live wallpaper preview with ours preselected. */
    fun requestLiveWallpaper() {
        val intent =
            Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, BellatrixWallpaperService::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure { Log.w(TAG, "no live wallpaper picker", it) }
    }

    private suspend fun syncSystemWallpaper(file: File?) {
        if (file == null) return
        refreshLiveWallpaper()
        if (file.isLiveBackground()) {
            if (!liveWallpaperActive) requestLiveWallpaper()
            return
        }
        if (liveWallpaperActive) return
        val done =
            withContext(Dispatchers.IO) {
                runCatching {
                    val wallpapers = WallpaperManager.getInstance(context)
                    if (!wallpapers.isWallpaperSupported || !wallpapers.isSetWallpaperAllowed) return@runCatching false
                    val bitmap = decodeForSystemWallpaper(file, context) ?: return@runCatching false
                    try {
                        runCatching { wallpapers.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) }
                            .onFailure { wallpapers.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM) }
                    } finally {
                        bitmap.recycle()
                    }
                    true
                }.onFailure { Log.w(TAG, "the phone's wallpaper wasn't set", it) }.getOrDefault(false)
            }
        if (done) {
            liveWallpaperPackage = null
            systemWallpaperVersion++
        }
    }

    /** Whether [load] has finished; until then it may still replace the wallpaper. */
    var loaded by mutableStateOf(false)
        private set

    /** Restores the saved theme; guest configs are already on disk from when it was applied. */
    suspend fun load() {
        try {
            restoreSaved()
        } finally {
            loaded = true
        }
    }

    private suspend fun restoreSaved() {
        themes = withContext(Dispatchers.IO) { scan() }
        val theme = themes.firstOrNull { it.name == picked } ?: return
        current = theme
        Palette.apply(theme.colors)
        if (isLinux() && prefs.getString(RENDERED_BY, null) != appStamp(context)) {
            withContext(Dispatchers.IO) { guest.render(theme) }
            prefs.edit().putString(RENDERED_BY, appStamp(context)).apply()
        }
        val bg = style.style[Style.BACKGROUND]?.let(theme::backgroundNamed) ?: theme.backgrounds().firstOrNull()
        background = bg
        if (bg != null && isLinux()) withContext(Dispatchers.IO) { runCatching { guest.linkBackground(bg) } }
        refreshLiveWallpaper()
    }

    /** Picks up themes an agent added or changed in Linux; the theme menu calls it when it opens. */
    suspend fun rescan() {
        themes = withContext(Dispatchers.IO) { scan() }
    }

    private fun scan(): List<BellatrixTheme> = dirs.scan { name, e -> Log.w(TAG, "bad theme $name", e) }

    /** Deletes a theme the user added; the shipped one takes over. */
    suspend fun remove(theme: BellatrixTheme) {
        val own = theme.own ?: return
        withContext(Dispatchers.IO) { LauncherData.deleteTree(own) }
        rescan()
        if (current?.name != theme.name) return
        val next = themes.firstOrNull { it.name == theme.name } ?: themes.firstOrNull { it.name == DEFAULT_THEME } ?: themes.firstOrNull()
        if (next != null) apply(next) else {
            current = null
            bundled.firstOrNull { it.name == DEFAULT_THEME }?.let(::pick)
        }
    }

    /** Picks [theme] with its first background, and shows it. */
    suspend fun apply(theme: BellatrixTheme) {
        style.update { it.with(Style.THEME, theme.name).with(Style.BACKGROUND, null) }
        val bg = theme.backgrounds().firstOrNull()
        scope.launch { syncSystemWallpaper(bg) }
        show(theme, bg)
    }

    private suspend fun show(theme: BellatrixTheme, bg: File?) {
        current = theme
        Palette.apply(theme.colors)
        showBackground(bg)
        if (!isLinux()) return
        withContext(Dispatchers.IO) {
            runCatching {
                guest.render(theme)
                prefs.edit().putString(RENDERED_BY, appStamp(context)).apply()
                onConfigChanged()
                guest.recolorTerminals(theme)
            }.onFailure { Log.e(TAG, "theme ${theme.name} not written to the guest", it) }
        }
    }

    suspend fun renderGuest() {
        val theme = current ?: return
        withContext(Dispatchers.IO) {
            guest.render(theme)
            background?.takeIf { !it.isLiveBackground() }?.let(guest::linkBackground)
        }
        prefs.edit().putString(RENDERED_BY, appStamp(context)).apply()
    }

    suspend fun setBackground(file: File?) {
        style.update { it.with(Style.BACKGROUND, file?.name) }
        scope.launch { syncSystemWallpaper(file) }
        showBackground(file)
    }

    private suspend fun showBackground(file: File?) {
        background = file
        if (file != null && !file.isLiveBackground() && isLinux()) withContext(Dispatchers.IO) { guest.linkBackground(file) }
        changeListeners.forEach { it() }
    }

    /** The theme or background in style.toml changed under us (an agent, the user in Linux). */
    suspend fun followStyle() {
        rescan()
        if (applyPicked()) return
        val name = picked
        val local = themes.firstOrNull { it.name == name }
        val asset = bundled.firstOrNull { it.name == name }
        when {
            local != null -> apply(local)
            asset != null -> Palette.apply(asset.colors)
            else -> {
                style.report("theme: no theme named \"$name\" in ~/.config/bellatrix/themes")
                Palette.apply(ThemeColors.resolve(ThemeColors.parse(BUILTIN_COLORS)))
            }
        }
    }

    /** Copies an image into the current theme's extra backgrounds and shows it. */
    suspend fun addBackground(uri: Uri) {
        val theme = current ?: return
        val file = withContext(Dispatchers.IO) {
            val dir = File(dirs.backgrounds, theme.name)
            val type = context.contentResolver.getType(uri).orEmpty()
            val kept = when {
                type == "image/gif" -> "gif"
                type == "video/webm" -> "webm"
                type == "video/x-matroska" -> "mkv"
                type == "video/3gpp" -> "3gp"
                type.startsWith("video/") -> "mp4"
                else -> null
            }
            val ext = kept ?: when (type) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val out = File(dir, "${if (type.startsWith("video/")) "video" else "image"}-${System.currentTimeMillis()}.$ext")
            runCatching {
                dir.mkdirs()
                val input = context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException(uri.toString())
                if (kept != null || type in listOf("image/png", "image/webp", "image/jpeg")) {
                    input.use { i -> out.outputStream().use { i.copyTo(it) } }
                } else {
                    val bmp = input.use(BitmapFactory::decodeStream) ?: error("can't decode $type")
                    out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                }
                out
            }.onFailure {
                out.delete()
                Log.e(TAG, "background not added", it)
            }.getOrNull()
        } ?: return
        setBackground(file)
    }

    /** Deletes one of the user's extra backgrounds. */
    suspend fun removeBackground(file: File) {
        val theme = current ?: return
        if (!theme.isExtraBackground(file)) return
        withContext(Dispatchers.IO) { file.delete() }
        if (file != background) return
        style.update { it.with(Style.BACKGROUND, null) }
        showBackground(theme.backgrounds().firstOrNull())
    }

    /** Guest configs for the current theme. */
    fun managed() = guest.managed(current ?: runCatching { dirs.load(picked) }.getOrNull())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val RENDERED_BY = "rendered_by"
        const val DEFAULT_THEME = "tokyo-night"

        /** The shipped palette: these are Bellatrix's built-in colors. */
        private const val BUILTIN_COLORS =
            "background = \"#1a1b26\"\n" +
                "foreground = \"#c0caf5\"\n" +
                "accent = \"#7aa2f7\"\n" +
                "muted = \"#414868\"\n" +
                "dark_foreground = \"#565f89\"\n" +
                "lighter_background = \"#24283b\"\n" +
                "red = \"#f7768e\"\n" +
                "green = \"#9ece6a\"\n" +
                "yellow = \"#e0af68\"\n" +
                "blue = \"#7aa2f7\"\n" +
                "magenta = \"#bb9af7\"\n" +
                "cyan = \"#7dcfff\"\n" +
                "mode = \"dark\"\n"

        val changeListeners = CopyOnWriteArraySet<() -> Unit>()

        fun prefs(context: Context) = context.getSharedPreferences("themes", Context.MODE_PRIVATE)

        private fun themeDirs(context: Context) =
            ThemeDirs(
                LauncherData.bellatrixThemeDir(context),
                LauncherData.userThemesDir(context),
                LauncherData.userBackgroundsDir(context),
            )

        /** Colors and background for the wallpaper service, which can run without the launcher. */
        fun savedSelection(context: Context): Pair<Map<String, String>, File?>? {
            val saved = StyleStore.read(context)
            val name = saved[Style.THEME] ?: DEFAULT_THEME
            var theme = runCatching { themeDirs(context).load(name) }.getOrNull()
            if (theme == null && name == DEFAULT_THEME) {
                val dir = File(LauncherData.bellatrixThemeDir(context), DEFAULT_THEME)
                dir.mkdirs()
                val file = File(dir, "colors.toml")
                if (!file.isFile) runCatching { file.writeText(BUILTIN_COLORS) }
                theme = runCatching { themeDirs(context).load(name) }.getOrNull()
            }
            theme ?: return null
            val bg = saved[Style.BACKGROUND]?.let(theme::backgroundNamed) ?: theme.backgrounds().firstOrNull()
            return theme.colors to bg
        }
    }
}
