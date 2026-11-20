package com.bellatrixos

import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.bellatrixos.app_image_viewer.UIImageViewer
import com.bellatrixos.windows.installed_apps.models.InstalledApp

/**
 * Base untuk semua "aplikasi" yang berjalan sebagai jendela di dalam MainActivity.
 * Nama field dan method sengaja mengikuti konvensi internal karena dipanggil lintas modul.
 */
open class UIFragment : Fragment() {
    @JvmField var s0: Int = 0
    @JvmField var t0: Boolean = true

    /** View root jendela (field turunan `c0`). */
    @JvmField var c0: View? = null

    /** Alias deskriptif untuk [c0], dipakai kode Kotlin baru. */
    var contentView: View?
        get() = c0
        set(value) { c0 = value }

    open fun a0(): FrameLayout? = null
    open fun b0(): InstalledApp? = null
    open fun c0(): String? = null
    open fun d0(): String? = null
    open fun e0(): Int = 0
    open fun f0(): Boolean = t0

    fun g0() {
        val view = c0
        if (isAdded && view != null) view.bringToFront()
    }

    open fun h0(i2: Int, keyEvent: KeyEvent) {}
    open fun i0(z: Boolean) {}
    open fun j0(z: Boolean) { t0 = z }

    open fun k0(str: String?) {
        c0?.bringToFront()
    }

    open fun m0(z: Boolean) {}
    open fun o0() {}
    open fun l0() {}

    open fun n0(): Boolean = this is UIImageViewer
}
