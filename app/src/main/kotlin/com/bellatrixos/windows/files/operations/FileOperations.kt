package com.bellatrixos.windows.files.operations

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bellatrixos.R
import com.bellatrixos.windows.dialogs.UIDialogProgress
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Operasi hapus/salin/pindah berkas dengan dialog progres. */
class FileOperations(context: Context) {

    class ConflictAction private constructor(@JvmField val name: String) {
        companion object {
            @JvmField val u = ConflictAction("ASK")
            @JvmField val v = ConflictAction("REPLACE")
            @JvmField val w = ConflictAction("SKIP")
            @JvmField val x = ConflictAction("RENAME")
            @JvmField val y = arrayOf(u, v, w, x)

            @JvmStatic
            fun valueOf(str: String): ConflictAction = y.first { it.name == str }

            @JvmStatic
            fun values(): Array<ConflictAction> = y.copyOf()
        }
    }

    fun interface TaskFailedListener {
        fun b()
    }

    fun interface TaskSuccessListener {
        fun a(list: List<*>)
    }

    @JvmField val f4924a: Context = context.applicationContext
    @JvmField var c: TaskSuccessListener? = null
    @JvmField var f4925d: TaskFailedListener? = null
    @JvmField val f4926e: UIDialogProgress = UIDialogProgress(context, R.style.UIDialogTransparent)
    @JvmField var f4927f: h? = null
    @JvmField var g: ConflictAction = ConflictAction.u
    @JvmField var f4928h: Boolean = false
    @JvmField val b: Handler = Handler(Looper.getMainLooper())

    companion object {
        @JvmField var f4923i: ExecutorService? = Executors.newFixedThreadPool(2)
        @JvmField @Volatile var j: FileOperations? = null

        @JvmStatic
        fun c(context: Context): FileOperations {
            var instance = j
            if (instance == null) {
                synchronized(FileOperations::class.java) {
                    if (j == null) j = FileOperations(context)
                    instance = j
                }
            }
            return instance!!
        }
    }

    fun a(file: File, iArr: IntArray, i2: Int, iArr2: IntArray): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.let { children ->
                for (child in children) {
                    if (!a(child, iArr, i2, iArr2)) return false
                }
            }
        }
        val deleted = file.delete()
        if (deleted) {
            iArr[0] = iArr[0] + 1
            val progress = iArr[0] * 100 / i2
            if (progress > iArr2[0]) {
                iArr2[0] = progress
                if (i2 > 0) b.post { if (f4926e.isShowing) f4926e.b(progress) }
            }
        }
        return deleted
    }

    fun b() {
        f4928h = false
        if (!f4926e.isShowing) return
        f4926e.dismiss()
    }

    fun d(file: File, file2: File, iArr: IntArray, num: Int, iArr2: IntArray): Boolean {
        file2.parentFile?.let { parent ->
            if (!parent.exists()) parent.mkdirs()
        }
        if (!file2.exists() && !file2.createNewFile()) return false
        return try {
            FileInputStream(file).use { input ->
                FileOutputStream(file2).use { output ->
                    val source = input.channel
                    val target = output.channel
                    val size = source.size()
                    var done = 0L
                    while (done < size) {
                        val transferred = source.transferTo(done, 1048576L, target)
                        if (transferred <= 0L) break
                        done += transferred
                        var progress = (100 * done / size).toInt()
                        if (num > 1) progress = progress / num + iArr[0] * 100 / num
                        if (progress > iArr2[0]) {
                            iArr2[0] = progress
                            val remain = Utils.b(f4924a, iArr, num, size, done)
                            b.post {
                                if (f4926e.isShowing) {
                                    f4926e.b(progress)
                                    f4926e.x?.text = remain
                                }
                            }
                        }
                    }
                    iArr[0] = iArr[0] + 1
                    true
                }
            }
        } catch (ignored: Exception) {
            false
        }
    }

    fun e(file: File, file2: File, iArr: IntArray, i2: Int, iArr2: IntArray): Boolean {
        if (!file.isDirectory) return d(file, file2, iArr, i2, iArr2)
        if (!file2.exists() && !file2.mkdirs()) return false
        val names = file.list() ?: return false
        for (name in names) {
            if (!e(File(file, name), File(file2, name), iArr, i2, iArr2)) return false
        }
        return true
    }

    fun f(str: String, str2: String) {
        f4928h = true
        f4927f?.let { b.removeCallbacks(it) }
        val runnable = h(this, str, str2)
        f4927f = runnable
        b.postDelayed(runnable, 400L)
    }
}
