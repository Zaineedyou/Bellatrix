package com.bellatrixos.windows.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.bellatrixos.R

/** Dialog progres penyalinan berkas dan konflik namanya. */
class UIDialogProgress @JvmOverloads constructor(
    context: android.content.Context,
    style: Int = 0
) : Dialog(context, style) {

    interface ConflictListener {
        fun a(z: Boolean)
        fun b(z: Boolean)
        fun c(z: Boolean)
    }

    @JvmField var A: LinearLayout? = null
    @JvmField var B: LinearLayout? = null
    @JvmField var C: Button? = null
    @JvmField var D: Button? = null
    @JvmField var E: Button? = null
    @JvmField var F: CheckBox? = null
    @JvmField var G: ConflictListener? = null
    @JvmField var u: TextView? = null
    @JvmField var v: TextView? = null
    @JvmField var w: TextView? = null
    @JvmField var x: TextView? = null
    @JvmField var y: TextView? = null
    @JvmField var z: ProgressBar? = null

    fun a() {
        A?.visibility = android.view.View.GONE
        B?.visibility = android.view.View.VISIBLE
        F?.isChecked = false
    }

    fun b(i2: Int) {
        z?.progress = i2
        u?.text = context.getString(R.string.msg_files_copy_progress, i2.toString())
    }

    fun c(str: String?, conflictListener: ConflictListener) {
        G = conflictListener
        if (!isShowing) show()
        A?.visibility = android.view.View.VISIBLE
        B?.visibility = android.view.View.GONE
        if (str != null) y?.text = context.getString(R.string.msg_files_conflict_message, str)
        C?.setOnClickListener { G?.a(F?.isChecked == true) }
        D?.setOnClickListener { G?.b(F?.isChecked == true) }
        E?.setOnClickListener { G?.c(F?.isChecked == true) }
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.ui_dialog_copy)
        u = findViewById(R.id.txt_percentage)
        v = findViewById(R.id.file_name)
        z = findViewById(R.id.progress_bar)
        w = findViewById(R.id.header_title)
        x = findViewById(R.id.item_remain)
        A = findViewById(R.id.layout_conflict)
        B = findViewById(R.id.layout_progress)
        C = findViewById(R.id.btn_replace)
        D = findViewById(R.id.btn_skip)
        E = findViewById(R.id.btn_rename)
        F = findViewById(R.id.checkbox_apply_all)
        y = findViewById(R.id.txt_conflict_message)
    }
}
