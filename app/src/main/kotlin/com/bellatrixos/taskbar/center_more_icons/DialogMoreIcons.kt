package com.bellatrixos.taskbar.center_more_icons

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.PopupWindow
import com.bellatrixos.R
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.variables.Global
import com.bellatrixos.windows.InsetManager
import com.bellatrixos.windows.Windows
import com.bellatrixos.windows.dialogs.Dialogs
import com.bellatrixos.windows.dialogs.UIDialogMouse

/** Popup berisi tombol sistem tambahan pada taskbar. */
@SuppressLint("InflateParams")
class DialogMoreIcons(context: Context, private val mainActivityIn: MainActivityIn) : PopupWindow(context) {
    @JvmField val f4817a: View
    @JvmField val b: GridLayout
    @JvmField var c: Boolean = false
    @JvmField var f4818d: Int

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.taskbar_dialog_more_icons, null as ViewGroup?)
        f4817a = root
        contentView = root
        f4816f = context.resources.getDimensionPixelSize(R.dimen.taskbar_more_icons_width)
        width = f4816f
        height = -2
        isClippingEnabled = false
        setBackgroundDrawable(ColorDrawable(0))
        b = root.findViewById(R.id.mainView)
        val update = root.findViewById<View>(R.id.btnDeviceUpdate)
        val mouse = root.findViewById<View>(R.id.btnDeviceMouse)
        val installer = root.findViewById<View>(R.id.btnDeviceInstaller)
        root.setPadding(0, 0, 0, Windows.f4880a + InsetManager.b)
        f4818d = InsetManager.c
        if (SharedPrefs.b()) {
            b.backgroundTintList = ColorStateList.valueOf(Global.c)
        }
        update.setOnClickListener {
            dismiss()
            mainActivityIn.c0("UISettings", "8")
        }
        mouse.setOnClickListener {
            dismiss()
            UIDialogMouse(context, mainActivityIn).show()
        }
        installer.setOnClickListener {
            dismiss()
            mainActivityIn.c0("UIInstaller", null)
        }
        root.setOnClickListener { dismiss() }
        isFocusable = true
        isOutsideTouchable = true
        setAnimationStyle(0)
    }

    internal fun dismissNow() {
        super.dismiss()
    }

    override fun dismiss() {
        if (c) return
        c = true
        Dialogs.a(b, 0, Runnable { a(this@DialogMoreIcons) })
    }

    companion object {
        @JvmField var f4815e: Int = 0
        @JvmField var f4816f: Int = 0

        @JvmStatic
        fun a(dialogMoreIcons: DialogMoreIcons) {
            dialogMoreIcons.c = false
            dialogMoreIcons.dismissNow()
        }
    }
}
