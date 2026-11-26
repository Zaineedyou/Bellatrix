package com.bellatrixos.desktop

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R
import com.bellatrixos.context_menu.ContextMenuManager
import com.bellatrixos.context_menu.OnItemClickListener
import com.bellatrixos.desktop.interfaces.TypeProvider
import com.bellatrixos.startmenu.g
import com.bellatrixos.variables.Global
import com.bellatrixos.windows.files.FileExtensions
import com.bellatrixos.windows.icons.IconEngine
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

/** Adapter ikon desktop dengan dukungan susun ulang dan menu konteks. */
class DesktopAdapter(@JvmField val x: Context) : RecyclerView.Adapter<DesktopAdapter.ViewHolder>(), TypeProvider {
    fun interface OnContextMenuListener {
        fun i(iArr: IntArray, desktopItem: DesktopItem)
    }

    fun interface OnItemsMovedListener {
        fun j()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField val u: ImageView = view.findViewById(R.id.image_view)
        @JvmField val v: TextView = view.findViewById(R.id.text_view)
        @JvmField var w: OnItemClickListener<Any>? = null
        @JvmField var x: OnContextMenuListener? = null
        @JvmField var y: OnContextMenuListener? = null

        init {
            ContextMenuManager.a(
                -1,
                view,
                { if (bindingAdapterPosition == -1) null else (bindingAdapter as? DesktopAdapter)?.E?.getOrNull(bindingAdapterPosition) },
                { iArr, obj ->
                    (obj as? DesktopItem)?.let { item ->
                        x?.i(iArr, item)
                        y?.i(iArr, item)
                    }
                },
                // Supplier klik: harus mengembalikan OnItemClickListener (w), bukan item desktop.
                { w },
                // The drag starts here, after the menu, so a long press always opens the menu first.
                { (bindingAdapter as? DesktopAdapter)?.dragStarter?.invoke(this) }
            )
        }

        fun t(
            desktopItem: DesktopItem,
            context: Context,
            z2: Boolean,
            onContextMenuListener: OnContextMenuListener?,
            onItemClickListener: OnItemClickListener<Any>?,
            onContextMenuListener2: OnContextMenuListener?
        ) {
            val type = desktopItem.c()
            val view = itemView
            if (type == -1) {
                y = onContextMenuListener2
                x = null
                w = null
                view.setBackgroundColor(0)
                view.isFocusable = true
                view.isFocusableInTouchMode = true
            } else {
                y = null
                x = onContextMenuListener
                w = onItemClickListener
                view.setBackgroundResource(R.drawable.widget_ui_item_bg_selector_light)
                view.isFocusable = z2
                view.isFocusableInTouchMode = z2
            }
            view.isClickable = z2

            when (Global.q) {
                2, 0 -> {
                    val params = u.layoutParams
                    val size = context.resources.getDimensionPixelSize(
                        if (Global.q == 2) R.dimen.ui_62dp else R.dimen.ui_36dp
                    )
                    params.width = size
                    params.height = size
                    u.layoutParams = params
                    v.maxLines = if (Global.q == 2) 1 else 2
                    DesktopAdapter.G = true
                }
                else -> if (DesktopAdapter.G) {
                    val params = u.layoutParams
                    val size = context.resources.getDimensionPixelSize(R.dimen.ui_46dp)
                    params.width = size
                    params.height = size
                    u.layoutParams = params
                    v.maxLines = 2
                }
            }

            val data = desktopItem.a()
            val name = desktopItem.b().orEmpty()
            val shortcut = type == 1 || type == 2 || (type == 3 && data != null && !data.contains("Bellatrix/Desktop"))
            u.foreground = if (shortcut) ContextCompat.getDrawable(context, R.drawable.image_shortcut) else null

            val lowerName = name.lowercase()
            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".webp")
            ) {
                Glide.with(context).load(data).override(46, 46).into(u)
            } else if (type != -1) {
                when (type) {
                    0 -> u.setImageResource(R.drawable.img_this_pc)
                    1 -> IconEngine.b(context).c(data.orEmpty(), g(u))
                    3 -> u.setImageResource(R.drawable.img_folder_default)
                    else -> u.setImageResource(FileExtensions.e(name))
                }
            } else {
                u.setImageDrawable(null)
            }

            val label = when (name) {
                "UiNotepad" -> R.string.app_notepad
                "UiTerminal" -> R.string.app_terminal
                "This PC" -> R.string.explorer_str_this_pc
                "UiInstaller" -> R.string.app_installer
                "UiBrowser" -> R.string.app_chrome
                "UiSettings" -> R.string.app_settings
                "UiExplorer" -> R.string.app_explorer
                else -> {
                    v.text = name
                    v.visibility = View.VISIBLE
                    u.visibility = View.VISIBLE
                    null
                }
            }
            if (label != null) v.text = context.getString(label)
            v.visibility = View.VISIBLE
            u.visibility = View.VISIBLE
        }
    }

    companion object {
        @JvmField var G: Boolean = false
        @JvmField var H: Boolean = true
        /** Jumlah kolom grid desktop; diisi saat layout diketahui. */
        @JvmField var spanCount: Int = 1
    }

    /** Starts dragging an icon: set by the home setup, called from an icon's long press once its menu is up. */
    @JvmField var dragStarter: ((RecyclerView.ViewHolder) -> Unit)? = null

    /** How many cells fit on screen (rows x columns), known after the first layout; 0 before. */
    @JvmField var capacity: Int = 0

    private var lastRows = 0
    private var lastCols = 0

    @JvmField var A: OnContextMenuListener? = null
    @JvmField var B: OnContextMenuListener? = null
    @JvmField var C: OnItemsMovedListener? = null
    @JvmField var D: OnItemClickListener<Any>? = null
    @JvmField val E: ArrayList<DesktopItem> = ArrayList()
    @JvmField val F: HashMap<String?, Int> = HashMap()
    @JvmField var y: Boolean = false
    @JvmField var z: Boolean = false

    init {
        // No stable ids: this adapter has no getItemId, so every item had the same id (-1) and RecyclerView handed one
        // icon's view to another position without rebinding it, which showed icons swapped, doubled or missing.
        val pins: MutableList<DesktopItemPins> = try {
            DesktopDB.a()
        } catch (_: Exception) {
            DesktopDB.b ?: run {
                val type = object : TypeToken<List<DesktopItemPins>>() {}.type
                val parsed = GsonBuilder().registerTypeAdapter(DesktopItemPins::class.java, DesktopPinItemDeserializer())
                    .create().fromJson<MutableList<DesktopItemPins>>(
                        DesktopDB.f4759a?.getString("desktop_items", "") ?: "",
                        type
                    ) ?: mutableListOf()
                DesktopDB.b = parsed
                parsed
            }
        }
        if (pins.isEmpty()) {
            E.add(DesktopItem(0, "This PC", null))
            E.add(DesktopItem(1, "UiInstaller", "UIInstaller"))
            E.add(DesktopItem(1, "UiBrowser", "UIBrowser"))
            DesktopDB.c(E)
        } else {
            for (pin in pins) E.add(DesktopItem(pin.c(), pin.b(), pin.a()))
        }
        B()
    }

    /** Tambah item desktop; saat auto-arrange nonaktif isi slot kosong. */
    fun A(i2: Int, str: String, str2: String) {
        // Pinning what is already on home again made two icons that shared one data string, and removing either
        // one removed both (C matches by data).
        if (E.any { it.c() != -1 && it.a() == str }) return
        val item = DesktopItem(i2, str2, str)
        if (H) {
            E.add(item)
            notifyItemInserted(E.size - 1)
            val list = DesktopDB.a()
            list.add(DesktopItemPins(i2, list.size, str2, str))
            DesktopDB.b = list
            DesktopDB.f4759a?.edit()?.putString("desktop_items", Gson().toJson(list))?.apply()
            B()
            return
        }
        // The first empty cell in reading order: down the first column, then from the top of the next one.
        val target = E.indexOfFirst { it.c() == -1 }
        if (target != -1) {
            E[target] = item
            notifyItemChanged(target)
            DesktopDB.b(target, item)
            B()
            return
        }
        // Cells are only made once the screen has been measured; a full list with room left grows by one.
        if (E.size < capacity) {
            E.add(item)
            notifyItemInserted(E.size - 1)
            DesktopDB.c(E)
            B()
            return
        }
        Toast.makeText(x, "No space left", Toast.LENGTH_SHORT).show()
    }

    /**
     * Pastikan ada slot kosong untuk penempatan bebas saat auto-arrange mati.
     * Called whenever home's layout changes (first layout, the phone turning): [rows] cells down, [cols] across.
     * The list is read down each column, so turning the phone only re-wraps it and the order never changes; if
     * the new screen has fewer cells, gaps between icons close up first so nothing ends up off screen.
     */
    fun ensureFreeCells(rows: Int, cols: Int) {
        spanCount = cols.coerceAtLeast(1)
        lastRows = rows
        lastCols = cols
        val target = kotlin.math.max(0, rows) * spanCount
        capacity = target
        if (H || target <= 0) return
        val drop = DesktopSlots.blanksToDrop(E.map { it.c() == -1 }, target)
        var changed = drop.isNotEmpty()
        for (index in drop.asReversed()) E.removeAt(index)
        while (E.size > target && E.last().c() == -1) {
            E.removeAt(E.lastIndex)
            changed = true
        }
        while (E.size < target) {
            E.add(DesktopItem(-1, "", "blank_${E.size}"))
            changed = true
        }
        if (!changed) return
        // Blanks are named by their cell, and cells have moved.
        for (i in E.indices) if (E[i].c() == -1) E[i] = DesktopItem(-1, "", "blank_$i")
        DesktopDB.c(E)
        B()
        notifyDataSetChanged()
    }

    /** Fits the cells to the last grid size [ensureFreeCells] was given, e.g. after auto-arrange is turned off. */
    fun refit() {
        if (lastRows > 0 && lastCols > 0) ensureFreeCells(lastRows, lastCols)
    }

    /** Bangun ulang peta data -> posisi. */
    fun B() {
        F.clear()
        for (i in E.indices) F[E[i].a()] = i
    }

    /** Hapus item berdasarkan data. */
    fun C(str: String) {
        for (size in E.indices.reversed()) {
            if (str == E[size].a()) {
                if (H) {
                    E.removeAt(size)
                    notifyItemRemoved(size)
                    runCatching { DesktopDB.a().removeAt(size) }
                    DesktopDB.c(E)
                } else {
                    val blank = DesktopItem(-1, "", "blank_$size")
                    E[size] = blank
                    notifyItemChanged(size)
                    DesktopDB.b(size, blank)
                }
                B()
            }
        }
    }

    /** Ubah ukuran ikon. */
    fun D(i2: Int) {
        Global.q = i2
        notifyDataSetChanged()
        DesktopDB.f4759a?.edit()?.putInt("desktop_icon_size", i2)?.apply()
    }

    /** Perbarui satu item berdasarkan data. */
    fun E(str: String) {
        for (i in E.indices) {
            if (str == E[i].a()) {
                notifyItemChanged(i)
                return
            }
        }
    }

    override fun getItemCount(): Int = if (Global.r) E.size else 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (!Global.r || position < 0 || position >= E.size) return
        holder.t(E[position], x, z, A, D, B)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.desktop_item, parent, false)
        return ViewHolder(view)
    }
}
