package com.bellatrixos.app_browser.widgets.dialogs

import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.core.util.Supplier
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.app_browser.utils.Clipboard

/** Panel pencarian teks pada halaman WebView. */
class UIFinder(@JvmField val f4671e: Supplier<WebView?>) {
    @JvmField var f4669a: View? = null
    @JvmField var b: Animation? = null
    @JvmField var c: Animation? = null
    @JvmField var f4670d: FrameLayout? = null
    @JvmField var f4672f: Runnable? = null

    /** Tutup panel dan hentikan pencarian tertunda. */
    fun a() {
        f4672f?.let { AppExecutors.c().removeCallbacks(it) }
        f4672f = null
        val view = f4669a
        if (view != null && view.parent != null) f4670d?.removeView(view)
        f4669a = null
        b = null
        c = null
    }

    /** Tampilkan panel pencarian di dalam container yang diberikan. */
    fun b(context: Context) {
        val container = f4670d ?: return
        if (f4669a == null) {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.app_browser_dialog_finder, container as ViewGroup?, false)
            f4669a = view
            val close = view.findViewById<ImageButton>(R.id.btn_close)
            val next = view.findViewById<ImageButton>(R.id.btn_next)
            val previous = view.findViewById<ImageButton>(R.id.btn_prev)
            val edit = view.findViewById<EditText>(R.id.edit_app_name)

            close.setOnClickListener {
                f4671e.get()?.clearMatches()
                if (f4669a != null && container.visibility == View.VISIBLE) {
                    c?.let { animation -> container.startAnimation(animation) }
                }
            }
            next.setOnClickListener { f4671e.get()?.findNext(true) }
            previous.setOnClickListener { f4671e.get()?.findNext(false) }

            edit.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(editable: Editable) {
                    val query = editable.toString()
                    f4672f?.let { AppExecutors.c().removeCallbacks(it) }
                    val task = Runnable {
                        val webView = f4671e.get() ?: return@Runnable
                        if (query.isEmpty()) webView.clearMatches() else webView.findAllAsync(query)
                    }
                    f4672f = task
                    AppExecutors.c().postDelayed(task, 300L)
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            })

            b = AnimationUtils.loadAnimation(context, R.anim.ui_browser_dialog_open)
            val closeAnimation = AnimationUtils.loadAnimation(context, R.anim.ui_browser_dialog_close)
            c = closeAnimation
            closeAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationEnd(animation: Animation) {
                    container.visibility = View.GONE
                    val panel = f4669a
                    if (panel != null && panel.parent != null) container.removeView(panel)
                }

                override fun onAnimationRepeat(animation: Animation) = Unit
                override fun onAnimationStart(animation: Animation) = Unit
            })
        }

        val panel = f4669a ?: return
        if (panel.isShown) return
        container.visibility = View.VISIBLE
        container.removeAllViews()
        container.addView(panel)
        b?.let { container.startAnimation(it) }
        val edit = panel.findViewById<EditText>(R.id.edit_app_name)
        if (edit != null) {
            edit.post(f(edit, 0))
            if (Clipboard.f4649a) {
                val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = manager?.primaryClip
                if (manager?.hasPrimaryClip() == true && clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).coerceToText(context)
                    edit.setText(text)
                    edit.setSelection(text.length)
                }
                Clipboard.f4649a = false
            }
        }
    }
}
