package com.bellatrixos.app_browser.widgets.dialogs

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import com.bellatrixos.AppExecutors
import com.bellatrixos.R

/** Popup status unduhan browser. */
class UIDownloads(context: Context) : PopupWindow(context) {
    companion object {
        @JvmField val g: Int = 0
    }

    @JvmField var f4665a: Callback? = null
    @JvmField val b: View
    @JvmField val c: TextView
    @JvmField val f4666d: TextView
    @JvmField val f4667e: ProgressBar
    @JvmField val f4668f: LinearLayout

    fun interface Callback {
        fun b(i2: Int)
    }

    init {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.app_browser_dialog_download, null as ViewGroup?, false)
        b = view
        setContentView(view)
        width = -2
        height = -2
        isFocusable = true
        c = view.findViewById(R.id.text_name)
        f4666d = view.findViewById(R.id.text_title)
        f4667e = view.findViewById(R.id.progress_bar)
        f4668f = view.findViewById(R.id.btn_open_downloads)
        view.findViewById<ImageButton>(R.id.btn_close).setOnClickListener(a(1, this))
        animationStyle = R.style.AnimationDialogMaterial
    }

    fun a(view: View, str: String?, i2: Int) {
        val context = view.context
        c.text = str ?: "My Downloads"
        when (i2) {
            0 -> {
                f4666d.text = context.getString(R.string.downloader_file_downloading_wait)
                f4667e.visibility = View.VISIBLE
            }
            1 -> {
                f4666d.text = context.getString(R.string.downloader_file_download_error)
                f4667e.visibility = View.GONE
            }
            2 -> {
                f4666d.text = context.getString(R.string.downloader_file_download_success)
                f4667e.visibility = View.GONE
            }
            else -> f4667e.visibility = View.GONE
        }
        val finalState = i2 == 3
        isOutsideTouchable = finalState
        setBackgroundDrawable(if (finalState) ColorDrawable(0) else null)
        f4668f.setOnClickListener(c(this, context, 2))
        f4665a?.b(i2)
        if (isShowing) {
            update()
            return
        }
        b.measure(0, 0)
        showAsDropDown(view, -(b.measuredWidth - view.width), 0)
    }

    fun b(view: View, str: String?, i2: Int) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            a(view, str, i2)
        } else {
            AppExecutors.c().post(d(this, view, str ?: "", i2))
        }
    }
}
