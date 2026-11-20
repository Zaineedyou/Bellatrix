package com.bellatrixos.app_installer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.UIFragment
import com.bellatrixos.interfaces.BackPressHandler
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.interfaces.SaveCallback
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.widgets.UINotification
import com.bellatrixos.widgets.UITitleBar
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize
import com.bellatrixos.windows.icons.IconEngine
import com.bellatrixos.windows.icons.dialogs.DialogAppIcons
import com.bellatrixos.windows.installed_apps.InstalledAppsDB

/** Jendela pemasang aplikasi: field dan method mengikuti urutan internal. */
class UIInstaller : UIFragment(), BackPressHandler {
    @JvmField var D0: EditText? = null
    @JvmField var E0: EditText? = null
    @JvmField var F0: DialogAppIcons? = null
    @JvmField var u0: FloatingLayout? = null
    @JvmField var v0: UITitleBar? = null
    @JvmField var w0: MainActivityIn? = null
    @JvmField var x0: ImageView? = null
    @JvmField var y0: CheckBox? = null
    @JvmField var z0: TextView? = null
    @JvmField var A0: Boolean = true
    @JvmField var B0: Boolean = true
    @JvmField var C0: Boolean = false
    @JvmField val G0: Window = Window(3)

    fun A() {
        // Fragment.a0 (mAdded) milik androidx.fragment tidak dapat diset dari port Kotlin.
        w0 = null
    }

    fun L(view: View, bundle: Bundle?) {
        val context = view.context
        val activity = context as MainActivityIn
        w0 = activity
        val orientation = context.resources.configuration.orientation

        val floating = u0 ?: return
        val titleBar = v0 ?: return
        WindowSize.e(view, floating, G0, titleBar, activity, this)

        x0 = view.findViewById(R.id.ui_icon_installer)
        D0 = view.findViewById(R.id.txt_installer_appName)
        E0 = view.findViewById(R.id.edittext_installer_url)
        z0 = view.findViewById(R.id.btn_positive)
        y0 = view.findViewById(R.id.txt_installer_app_shortcut)
        val detectBox: CheckBox = view.findViewById(R.id.checkbox_img_detect)

        val iconView = x0 ?: return
        val nameInput = D0 ?: return
        val urlInput = E0 ?: return
        val installButton = z0 ?: return

        iconView.setOnClickListener {
            if (A0) return@setOnClickListener
            val dialog = F0 ?: DialogAppIcons().also { newDialog ->
                val handler = IconChoiceHandler(context)
                newDialog.N0 = handler
                newDialog.O0 = handler
                F0 = newDialog
            }
            dialog.show(parentFragmentManager, "DialogIconList")
        }

        detectBox.setOnCheckedChangeListener { _, checked ->
            A0 = checked
            if (checked) {
                detectBox.setText(context.getString(R.string.app_installer_ic_auto))
                iconView.setImageResource(R.drawable.img_app_photos_auto)
            } else {
                detectBox.setText(context.getString(R.string.app_installer_ic_manual))
                iconView.setImageResource(R.drawable.img_app_photos)
            }
        }

        installButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val url = urlInput.text.toString().trim()
            var nameOk = true
            if (name.isEmpty()) {
                nameInput.setError(getString(R.string.app_installer_error_app_name_required))
                nameInput.requestFocus()
                nameOk = false
            }
            if (url.isEmpty()) {
                urlInput.setError(getString(R.string.app_installer_error_url_required))
                if (nameOk) urlInput.requestFocus()
                return@setOnClickListener
            }
            if (InstalledAppsDB.b.containsKey(url)) {
                UINotification.a(
                    context,
                    context.getString(R.string.app_already_installed),
                    context.getString(R.string.app_installer)
                )
                return@setOnClickListener
            }

            installButton.setText(getString(R.string.app_installer_btn_installing))
            val rawName = nameInput.text.toString()
            val shortcut = y0?.isChecked ?: false
            // Nilai null untuk str3 diganti String non-null agar aman.
            if (C0) {
                IconManager.a(context, iconView.drawable, url, SaveCallback { path, success ->
                    activity.d0(url, rawName, if (success) path else "", shortcut)
                })
            } else {
                activity.d0(url, rawName, "", shortcut)
            }
            AppExecutors.c().postDelayed(Runnable { activity.P(this) }, 400L)
        }

        val focusResize = object : Window.OnResizeStateChangeListener,
            FloatingLayout.OnAppFocusListener {
            override fun a() {
                view.bringToFront()
                activity.d(this@UIInstaller)
                floating.setFocusState(true)
                B0 = true
            }

            override fun b(i2: Int) {
                WindowSize.d(view, floating, G0)
                WindowSize.f(floating, titleBar, i2)
            }
        }
        G0.f4870e = focusResize

        fun onTitleBarClick(index: Int) {
            when (index) {
                0 -> WindowSize.c(G0)
                1 -> activity.P(this)
                else -> activity.A(tag ?: "")
            }
        }
        titleBar.setResizeBtnListener { onTitleBarClick(0) }
        titleBar.setCloseBtnListener { onTitleBarClick(1) }
        titleBar.setMinimizeBtnListener { onTitleBarClick(2) }

        floating.setOnFocusListener(focusResize)

        val style: Int
        if (orientation == 2) {
            WindowSize.d(view, floating, G0)
            style = 3
        } else {
            style = 0
        }
        WindowSize.f(floating, titleBar, style)
        G0.f4869d = style
    }

    override fun b(): Boolean {
        w0?.A(tag ?: "")
        return true
    }

    override fun f0(): Boolean = B0

    override fun j0(z: Boolean) {
        val floating = u0 ?: return
        floating.setFocusState(z)
        B0 = z
    }

    fun z(layoutInflater: LayoutInflater, viewGroup: ViewGroup?, bundle: Bundle?): View {
        val view = layoutInflater.inflate(R.layout.app_ui_installer, viewGroup, false)
        u0 = view.findViewById(R.id.main_view)
        v0 = view.findViewById(R.id.titleBar)
        u0?.clipToOutline = true
        return view
    }

    // --- Android lifecycle bridges ---

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = z(inflater, container, savedInstanceState)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        L(view, savedInstanceState)
    }

    override fun onDetach() {
        A()
        super.onDetach()
    }

    /** h.b + callback `e` (u=17/18), disatukan dalam satu objek lokal. */
    private inner class IconChoiceHandler(private val ctx: Context) :
        DialogAppIcons.SelectionListener,
        DialogAppIcons.SelectionPkgListener,
        IconEngine.Callback {

        override fun b(drawable: Drawable?) {
            C0 = true
            x0?.setImageDrawable(drawable)
        }

        override fun a(str: String) {
            C0 = true
            IconEngine.b(ctx).c(str, this)
        }

        override fun i(bitmap: Bitmap?) {
            x0?.setImageBitmap(bitmap)
        }
    }
}
