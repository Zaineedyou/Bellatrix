package com.bellatrixos.app_settings.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R
import com.bellatrixos.windows.icons.IconEngine
import com.bellatrixos.windows.installed_apps.models.InstalledApp

/** Daftar aplikasi terpasang dengan kolom pencarian di header. */
class InstalledAppsAdapter(
    @JvmField val x: Context,
    @JvmField val y: MutableList<InstalledApp>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField var u: TextView = view.findViewById(R.id.page_title)
        @JvmField var v: EditText = view.findViewById(R.id.search_apps)
    }

    class InstalledAppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField var u: ImageView = view.findViewById(R.id.appIcon)
        @JvmField var v: ImageButton = view.findViewById(R.id.appBtnMore)
        @JvmField var w: TextView = view.findViewById(R.id.appName)
        @JvmField var x: TextView = view.findViewById(R.id.appPackage)
    }

    fun interface OnMoreClickListener {
        fun a(view: View, i2: Int)
    }

    @JvmField var A: OnMoreClickListener? = null
    @JvmField var B: TextView? = null
    @JvmField var C: EditText? = null
    @JvmField val z: ArrayList<InstalledApp> = ArrayList(y)

    override fun getItemCount(): Int = y.size + 1

    override fun getItemViewType(position: Int): Int = if (position == 0) 0 else 1

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val result = ArrayList<InstalledApp>()
            if (constraint.isNullOrEmpty()) {
                result.addAll(z)
            } else {
                val query = constraint.toString().lowercase().trim()
                for (app in z) {
                    if (app.b()?.lowercase()?.contains(query) == true) result.add(app)
                }
            }
            return FilterResults().apply { values = result }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            val size = y.size
            y.clear()
            y.addAll((results?.values as? List<InstalledApp>) ?: emptyList())
            val newSize = y.size
            if (size > 0) notifyItemRangeRemoved(1, size)
            if (newSize > 0) notifyItemRangeInserted(1, newSize)
            B?.text = x.getString(R.string.settings_apps_installed, newSize.toString())
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder !is InstalledAppViewHolder) {
            val header = holder as HeaderViewHolder
            B = header.u
            C = header.v
            header.u.text = x.getString(R.string.settings_apps_installed, y.size.toString())
            if (header.v.tag == null) {
                header.v.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        filter.filter(s)
                    }
                    override fun afterTextChanged(s: android.text.Editable?) {}
                })
                header.v.tag = java.lang.Boolean.TRUE
            }
            return
        }
        val app = y[holder.bindingAdapterPosition - 1]
        holder.w.text = app.b()
        holder.x.text = app.c()
        app.c()?.let { pkg -> IconEngine.b(x).c(pkg, IconEngine.Callback { bitmap -> holder.u.setImageBitmap(bitmap) }) }
        holder.v.setOnClickListener(a(this, 1, holder))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            val view = inflater.inflate(R.layout.settings_apps_installed_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.settings_apps_installed_item, parent, false)
            InstalledAppViewHolder(view)
        }
    }
}
