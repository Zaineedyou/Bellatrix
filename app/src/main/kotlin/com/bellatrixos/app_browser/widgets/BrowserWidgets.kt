package com.bellatrixos.app_browser.widgets

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import com.bellatrixos.app_browser.widgets.dialogs.a

/** Address bar browser dengan tampilan URL terformat dan perilaku fokus. */
class AddressBarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    AppCompatAutoCompleteTextView(context, attrs) {

    companion object {
        @JvmField
        val A: Int = 0

        @JvmStatic
        fun a(str: String?): String {
            if (str.isNullOrEmpty()) return ""
            val uri = Uri.parse(str)
            var host = uri.host
            val path = uri.path
            val query = uri.query
            if ("www.google.com".equals(host, ignoreCase = true) &&
                (path.isNullOrEmpty() || path == "/" || "/webhp".equals(path, ignoreCase = true))
            ) {
                return ""
            }
            if (host != null && host.startsWith("www.")) {
                host = host.substring(4)
            }
            val sb = StringBuilder()
            if (host != null) sb.append(host)
            if (path != null && path != "/") sb.append(path)
            if (!query.isNullOrEmpty()) {
                sb.append("?")
                sb.append(query)
            }
            return sb.toString()
        }
    }

    @JvmField
    var y: String = ""

    @JvmField
    var z: Boolean = false

    init {
        isSingleLine = true
        setOnClickListener(a(6, this))
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused) {
            post {
                setSelection(0, text?.length ?: 0)
                z = true
            }
            return
        }
        setText(a(y))
        setSelection(0)
        z = false
    }

    fun setAddress(str: String?) {
        y = str ?: ""
        setText(a(y))
        z = false
    }
}
