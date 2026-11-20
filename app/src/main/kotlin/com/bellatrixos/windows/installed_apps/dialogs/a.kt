package com.bellatrixos.windows.installed_apps.dialogs

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bellatrixos.windows.icons.IconEngine
import com.bellatrixos.windows.icons.dialogs.DialogAppIcons

/** Callback ikon untuk editor aplikasi. */
class a(@JvmField val u: Int, @JvmField val v: DialogAppEdit) :
    IconEngine.Callback,
    DialogAppIcons.SelectionListener,
    DialogAppIcons.SelectionPkgListener {

    override fun a(str: String) {
        IconEngine.b(v.v).c(str, a(3, v))
        markChanged()
    }

    override fun b(drawable: Drawable?) {
        v.u?.setImageDrawable(drawable)
        markChanged()
    }

    override fun i(bitmap: Bitmap?) {
        v.u?.setImageBitmap(bitmap)
    }

    private fun markChanged() {
        v.F = true
        v.x?.isEnabled = true
        v.x?.alpha = 1.0f
    }
}
