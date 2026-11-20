package com.bellatrixos.app_image_viewer

import android.view.View
import com.bellatrixos.R
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.context_menu.UIMenuItem
import com.bellatrixos.windows.WindowSize
import com.bellatrixos.windows.dialogs.UIDialogProperties
import com.bellatrixos.windows.drivers.Printer
import com.bellatrixos.windows.installed_apps.IntentSites

/** Klik tombol-tombol penampil gambar. */
class d(
    @JvmField val v: UIImageViewer,
    @JvmField val u: Int
) : View.OnClickListener {

    override fun onClick(view: View) {
        when (u) {
            0 -> {
                val viewer = v
                viewer.H0.resetScaleAndCenter()
                viewer.w0.progress = viewer.N0
            }
            1 -> {
                val viewer = v
                val dialog = UIDialogProperties(viewer.v0, viewer.u0File)
                dialog.setCanceledOnTouchOutside(false)
                dialog.show()
            }
            2 -> v.v0()
            3 -> v.u0()
            4 -> WindowSize.c(v.S0)
            5 -> {
                val viewer = v
                viewer.D0.P(viewer)
            }
            6 -> {
                val viewer = v
                viewer.D0.A(viewer.T)
            }
            7 -> {
                val viewer = v
                val context = viewer.D0.f() ?: return
                Printer.a(context, viewer.u0File)
            }
            8 -> {
                val viewer = v
                IntentSites.d(viewer.v0, viewer.u0File)
            }
            9 -> {
                val viewer = v
                var rotation = viewer.O0 + 90.0f
                if (rotation >= 360.0f) rotation = 0.0f
                viewer.O0 = rotation
                viewer.H0.animate().rotation(rotation).setDuration(180L).start()
                viewer.H0.post { viewer.H0.invalidate() }
            }
            else -> {
                val viewer = v
                val menu = viewer.D0.l0() as? UIContextMenu ?: return
                menu.f4745d.clear()
                val copy = UIMenuItem(
                    0, 7, R.drawable.ui_webapp_ic_copy,
                    viewer.v0.getString(R.string.context_menu_copy)
                )
                copy.f4748d = "CTRL + C"
                menu.f4745d.add(copy)
                val setAs = UIMenuItem(
                    0, 23, R.drawable.ic_fluent_image_arrow_right_24_regular,
                    viewer.v0.getString(R.string.context_menu_set_as)
                )
                setAs.g = true
                menu.f4745d.add(setAs)
                menu.c = a(viewer, menu, 0)
                menu.a(R.style.AnimationDialogFadeDown)
                val offset = viewer.v0.resources.getDimension(R.dimen.ui_6dp).toInt()
                menu.g(viewer.A0.getMoreBtnView(), offset)
            }
        }
    }
}
