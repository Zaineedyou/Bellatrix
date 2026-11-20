package com.bellatrixos.app_browser.widgets.dialogs

import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.bellatrixos.R
import com.bellatrixos.app_browser.utils.Scaling
import com.bellatrixos.app_browser.utils.d
import com.bellatrixos.app_browser.utils.e

/** Klik item menu pengaturan browser. */
class b(@JvmField val v: DialogSettings, @JvmField val u: Int) : View.OnClickListener {

    override fun onClick(view: View) {
        val dialog = v
        when (u) {
            0 -> dispatch(dialog, 4)
            1 -> {
                dialog.f4663k.visibility = View.GONE
                dialog.j.visibility = View.VISIBLE
                dialog.f4664n = false
            }
            2 -> dispatch(dialog, 5)
            3 -> dispatch(dialog, 6)
            4 -> dispatch(dialog, 2)
            5 -> dispatch(dialog, 8)
            6 -> dispatch(dialog, 7)
            7 -> openZoom(dialog)
            8 -> changeZoom(dialog, 5)
            else -> changeZoom(dialog, -5)
        }
    }

    private fun dispatch(dialog: DialogSettings, what: Int) {
        val listener = dialog.f4658d ?: return
        listener.a(what, 0)
        dialog.dismiss()
    }

    private fun openZoom(dialog: DialogSettings) {
        if (dialog.f4659e == null) {
            val zoomView = LayoutInflater.from(dialog.contentView.context)
                .inflate(R.layout.app_browser_settings_zoom, dialog.f4663k, false)
            dialog.f4659e = zoomView
            val back = zoomView.findViewById<ImageButton>(R.id.btnBack)
            dialog.f4661h = zoomView.findViewById(R.id.txtZoom)
            val plus = zoomView.findViewById<Button>(R.id.btnZoomPlus)
            val minus = zoomView.findViewById<Button>(R.id.btnZoomMinus)
            dialog.f4660f = zoomView.findViewById(R.id.txtScale)
            dialog.f4662i = zoomView.findViewById(R.id.viewInfo)
            val scalePlus = zoomView.findViewById<Button>(R.id.btnScalePlus)
            val scaleMinus = zoomView.findViewById<Button>(R.id.btnScaleMinus)

            dialog.f4660f?.inputType = InputType.TYPE_CLASS_NUMBER
            dialog.f4660f?.setOnFocusChangeListener(
                d(
                    IntArray(1),
                    intArrayOf(dialog.b),
                    object : Scaling.OnScaleChangedListener {
                        override fun k(i2: Int) {
                            dialog.b = i2
                            dialog.a(i2)
                        }
                    }
                )
            )
            dialog.f4660f?.setOnEditorActionListener(e())
            plus.setOnClickListener(b(dialog, 8))
            minus.setOnClickListener(b(dialog, 9))
            scalePlus.setOnClickListener(c(dialog, scalePlus, 0))
            scaleMinus.setOnClickListener(c(dialog, scaleMinus, 1))
            back.setOnClickListener(b(dialog, 1))
            dialog.f4663k.addView(zoomView)
        }
        dialog.j.visibility = View.GONE
        dialog.f4663k.visibility = View.VISIBLE
        val scaleText = dialog.f4660f ?: return
        val zoomText = dialog.f4661h ?: return
        scaleText.setText(zoomText.context.getString(R.string.browser_zoom_value_sm, dialog.b))
        zoomText.setText(zoomText.context.getString(R.string.browser_zoom_value_sm, dialog.c))
        dialog.f4664n = true
    }

    private fun changeZoom(dialog: DialogSettings, delta: Int) {
        val listener = dialog.f4658d ?: return
        dialog.c += delta
        dialog.a(dialog.c)
        dialog.f4661h?.let { zoomText ->
            zoomText.setText(zoomText.context.getString(R.string.browser_zoom_value_sm, dialog.c))
        }
        listener.a(1, dialog.c)
    }
}
