package com.bellatrixos.app_explorer.files

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R
import com.bellatrixos.app_explorer.files.headers.DateGroup
import com.bellatrixos.app_explorer.files.headers.DateGrouping
import com.bellatrixos.app_explorer.files.headers.DatesFiles
import com.bellatrixos.app_explorer.files.interfaces.OnContextMenuListener
import com.bellatrixos.context_menu.OnItemClickListener
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.FilesRecyclerView
import com.bellatrixos.widgets.GridAutofillLayoutManager
import com.bellatrixos.windows.files.FileExtensions
import com.bellatrixos.windows.files.FileMeta
import java.io.File
import java.time.Instant
import java.util.Date

private val fileAdapterDiff = object : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        if (oldItem !is FileMeta || newItem !is FileMeta) return true
        return oldItem.modified == newItem.modified && oldItem.size == newItem.size
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        if (oldItem is FileMeta && newItem is FileMeta) return oldItem.pathHash == newItem.pathHash
        if (oldItem is DateGroup) return newItem is DateGroup && oldItem.f4683a == newItem.f4683a
        return false
    }
}

/** Adapter daftar berkas dengan pengelompokan tanggal dan mode pilih. */
@SuppressLint("NotifyDataSetChanged")
class FileAdapter(@JvmField val C: Context) : ListAdapter<Any, RecyclerView.ViewHolder>(fileAdapterDiff) {
    companion object {
        @JvmField val L: Any = Any()

        @JvmField val M: DiffUtil.ItemCallback<Any> = fileAdapterDiff

        /** Urutan androidx.core.provider.a mode 2-5 untuk FileMeta. */
        private fun sortFileMeta(list: MutableList<FileMeta>, sortBy: SortBy) {
            when (sortBy) {
                SortBy.NAME_ASC -> list.sortWith(
                    compareByDescending<FileMeta> { it.isDirectory }.thenBy { it.lowerName }
                )
                SortBy.NAME_DESC -> list.sortWith(
                    compareByDescending<FileMeta> { !it.isDirectory }.thenByDescending { it.lowerName }
                )
                SortBy.DATE_MODIFIED_ASC -> list.sortBy { it.modified }
                SortBy.DATE_MODIFIED_DESC -> list.sortByDescending { it.modified }
            }
        }

        private fun formatSize(size: Long): String {
            if (size < 1024) return "$size B"
            val kb = size / 1024.0
            if (kb < 1024) return String.format("%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024) return String.format("%.1f MB", mb)
            return String.format("%.1f GB", mb / 1024.0)
        }
    }

    class DateHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField var u: TextView = view.findViewById(R.id.header_text)
    }

    class DetailsViewHolder(view: View, adapter: FileAdapter) : BaseFileViewHolder(
        view,
        view.findViewById<ImageView>(R.id.image_view),
        view.findViewById<TextView>(R.id.text_view),
        adapter
    ) {
        @JvmField val A: TextView = view.findViewById(R.id.details_view)
        @JvmField val B: TextView = view.findViewById(R.id.size_view)

        override fun t(fileMeta: FileMeta) {
            u.setImageResource(FileExtensions.e(fileMeta.file.name))
            B.text = if (fileMeta.isDirectory) "" else formatSize(fileMeta.size)
            A.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Global.t?.format(Instant.ofEpochMilli(fileMeta.modified)) ?: ""
            } else {
                DateFormat.format("dd/MM/yyyy h:mm a", Date(fileMeta.modified)).toString()
            }
        }
    }

    class MediumIconsViewHolder(
        view: View,
        imageView: ImageView,
        textView: TextView,
        adapter: FileAdapter
    ) : BaseFileViewHolder(view, imageView, textView, adapter) {
        override fun t(fileMeta: FileMeta) {
            u.setImageResource(FileExtensions.e(fileMeta.file.name))
        }
    }

    fun interface OnItemAddRemoveListener {
        fun b(i2: Int)
    }

    @JvmField var A: Int = 0
    @JvmField var B: Int = 0
    @JvmField var D: OnItemAddRemoveListener? = null
    @JvmField var E: OnItemClickListener<Any>? = null
    @JvmField var F: OnContextMenuListener<Any>? = null
    @JvmField var G: Boolean = false
    @JvmField val H: ArrayList<FileMeta> = ArrayList()
    @JvmField val I: HashSet<File> = HashSet()
    @JvmField val J: HashSet<File> = HashSet()
    @JvmField val K: HashMap<File, Int> = HashMap()
    @JvmField var z: SortBy = SortBy.NAME_ASC

    init {
        setHasStableIds(true)
        val i2 = SharedPrefs.f4755a?.getInt("file_sort_by", 0) ?: 0
        for (sortBy in SortBy.values()) {
            if (sortBy.u == i2) z = sortBy
        }
        z = SortBy.NAME_ASC
    }

    fun B(list: List<Any>) {
        K.clear()
        for (i2 in list.indices) {
            val obj = list[i2]
            if (obj is FileMeta) K[obj.file] = i2
        }
    }

    fun D(list: List<FileMeta>): ArrayList<Any> {
        val result = ArrayList<Any>()
        if (A != Files.f4678d || z != SortBy.DATE_MODIFIED_DESC) {
            result.addAll(list)
            return result
        }
        if ((System.currentTimeMillis() / DatesFiles.f4684a).toInt() != DatesFiles.g) DatesFiles.a()
        var previous: DateGrouping? = null
        for (fileMeta in list) {
            val j = fileMeta.modified
            val group = when {
                j >= DatesFiles.b -> DateGrouping.TODAY
                j >= DatesFiles.c && j < DatesFiles.b -> DateGrouping.YESTERDAY
                j >= DatesFiles.f4685d -> DateGrouping.WEEK
                j >= DatesFiles.f4686e -> DateGrouping.MONTH
                j < DatesFiles.f4687f || j >= DatesFiles.f4686e -> DateGrouping.OLD
                else -> DateGrouping.LAST_MONTH
            }
            if (group != previous) {
                result.add(DateGroup(group))
                previous = group
            }
            result.add(fileMeta)
        }
        return result
    }

    fun E() {
        if (G) {
            G = false
            I.clear()
            J.clear()
            notifyItemRangeChanged(0, itemCount, L)
        }
    }

    fun F() {
        if (I.isEmpty()) return
        val copy = HashSet(I)
        I.clear()
        for (file in copy) {
            val index = G(file)
            if (index != -1) notifyItemChanged(index)
        }
    }

    fun G(file: File?): Int = if (file == null) -1 else K[file] ?: -1

    fun H(file: File) {
        var i2 = 0
        while (i2 < H.size) {
            if (H[i2].file == file) {
                H[i2] = FileMeta(file)
                break
            }
            i2++
        }
        if (i2 >= H.size) H.add(FileMeta(file))
        submitList(D(H))
        D?.b(H.size)
    }

    fun I(i2: Int, filesRecyclerView: FilesRecyclerView, z: Boolean) {
        B = i2
        if (i2 == 0) {
            val layout = GridAutofillLayoutManager(C)
            layout.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int =
                    if (getItemViewType(position) == 100) layout.spanCount else 1
            }
            filesRecyclerView.layoutManager = layout
        } else if (i2 == 1) {
            filesRecyclerView.layoutManager = LinearLayoutManager(C)
        }
        if (z) return
        notifyDataSetChanged()
        Files.f4681h = i2
        SharedPrefs.f4755a?.edit()?.putInt("file_explorer_files_view_type", i2)?.apply()
    }

    fun J(i2: Int, z: Boolean) {
        if (i2 !in H.indices) return
        val obj = getItem(i2)
        if (obj is FileMeta) {
            if (z) I.add(obj.file) else I.remove(obj.file)
            notifyItemChanged(i2)
        }
    }

    fun K(list: List<FileMeta>, i2: Int, bVar: Runnable?) {
        val sortBy = when (i2) {
            0 -> Files.f4679e ?: SortBy.NAME_ASC
            Files.f4678d -> Files.f4680f ?: z
            else -> z
        }
        this.z = sortBy
        A = i2
        @Suppress("UNCHECKED_CAST")
        sortFileMeta(list as MutableList<FileMeta>, sortBy)
        H.clear()
        H.addAll(list)
        submitList(D(H), bVar)
    }

    fun L(i2: Int) {
        var newSort = z
        val asc = newSort == SortBy.NAME_ASC || newSort == SortBy.DATE_MODIFIED_ASC
        if (i2 == 0) {
            newSort = if (asc) SortBy.NAME_ASC else SortBy.NAME_DESC
        } else if (i2 == Files.b) {
            newSort = if (asc) SortBy.DATE_MODIFIED_ASC else SortBy.DATE_MODIFIED_DESC
        }
        z = newSort
        sortFileMeta(H, newSort)
        submitList(D(H))
        SharedPrefs.e(z, A)
    }

    fun M(i2: Int) {
        var newSort = z
        val isAscending = i2 == 0
        if (newSort != SortBy.NAME_ASC && newSort != SortBy.NAME_DESC) {
            if (newSort == SortBy.DATE_MODIFIED_ASC || newSort == SortBy.DATE_MODIFIED_DESC) {
                newSort = if (isAscending) SortBy.DATE_MODIFIED_ASC else SortBy.DATE_MODIFIED_DESC
            }
        } else {
            newSort = if (isAscending) SortBy.NAME_ASC else SortBy.NAME_DESC
        }
        z = newSort
        sortFileMeta(H, newSort)
        submitList(D(H))
        SharedPrefs.e(z, A)
    }

    override fun getItemId(position: Int): Long {
        val obj = getItem(position)
        if (obj is FileMeta) return obj.pathHash
        if (obj is DateGroup) return ("HEADER_" + obj.f4683a.name).hashCode().toLong()
        return -1L
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position) is DateGroup) 100 else B

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val obj = getItem(position)
        if (obj is DateGroup) {
            val header = holder as DateHeaderViewHolder
            header.u.text = header.itemView.context.getString(obj.f4683a.u)
            return
        }
        if (obj is FileMeta && holder is BaseFileViewHolder) {
            val file = obj.file
            holder.z = obj
            holder.y = F
            holder.x = E
            val focusable = Global.p == 1
            holder.itemView.isFocusable = focusable
            holder.itemView.isFocusableInTouchMode = Global.p == 1
            holder.v.text = file.name
            holder.itemView.isSelected = holder.w.I.contains(file)
            holder.itemView.alpha = if (holder.w.G && holder.w.J.contains(file)) 0.6f else 1.0f
            holder.t(obj)
            holder.itemView.isSelected = I.contains(file)
            holder.itemView.alpha = if (G && J.contains(file)) 0.6f else 1.0f
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val obj = getItem(position)
            if (obj is FileMeta && holder is BaseFileViewHolder) {
                val file = obj.file
                val view = holder.itemView
                view.isSelected = I.contains(file)
                view.alpha = if (G && J.contains(file)) 0.6f else 1.0f
                return
            }
        }
        onBindViewHolder(holder, position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(C)
        if (viewType == 100) {
            val view = inflater.inflate(R.layout.explorer_file_title, parent, false)
            return DateHeaderViewHolder(view)
        }
        if (viewType == 0) {
            val view = inflater.inflate(R.layout.explorer_item_file_medium_icons, parent, false)
            return MediumIconsViewHolder(view, view.findViewById(R.id.image_view), view.findViewById(R.id.text_view), this)
        }
        if (viewType != 1) throw IllegalArgumentException("Invalid view type")
        val view = inflater.inflate(R.layout.explorer_item_file_details, parent, false)
        val params = view.layoutParams
        params.width = (parent.width * 0.6).toInt()
        view.layoutParams = params
        return DetailsViewHolder(view, this)
    }
}
