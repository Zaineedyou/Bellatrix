package com.bellatrixos.windows.files.operations

import com.bellatrixos.app_explorer.files.Files
import java.io.File
import java.util.concurrent.CountDownLatch

/** Runnable satu berkas: salin berkas (u=0) atau direktori (u=1). */
class c private constructor(
    @JvmField val u: Int,
    @JvmField val v: FileOperations,
    @JvmField val w: File,
    @JvmField val x: String,
    @JvmField val y: String
) : Runnable {

    constructor(operations: FileOperations, file: File, str: String, str2: String) :
        this(0, operations, file, str, str2)

    constructor(operations: FileOperations, str: String, str2: String, file: File) :
        this(1, operations, file, str, str2)

    override fun run() {
        val counts = IntArray(1)
        val progress = IntArray(1)
        var target = File(x, y)
        if (target.exists()) {
            val latch = CountDownLatch(1)
            val targetRef = arrayOf(target)
            val skip = booleanArrayOf(false)
            v.b.post(e(v, y, targetRef, counts, progress, latch, skip, x, if (u == 0) 1 else 0))
            try {
                latch.await()
            } catch (ignored: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            if (skip[0]) {
                v.b()
                return
            }
            target = targetRef[0]
        }
        try {
            val success = if (u == 0) {
                v.d(w, target, counts, 1, progress)
            } else {
                v.e(w, target, counts, Utils.a(w), progress)
            }
            if (Files.g && success) {
                try {
                    v.a(w, intArrayOf(0), Utils.a(w), intArrayOf(progress[0]))
                    return
                } catch (ignored: Exception) {
                }
            }
            v.b.post(f(v, success, target, if (u == 0) 2 else 0))
        } catch (ignored: Exception) {
            v.b.post(d(if (u == 0) 0 else 1, v))
        }
    }
}
