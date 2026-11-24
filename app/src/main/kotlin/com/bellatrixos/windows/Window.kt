package com.bellatrixos.windows

/** Informasi ukuran/status sebuah jendela aplikasi. */
class Window(@JvmField var c: Int) {
    fun interface OnResizeStateChangeListener {
        fun b(i2: Int)
    }

    @JvmField var f4870e: OnResizeStateChangeListener? = null
    @JvmField var f4868a: Float = -1.0f
    @JvmField var b: Float = -1.0f
    @JvmField var f4869d: Int = 1

    /** The fragment class this window belongs to, so native-ui window rules can match it. */
    @JvmField var tag: String? = null

    fun a(i2: Int) {
        c = i2
        f4870e?.b(i2)
    }
}
