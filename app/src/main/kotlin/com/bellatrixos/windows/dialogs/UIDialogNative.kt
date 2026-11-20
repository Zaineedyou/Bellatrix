package com.bellatrixos.windows.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bellatrixos.R

/** Dialog konfirmasi native dengan ikon dan dua tombol. */
class UIDialogNative(
    context: Context,
    @JvmField val A: String?,
    @JvmField val v: String?,
    @JvmField val z: String?,
    @JvmField val w: Int
) : Dialog(context, R.style.UIDialogDefault) {

    companion object {
        const val E: Int = 0
    }

    @JvmField var B: String? = null
    @JvmField var C: String? = null
    @JvmField var D: DialogInterface.OnClickListener? = null
    @JvmField var u: TextView? = null
    @JvmField val x: Drawable? = null
    @JvmField var y: ImageView? = null

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.ui_dialog_native)
        B = context.getString(R.string.dialog_actions_yes)
        C = context.getString(R.string.dialog_actions_no)
        val close = findViewById<ImageView>(R.id.btn_close)
        val icon = findViewById<ImageView>(R.id.body_icon)
        y = icon
        val bodyTitle = findViewById<TextView>(R.id.body_title)
        val headerTitle = findViewById<TextView>(R.id.header_title)
        val bodySubtitle = findViewById<TextView>(R.id.body_subtitle)
        headerTitle.text = A
        bodyTitle.text = v
        val subtitle = z
        if (subtitle != null) {
            bodySubtitle.text = subtitle
        } else {
            bodySubtitle.visibility = View.GONE
        }
        val drawable = x
        if (drawable != null) {
            icon.setImageDrawable(drawable)
        } else {
            icon.setImageResource(w)
        }
        val positive = findViewById<TextView>(R.id.btn_positive)
        positive.text = B
        positive.setOnClickListener {
            val listener = D
            if (listener != null) {
                listener.onClick(this, DialogInterface.BUTTON_POSITIVE)
            }
            dismiss()
        }
        val negative = findViewById<TextView>(R.id.btn_negative)
        u = negative
        negative.text = C
        negative.setOnClickListener {
            u?.visibility = View.GONE
            dismiss()
        }
        close.setOnClickListener { dismiss() }
    }
}
