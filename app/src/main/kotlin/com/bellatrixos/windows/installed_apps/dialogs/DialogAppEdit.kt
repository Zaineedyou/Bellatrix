package com.bellatrixos.windows.installed_apps.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager

import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.app_installer.IconManager
import com.bellatrixos.windows.icons.IconEngine
import com.bellatrixos.windows.icons.dialogs.DialogAppIcons
import com.bellatrixos.windows.installed_apps.AppsDatabase
import com.bellatrixos.windows.installed_apps.InstalledAppsDB
import com.bellatrixos.windows.installed_apps.models.AppDataDBItem
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import com.google.gson.Gson
import java.util.Objects

/** Editor nama, URL, dan ikon untuk aplikasi terpasang. */
class DialogAppEdit(
    context: Context,
    @JvmField val C: FragmentManager,
    @JvmField val E: String
) : Dialog(context, R.style.UIDialogDefault) {

    fun interface OnItemUpdateListener {
        fun c(z: Boolean)
    }

    @JvmField var A: EditText? = null
    @JvmField var B: DialogAppIcons? = null
    @JvmField var D: OnItemUpdateListener? = null
    @JvmField var F: Boolean = false
    @JvmField var u: ImageView? = null
    @JvmField var v: Context = context
    @JvmField var w: InstalledApp? = null
    @JvmField var x: TextView? = null
    @JvmField var y: AppDataDBItem? = null
    @JvmField var z: EditText? = null

    companion object {
        @JvmField var G: Int = 0
    }

    fun a(str: String, str2: String, str3: String?) {
        val str4 = E
        if (G == 1) {
            val installed = w
            if (installed != null) {
                if (str3 != null) installed.i(str3)
                if (!Objects.equals(str4, str)) installed.m(str)
                installed.j(str2)
                InstalledAppsDB.e(str4, installed)
            }
        } else if (G == 2) {
            val item = y ?: AppDataDBItem(str2, str3).also { y = it }
            if (str3 != null) item.iconPath = str3
            item.customName = str2
            val editor = v.getSharedPreferences("AppDataPrefs", 0).edit()
            @Suppress("UNCHECKED_CAST")
            val map = AppsDatabase.c as MutableMap<String, Any?>
            map[str4] = item
            editor.putString("app_data", Gson().toJson(AppsDatabase.c))
            editor.apply()
            y = item
        }
        @Suppress("UNCHECKED_CAST")
        val cached = AppsDatabase.f4954d as MutableMap<String, AppDataDBItem?>
        cached[str4] = AppDataDBItem(str2, str3)
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.app_web_editor)
        val dimensionPixelSize = v.resources.getDimensionPixelSize(R.dimen.ui_380dp)
        window?.let { win ->
            val attributes: WindowManager.LayoutParams = win.attributes
            attributes.width = dimensionPixelSize
            attributes.height = -1
            win.attributes = attributes
        }
        val close = findViewById<ImageView>(R.id.btn_close)
        u = findViewById(R.id.ui_icon_installer)
        z = findViewById(R.id.txt_installer_appName)
        A = findViewById(R.id.edittext_installer_url)
        x = findViewById(R.id.btn_positive)
        val str = E
        w = InstalledAppsDB.c(str)
        var strA: String
        var strF: String
        if (AppsDatabase.b.containsKey(str)) {
            G = 0
            strA = AppsDatabase.b[str] ?: AppsDatabase.a(str)
            z?.isEnabled = false
            A?.isEnabled = false
            u?.isEnabled = false
            strF = str
        } else {
            val installedApp = w
            if (installedApp != null) {
                G = 1
                strA = installedApp.b() ?: AppsDatabase.a(str)
                strF = if (!Objects.equals(installedApp.c(), installedApp.f())) installedApp.f() else str
            } else {
                G = 2
                strA = AppsDatabase.a(str)
                A?.isEnabled = false
                var appDataDBItem = AppsDatabase.f4954d[str]
                y = appDataDBItem
                if (appDataDBItem == null) {
                    appDataDBItem = AppDataDBItem(strA, null)
                    y = appDataDBItem
                }
                strF = str
            }
        }
        A?.setText(strF)
        z?.setText(strA)
        IconEngine.b(v).c(str, a(0, this))
        u?.setOnClickListener {
            if (B == null) {
                val dialogAppIcons = DialogAppIcons()
                B = dialogAppIcons
                dialogAppIcons.N0 = DialogAppIcons.SelectionListener { drawable ->
                    this.u?.setImageDrawable(drawable)
                    this.F = true
                    this.x?.isEnabled = true
                    this.x?.alpha = 1.0f
                }
                dialogAppIcons.O0 = DialogAppIcons.SelectionPkgListener { name ->
                    IconEngine.b(this.v).c(name, a(3, this))
                    this.F = true
                    this.x?.isEnabled = true
                    this.x?.alpha = 1.0f
                }
            }
            B?.show(C, "DialogIconList")
        }
        val watcher = object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                val enabled = editable != null && editable.isNotEmpty()
                this@DialogAppEdit.x?.isEnabled = enabled
                this@DialogAppEdit.x?.alpha = if (enabled) 1.0f else 0.6f
            }

            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i2: Int, i3: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, i: Int, i2: Int, i3: Int) {}
        }
        A?.addTextChangedListener(watcher)
        z?.addTextChangedListener(watcher)
        x?.setOnClickListener {
            val listener = D
            if (listener != null) {
                x?.isEnabled = false
                x?.text = v.getString(R.string.dialog_app_editor_btn_updating)
                val url = A?.text?.toString().orEmpty()
                if (F) {
                    IconManager.a(v, u?.drawable, url) { path, _ ->
                        a(url, z?.text?.toString().orEmpty(), path)
                        listener.c(false)
                    }
                } else {
                    a(url, z?.text?.toString().orEmpty(), null)
                    listener.c(false)
                }
            }
            AppExecutors.c().postDelayed({ dismiss() }, 400L)
        }
        close.setOnClickListener { dismiss() }
    }
}
