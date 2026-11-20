package com.bellatrixos.windows.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import com.bellatrixos.R
import com.bellatrixos.windows.WindowSize

/** Dialog teks umum dengan tombol positif/negatif dan input opsional. */
class UIDialog(context: Context, @JvmField val u: String, @JvmField val v: String?) :
    Dialog(context, R.style.UIDialogAnimated) {

    @JvmField var A: String = ""
    @JvmField var B: TextView? = null
    @JvmField var C: DialogInterface.OnClickListener? = null
    @JvmField var D: DialogInterface.OnClickListener? = null
    @JvmField var E: Boolean = false
    @JvmField var F: TextWatcher? = null
    @JvmField var G: Boolean = true
    @JvmField var w: EditText? = null
    @JvmField var x: String? = null
    @JvmField var y: String? = null
    @JvmField var z: String = ""

    companion object {
        const val H: Int = 0
    }

    fun a(z: Boolean) {
        val view = B ?: return
        view.isEnabled = z
        view.alpha = if (z) 1.0f else 0.57f
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.ui_dialog)
        window?.let { window ->
            val params = window.attributes
            params.width = -1
            params.height = WindowSize.b
            window.attributes = params
        }
        findViewById<TextView>(R.id.titleTextView).text = u
        val positive = findViewById<TextView>(R.id.btn_positive)
        B = positive
        if (z.isNotEmpty()) positive.text = z
        positive.setOnClickListener(a(this, 0))
        val body = findViewById<TextView>(R.id.bodyTextView)
        if (v == null) {
            body.visibility = android.view.View.GONE
        } else {
            body.text = v
        }
        val edit = findViewById<EditText>(R.id.bodyEditText)
        w = edit
        if (x != null) {
            edit.visibility = android.view.View.VISIBLE
            edit.hint = x
            val watcher = object : TextWatcher {
                override fun afterTextChanged(editable: Editable) {
                    this@UIDialog.a(editable.toString().trim().isNotEmpty())
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
            F = watcher
            edit.addTextChangedListener(watcher)
        }
        y?.let { edit.setText(it) }
        val negative = findViewById<TextView>(R.id.btn_negative)
        negative.text = A
        if (A.isNotEmpty()) negative.visibility = android.view.View.VISIBLE
        negative.setOnClickListener(a(this, 1))
        if (E) findViewById<android.view.View>(R.id.dialog_root).setOnClickListener(a(this, 2))
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        F?.let { watcher -> w?.removeTextChangedListener(watcher) }
        F = null
        w?.onFocusChangeListener = null
        w?.setOnLongClickListener(null)
    }

    override fun setCanceledOnTouchOutside(z: Boolean) {
        super.setCanceledOnTouchOutside(z)
        E = z
    }
}
