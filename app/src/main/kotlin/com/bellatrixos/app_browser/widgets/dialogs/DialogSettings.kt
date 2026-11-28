package com.bellatrixos.app_browser.widgets.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bellatrixos.R
import com.bellatrixos.variables.Global
import com.bellatrixos.windows.installed_apps.InstalledAppsDB
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import kotlin.math.round

/** Menu pengaturan zoom dan tindakan browser. */
class DialogSettings(context: Context, installedApp: InstalledApp?) : PopupWindow(context) {
    companion object {
        @JvmField val o: Int = 0
    }

    @JvmField val f4657a: Int
    @JvmField var b: Int
    @JvmField var c: Int
    @JvmField var f4658d: OnMenuItemClickListener? = null
    @JvmField var f4659e: View? = null
    @JvmField var f4660f: EditText? = null
    @JvmField val g: TextView
    @JvmField var f4661h: TextView? = null
    @JvmField var f4662i: TextView? = null
    @JvmField val j: LinearLayout
    @JvmField val f4663k: FrameLayout
    @JvmField val l: InstalledApp?
    @JvmField var m: Boolean = false
    @JvmField var f4664n: Boolean = false

    fun interface OnMenuItemClickListener {
        fun a(i2: Int, i3: Int)
    }

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.app_browser_settings, null as ViewGroup?)
        l = installedApp
        g = view.findViewById(R.id.btnZoom)
        val newTab = view.findViewById<TextView>(R.id.btnNewTab)
        val about = view.findViewById<TextView>(R.id.btnAbout)
        val install = view.findViewById<TextView>(R.id.btnInstall)
        val print = view.findViewById<TextView>(R.id.btnPrint)
        val search = view.findViewById<TextView>(R.id.btnSearch)
        val translate = view.findViewById<TextView>(R.id.btnTranslate)
        val siteSettings = view.findViewById<TextView>(R.id.btnSiteSettings)
        val addons = view.findViewById<TextView>(R.id.btnAddons)
        j = view.findViewById(R.id.menuContainer)
        f4663k = view.findViewById(R.id.zoomContainer)

        setContentView(view)
        windowLayoutType = 1002
        elevation = 12.0f
        isFocusable = true
        setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.ff_bg_menu))

        b = round((installedApp?.e() ?: Global.f4838e) * 100.0f).toInt()
        c = installedApp?.g() ?: 100
        a(c)

        newTab.setOnClickListener(com.bellatrixos.app_browser.widgets.dialogs.b(this, 0))
        about.setOnClickListener(com.bellatrixos.app_browser.widgets.dialogs.b(this, 2))
        install.setOnClickListener(com.bellatrixos.app_browser.widgets.dialogs.b(this, 3))
        print.setOnClickListener(com.bellatrixos.app_browser.widgets.dialogs.b(this, 4))
        search.setOnClickListener(com.bellatrixos.app_browser.widgets.dialogs.b(this, 5))
        translate.setOnClickListener(com.bellatrixos.app_browser.widgets.dialogs.b(this, 6))
        siteSettings.setOnClickListener { dismiss(); f4658d?.a(12, 0) }
        addons.setOnClickListener { dismiss(); f4658d?.a(13, 0) }
        f4657a = context.resources.getDimensionPixelSize(R.dimen.ui_12dp)
        g.setOnClickListener(com.bellatrixos.app_browser.widgets.dialogs.b(this, 7))
    }

    fun a(i2: Int) {
        g.text = g.context.getString(R.string.browser_zoom_value, i2)
    }

    override fun dismiss() {
        if (f4664n) {
            f4663k.visibility = View.GONE
            j.visibility = View.VISIBLE
            f4664n = false
        }
        if (m && l != null) {
            l?.l(b / 100.0f)
            InstalledAppsDB.e(l!!.c(), l!!)
            m = false
            f4662i?.let { info ->
                info.text = info.context.getString(R.string.browser_scale_info)
            }
        }
        super.dismiss()
    }
}
