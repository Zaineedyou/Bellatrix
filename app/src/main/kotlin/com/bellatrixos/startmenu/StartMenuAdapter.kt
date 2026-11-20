package com.bellatrixos.startmenu

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.os.UserManager
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.R
import com.bellatrixos.context_menu.ContextMenuManager
import com.bellatrixos.context_menu.OnItemClickListener
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.startmenu.listeners.StartMenuItemDiffCallback
import com.bellatrixos.variables.Global
import com.bellatrixos.windows.icons.IconEngine
import com.bellatrixos.windows.installed_apps.AppsDatabase
import com.bellatrixos.windows.installed_apps.InstalledAppsDB
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Adapter daftar aplikasi Start Menu beserta filter dan menu konteks. */
class StartMenuAdapter(@JvmField val B: Context, @JvmField val x: MutableList<StartMenuItem>) :
    RecyclerView.Adapter<StartMenuAdapter.ViewHolder>(), Filterable {

    inner class AppFilter : Filter() {
        @JvmField val f4785a: ArrayList<StartMenuItem> = ArrayList()
        @JvmField val b: StartMenuItemDiffCallback = StartMenuItemDiffCallback()

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            f4785a.clear()
            val source = if (Global.f4842k == 3) y else x
            val candidates = ArrayList<StartMenuItem>()
            for (item in source) {
                if (Global.f4842k != 2 || item.a() == 1) candidates.add(item)
            }
            if (TextUtils.isEmpty(constraint)) {
                val comparator = startMenuOrderComparator(Global.l)
                candidates.sortWith(comparator)
                f4785a.addAll(candidates)
            } else {
                val query = constraint.toString().lowercase(Locale.ROOT).trim()
                for (item in candidates) {
                    val name = item.b()
                    if (name != null && name.lowercase(Locale.ROOT).contains(query)) f4785a.add(item)
                }
            }
            return Filter.FilterResults().apply {
                values = ArrayList(f4785a)
                count = f4785a.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            @Suppress("UNCHECKED_CAST")
            val newList = results?.values as? List<StartMenuItem> ?: emptyList()
            b.f4801a = ArrayList(z)
            b.b = newList
            val diff = DiffUtil.calculateDiff(b)
            z.clear()
            z.addAll(newList)
            diff.dispatchUpdatesTo(AdapterListUpdateCallback(this@StartMenuAdapter))
        }
    }

    interface OnContextMenuListener {
        fun c(iArr: IntArray, startMenuItem: StartMenuItem)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @JvmField var u: ImageView? = null
        @JvmField var v: TextView? = null
    }

    companion object {
        @JvmField val G: ConcurrentHashMap<String, Any> = ConcurrentHashMap()
        @JvmField var H: Boolean = false

        /** 6/8 nama A-Z, 7/9 nama Z-A (mode dari androidx.core.provider.a). */
        private fun startMenuOrderComparator(mode: Int): Comparator<StartMenuItem> = when (mode) {
            7, 9 -> compareByDescending<StartMenuItem> { it.b().orEmpty() }
            else -> compareBy<StartMenuItem> { it.b().orEmpty() }
        }
    }

    @JvmField var A: AppFilter? = null
    @JvmField val C: LayoutInflater = LayoutInflater.from(B)
    @JvmField var D: OnItemClickListener<Any>? = null
    @JvmField var E: OnContextMenuListener? = null
    @JvmField val y: ArrayList<StartMenuItem> = ArrayList()
    @JvmField val z: ArrayList<StartMenuItem> = ArrayList()
    @JvmField var F: Boolean = false

    init {
        Global.f4842k = SharedPrefs.f4755a?.getInt("start_menu_apps_view", 2) ?: 2
        Global.l = SharedPrefs.f4755a?.getInt("start_menu_apps_order", 0) ?: 0
        try {
            val userManager = B.getSystemService(Context.USER_SERVICE) as? UserManager
            H = (userManager?.userProfiles?.size ?: 0) > 1
            B()
        } catch (_: RuntimeException) {
        }
        E(Global.f4842k)
    }

    /** Tambah aplikasi bila belum terdaftar. */
    fun A(i2: Int, str: String, str2: String) {
        if (G.containsKey(str)) return
        val item = StartMenuItem(i2, str2, str)
        x.add(item)
        G[str] = str2
        val filter = A
        if (filter != null) {
            filter.filter("")
            return
        }
        z.clear()
        z.addAll(x)
        notifyDataSetChanged()
    }

    /** Isi daftar aplikasi dari launcher dan penyimpanan. */
    fun B() {
        G.clear()
        val packageManager: PackageManager = B.packageManager
        val intent = Intent("android.intent.action.MAIN", null as Uri?)
        intent.addCategory("android.intent.category.LAUNCHER")
        for (resolveInfo in packageManager.queryIntentActivities(intent, 0)) {
            val label = resolveInfo.loadLabel(packageManager).toString()
            val pkg = resolveInfo.activityInfo.packageName
            if (!G.containsKey(pkg)) {
                x.add(StartMenuItem(if (pkg.startsWith("org.chromium.webapk.")) 1 else 0, label, pkg))
                G[pkg] = label
            }
        }
        for (installed in InstalledAppsDB.b()) A(1, installed.c(), installed.b().orEmpty())
        A(1, "UINotepad", "UiNotepad")
        A(1, "UITerminal", "Terminal")
        A(1, "UIBrowser", "UiBrowser")
        A(1, "UIInstaller", "UIInstaller")
        A(1, "UIMusic", "UiMusic")
    }

    /** Muat aplikasi dari profil lain. */
    fun C() {
        if (F) return
        val launcherApps = B.getSystemService("launcherapps") as? LauncherApps ?: return
        val userManager = B.getSystemService(Context.USER_SERVICE) as? UserManager ?: return
        val current = Process.myUserHandle()
        for (handle in userManager.userProfiles) {
            if (handle == current) continue
            for (info in launcherApps.getActivityList(null, handle)) {
                y.add(
                    StartMenuItem(
                        info.label.toString(),
                        info.applicationInfo.packageName,
                        2,
                        info.componentName,
                        handle
                    )
                )
            }
        }
        F = true
    }

    /** Alias untuk notifyDataSetChanged setelah perubahan daftar. */
    fun h() { notifyDataSetChanged() }

    /** Muat ulang seluruh daftar. */
    fun D(cVar: Runnable?) {
        x.clear()
        z.clear()
        y.clear()
        F = false
        G.clear()
        val userManager = B.getSystemService(Context.USER_SERVICE) as? UserManager
        H = (userManager?.userProfiles?.size ?: 0) > 1
        B()
        if (Global.f4842k == 3) C()
        F(Global.l, false)
        notifyDataSetChanged()
        cVar?.run()
    }

    /** Ganti mode tampilan daftar aplikasi. */
    fun E(i2: Int) {
        val selected: ArrayList<StartMenuItem> = when (i2) {
            0 -> ArrayList(x)
            2 -> ArrayList(x.filter { it.a() == 1 })
            3 -> { C(); ArrayList(y) }
            else -> ArrayList(x)
        }
        selected.sortWith(startMenuOrderComparator(Global.l))
        val callback = StartMenuItemDiffCallback()
        callback.f4801a = ArrayList(z)
        callback.b = selected
        val diff = DiffUtil.calculateDiff(callback)
        z.clear()
        z.addAll(selected)
        diff.dispatchUpdatesTo(AdapterListUpdateCallback(this))
        if (i2 != 3) SharedPrefs.f4755a?.edit()?.putInt("start_menu_apps_view", i2)?.apply()
    }

    /** Ubah urutan aplikasi. */
    fun F(i2: Int, notify: Boolean) {
        if (i2 != 0) {
            @Suppress("UNCHECKED_CAST")
            (z as? MutableList<StartMenuItem>)?.sortWith(startMenuOrderComparator(if (i2 == 1) 8 else 9))
        } else {
            z.clear()
            z.addAll(x)
        }
        Global.l = i2
        if (notify) notifyDataSetChanged()
    }

    /** Perbarui satu aplikasi berdasarkan package name. */
    fun G(str: String) {
        for (i in z.indices) {
            if (z[i].c() == str) {
                notifyItemChanged(i)
                return
            }
        }
    }

    override fun getItemCount(): Int = z.size

    override fun getFilter(): Filter {
        if (A == null) A = AppFilter()
        return A!!
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = z[position]
        IconEngine.b(B).c(item.c().orEmpty(), IconEngine.Callback { bitmap ->
            if (holder.bindingAdapterPosition != -1) holder.u?.setImageBitmap(bitmap)
        })
        holder.v?.text = AppsDatabase.a(item.c().orEmpty())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = C.inflate(R.layout.start_menu_item, parent, false)
        val holder = ViewHolder(view)
        holder.u = view.findViewById(R.id.appIconImageView)
        holder.v = view.findViewById(R.id.appNameTextView)
        ContextMenuManager.a(
            0,
            view,
            { if (holder.bindingAdapterPosition == -1) null else z.getOrNull(holder.bindingAdapterPosition) },
            { iArr, obj -> (obj as? StartMenuItem)?.let { item -> E?.c(iArr, item) } },
            { D }
        )
        return holder
    }
}
