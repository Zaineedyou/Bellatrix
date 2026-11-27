package com.bellatrixos.app_webapp

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

/** Menu pengaturan zoom dan aksi halaman web. */
class UISettingsMenu(context: Context, installedApp: InstalledApp?) : PopupWindow(context) {
    fun interface OnMenuItemClickListener {
        fun a(i2: Int, i3: Int)
    }

    companion object { const val o: Int = 0 }

    @JvmField var f4730a: Int
    @JvmField var b: Int
    @JvmField val c: TextView
    @JvmField var f4731d: OnMenuItemClickListener? = null
    @JvmField val f4732e: Int
    @JvmField var f4733f: View? = null
    @JvmField var g: EditText? = null
    @JvmField var f4734h: TextView? = null
    @JvmField var f4735i: TextView? = null
    @JvmField val j: LinearLayout
    @JvmField val f4736k: FrameLayout
    @JvmField val l: InstalledApp? = installedApp
    @JvmField var m: Boolean = false
    @JvmField var f4737n: Boolean = false

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.app_web_settings, null as ViewGroup?)
        c = view.findViewById(R.id.btnZoom)
        val print = view.findViewById<TextView>(R.id.btnPrint)
        val copyUrl = view.findViewById<TextView>(R.id.btnCopyURL)
        val downloads = view.findViewById<TextView>(R.id.btnDownloads)
        val search = view.findViewById<TextView>(R.id.btnSearch)
        val siteSettings = view.findViewById<TextView>(R.id.btnSiteSettings)
        j = view.findViewById(R.id.menuContainer)
        f4736k = view.findViewById(R.id.zoomContainer)
        contentView = view
        windowLayoutType = 1002
        elevation = 4.0f
        isFocusable = true
        setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.ui_webapp_settings_bg))
        f4730a = round((installedApp?.e() ?: Global.f4838e) * 100.0f).toInt()
        b = installedApp?.g() ?: 100
        a(b)
        copyUrl.setOnClickListener(a(this, 0))
        print.setOnClickListener(a(this, 2))
        downloads.setOnClickListener(a(this, 3))
        search.setOnClickListener(a(this, 4))
        siteSettings.setOnClickListener { dismiss(); f4731d?.a(12, 0) }
        f4732e = context.resources.getDimensionPixelOffset(R.dimen.ui_4dp)
        c.setOnClickListener(a(this, 5))
    }

    fun a(i2: Int) {
        c.text = c.context.getString(R.string.browser_zoom_value, i2)
    }

    override fun dismiss() {
        if (f4737n) {
            f4736k.visibility = View.GONE
            j.visibility = View.VISIBLE
            f4737n = false
        }
        if (m) {
            l?.let { app ->
                app.l(f4730a / 100.0f)
                InstalledAppsDB.e(app.c(), app)
            }
        }
        super.dismiss()
    }
}
