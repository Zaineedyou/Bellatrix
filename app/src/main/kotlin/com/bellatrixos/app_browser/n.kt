package com.bellatrixos.app_browser

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.webkit.PermissionRequest
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bellatrixos.R
import com.bellatrixos.interfaces.MainActivityIn

/** Runnable izin kamera/mikrofon WebView dan pembuka halaman hasil notifikasi. */
class n(@JvmField val v: Any?, @JvmField val u: Int, @JvmField val w: Any?) : Runnable {

    override fun run() {
        if (u == 0) {
            val tab = v as? UIBrowserTab ?: return
            val request = w as? PermissionRequest ?: return
            val context = tab.s0 ?: return

            val hasMicrophone = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == 0
            val hasCamera = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == 0

            val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            if (hasCamera && hasMicrophone) {
                request.grant(request.resources)
                return
            }

            val activity = tab.activity ?: (context as? Activity)
            val shouldExplain = activity != null &&
                (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.CAMERA
                ) || ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.RECORD_AUDIO
                ))

            if (shouldExplain) {
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.browser_dialog_permission))
                    .setMessage(context.getString(R.string.browser_dialog_permission_info))
                    .setPositiveButton(context.getString(R.string.dialog_actions_ok)) { _: DialogInterface, _: Int ->
                        tab.E0 = request
                        tab.F0?.launch(permissions)
                    }
                    .setNegativeButton(context.getString(R.string.dialog_actions_cancel)) { _: DialogInterface, _: Int ->
                        request.deny()
                    }
                    .create()
                    .show()
            } else {
                tab.E0 = request
                tab.F0?.launch(permissions)
            }
            return
        }

        // u == 1: buka halaman dari notifikasi hasil pemasangan aplikasi.
        val browser = v as? UIBrowser ?: return
        val url = w as? String ?: return
        val context: Context? = browser.context
        val activity = if (context is MainActivityIn) {
            context
        } else {
            browser.activity as? MainActivityIn
        }
        activity?.c0(url, url)
    }
}
