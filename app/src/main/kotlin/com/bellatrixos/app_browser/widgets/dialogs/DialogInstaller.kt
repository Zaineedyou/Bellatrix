package com.bellatrixos.app_browser.widgets.dialogs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import com.bellatrixos.R
import com.bellatrixos.app_browser.utils.i
import java.net.URI

/** Popup konfirmasi pemasangan aplikasi web. */
class DialogInstaller(context: Context, str: String?, str2: String?, bitmap: Bitmap?) {
    @JvmField val f4656a: PopupWindow
    @JvmField var b: OnInstallClickListener? = null

    fun interface OnInstallClickListener {
        fun a(drawable: Drawable?, str: String?, str2: String?)
    }

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.app_browser_dialog_install, null as ViewGroup?)
        f4656a = PopupWindow(view, -2, -2, true)
        val install = view.findViewById<Button>(R.id.btn_install)
        val cancel = view.findViewById<Button>(R.id.btn_cancel)
        val urlText = view.findViewById<TextView>(R.id.url_text)
        val editName = view.findViewById<EditText>(R.id.edit_app_name)
        val icon = view.findViewById<ImageView>(R.id.app_icon)

        var displayUrl: String? = null
        try {
            val uri = URI(str2)
            var host = uri.host
            if (host == null) {
                displayUrl = str2
            } else {
                if (host.startsWith("www.")) host = host.substring(4)
                val builder = StringBuilder(host)
                val path = uri.rawPath
                val query = uri.rawQuery
                if (!path.isNullOrEmpty()) builder.append(path)
                if (query != null) builder.append("?").append(query)
                displayUrl = builder.toString()
                if (displayUrl!!.endsWith("/") && displayUrl!!.length > 1) {
                    displayUrl = displayUrl!!.substring(0, displayUrl!!.length - 1)
                }
            }
        } catch (_: Exception) {
        }
        urlText.text = displayUrl

        val name = if (str.isNullOrEmpty()) {
            ""
        } else {
            val parts = str.trim().split("\\s+".toRegex())
            if (parts.size <= 3) parts.joinToString(" ") else parts.take(3).joinToString(" ")
        }
        editName.setText(name)
        if (bitmap != null) icon.setImageBitmap(bitmap)

        install.setOnClickListener(i(this, icon, editName, str2))
        cancel.setOnClickListener(a(0, this))
        f4656a.animationStyle = R.style.AnimationDialogMaterial
    }
}
