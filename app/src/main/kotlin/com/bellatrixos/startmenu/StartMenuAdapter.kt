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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.compose.ui.graphics.asAndroidBitmap
import com.bellatrixos.App
import com.bellatrixos.R
import com.bellatrixos.context_menu.ContextMenuManager
import com.bellatrixos.linux.LINUX_APP_PREFIX
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
                // Section headers only make sense in the full, grouped list, not in search results.
                if (isHeader(item)) continue
                candidates.add(item)
            }
            if (TextUtils.isEmpty(constraint)) {
                candidates.sortWith(startMenuOrderComparator(Global.l))
                f4785a.addAll(candidates)
            } else {
                val query = constraint.toString().lowercase(Locale.ROOT).trim()
                for (item in candidates) {
                    val name = item.b()
                    if (name != null && name.lowercase(Locale.ROOT).contains(query)) f4785a.add(item)
                }
                f4785a.sortWith(startMenuOrderComparator(Global.l))
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

        private const val SECTION_PREFIX = "section:"
        private const val SECTION_BELLATRIX = SECTION_PREFIX + "bellatrix"
        private const val SECTION_ANDROID = SECTION_PREFIX + "android"
        private const val SECTION_AGENTS = SECTION_PREFIX + "agents"
        private const val SECTION_LINUX = SECTION_PREFIX + "linux"

        private const val VIEW_TYPE_APP = 0
        private const val VIEW_TYPE_HEADER = 1

        /** Bellatrix's own windows; everything else with category 1 is an installed app. */
        private val BELLATRIX_TAGS =
            setOf(
                "UISettings",
                "UIExplorer",
                "UINotepad",
                "UITerminal",
                "UILinuxWindow",
                "UIVoice",
                "UIBrowser",
                "UIInstaller",
                "UIVideo",
                "UIMusic",
                "UIPhotos",
            )

        fun isHeader(item: StartMenuItem): Boolean = item.c()?.startsWith(SECTION_PREFIX) == true

        /** Separates the drawer into Bellatrix, Android, coding agents and other Linux apps. */
        private fun sectionRank(item: StartMenuItem): Int {
            val key = item.c().orEmpty()
            return when (key) {
                SECTION_BELLATRIX -> 0
                SECTION_ANDROID -> 1
                SECTION_AGENTS -> 2
                SECTION_LINUX -> 3
                else -> when {
                    key.startsWith(LINUX_APP_PREFIX + "agent-") -> 2
                    key.startsWith(LINUX_APP_PREFIX) -> 3
                    key in BELLATRIX_TAGS -> 0
                    else -> 1
                }
            }
        }

        /** 6/8 nama A-Z, 7/9 nama Z-A (mode dari androidx.core.provider.a). */
        private fun startMenuOrderComparator(mode: Int): Comparator<StartMenuItem> {
            val byName = when (mode) {
                7, 9 -> compareByDescending<StartMenuItem> { it.b().orEmpty() }
                else -> compareBy<StartMenuItem> { it.b().orEmpty() }
            }
            return Comparator { a, b ->
                val bySection = sectionRank(a).compareTo(sectionRank(b))
                if (bySection != 0) return@Comparator bySection
                // Keep each header above the apps in its own section.
                val byKind = isHeader(b).compareTo(isHeader(a))
                if (byKind != 0) return@Comparator byKind
                byName.compare(a, b)
            }
        }
    }

    @JvmField var A: AppFilter? = null
    @Volatile private var attachedRecyclerView: RecyclerView? = null
    @JvmField val C: LayoutInflater = LayoutInflater.from(B)
    @JvmField var D: OnItemClickListener<Any>? = null
    @JvmField var E: OnContextMenuListener? = null
    @JvmField val y: ArrayList<StartMenuItem> = ArrayList()
    @JvmField val z: ArrayList<StartMenuItem> = ArrayList()
    @JvmField var F: Boolean = false

    init {
        val prefs = SharedPrefs.f4755a
        val savedView = prefs?.getInt("start_menu_apps_view", 0) ?: 0
        try {
            val userManager = B.getSystemService(Context.USER_SERVICE) as? UserManager
            H = (userManager?.userProfiles?.size ?: 0) > 1
        } catch (_: RuntimeException) {
        }
        // Mode 2 hid every APK; mode 3 shows ONLY work-profile apps and hides all
        // built-ins when there is no second profile. Collapse both to "all apps".
        val effectiveView = when {
            savedView == 2 -> 0
            savedView == 3 && !H -> 0
            savedView !in 0..3 -> 0
            else -> savedView
        }
        if (effectiveView != savedView) {
            prefs?.edit()?.putInt("start_menu_apps_view", effectiveView)?.apply()
        }
        Global.f4842k = effectiveView
        Global.l = prefs?.getInt("start_menu_apps_order", 0) ?: 0
        try {
            B()
        } catch (_: RuntimeException) {
        }
        E(Global.f4842k)
    }

    /** Tambah aplikasi bila belum terdaftar. */
    fun A(i2: Int, str: String, str2: String) {
        if (G.containsKey(str)) return
        // Bellatrix's own Terminal is seeded first. No APK, web app or guest .desktop
        // entry may add a second row with the same label: the drawer shows one Terminal.
        if (
            str2.equals("Terminal", ignoreCase = true) &&
            (G.containsKey("UITerminal") || G.values.any { (it as? String)?.equals("Terminal", ignoreCase = true) == true })
        ) return
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

    private fun addHeader(key: String, label: String) {
        A(0, key, label)
    }

    /** Isi daftar aplikasi dari launcher dan penyimpanan. */
    fun B() {
        G.clear()
        // One drawer, four clearly separated kinds. Coding agents get their own section:
        // install them from Settings, then they appear here as installed apps.
        addHeader(SECTION_BELLATRIX, B.getString(R.string.start_section_bellatrix))
        addHeader(SECTION_ANDROID, B.getString(R.string.start_section_android))
        addHeader(SECTION_AGENTS, B.getString(R.string.settings_agents))
        addHeader(SECTION_LINUX, B.getString(R.string.start_section_linux))

        A(1, "UISettings", B.getString(R.string.app_settings))
        A(1, "UIExplorer", B.getString(R.string.app_explorer))
        A(1, "UINotepad", B.getString(R.string.app_notepad))
        A(1, "UITerminal", B.getString(R.string.app_terminal))
        A(1, "UIBrowser", B.getString(R.string.app_chrome))
        A(1, "UIInstaller", B.getString(R.string.app_installer))
        A(1, "UIVideo", B.getString(R.string.app_media_player_title))
        A(1, "UIMusic", B.getString(R.string.app_music))
        A(1, "UIPhotos", B.getString(R.string.app_image))

        val packageManager: PackageManager = B.packageManager
        val intent = Intent("android.intent.action.MAIN", null as Uri?)
        intent.addCategory("android.intent.category.LAUNCHER")
        for (resolveInfo in packageManager.queryIntentActivities(intent, 0)) {
            val label = resolveInfo.loadLabel(packageManager).toString()
            val pkg = resolveInfo.activityInfo.packageName
            A(if (pkg.startsWith("org.chromium.webapk.")) 1 else 0, pkg, label)
        }
        try {
            for (installed in InstalledAppsDB.b()) A(1, installed.c(), installed.b().orEmpty())
        } catch (_: RuntimeException) {
        }
        // Guest apps keep their desktop id. Agents are agent-*.desktop, so the comparator
        // puts them under "coding agents"; every other guest app under "Linux apps".
        try {
            val runtime = (B.applicationContext as? App)?.runtime ?: return
            for (app in runtime.linuxApps.apps) A(1, LINUX_APP_PREFIX + app.id, app.name)
        } catch (_: RuntimeException) {
        }
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
            2 -> ArrayList(x)
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
            // x is built section by section; sort it so headers sit above their apps.
            z.sortWith(startMenuOrderComparator(0))
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

    override fun getItemViewType(position: Int): Int =
        if (isHeader(z[position])) VIEW_TYPE_HEADER else VIEW_TYPE_APP

    override fun getFilter(): Filter {
        if (A == null) A = AppFilter()
        return A!!
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedRecyclerView = recyclerView
        applySpanLookup(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        if (attachedRecyclerView === recyclerView) attachedRecyclerView = null
        super.onDetachedFromRecyclerView(recyclerView)
    }

    /** MainActivity replaces the GridLayoutManager on rotate; keep full-width headers working. */
    private fun applySpanLookup(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return
        if (layoutManager.spanSizeLookup is SectionSpanLookup) return
        layoutManager.spanSizeLookup = SectionSpanLookup(layoutManager)
    }

    private inner class SectionSpanLookup(private val layoutManager: GridLayoutManager) : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int =
            if (getItemViewType(position) == VIEW_TYPE_HEADER) layoutManager.spanCount else 1
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        attachedRecyclerView?.let(::applySpanLookup)
        val item = z[position]
        val key = item.c().orEmpty()
        if (isHeader(item)) {
            holder.u?.visibility = View.GONE
            holder.v?.text = item.b().orEmpty()
            return
        }
        holder.u?.visibility = View.VISIBLE
        if (key.startsWith(LINUX_APP_PREFIX)) {
            // The runtime already decoded the guest's Icon=; reuse it instead of asking
            // IconEngine to resolve a desktop id as if it were an Android package.
            val id = key.removePrefix(LINUX_APP_PREFIX)
            val icon = (B.applicationContext as? App)?.runtime?.linuxApps?.icons?.get(id)?.asAndroidBitmap()
            holder.u?.setImageBitmap(icon)
        } else {
            IconEngine.b(B).c(key, IconEngine.Callback { bitmap ->
                if (holder.bindingAdapterPosition != -1) holder.u?.setImageBitmap(bitmap)
            })
        }
        // Nama asli APK sudah ada di item.b(); AppsDatabase.a() hanya untuk tag window internal (UI...).
        val packageName = item.c().orEmpty()
        holder.v?.text =
            if (AppsDatabase.b.containsKey(packageName)) {
                AppsDatabase.a(packageName)
            } else {
                item.b()?.takeIf { it.isNotBlank() } ?: AppsDatabase.a(packageName)
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            C.inflate(
                if (viewType == VIEW_TYPE_HEADER) R.layout.start_menu_section else R.layout.start_menu_item,
                parent,
                false,
            )
        val holder = ViewHolder(view)
        holder.u = view.findViewById(R.id.appIconImageView)
        holder.v = view.findViewById(R.id.appNameTextView)
        if (viewType == VIEW_TYPE_APP) {
            ContextMenuManager.a(
                0,
                view,
                { if (holder.bindingAdapterPosition == -1) null else z.getOrNull(holder.bindingAdapterPosition) },
                { iArr, obj -> (obj as? StartMenuItem)?.let { item -> E?.c(iArr, item) } },
                { D }
            )
        }
        return holder
    }
}
