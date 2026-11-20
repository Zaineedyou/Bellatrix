package com.bellatrixos.app_explorer.files.headers

import java.util.concurrent.TimeUnit

/** Batas waktu untuk grouping tanggal berkas. */
object DatesFiles {
    @JvmField val f4684a: Long = TimeUnit.DAYS.toMillis(1)
    @JvmField var b: Long = 0
    @JvmField var c: Long = 0
    @JvmField var f4685d: Long = 0
    @JvmField var f4686e: Long = 0
    @JvmField var f4687f: Long = 0
    @JvmField var g: Int = 0

    init {
        a()
    }

    @JvmStatic
    fun a() {
        val now = System.currentTimeMillis()
        val day = f4684a
        val today = now - now % day
        b = today
        c = today - day
        f4685d = today - 6 * day
        val month = 31 * day
        f4686e = today - month
        f4687f = f4686e - month
        g = (now / day).toInt()
    }
}
