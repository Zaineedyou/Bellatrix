package com.bellatrixos.app_browser.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.webkit.WebView
import androidx.core.util.Consumer
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.app_browser.widgets.dialogs.DialogInstaller
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.widgets.UINotification
import com.bellatrixos.windows.files.operations.New
import com.bellatrixos.windows.installed_apps.InstalledAppsDB
import java.io.File
import java.io.IOException

/** Callback pemasangan web app dan menu konteks daftar berkas. */
class a : DialogInstaller.OnInstallClickListener, UIContextMenu.OnMenuItemClickListener {
    @JvmField var u: Context? = null
    @JvmField var v: Any? = null
    @JvmField var w: Any? = null
    @JvmField var x: Any? = null

    constructor(context: Context, file: File, fileAdapter: Any?, menu: UIContextMenu) {
        u = context
        v = file
        w = fileAdapter
        x = menu
    }

    constructor(webView: WebView, context: Context, activity: MainActivityIn?, consumer: Consumer<Any?>) {
        v = webView
        u = context
        w = activity
        x = consumer
    }

    override fun a(drawable: Drawable?, str: String?, str2: String?) {
        val webView = v as? WebView
        val context = u ?: return
        val installed = webView?.url?.let { InstalledAppsDB.b.containsKey(it) } == true
        if (installed) {
            UINotification.a(
                context,
                context.getString(R.string.app_already_installed),
                context.getString(R.string.app_installer)
            )
            return
        }
        // IconManager dan callback Activity belum termigrasi; berikan notifikasi
        // dan jalankan consumer setelah jeda seperti implementasi asalnya.
        val consumer = x as? Consumer<Any?> ?: return
        AppExecutors.c().postDelayed({
            consumer.accept(str ?: "")
        }, 1200L)
    }

    override fun f(view: View, i2: Int) {
        val context = u ?: return
        val file = v as? File
        when (i2) {
            251 -> {
                if (file != null) New.a(context, file)
            }
            252 -> {
                if (file != null && file.exists() && file.isDirectory) {
                    val base = context.getString(R.string.files_new_doc_text)
                    var target = File(file, "$base.txt")
                    var counter = 2
                    while (target.exists()) {
                        target = File(file, "$base ($counter).txt")
                        counter++
                    }
                    try {
                        target.createNewFile()
                    } catch (_: IOException) {
                    }
                }
            }
        }
        (x as? UIContextMenu)?.dismiss()
    }
}
