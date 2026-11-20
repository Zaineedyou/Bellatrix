package com.bellatrixos.windows.drivers

import android.media.SoundPool

/** Suara sistem (klik dan notifikasi). */
object Sounds {
    @JvmField var f4902a: SoundPool? = null
    @JvmField var b: Int = 0
    @JvmField var c: Int = 0
    @JvmField var f4903d: Float = 1.0f

    @JvmStatic
    fun a(i2: Int) {
        val pool = f4902a ?: return
        val sound = if (i2 == 1) c else if (i2 == 0) b else return
        val volume = f4903d
        pool.play(sound, volume, volume, 1, 0, 1.0f)
    }
}
