package com.bellatrixos.windows.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import com.bellatrixos.R
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.variables.Global

/** Dialog pemilih metode klik mouse. */
class UIDialogMouse(context: Context, @JvmField val u: MainActivityIn) :
    Dialog(context, R.style.UIDialogDefault) {

    companion object {
        const val v: Int = 0
    }

    init {
        setCanceledOnTouchOutside(true)
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.ui_dialog_mouse)
        val group = findViewById<RadioGroup>(R.id.radio_group)
        val single = findViewById<RadioButton>(R.id.radio_click_single)
        val double = findViewById<RadioButton>(R.id.radio_click_double)
        if (Global.p == 0) single.isChecked = true else double.isChecked = true
        group.setOnCheckedChangeListener { _, id ->
            val mode = if (id == R.id.radio_click_single) 0 else 1
            SharedPrefs.f(mode, Runnable {
                Global.p = mode
                u.H(mode != 0)
                Handler().postDelayed(Runnable { dismiss() }, 500L)
            })
        }
        findViewById<Button>(R.id.btn_negative).setOnClickListener(a(this, 3))
    }
}
