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
                { w }
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
    }

    @JvmField var A: OnContextMenuListener? = null
    @JvmField var B: OnContextMenuListener? = null
    @JvmField var C: OnItemsMovedListener? = null
    @JvmField var D: OnItemClickListener<Any>? = null
    @JvmField val E: ArrayList<DesktopItem> = ArrayList()
    @JvmField val F: HashMap<String?, Int> = HashMap()
    @JvmField var y: Boolean = false
    @JvmField var z: Boolean = false

    init {
        setHasStableIds(true)
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
        for (i in E.indices) {
            if (E[i].c() == -1) {
                E[i] = item
                notifyItemChanged(i)
                DesktopDB.b(i, item)
                B()
                return
            }
        }
        Toast.makeText(x, "No space left", Toast.LENGTH_SHORT).show()
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
