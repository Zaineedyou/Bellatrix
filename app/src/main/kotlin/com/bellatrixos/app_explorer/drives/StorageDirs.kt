package com.bellatrixos.app_explorer.drives

import android.content.Context
import android.os.StatFs
import com.bellatrixos.R
import java.io.File
import kotlin.math.round

/** Informasi kapasitas sebuah drive. */
object StorageDirs {
    @JvmStatic
    fun a(context: Context, file: File?): StorageStat {
        if (file == null || !file.exists()) {
            return StorageStat(context.getString(R.string.storage_unavailable), 0, 0.0, null)
        }
        val stat = StatFs(file.absolutePath)
        val total = stat.blockCountLong * stat.blockSizeLong
        val free = stat.availableBlocksLong * stat.blockSizeLong
        val freeGb = round(free / 1.073741824E9 * 10.0) / 10.0
        val totalGb = round(total / 1.073741824E9 * 10.0) / 10.0
        val used = if (total != 0L) ((total - free) * 100 / total).toInt() else 0
        val label = context.getString(R.string.storage_free_of, freeGb, totalGb)
        return StorageStat(label, used, totalGb, file.absolutePath)
    }
}
