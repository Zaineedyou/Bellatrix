package com.bellatrixos.variables

import com.bellatrixos.app_explorer.drives.StorageStat
import java.time.format.DateTimeFormatter

/**
 * State global proses. Menggantikan `Global.java`.
 * Nama anggota dipertahankan agar kompatibel dengan kode lain yang masih
 * memanggilnya lewat nama field hasil R8.
 */
object Global {
    val A: IntArray = intArrayOf(10, 10)

    @JvmField var B: Int = 0
    @JvmField var C: Boolean = false

    @JvmField var f4836a: Int = 0
    @JvmField var b: Boolean = false
    @JvmField var c: Int = 0
    @JvmField var f4837d: Int = -1
    @JvmField var f4838e: Float = 1.0f
    @JvmField var f4839f: Boolean = false
    @JvmField var g: Boolean = false
    @JvmField var f4840h: Boolean = true
    @JvmField var f4841i: Boolean = false
    @JvmField var j: Boolean = false
    @JvmField var f4842k: Int = 0
    @JvmField var l: Int = 0
    @JvmField var m: Boolean = false
    @JvmField var f4843n: Int = -1
    @JvmField var o: Boolean = true
    @JvmField var p: Int = 0
    @JvmField var q: Int = 1
    @JvmField var r: Boolean = true
    @JvmField var s: Int = -1

    @JvmField var t: DateTimeFormatter? = null
    @JvmField var u: DateTimeFormatter? = null
    @JvmField var v: DateTimeFormatter? = null
    @JvmField var w: StorageStat? = null
    @JvmField var x: StorageStat? = null
    @JvmField var y: String? = null
    @JvmField var z: String? = null
}
