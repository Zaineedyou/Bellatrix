package com.bellatrixos.app_image_viewer

import android.view.View
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.app_browser.widgets.dialogs.d
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.context_menu.UIMenuItem
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.windows.files.clipboard.ClipboardItem
import com.bellatrixos.windows.files.clipboard.Clipboards

/** Callback menu konteks penampil gambar. */
class a(
    @JvmField val v: UIImageViewer,
    @JvmField val w: UIContextMenu,
    @JvmField val u: Int
) : UIContextMenu.OnMenuItemClickListener {

    override fun f(view: View, i2: Int) {
        when (u) {
            0 -> {
                val viewer = v
                if (i2 == 7) {
                    Clipboards.f4922a.add(
                        ClipboardItem(2, viewer.u0File.parent ?: "", viewer.u0File.name)
                    )
                }
                if (i2 == 23) {
                    val menu = viewer.D0.k() as? UIContextMenu ?: return
                    menu.f4745d.clear()
                    menu.f4745d.add(
                        UIMenuItem(
                            0, 104, R.drawable.ic_taskbar_24_regular,
                            viewer.v0.getString(R.string.settings_personalize_background)
                        )
                    )
                    menu.c = a(viewer, w, 1)
                    menu.a(0)
                    menu.e(view, view.width, -view.height)
                }
            }
            else -> {
                val viewer = v
                if (i2 == 104) {
                    val context = viewer.v0
                    val main: MainActivityIn = viewer.D0
                    val quality = SharedPrefs.f4755a?.getInt("wallpaper_quality", 0) ?: 0
                    AppExecutors.d().execute(
                        d(viewer.u0File.absolutePath, context, quality, main)
                    )
                }
                w.dismiss()
            }
        }
    }
}
