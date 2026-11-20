package com.bellatrixos.app_browser.utils

import android.database.Cursor
import android.util.Patterns
import android.view.View
import android.widget.AdapterView
import com.bellatrixos.app_settings.searchbar.SuggestionItem
import com.bellatrixos.widgets.UITitleBarSearch
import java.net.URLEncoder

/** OnItemClickListener untuk saran browser dan bilah pencarian judul. */
class h(@JvmField val u: Int, @JvmField val v: Any?) : AdapterView.OnItemClickListener {

    override fun onItemClick(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        when (u) {
            0 -> {
                val cursor = parent.getItemAtPosition(position) as? Cursor ?: return
                var text = cursor.getString(cursor.getColumnIndex("suggest_text_1"))
                if (!Patterns.WEB_URL.matcher(text).matches()) {
                    text = try {
                        "https://www.google.com/search?q=" + URLEncoder.encode(text, "UTF-8")
                    } catch (_: Exception) {
                        "https://www.google.com/search?q=$text"
                    }
                } else if (!text.startsWith("http://") && !text.startsWith("https://")) {
                    text = "https://$text"
                }
                (v as? Suggestion.StringCallback)?.a(text)
            }
            else -> {
                val search = v as? UITitleBarSearch ?: return
                val item = parent.getItemAtPosition(position) as? SuggestionItem ?: return
                search.w.setText("")
                search.w.clearFocus()
                search.w.isActivated = false
                search.A?.b(item.c)
            }
        }
    }
}
