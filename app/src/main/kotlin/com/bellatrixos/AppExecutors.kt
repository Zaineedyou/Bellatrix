package com.bellatrixos

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Kumpulan executor bersama untuk seluruh aplikasi. */
object AppExecutors {
    @JvmField var f4623a: Handler? = null
    @JvmField @Volatile var b: ExecutorService? = null
    @JvmField @Volatile var c: ExecutorService? = null
    @JvmField @Volatile var f4624d: ExecutorService? = null

    private fun healthy(service: ExecutorService?): Boolean =
        service != null && !service.isShutdown && !service.isTerminated

    @JvmStatic
    fun a(): ExecutorService {
        if (!healthy(f4624d)) {
            synchronized(this) {
                if (!healthy(f4624d)) f4624d = Executors.newFixedThreadPool(2)
            }
        }
        return f4624d!!
    }

    @JvmStatic
    fun b(): ExecutorService {
        if (!healthy(b)) {
            synchronized(this) {
                if (!healthy(b)) b = Executors.newCachedThreadPool()
            }
        }
        return b!!
    }

    @JvmStatic
    fun c(): Handler {
        if (f4623a == null) f4623a = Handler(Looper.getMainLooper())
        return f4623a!!
    }

    @JvmStatic
    fun d(): ExecutorService {
        if (!healthy(c)) {
            synchronized(this) {
                if (!healthy(c)) c = Executors.newSingleThreadExecutor()
            }
        }
        return c!!
    }
}
