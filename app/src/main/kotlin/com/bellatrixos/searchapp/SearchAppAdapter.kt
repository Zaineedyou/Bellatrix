package com.bellatrixos.searchapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R
import com.bellatrixos.app_settings.searchbar.SuggestionItem
import com.bellatrixos.app_settings.searchbar.SuggestionItems
import com.bellatrixos.context_menu.OnItemClickListener
import com.bellatrixos.startmenu.StartMenuItem
import com.bellatrixos.startmenu.g
import com.bellatrixos.windows.icons.IconEngine

/** Adapter hasil pencarian aplikasi dan saran pengaturan. */
class SearchAppAdapter(@JvmField val z: Context, list: List<*>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {

    open class AppBaseHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField val u: TextView = view.findViewById(R.id.title)
        @JvmField val v: ImageView = view.findViewById(R.id.icon)
    }

    class AppViewHolder(view: View) : AppBaseHolder(view)
    class SettingViewHolder(view: View) : AppBaseHolder(view)

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField var u: TextView? = null
    }

    class TopViewHolder(view: View) : AppBaseHolder(view) {
        @JvmField var w: TextView? = null
    }

    @JvmField val A: LayoutInflater = LayoutInflater.from(z)
    @JvmField val B: ArrayList<StartMenuItem> = ArrayList(list.filterIsInstance<StartMenuItem>())
    @JvmField val C: ArrayList<SearchItem> = ArrayList()
    @JvmField var D: OnFilterCompleteListener? = null
    @JvmField var E: OnItemClickListener<Any>? = null
    @JvmField var F: OnItemClickListener<Any>? = null
    @JvmField var x: Int = 0
    @JvmField var y: String = ""

    @JvmField val G: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = (constraint?.toString() ?: "").lowercase().trim()
            y = query
            val results = ArrayList<SearchItem>()

            if (query.length < 2) {
                repeat(minOf(5, B.size)) { i ->
                    val item = SearchItem(2)
                    item.b = B[i]
                    results.add(item)
                }
                return Filter.FilterResults().apply { values = results }
            }

            val apps = ArrayList<StartMenuItem>()
            if (x != 2) {
                for (app in B) {
                    if (app.b().orEmpty().lowercase().contains(query)) apps.add(app)
                }
            }
            val settings = ArrayList<SuggestionItem>()
            if (x != 1) {
                for (suggestion in SuggestionItems.a(z)) {
                    if (suggestion.f4723a.lowercase().contains(query)) settings.add(suggestion)
                }
            }

            val firstApp = apps.firstOrNull()
            val firstSetting = settings.firstOrNull()
            var best: SearchItem? = null
            if (firstApp != null && firstSetting != null) {
                val appStarts = firstApp.b().orEmpty().lowercase().startsWith(query)
                val settingStarts = firstSetting.f4723a.lowercase().startsWith(query)
                if (!appStarts && settingStarts) {
                    val item = SearchItem(0)
                    item.c = firstSetting
                    best = item
                } else {
                    val item = SearchItem(0)
                    item.b = firstApp
                    best = item
                }
            } else if (firstApp != null) {
                val item = SearchItem(0)
                item.b = firstApp
                best = item
            } else if (firstSetting != null) {
                val item = SearchItem(0)
                item.c = firstSetting
                best = item
            }
            best?.let { results.add(it) }

            if (apps.isNotEmpty()) {
                val offset = if (best?.b == null) 0 else 1
                if (apps.size > offset) {
                    val header = SearchItem(1)
                    header.f4774d = z.getString(R.string.app_default_title)
                    results.add(header)
                    for (i in offset until minOf(offset + 5, apps.size)) {
                        val item = SearchItem(2)
                        item.b = apps[i]
                        results.add(item)
                    }
                }
            }
            if (settings.isNotEmpty()) {
                val offset = if (best?.c == null) 0 else 1
                if (settings.size > offset) {
                    val header = SearchItem(1)
                    header.f4774d = z.getString(R.string.settings_str_name)
                    results.add(header)
                    for (i in offset until minOf(offset + 5, settings.size)) {
                        val item = SearchItem(3)
                        item.c = settings[i]
                        results.add(item)
                    }
                }
            }
            if (results.isEmpty()) {
                val item = SearchItem(0)
                // SuggestionItem.c tidak nullable di port Kotlin; string kosong menandakan "tidak ada hasil".
                item.c = SuggestionItem(R.drawable.ic_search_24_sparkle, query, "")
                results.add(item)
            }
            return Filter.FilterResults().apply { values = results }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            C.clear()
            @Suppress("UNCHECKED_CAST")
            (results?.values as? List<SearchItem>)?.let { C.addAll(it) }
            C.firstOrNull { it.f4773a == 0 }?.let { D?.a(it) }
            notifyDataSetChanged()
        }
    }

    /** Ikat aplikasi ke holder, termasuk ikon dan aksi klik. */
    fun A(holder: AppBaseHolder, item: StartMenuItem) {
        holder.u.text = item.b()
        IconEngine.b(z).c(item.c().orEmpty(), g(holder.v))
        holder.itemView.setOnClickListener { F?.a(item) }
    }

    override fun getItemCount(): Int = C.size

    override fun getItemViewType(position: Int): Int = C[position].f4773a

    override fun getFilter(): Filter = G

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val searchItem = C[position]
        when (holder) {
            is TopViewHolder -> {
                val app = searchItem.b
                if (app != null) {
                    A(holder, app)
                    holder.w?.text = z.getString(R.string.app_default_title)
                    return
                }
                val suggestion = searchItem.c ?: return
                holder.u.text = suggestion.f4723a
                holder.v.setImageResource(suggestion.b)
                if (suggestion.c.isEmpty()) {
                    holder.w?.setText(R.string.st_menu_search_results_none)
                    holder.itemView.setOnClickListener(null)
                } else {
                    holder.w?.setText(R.string.settings_str_name)
                    holder.itemView.setOnClickListener { E?.a(suggestion.c) }
                }
            }
            is AppViewHolder -> A(holder, searchItem.b ?: return)
            is SettingViewHolder -> {
                val suggestion = searchItem.c ?: return
                holder.u.text = suggestion.f4723a
                holder.v.setImageResource(suggestion.b)
                holder.itemView.setOnClickListener { E?.a(suggestion.c) }
            }
            is HeaderViewHolder -> holder.u?.text = searchItem.f4774d
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
        0 -> {
            val view = A.inflate(R.layout.start_menu_search_item_top, parent, false)
            val holder = TopViewHolder(view)
            holder.w = view.findViewById(R.id.subtitle)
            view.isActivated = true
            holder
        }
        1 -> {
            val view = A.inflate(R.layout.start_menu_search_title, parent, false)
            val holder = HeaderViewHolder(view)
            holder.u = view.findViewById(R.id.title)
            holder
        }
        2 -> AppViewHolder(A.inflate(R.layout.start_menu_search_item, parent, false))
        else -> SettingViewHolder(A.inflate(R.layout.start_menu_search_item, parent, false))
    }
}
