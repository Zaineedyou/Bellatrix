package com.bellatrixos.app_browser.widgets.dialogs

import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.PopupWindow
import com.bellatrixos.R
import com.bellatrixos.app_browser.utils.Scaling
import com.bellatrixos.interfaces.MainActivityIn

/** Penyesuaian skala pada DialogSettings dan pembuka folder unduhan. */
class c(@JvmField val v: PopupWindow, @JvmField val w: Any?, @JvmField val u: Int) :
    View.OnClickListener {

    override fun onClick(view: View) {
        when (u) {
            0, 1 -> changeScale()
            else -> openDownloads()
        }
    }

    private fun changeScale() {
        val dialog = v as? DialogSettings ?: return
        val button = w as? Button
        if (!dialog.m) {
            val info = dialog.f4662i
            val message = button?.context?.getString(R.string.app_web_require_refresh) ?: ""
            if (info != null) Scaling.a(info, message)
        }
        dialog.m = true
        dialog.b += if (u == 0) 5 else -5
        val scaleText = dialog.f4660f ?: return
        val zoomText = dialog.f4661h ?: return
        scaleText.setText(zoomText.context.getString(R.string.browser_zoom_value_sm, dialog.b))
        dialog.f4658d?.a(11, 0)
    }

    private fun openDownloads() {
        val activity = w as? MainActivityIn ?: return
        val folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!folder.exists() || !folder.isDirectory || !folder.canRead()) return
        val absolutePath = folder.absolutePath
        if (absolutePath.isEmpty()) return
        (v as? UIDownloads)?.dismiss()
        activity.c0("UIExplorer", absolutePath)
    }
}
