package com.bellatrixos.app_webapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import com.bellatrixos.R
import com.bellatrixos.app_browser.utils.Scaling

/** Klik menu pengaturan web app; `u` adalah indeks aksi. */
class a(@JvmField val v: UISettingsMenu, @JvmField val u: Int) : View.OnClickListener {
    override fun onClick(view: View) {
        val menu = v
        when (u) {
            0 -> { menu.dismiss(); menu.f4731d?.a(3, 0) }
            1 -> {
                menu.f4736k.visibility = View.GONE
                menu.j.visibility = View.VISIBLE
                menu.f4737n = false
            }
            2 -> { menu.dismiss(); menu.f4731d?.a(2, 0) }
            3 -> { menu.dismiss(); menu.f4731d?.a(9, 0) }
            4 -> { menu.dismiss(); menu.f4731d?.a(8, 0) }
            5 -> showZoom(menu)
            6 -> changeZoom(menu, 5)
            else -> changeZoom(menu, -5)
        }
    }

    private fun showZoom(menu: UISettingsMenu) {
        val frame = menu.f4736k
        var zoomView = menu.f4733f
        if (zoomView == null) {
            zoomView = LayoutInflater.from(menu.contentView.context)
                .inflate(R.layout.app_web_settings_zoom, frame, false)
            menu.f4733f = zoomView
            val back = zoomView.findViewById<ImageButton>(R.id.btnBack)
            menu.f4734h = zoomView.findViewById(R.id.txtZoom)
            val plus = zoomView.findViewById<Button>(R.id.btnZoomPlus)
            val minus = zoomView.findViewById<Button>(R.id.btnZoomMinus)
            menu.g = zoomView.findViewById(R.id.txtScale)
            menu.f4735i = zoomView.findViewById(R.id.viewInfo)
            val scalePlus = zoomView.findViewById<Button>(R.id.btnScalePlus)
            val scaleMinus = zoomView.findViewById<Button>(R.id.btnScaleMinus)
            menu.g?.let { edit ->
                edit.inputType = 2
                edit.onFocusChangeListener =
                    com.bellatrixos.app_browser.utils.d(
                        IntArray(1), intArrayOf(menu.f4730a), g(menu)
                    )
                edit.setOnEditorActionListener(com.bellatrixos.app_browser.utils.e())
            }
            plus.setOnClickListener { changeZoom(menu, 5) }
            minus.setOnClickListener { changeZoom(menu, -5) }
            scalePlus.setOnClickListener { changeScale(menu, 5, it) }
            scaleMinus.setOnClickListener { changeScale(menu, -5, it) }
            back.setOnClickListener { a(menu, 1).onClick(it) }
            frame.addView(zoomView)
        }
        menu.j.visibility = View.GONE
        frame.visibility = View.VISIBLE
        menu.g?.setText(
            menu.g?.context?.getString(R.string.browser_zoom_value_sm, menu.f4730a)
        )
        menu.f4734h?.let {
            it.text = it.context.getString(R.string.browser_zoom_value_sm, menu.b)
        }
        menu.f4737n = true
    }

    private fun changeZoom(menu: UISettingsMenu, delta: Int) {
        menu.f4731d?.let { listener ->
            menu.b += delta
            menu.a(menu.b)
            menu.f4734h?.let {
                it.text = it.context.getString(R.string.browser_zoom_value_sm, menu.b)
            }
            listener.a(1, menu.b)
        }
    }

    private fun changeScale(menu: UISettingsMenu, delta: Int, view: View) {
        if (!menu.m) {
            Scaling.a(
                menu.f4735i ?: return,
                view.context.getString(R.string.app_web_require_refresh)
            )
        }
        menu.m = true
        menu.f4730a += delta
        menu.g?.setText(
            menu.g?.context?.getString(R.string.browser_zoom_value_sm, menu.f4730a)
        )
        menu.f4731d?.a(11, menu.f4730a)
    }
}
