package com.bellatrixos.windows.config

import android.os.Build
import android.text.format.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

/** Format tanggal/waktu yang kompatibel dengan API 23+. */
object Dates {
    @JvmStatic
    fun a(dateTimeFormatter: DateTimeFormatter?, str: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return DateFormat.format(str, Date()).toString()
        }
        val formatter = dateTimeFormatter ?: DateTimeFormatter.ofPattern(str).withZone(ZoneId.systemDefault())
        return formatter.format(Instant.now())
    }
}
