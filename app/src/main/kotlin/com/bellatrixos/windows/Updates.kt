package com.bellatrixos.windows

import com.google.android.play.core.appupdate.AppUpdateManager

/** Pemeriksa update aplikasi. */
object Updates {
    @JvmField var f4866a: AppUpdateManager? = null

    fun interface UpdateListener {
        fun b()
    }
}
