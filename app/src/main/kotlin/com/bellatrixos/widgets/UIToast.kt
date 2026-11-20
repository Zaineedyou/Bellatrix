package com.bellatrixos.widgets

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import com.bellatrixos.R
import com.bellatrixos.windows.Windows

/** Toast kecil di bagian atas layar. */
object UIToast {
    @JvmField var f4856a: String? = null
    @JvmField var b: PopupWindow? = null
    @JvmField val c: Handler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun a(context: Context, message: String?, icon: Int, duration: Int) {
        val popup = b ?: run {
            val view = LayoutInflater.from(context).inflate(R.layout.widget_toast_sm, null, false)
            f4856a = context.resources.getString(R.string.action_center)
            val created = PopupWindow(view, -2, -2)
            b = created
            created.isOutsideTouchable = false
            created.isFocusable = false
            created.isClippingEnabled = false
            created.animationStyle = R.style.AnimationDialogFadeUp
            created
        }
        val content = popup.contentView ?: return
        content.findViewById<TextView>(R.id.toast_title).text = message ?: f4856a
        content.findViewById<ImageView>(R.id.toast_icon).setImageResource(icon)
        if (popup.isShowing) popup.dismiss()
        val decor = (context as? Activity)?.window?.decorView ?: return
        if (decor.windowToken != null) popup.showAtLocation(decor, 81, 0, Windows.f4880a)
        c.removeCallbacksAndMessages(null)
        c.postDelayed(com.bellatrixos.h(3), duration.toLong())
    }
}
