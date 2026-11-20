package com.bellatrixos.app_browser.utils

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.cursoradapter.widget.SimpleCursorAdapter
import com.bellatrixos.app_browser.widgets.AddressBarView
import com.bellatrixos.app_browser.widgets.dialogs.DialogInstaller

/** Klik hapus riwayat alamat dan klik tombol pasang dialog. */
class i : View.OnClickListener {
    @JvmField var u: Int = 0
    @JvmField var v: String? = null
    @JvmField var w: Any? = null
    @JvmField var x: View? = null
    @JvmField var y: Any? = null

    constructor(context: Context, str: String, view: AddressBarView, adapter: SimpleCursorAdapter) {
        w = context
        v = str
        x = view
        y = adapter
        u = 0
    }

    constructor(dialog: DialogInstaller, imageView: ImageView, editText: EditText, str: String?) {
        w = dialog
        x = imageView
        y = editText
        v = str
        u = 1
    }

    override fun onClick(view: View) {
        if (u == 0) {
            val context = w as? Context ?: return
            context.contentResolver.delete(
                Uri.parse("content://com.bellatrixos.app_browser.MySuggestionProvider/suggestions"),
                "term = ?",
                arrayOf(v ?: "")
            )
            val adapter = y as? SimpleCursorAdapter
            val address = x as? AddressBarView
            address?.post {
                adapter?.notifyDataSetChanged()
                address.dismissDropDown()
            }
            return
        }

        val dialog = w as? DialogInstaller ?: return
        dialog.f4656a.dismiss()
        val drawable = (x as? ImageView)?.drawable
        val name = (y as? EditText)?.text?.toString() ?: ""
        dialog.b?.a(drawable, v, name)
    }
}
