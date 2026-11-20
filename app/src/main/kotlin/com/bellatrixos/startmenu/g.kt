package com.bellatrixos.startmenu

import android.graphics.Bitmap
import android.widget.ImageView
import com.bellatrixos.startmenu.listeners.StartMenuBG
import com.bellatrixos.windows.icons.IconEngine

/** Callback ikon Start Menu: bersihkan atau pasang bitmap hasil loader. */
class g(@JvmField val u: ImageView) : StartMenuBG.OnChangeListener, IconEngine.Callback {
    override fun a() {
        u.setImageBitmap(null)
    }

    override fun i(bitmap: Bitmap?) {
        u.setImageBitmap(bitmap)
    }
}
