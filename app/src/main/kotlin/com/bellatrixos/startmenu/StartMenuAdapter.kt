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
            val source = if (Global.f4842k == MODE_WORK) y else x
            val candidates = ArrayList<StartMenuItem>()
            for (item in source) {
                // The drawer filter lives in the drop-down; keep search inside that selection.
                if (matchesKind(item, Global.f4842k)) candidates.add(item)
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

        const val MODE_ALL = 0
        const val MODE_FREEFORM = 2
        const val MODE_WORK = 3

        /** App types live in the drawer drop-down, not as category rows inside All. */
        const val FILTER_BELLATRIX = 4
        const val FILTER_ANDROID = 5
        const val FILTER_CODING_AGENTS = 6
        const val FILTER_LINUX = 7

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

        fun labelRes(mode: Int): Int =
            when (mode) {
                FILTER_BELLATRIX -> R.string.start_filter_bellatrix
                FILTER_ANDROID -> R.string.start_filter_android
                FILTER_CODING_AGENTS -> R.string.start_filter_coding_agents
                FILTER_LINUX -> R.string.start_filter_linux
                MODE_WORK -> R.string.st_menu_apps_order_work
                MODE_FREEFORM -> R.string.st_menu_apps_order_freeform
                else -> R.string.st_menu_apps_order_all
            }

        private fun kindRank(item: StartMenuItem): Int {
            val key = item.c().orEmpty()
            return when {
                key.startsWith(LINUX_APP_PREFIX + "agent-") -> FILTER_CODING_AGENTS - FILTER_BELLATRIX
                key.startsWith(LINUX_APP_PREFIX) -> FILTER_LINUX - FILTER_BELLATRIX
                key in BELLATRIX_TAGS -> FILTER_BELLATRIX - FILTER_BELLATRIX
                else -> FILTER_ANDROID - FILTER_BELLATRIX
            }
        }

        private fun matchesKind(
            item: StartMenuItem,
            mode: Int,
        ): Boolean = mode !in FILTER_BELLATRIX..FILTER_LINUX || kindRank(item) == mode - FILTER_BELLATRIX

        /** 6/8 nama A-Z, 7/9 nama Z-A (mode dari androidx.core.provider.a). */
        private fun startMenuOrderComparator(mode: Int): Comparator<StartMenuItem> {
            val byName = when (mode) {
                7, 9 -> compareByDescending<StartMenuItem> { it.b().orEmpty() }
                else -> compareBy<StartMenuItem> { it.b().orEmpty() }
            }
            return Comparator { a, b -> byName.compare(a, b) }
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
        val prefs = SharedPrefs.f4755a
        val savedView = prefs?.getInt("start_menu_apps_view", 0) ?: 0
        try {
            val userManager = B.getSystemService(Context.USER_SERVICE) as? UserManager
            H = (userManager?.userProfiles?.size ?: 0) > 1
        } catch (_: RuntimeException) {
        }
        // Mode 2 hid every APK; mode 3 shows ONLY work-profile apps and hides all
        // built-ins when there is no second profile. Collapse both to "all apps".
        val validViews = setOf(MODE_ALL, MODE_FREEFORM, MODE_WORK, FILTER_BELLATRIX, FILTER_ANDROID, FILTER_CODING_AGENTS, FILTER_LINUX)
        val effectiveView = when {
            savedView == MODE_FREEFORM -> MODE_ALL
            savedView == MODE_WORK && !H -> MODE_ALL
            savedView !in validViews -> MODE_ALL
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

    /** Isi daftar aplikasi dari launcher dan penyimpanan. */
    fun B() {
        G.clear()
        // Types are chosen in the drop-down; All stays one flat, alphabetised list.
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
        // Guest apps keep their desktop id. Agents are agent-*.desktop so the drop-down
        // can show them as Coding Agents; every other guest app as Linux apps.
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
        if (Global.f4842k == MODE_WORK) C()
        E(Global.f4842k)
        notifyDataSetChanged()
        cVar?.run()
    }

    /** Ganti mode tampilan daftar aplikasi. */
    fun E(i2: Int) {
        val source: ArrayList<StartMenuItem> = when (i2) {
            MODE_WORK -> { C(); ArrayList(y) }
            else -> ArrayList(x)
        }
        val selected = ArrayList<StartMenuItem>()
        for (item in source) {
            if (matchesKind(item, i2)) selected.add(item)
        }
        selected.sortWith(startMenuOrderComparator(Global.l))
        val callback = StartMenuItemDiffCallback()
        callback.f4801a = ArrayList(z)
        callback.b = selected
        val diff = DiffUtil.calculateDiff(callback)
        z.clear()
        z.addAll(selected)
        diff.dispatchUpdatesTo(AdapterListUpdateCallback(this))
        if (i2 != MODE_WORK) SharedPrefs.f4755a?.edit()?.putInt("start_menu_apps_view", i2)?.apply()
    }

    /** Ubah urutan aplikasi tanpa keluar dari filter drop-down yang aktif. */
    fun F(i2: Int, notify: Boolean) {
        val source = if (Global.f4842k == MODE_WORK) y else x
        val selected = ArrayList<StartMenuItem>()
        for (item in source) {
            if (matchesKind(item, Global.f4842k)) selected.add(item)
        }
        selected.sortWith(startMenuOrderComparator(if (i2 == 1) 8 else if (i2 == 0) 0 else 9))
        z.clear()
        z.addAll(selected)
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
        val key = item.c().orEmpty()
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
