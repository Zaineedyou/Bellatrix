package com.bellatrixos.windows.files.operations

import android.view.View
import android.widget.Toast
import com.bellatrixos.R
import com.bellatrixos.app_explorer.UIExplorer

/** Runnable akhir operasi + inisialisasi tampilan explorer (bila dipakai). */
class i(
    @JvmField val u: Int,
    obj: Any?,
    obj2: Any?,
    z: Boolean
) : Runnable {
    @JvmField val v: Boolean = z
    @JvmField val w: Any? = obj
    @JvmField val x: Any? = obj2

    override fun run() {
        when (u) {
            0 -> {
                val operations = w as FileOperations
                operations.b()
                if (v) {
                    operations.c?.a(x as List<*>)
                } else {
                    // androidx.media3.exoplayer.audio.l.o(context, id, context, 0)
                    // dalam bytecode hanya membungkus Toast.makeText(..., 0).show().
                    val ctx = operations.f4924a
                    Toast.makeText(ctx, ctx.getString(R.string.toast_file_failed_copy), 0).show()
                }
            }
            else -> {
                val explorer = x as UIExplorer
                val view = w as View
                // Helper f.f/f.b/f.c/f.d/f.e sudah disatukan sebagai
                // UIExplorer.setupAfterInit pada port Kotlin ini.
                explorer.setupAfterInit(view, v)
            }
        }
    }
}
