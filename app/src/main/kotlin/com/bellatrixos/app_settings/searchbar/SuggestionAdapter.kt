package com.bellatrixos.app_settings.searchbar

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView
import com.bellatrixos.R

/** Adapter saran pengaturan dengan filter berdasarkan judul. */
class SuggestionAdapter(context: Context, items: List<SuggestionItem>) :
    ArrayAdapter<SuggestionItem>(context, 0, ArrayList(items)) {

    fun interface OnResultsStateListener {
        fun j(hasResults: Boolean)
    }

    @JvmField var u: OnResultsStateListener? = null
    @JvmField val v: ArrayList<SuggestionItem> = ArrayList(items)
    @JvmField val w: ArrayList<SuggestionItem> = ArrayList()
    @JvmField val x: Filter = object : Filter() {
        override fun convertResultToString(resultValue: Any?): CharSequence =
            (resultValue as SuggestionItem).f4723a

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            w.clear()
            if (constraint.isNullOrEmpty()) {
                w.addAll(v)
            } else {
                val query = constraint.toString().lowercase().trim()
                for (item in v) {
                    if (item.f4723a.lowercase().contains(query)) w.add(item)
                }
            }
            return FilterResults().apply {
                values = w
                count = w.size
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            clear()
            val hasResults = (constraint?.length ?: 0) >= 2 && results?.values != null && results.count > 0
            if (hasResults) addAll(results!!.values as List<SuggestionItem>)
            notifyDataSetChanged()
            u?.j(hasResults)
        }
    }

    override fun getFilter(): Filter = x

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.settings_autocomplete_item, parent, false)
        val item = getItem(position)
        val text = view.findViewById<TextView>(R.id.text)
        val icon = view.findViewById<ImageView>(R.id.icon)
        if (item != null) {
            text.text = item.f4723a
            icon.setImageResource(item.b)
        }
        return view
    }
}
