package com.bellatrixos.storage

import android.content.Context
import android.content.SharedPreferences
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * The launcher's own data that Linux also sees. It lives outside the rootfs,
 * so it exists without Linux and survives installing or removing it; proot
 * binds it to the paths Bellatrix and agents expect (see [com.bellatrixos.linux.Proot]).
 */
object LauncherData {
    /** Themes shipped with Bellatrix. The guest sees ~/.local/share/bellatrix/themes. */
    fun bellatrixThemeDir(context: Context) = File(context.filesDir, "bellatrix-themes")

    /** The user's own Bellatrix themes. The guest sees ~/.config/bellatrix/themes. */
    fun userThemesDir(context: Context) = File(context.filesDir, "themes")

    /** The user's extra backgrounds, per theme. The guest sees ~/.config/bellatrix/backgrounds. */
    fun userBackgroundsDir(context: Context) = File(context.filesDir, "backgrounds")

    /** Widget files. The guest sees ~/.config/bellatrix/widgets. */
    fun widgetsDir(context: Context) = File(context.filesDir, "widgets")

    /** style.toml and its defaults; see [com.bellatrixos.theme.StyleStore]. The guest sees ~/.config/bellatrix/style. */
    fun styleDir(context: Context) = File(context.filesDir, "style")

    /** look.toml, the guest-editable native view overrides. The guest sees ~/.config/bellatrix/native-ui. */
    fun nativeUiDir(context: Context) = File(context.filesDir, "native-ui")

    /** layout.toml, where home's tiles and pages are; see [com.bellatrixos.widgets.HomeStore]. The guest sees ~/.config/bellatrix/home. */
    fun homeDir(context: Context) = File(context.filesDir, "home")

    /** layout.toml, the taskbar's pinned apps; see [com.bellatrixos.taskbar.TaskBarStore]. The guest sees ~/.config/bellatrix/taskbar. */
    fun taskbarDir(context: Context) = File(context.filesDir, "taskbar")

    /** Settings that belong to no one store: setup, gestures, the GPU, the home role. Keys carry their owner's prefix. */
    fun prefs(context: Context): SharedPreferences = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    /** Manifest of the launcher-side [com.bellatrixos.upgrade.Provisioner]; the guest's is managed.json. */
    const val LAUNCHER_MANIFEST = "launcher-managed.json"
    const val GUEST_MANIFEST = "managed.json"

    /**
     * Deletes [dir] and everything in it without following symlinks: the
     * rootfs holds absolute guest links that resolve elsewhere on the host.
     * Directories get write permission first, since the rootfs has read-only
     * ones (the tarball's modes). Throws if anything is left.
     */
    fun deleteTree(dir: File) {
        val root = dir.toPath()
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return
        Files.walkFileTree(
            root,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(
                    d: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    runCatching { Os.chmod(d.toString(), Os.lstat(d.toString()).st_mode and 0x1ff or 0x1c0) }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }

                /** The walker lists a directory before [preVisitDirectory]: an unreadable one (mode 000) fails here first. */
                override fun visitFileFailed(
                    file: Path,
                    exc: IOException,
                ): FileVisitResult {
                    val path = file.toString()
                    val mode = runCatching { Os.lstat(path).st_mode }.getOrNull() ?: throw exc
                    if (!OsConstants.S_ISDIR(mode) || mode and 0x1c0 == 0x1c0) throw exc
                    Os.chmod(path, mode and 0x1ff or 0x1c0)
                    deleteTree(file.toFile())
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(
                    d: Path,
                    exc: IOException?,
                ): FileVisitResult {
                    Files.delete(d)
                    return FileVisitResult.CONTINUE
                }
            },
        )
    }
}
