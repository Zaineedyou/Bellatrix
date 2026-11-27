package com.bellatrixos.app_webapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import com.bellatrixos.R
import com.bellatrixos.app_browser.utils.Scaling
import com.bellatrixos.widgets.UINotification
import com.bellatrixos.widgets.UIToast
import com.bellatrixos.windows.icons.IconEngine

/** Callback menu, hasil pemilih berkas, dan ikon web app. */
class c(@JvmField val u: UIWebApp) :
    IconEngine.Callback,
    UISettingsMenu.OnMenuItemClickListener,
    ActivityResultCallback<Any?> {

    override fun a(i2: Int, i3: Int) {
        val app = u
        when (i2) {
            1 -> Scaling.d(app.x0, app.E0, i3)
            2 -> {
                UINotification.a(app.x0, "Printing isn't available in this browser yet", null)
            }
            3 -> {
                val context: Context = app.x0
                val url = app.w0.url ?: return
                val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                if (manager != null) {
                    manager.setPrimaryClip(ClipData.newPlainText("url", url))
                    UIToast.a(
                        context,
                        context.getString(R.string.app_web_link_copied),
                        R.drawable.ui_webapp_ic_copy,
                        2000
                    )
                }
            }
            8 -> if (app.w0.isLaidOut) app.Q0.b(app.x0)
            9 -> app.R0.a(app.C0.getBtnMore(), null, 3)
            else -> {
                if (i2 == 11) {
                    app.P0 = i3
                    app.O0 = true
                }
            }
        }
    }

    override fun onActivityResult(result: Any?) {
        val app = u
        app.onFilesChosen(result as? ActivityResult)
    }

    override fun i(bitmap: Bitmap?) {
        u.A0.setImageBitmap(bitmap)
    }
}
