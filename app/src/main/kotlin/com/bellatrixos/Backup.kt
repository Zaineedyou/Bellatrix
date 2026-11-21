package com.bellatrixos

import com.bellatrixos.storage.LauncherData
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.system.Os
import android.system.OsConstants
import android.system.StructStat
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bellatrixos.linux.Distro
import com.bellatrixos.linux.Packages
import com.bellatrixos.linux.Proot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Date
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** What the backup card in settings › linux shows. */
sealed interface BackupState {
    object Idle : BackupState

    /** [progress] is 0 to 1, or null while the size isn't known. */
    data class Running(
        val label: String,
        val progress: Float?,
    ) : BackupState

    data class Finished(
        val message: String,
        val ok: Boolean,
    ) : BackupState
}

/**
 * One file with the Linux system and the launcher's own data, in a place the user picks (the system's file picker:
 * a folder on the phone, an SD card, a cloud drive). A `.tar.gz`, so a computer can open it too:
 *
 * - `bellatrix-backup.json`, first: the format and when it was made. A restore refuses a file without it.
 * - `files/...`: the app's files folder, which holds the rootfs, the setup records, the themes, widgets, home layout
 *   and style. Left out are what the launcher makes again by itself ([EXCLUDED]).
 * - `prefs/...`: the settings the launcher keeps in SharedPreferences. They can't be swapped while the launcher runs
 *   (it holds them in memory), so a restore leaves them for [applyPendingPrefs], which runs first thing on the next start.
 *
 * Linux's processes stop while this runs, and the package manager's lock is held, so nothing changes under it.
 */
class Backup(
    private val context: Context,
    private val proot: Proot,
    private val packages: Packages,
) {
    var state by mutableStateOf<BackupState>(BackupState.Idle)
        private set

    fun dismiss() {
        if (state is BackupState.Finished) state = BackupState.Idle
    }

    /**
     * Writes the backup to [uri]. The file picker has already made that file, empty; when the backup fails it is
     * deleted again, so a failure doesn't leave a file that looks like a backup and holds nothing.
     */
    suspend fun createTo(uri: Uri) {
        val made =
            guarded("backing up") { progress ->
            proot.killAll()
            val root = context.filesDir
            var total = 0L
            walk(root) { _, _, st -> if (OsConstants.S_ISREG(st.st_mode)) total += st.st_size }
            val prefs = prefsFiles()
            total += prefs.sumOf { it.length() }
            var done = 0L
            var files = 0
            var skipped = 0
            val stream = context.contentResolver.openOutputStream(uri, "w") ?: throw IOException("can't write to that file")
            TarArchiveOutputStream(FastGzip(BufferedOutputStream(stream, 1 shl 16)), "UTF-8").use { tar ->
                tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
                tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
                putBytes(tar, MANIFEST, JSONObject().put("format", FORMAT).put("createdAt", System.currentTimeMillis()).put("distro", Distro.active.id).toString().toByteArray())
                // Flushed first: apply() writes a moment later.
                runCatching { LauncherData.prefs(context).edit().commit() }
                for (p in prefs) {
                    putBytes(tar, "prefs/${p.name}", p.readBytes())
                    done += p.length()
                }
                walk(root) { rel, file, st ->
                    try {
                        when {
                            OsConstants.S_ISDIR(st.st_mode) -> {
                                tar.putArchiveEntry(entry("files/$rel/", st))
                                tar.closeArchiveEntry()
                            }
                            OsConstants.S_ISLNK(st.st_mode) -> {
                                val e = TarArchiveEntry("files/$rel", TarConstants.LF_SYMLINK)
                                e.linkName = Os.readlink(file.path)
                                e.modTime = Date(st.st_mtime * 1000)
                                tar.putArchiveEntry(e)
                                tar.closeArchiveEntry()
                            }
                            OsConstants.S_ISREG(st.st_mode) -> {
                                // Opened before the entry is written: one that can't be read leaves no half entry.
                                FileInputStream(file).use { input ->
                                    val e = entry("files/$rel", st)
                                    e.size = st.st_size
                                    tar.putArchiveEntry(e)
                                    copyExactly(input, tar, st.st_size) { n ->
                                        done += n
                                        progress(done, total)
                                    }
                                    tar.closeArchiveEntry()
                                }
                                files++
                            }
                        }
                    } catch (e: IOException) {
                        Log.w(TAG, "skipped $rel", e)
                        skipped++
                    }
                }
            }
            "Backed up $files files (${formatBytes(done)})" + if (skipped > 0) ", $skipped couldn't be read" else ""
        }
        if (made == null) runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
    }

    /** Replaces the Linux system and the launcher's data with the ones in [uri]. True when it worked, so the caller restarts. */
    suspend fun restoreFrom(uri: Uri): Boolean = guarded("restoring") { progress -> restore(uri, progress) } != null

    private suspend fun restore(
        uri: Uri,
        progress: (Long, Long) -> Unit,
    ): String {
        val root = context.filesDir
        val staging = File(root, STAGING)
        val prefsStage = File(root, PREFS_STAGING)
        val total = runCatching { context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull() ?: -1L
        // The new files sit next to the old ones until the swap, and a restored rootfs is about twice the size of its archive.
        if (total > 0 && root.usableSpace < total * 3) {
            throw IOException("not enough space: about ${formatBytes(total * 3)} is needed for a file of ${formatBytes(total)}")
        }
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("can't read that file")
        try {
            proot.killAll()
            LauncherData.deleteTree(staging)
            LauncherData.deleteTree(prefsStage)
            staging.mkdirs()
            val counter = ByteCounter(input)
            var files = 0
            TarArchiveInputStream(GZIPInputStream(BufferedInputStream(counter, 1 shl 16), 1 shl 16), "UTF-8").use { tar ->
                val first = tar.nextEntry
                if (first == null || first.name != MANIFEST) throw IOException("this isn't an Bellatrix backup")
                val manifest = runCatching { JSONObject(String(tar.readBytes())) }.getOrNull()
                if (manifest?.optInt("format") != FORMAT) throw IOException("this backup is from a version this launcher doesn't know")
                while (true) {
                    val entry = tar.nextEntry ?: break
                    when {
                        entry.name.startsWith("prefs/") -> {
                            val name = prefsName(entry.name.removePrefix("prefs/")) ?: continue
                            prefsStage.mkdirs()
                            File(prefsStage, name).outputStream().use { tar.copyTo(it) }
                        }
                        entry.name.startsWith("files/") -> {
                            val rel = safeRelative(entry.name.removePrefix("files/")) ?: throw IOException("unsafe path in the backup: ${entry.name}")
                            if (rel.substringBefore('/') in EXCLUDED) continue
                            extract(tar, entry, File(staging, rel), staging)
                            if (entry.isFile) files++
                        }
                    }
                    if (total > 0) progress(counter.count, total)
                }
            }
            if (files == 0 && !prefsStage.exists()) throw IOException("the backup is empty")
            // Everything is unpacked and valid: now the swap, a rename per top-level item.
            progress(total, total)
            for (child in staging.listFiles().orEmpty()) {
                val target = File(root, child.name)
                LauncherData.deleteTree(target)
                if (!child.renameTo(target)) throw IOException("couldn't move ${child.name} into place")
            }
            LauncherData.deleteTree(staging)
            return "Restored $files files"
        } catch (e: Throwable) {
            runCatching { LauncherData.deleteTree(staging) }
            runCatching { LauncherData.deleteTree(prefsStage) }
            throw e
        } finally {
            input.close()
        }
    }

    /** One entry of the archive into [target], which must stay inside [staging] even through links the archive made. */
    private fun extract(
        tar: TarArchiveInputStream,
        entry: TarArchiveEntry,
        target: File,
        staging: File,
    ) {
        val parent = target.parentFile ?: throw IOException("bad path")
        parent.mkdirs()
        if (!parent.canonicalPath.startsWith(staging.canonicalPath)) throw IOException("unsafe path in the backup: ${entry.name}")
        when {
            entry.isDirectory -> target.mkdirs()
            entry.isSymbolicLink -> {
                target.delete()
                Os.symlink(entry.linkName, target.path)
            }
            entry.isFile -> target.outputStream().use { tar.copyTo(it) }
            else -> return // hard links (the rootfs has none: proot turns them into symlinks), devices, fifos
        }
        // Owner can always read and write, so what is inside a read-only folder can still be unpacked and deleted later.
        if (!entry.isSymbolicLink) {
            runCatching { Os.chmod(target.path, (entry.mode and 0xfff) or (if (entry.isDirectory) 0x1c0 else 0x180)) }
            if (entry.isFile) target.setLastModified(entry.modTime.time)
        }
    }

    /** Runs [block] under the package manager's lock, with the card showing its progress; null when it failed. */
    private suspend fun <T> guarded(
        label: String,
        block: suspend ((Long, Long) -> Unit) -> T,
    ): T? {
        if (state is BackupState.Running) return null
        state = BackupState.Running(label, null)
        var last = 0L
        val report = { done: Long, total: Long ->
            val now = System.currentTimeMillis()
            if (now - last > 250) {
                last = now
                state = BackupState.Running(label, if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else null)
            }
        }
        return try {
            val result =
                withContext(Dispatchers.IO) {
                    // Not waited for: an install or an update can hold the lock for a long time, and a backup that sits
                    // there saying "backing up" while it has not started is worse than one that says why it can't.
                    if (!packages.lock.tryLock()) throw IOException("Linux is busy installing or updating; try again when that is done")
                    try {
                        block(report)
                    } finally {
                        packages.lock.unlock()
                    }
                }
            state = BackupState.Finished(result as? String ?: "Done", true)
            result
        } catch (e: Throwable) {
            Log.e(TAG, "$label failed", e)
            state = BackupState.Finished("${label.replaceFirstChar { it.uppercase() }} failed: ${e.message ?: e.javaClass.simpleName}", false)
            null
        }
    }

    private fun prefsFiles(): List<File> =
        File(context.applicationInfo.dataDir, "shared_prefs").listFiles { f -> prefsName(f.name) != null }?.toList().orEmpty()

    private fun entry(
        name: String,
        st: StructStat,
    ) = TarArchiveEntry(name).also {
        it.mode = st.st_mode and 0xfff
        it.modTime = Date(st.st_mtime * 1000)
    }

    private fun putBytes(
        tar: TarArchiveOutputStream,
        name: String,
        bytes: ByteArray,
    ) {
        val e = TarArchiveEntry(name)
        e.size = bytes.size.toLong()
        tar.putArchiveEntry(e)
        tar.write(bytes)
        tar.closeArchiveEntry()
    }

    /** Exactly [size] bytes: a file that grew or shrank while it was read can't leave the tar misaligned. */
    private inline fun copyExactly(
        input: FileInputStream,
        out: OutputStream,
        size: Long,
        onBytes: (Long) -> Unit,
    ) {
        val buf = ByteArray(1 shl 16)
        var left = size
        while (left > 0) {
            val n = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
            if (n < 0) {
                java.util.Arrays.fill(buf, 0)
                while (left > 0) {
                    val pad = minOf(buf.size.toLong(), left).toInt()
                    out.write(buf, 0, pad)
                    left -= pad
                }
                break
            }
            out.write(buf, 0, n)
            left -= n
            onBytes(n.toLong())
        }
    }

    /** The files folder, depth first, parents before their children, without following links. */
    private fun walk(
        root: File,
        visit: (String, File, StructStat) -> Unit,
    ) {
        val pending = ArrayDeque<String>()
        pending.addLast("")
        while (pending.isNotEmpty()) {
            val relDir = pending.removeLast()
            val dir = if (relDir.isEmpty()) root else File(root, relDir)
            val names = dir.list() ?: continue
            for (name in names.sorted()) {
                if (relDir.isEmpty() && name in EXCLUDED) continue
                val rel = if (relDir.isEmpty()) name else "$relDir/$name"
                val file = File(dir, name)
                val st = runCatching { Os.lstat(file.path) }.getOrNull() ?: continue
                visit(rel, file, st)
                if (OsConstants.S_ISDIR(st.st_mode)) pending.addLast(rel)
            }
        }
    }

    /** A gzip that favors speed: a rootfs is gigabytes, and a phone deflates at a few MB a second at the default level. */
    private class FastGzip(
        out: OutputStream,
    ) : GZIPOutputStream(out, 1 shl 16) {
        init {
            def.setLevel(Deflater.BEST_SPEED)
        }
    }

    companion object {
        private const val TAG = "backup"
        private const val FORMAT = 1
        private const val MANIFEST = "bellatrix-backup.json"
        private const val STAGING = "restore-staging"
        private const val PREFS_STAGING = "restore-prefs"

        /** What the launcher makes again by itself, or that only means something while it runs; the same list as backup_rules.xml. */
        val EXCLUDED =
            setOf("tmp", "xdg", "sys", "proc", "keymap.xkb", "bwrap-shim", ".bellatrix.download", "arch", STAGING, PREFS_STAGING)

        /** [path] as a clean relative path, or null when it would leave the folder it is unpacked into. */
        fun safeRelative(path: String): String? {
            val parts = path.split('/').filter { it.isNotEmpty() }
            if (parts.isEmpty() || parts.any { it == "." || it == ".." || '\u0000' in it }) return null
            return parts.joinToString("/")
        }

        /** [name] when it is a settings file the launcher may restore (one XML, no folders), else null. */
        fun prefsName(name: String): String? = name.takeIf { Regex("[A-Za-z0-9_.-]+\\.xml").matches(it) && !it.startsWith(".") }

        fun formatBytes(bytes: Long): String =
            when {
                bytes >= 1L shl 30 -> "%.1f GB".format(bytes / (1L shl 30).toDouble())
                bytes >= 1L shl 20 -> "%.0f MB".format(bytes / (1L shl 20).toDouble())
                else -> "${bytes / 1024} KB"
            }

        /**
         * Puts the settings a restore left (see [Backup]) in place. Called first thing from [App.onCreate]: nothing has
         * read SharedPreferences yet, and a leftover `.bak` would otherwise win over the restored file.
         */
        fun applyPendingPrefs(context: Context) {
            val staged = File(context.filesDir, PREFS_STAGING)
            if (!staged.isDirectory) return
            runCatching {
                val dest = File(context.applicationInfo.dataDir, "shared_prefs").apply { mkdirs() }
                for (f in staged.listFiles().orEmpty()) {
                    val name = prefsName(f.name) ?: continue
                    File(dest, "$name.bak").delete()
                    f.copyTo(File(dest, name), overwrite = true)
                }
            }.onFailure { Log.e(TAG, "couldn't apply restored settings", it) }
            runCatching { LauncherData.deleteTree(staged) }
        }
    }
}

/** Counts the bytes read through it; the library's own counter changed its accessor between versions. */
private class ByteCounter(input: java.io.InputStream) : java.io.FilterInputStream(input) {
    @Volatile var count = 0L
        private set

    override fun read(): Int = super.read().also { if (it >= 0) count++ }

    override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b, off, len).also { if (it > 0) count += it }

    override fun skip(n: Long): Long = super.skip(n).also { if (it > 0) count += it }
}
