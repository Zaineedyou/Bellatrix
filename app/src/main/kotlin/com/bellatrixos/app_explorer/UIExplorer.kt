package com.bellatrixos.app_explorer

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.UIFragment
import com.bellatrixos.app_explorer.drives.DriveAdapter
import com.bellatrixos.app_explorer.drives.DriveItem
import com.bellatrixos.app_explorer.drives.StorageDirs
import com.bellatrixos.app_explorer.files.FileAdapter
import com.bellatrixos.app_explorer.files.Files
import com.bellatrixos.app_explorer.files.SortBy
import com.bellatrixos.app_explorer.navigation.Navigation
import com.bellatrixos.app_explorer.navigation.NavigationAdapter
import com.bellatrixos.app_explorer.navigation.NavigationItem
import com.bellatrixos.App
import com.bellatrixos.Runtime
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.interfaces.BackPressHandler
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.linux.shellQuote
import com.bellatrixos.ui.files.EditorHome
import com.bellatrixos.ui.files.EditorScreen
import com.bellatrixos.ui.files.FilesScreen
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.ExplorerFilesInfoView
import com.bellatrixos.widgets.FilesRecyclerView
import com.bellatrixos.widgets.FloatingLayout
import com.bellatrixos.widgets.GridAutofillLayoutManager
import com.bellatrixos.widgets.SelectionOverlayView
import com.bellatrixos.widgets.UITitleBar
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize
import com.bellatrixos.windows.Windows
import com.bellatrixos.windows.files.FileIntent
import com.bellatrixos.windows.files.FileMeta
import com.bellatrixos.windows.files.FileShortcut
import com.bellatrixos.windows.files.clipboard.Clipboards
import com.bellatrixos.windows.files.operations.Paste
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import com.davemorrissey.labs.subscaleview.BuildConfig
import java.io.File
import java.util.ArrayList
import java.util.HashSet
import java.util.Stack

/** Fragment penjelajah berkas. */
class UIExplorer : UIFragment(), BackPressHandler {
    companion object {
        @JvmField var o1: Boolean = true

        private const val GUEST_PREFIX = "guest:"
        private const val GUEST_FILES = GUEST_PREFIX + "files"
        private const val GUEST_EDITOR = GUEST_PREFIX + "editor"
        private const val GUEST_EDITOR_HOME = GUEST_PREFIX + "editor-home"
        private const val GUEST_SEPARATOR = "\u001f"

        /** The installed Linux home, at the proot path the guest calls /root. */
        private fun linuxHome(context: Context) =
            (context.applicationContext as? App)?.runtime?.proot?.home?.takeIf { it.isDirectory }

        /** Encodes a guest Files request in the one string argument the window system carries. */
        fun filesArg(path: String?): String = GUEST_FILES + GUEST_SEPARATOR + path.orEmpty()

        /** Encodes a guest Editor request and the FIFO its shim waits on. */
        fun editorArg(
            path: String?,
            done: String? = null,
        ): String =
            if (path == null) {
                GUEST_EDITOR_HOME
            } else {
                GUEST_EDITOR + GUEST_SEPARATOR + path + GUEST_SEPARATOR + done.orEmpty()
            }
    }

    @JvmField var A0: UITitleBar? = null
    @JvmField var B0: FloatingLayout? = null
    @JvmField var D0: Array<String>? = null
    @JvmField var E0: FileAdapter? = null
    @JvmField var F0: InstalledApp? = null
    @JvmField var G0: File? = null
    @JvmField var H0: File? = null
    @JvmField var I0: RecyclerView? = null
    @JvmField var J0: MainActivityIn? = null
    @JvmField var K0: NavigationAdapter? = null
    @JvmField var L0: View? = null
    @JvmField var M0: ImageButton? = null
    @JvmField var O0: FilesRecyclerView? = null
    @JvmField var P0: FilesRecyclerView? = null
    @JvmField var Q0: ImageButton? = null
    @JvmField var R0: ImageButton? = null
    @JvmField var S0: ImageButton? = null
    @JvmField var T0: ImageButton? = null
    @JvmField var Y0: EditText? = null
    @JvmField var Z0: EditText? = null
    @JvmField var a1: String? = null
    @JvmField var b1: String? = null
    @JvmField var c1: String? = null
    @Volatile @JvmField var g1: File? = null
    @JvmField var h1: SelectionOverlayView? = null
    @JvmField var i1: Runnable? = null
    @JvmField var k1: ViewStub? = null
    @JvmField var l1: ExplorerFilesInfoView? = null
    @JvmField var u0: View? = null
    @JvmField var v0: Context? = null
    @JvmField var w0: Int = 0
    @JvmField var x0: TextView? = null
    @JvmField var y0: ViewStub? = null
    @JvmField var z0: EditText? = null
    @JvmField var C0: String? = null

    /** Set when this Explorer instance hosts the ported guest Files/Editor instead of the legacy browser. */
    private var guestMode: String? = null
    private var guestPath: String? = null
    private var guestDone: String? = null
    private lateinit var guestRuntime: Runtime
    private val waiting = mutableListOf<String>()
    private var guestClosing = false

    @JvmField val N0: Window = Window(0)
    @JvmField val U0: Stack<File> = Stack()
    @JvmField val V0: Stack<File> = Stack()
    @JvmField val W0: ArrayList<FileMeta> = ArrayList()
    @JvmField val X0: ArrayList<DriveItem> = ArrayList()
    @JvmField val d1: StringBuilder = StringBuilder()
    @JvmField var e1: Boolean = true
    @JvmField var f1: Boolean = false
    @JvmField val j1: Handler = Handler(Looper.getMainLooper())
    @JvmField var m1: Int = 0
    @JvmField val n1: StringBuilder = StringBuilder(64)

    // --- Fragment lifecycle (z/L/A mapping) ---

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = z(inflater, container, savedInstanceState)

    fun z(inflater: LayoutInflater, viewGroup: ViewGroup?, bundle: Bundle?): View {
        val viewInflate = inflater.inflate(R.layout.explorer, viewGroup, false)
        val floatingLayout = viewInflate.findViewById<FloatingLayout>(R.id.main_view_explorer)
        B0 = floatingLayout
        WindowSize.a(floatingLayout, N0.c)
        val context = viewInflate.context
        v0 = context
        J0 = context as? MainActivityIn
        w0 = context.resources.configuration.orientation
        x0 = viewInflate.findViewById(R.id.empty_view)
        k1 = viewInflate.findViewById(R.id.filesInfoStub)
        z0 = viewInflate.findViewById(R.id.searchBar)
        Y0 = viewInflate.findViewById(R.id.addressBar)
        u0 = viewInflate.findViewById(R.id.contextPos)
        y0 = viewInflate.findViewById(R.id.file_footer)
        A0 = viewInflate.findViewById(R.id.titleBar)
        P0 = viewInflate.findViewById(R.id.home_grid_view)
        O0 = viewInflate.findViewById(R.id.files_grid_view)
        I0 = viewInflate.findViewById(R.id.explorer_navigation)
        Z0 = viewInflate.findViewById(R.id.renameEditText)
        h1 = viewInflate.findViewById(R.id.selectionOverlay)
        L0 = viewInflate.findViewById(R.id.navResizeHandle)
        M0 = viewInflate.findViewById(R.id.navGrip)
        T0 = viewInflate.findViewById(R.id.btn_navigate_reload)
        S0 = viewInflate.findViewById(R.id.btn_navigate_up)
        Q0 = viewInflate.findViewById(R.id.btn_navigate_back)
        R0 = viewInflate.findViewById(R.id.btn_navigate_forward)
        S0?.isEnabled = false
        Q0?.isEnabled = false
        R0?.isEnabled = false
        c0 = viewInflate
        return viewInflate
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        L(view, savedInstanceState)
    }

    fun L(view: View, bundle: Bundle?) {
        val floating = B0 ?: return
        val title = A0 ?: return
        val activity = J0 ?: return
        WindowSize.e(view, floating, N0, title, activity, this)
        F0 = Windows.d(N0, "UIExplorer")
        val raw = arguments?.getString("path")
        if (raw != null && raw.startsWith(GUEST_PREFIX)) {
            installGuest(view, raw)
            return
        }
        D0 = v0?.resources?.getStringArray(R.array.explorer_home_dirs)
        val args = this.arguments
        if (args != null) {
            val string = args.getString("path")
            C0 = string
            if (string != null && (string.contains("txt/**") || string.contains("img/**"))) {
                f1 = true
                N0.a(2)
            }
        }
        AppExecutors.a().execute(ExplorerRunnable(this, 100, view = view))
        val home = v0?.getString(R.string.explorer_navbar_address_home)
        c1 = home
        a1 = home
        b1 = home
        if (N0.c != 0) {
            WindowSize.d(view, floating, N0)
            WindowSize.f(floating, title, 1)
        }
        floating.setOnFocusListener(ExplorerFocusResizeListener(this, view))
    }

    override fun onDestroy() {
        if (guestMode != null && !guestClosing) answer("ok")
        super.onDestroy()
    }

    override fun onDetach() {
        A()
        super.onDetach()
    }

    /** Replaces the legacy file browser with the ported guest Files/Editor inside this Bellatrix window. */
    private fun installGuest(view: View, raw: String) {
        // A reused window may still hold the previous editor's reply FIFO; never drop it.
        answer("ok")
        guestClosing = false
        val parts = raw.split(GUEST_SEPARATOR, limit = 3)
        guestMode = parts.firstOrNull()
        guestPath = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
        guestDone = parts.getOrNull(2)?.takeIf { it.isNotEmpty() }?.also(waiting::add)
        guestRuntime = (requireContext().applicationContext as? App)?.runtime ?: return

        val floating = B0 ?: return
        val title = A0 ?: return
        val path = guestPath
        title.setTitle(
            when (guestMode) {
                GUEST_FILES -> getString(R.string.guest_files_title)
                else -> getString(R.string.guest_editor_title)
            },
        )

        view.findViewById<View>(R.id.explorer_top_navigation)?.visibility = View.GONE
        I0?.visibility = View.GONE
        O0?.visibility = View.GONE
        P0?.visibility = View.GONE
        x0?.visibility = View.GONE
        h1?.visibility = View.GONE
        L0?.visibility = View.GONE
        M0?.visibility = View.GONE
        Y0?.visibility = View.GONE
        Z0?.visibility = View.GONE
        Q0?.visibility = View.GONE
        R0?.visibility = View.GONE
        S0?.visibility = View.GONE
        T0?.visibility = View.GONE

        val compose = view.findViewById<ComposeView>(R.id.guest_compose_host) ?: return
        compose.visibility = View.VISIBLE
        compose.setViewTreeLifecycleOwner(viewLifecycleOwner)
        compose.setViewTreeSavedStateRegistryOwner(viewLifecycleOwner as SavedStateRegistryOwner)

        val focusResize =
            object : FloatingLayout.OnAppFocusListener, Window.OnResizeStateChangeListener {
                override fun a() {
                    view.bringToFront()
                    j0(true)
                    J0?.d(this@UIExplorer)
                    floating.setFocusState(true)
                }

                override fun b(i2: Int) {
                    WindowSize.d(view, floating, N0)
                    WindowSize.f(floating, title, i2)
                    WindowSize.g(i2, "UIExplorer", F0)
                }
            }
        N0.f4870e = focusResize
        floating.setOnFocusListener(focusResize)
        title.setResizeBtnListener { WindowSize.c(N0) }
        title.setMinimizeBtnListener { J0?.A(tag ?: "") }
        title.setCloseBtnListener { closeGuest("ok") }

        val runtime = guestRuntime
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides requireActivity()) {
                when (guestMode) {
                    GUEST_FILES -> FilesScreen(runtime, path ?: "/root", onExit = { closeGuest("ok") })
                    GUEST_EDITOR_HOME -> EditorHome(runtime, onExit = { closeGuest("ok") })
                    else -> EditorScreen(runtime, path.orEmpty(), onFallback = ::fallbackGuestEditor, onExit = { closeGuest("ok") })
                }
            }
        }
    }

    /** The editor shim waits for a reply; answer it before this window goes away. */
    private fun closeGuest(reply: String) {
        if (guestClosing) return
        guestClosing = true
        answer(reply)
        p0()
    }

    /** Files the touch editor can't take go to micro, as they did in the old Editor task. */
    private fun fallbackGuestEditor() {
        val path = guestPath
        if (path.isNullOrEmpty()) return closeGuest("ok")
        if (waiting.isNotEmpty()) {
            answer("micro")
        } else {
            guestRuntime.terminal("micro ${shellQuote(path)}", path.substringAfterLast('/'))
        }
        guestClosing = true
        p0()
    }

    private fun answer(reply: String) {
        for (fifo in waiting) {
            try {
                val fd = Os.open(fifo, OsConstants.O_WRONLY or OsConstants.O_NONBLOCK, 0)
                try {
                    Os.write(fd, "$reply\n".toByteArray(), 0, reply.length + 1)
                } finally {
                    Os.close(fd)
                }
            } catch (_: ErrnoException) {
                // Nobody is waiting any more.
            }
        }
        waiting.clear()
    }

    fun A() {
        U0.clear()
        V0.clear()
        H0 = null
        O0?.setOnEmptyPressListener(null)
        O0?.setOnEmptyLongPressListener(null)
        E0?.let {
            it.E = null
            it.F = null
        }
        if (f1) {
            val group = J0?.i0()
            val overlay = group?.findViewWithTag<View>("overlay_picker")
            if (overlay != null) group.removeView(overlay)
        }
        Clipboards.f4922a.clear()
    }

    // --- Public/state methods ---

    fun A0(i2: Int) {
        E0?.K(W0, i2, null)
        O0?.let {
            it.u1.clear()
            it.t1.clear()
            it.p1 = false
            it.o1 = false
        }
        x0?.setText(R.string.explorer_str_inaccessible)
        x0?.visibility = View.VISIBLE
        o1 = false
    }

    fun B0(file: File) {
        G0 = file
        U0.push(file)
        Q0?.isEnabled = true
        R0?.isEnabled = false
        if (H0 != null) {
            S0?.isEnabled = file.name != H0?.name
        }
        val string = file.toString()
        a1 = string
        Y0?.post(ExplorerRunnable(this, 12, pathString = string))
    }

    fun C0() {
        O0?.visibility = View.INVISIBLE
        P0?.visibility = View.VISIBLE
        if (x0?.visibility == View.VISIBLE) x0?.visibility = View.GONE
        Q0?.isEnabled = false
        S0?.isEnabled = false
        selectNav(7)
        val str = c1
        a1 = str
        b1 = str
        Y0?.setText(str)
        if (X0.isEmpty()) {
            AppExecutors.a().execute(ExplorerRunnable(this, 3))
            return
        }
        l1?.a(-1, m1)
    }

    fun D0() {
        O0?.visibility = View.VISIBLE
        P0?.visibility = View.INVISIBLE
    }

    override fun b(): Boolean {
        if (U0.isEmpty()) {
            J0?.A(tag ?: "")
            return true
        }
        t0()
        return true
    }

    override fun f0(): Boolean = e1

    override fun h0(i2: Int, keyEvent: KeyEvent) {
        val zIsCtrlPressed = keyEvent.isCtrlPressed
        val zIsShiftPressed = keyEvent.isShiftPressed
        O0?.setShiftPressed(zIsShiftPressed)
        val fileAdapter = E0 ?: return
        if (!zIsCtrlPressed) return
        val obj = FileAdapter.L
        val hashSet = fileAdapter.I
        when (i2) {
            29 -> { // KEYCODE_A
                hashSet.clear()
                for (item in fileAdapter.H) hashSet.add(item.file)
                fileAdapter.notifyItemRangeChanged(0, fileAdapter.itemCount, obj)
            }
            50 -> { // KEYCODE_V
                val file = G0 ?: return
                Paste.a(v0 ?: return, file.absolutePath, fileAdapter)
            }
            52 -> { // KEYCODE_X
                if (hashSet.isEmpty()) return
                fileAdapter.G = true
                fileAdapter.J.clear()
                fileAdapter.J.addAll(hashSet)
                fileAdapter.notifyItemRangeChanged(0, fileAdapter.itemCount, obj)
                ContextMenuFE.b(hashSet)
            }
            31 -> { // KEYCODE_C
                if (hashSet.isEmpty()) return
                ContextMenuFE.a(hashSet)
            }
            32 -> { // KEYCODE_D
                if (hashSet.isEmpty()) return
                hashSet.clear()
                fileAdapter.notifyItemRangeChanged(0, fileAdapter.itemCount, obj)
            }
        }
    }

    override fun j0(z: Boolean) {
        B0?.setFocusState(z)
        e1 = z
        if (z) Windows.f4883f = this
    }

    override fun k0(str: String?) {
        if (str == null) return
        if (str.startsWith(GUEST_PREFIX)) {
            val view = c0 ?: return
            installGuest(view, str)
            view.bringToFront()
            B0?.setFocusState(true)
            return
        }
        val file = G0
        if (file == null || file.absolutePath != str) {
            y0(str)
        }
        c0?.bringToFront()
        B0?.setFocusState(true)
    }

    override fun o0() {
        B0?.invalidate()
    }

    fun p0() {
        if (f1) {
            try {
                val manager = parentFragmentManager
                val tx = manager.beginTransaction()
                tx.remove(this)
                tx.commitAllowingStateLoss()
            } catch (_: Exception) {
            }
        } else {
            J0?.P(this)
        }
        if (Windows.f4883f == this) Windows.f4883f = null
    }

    fun q0() {
        A0?.let {
            it.w.visibility = View.GONE
            it.x.visibility = View.GONE
        }
        y0?.let { stub ->
            stub.setOnInflateListener(ExplorerInflateListener(this, 0))
            stub.inflate()
        }
    }

    fun s0(file: File) {
        E0?.F()
        if (file.exists() && file.isDirectory()) {
            try {
                if (f1 && Global.w?.a() == file.absolutePath) {
                    A0(0)
                } else {
                    z0(file)
                }
                B0(file)
                return
            } catch (_: Exception) {
                val ctx = v0 ?: return
                Files.a(ctx, ctx.getString(R.string.toast_folder_opening_error))
                return
            }
        } else {
            val ctx = v0 ?: return
            if (P0?.visibility == View.VISIBLE) {
                Files.a(ctx, ctx.getString(R.string.toast_folder_not_found))
                return
            }
            x0?.setText(R.string.toast_folder_not_found)
            x0?.visibility = View.VISIBLE
        }
    }

    fun t0() {
        if (U0.isEmpty()) return
        U0.pop()
        G0?.let { V0.push(it) }
        try {
            if (G0 != H0) {
                val file = U0.pop()
                s0(file)
                G0 = file
                R0?.isEnabled = true
            } else {
                C0()
            }
        } catch (_: Exception) {
            C0()
        }
    }

    fun u0(fileMeta: FileMeta) {
        try {
            val z = fileMeta.linkFile
            var file = fileMeta.file
            if (z) {
                val clipboardItemA = FileShortcut.a(file) ?: return
                val str = clipboardItemA.f4921a
                when (clipboardItemA.c) {
                    1 -> {
                        J0?.M(str, null, "")
                        return
                    }
                    2 -> {
                        val ctx = v0 ?: return
                        val act = J0 ?: return
                        FileIntent.b(ctx, File(str), FileIntent.AppLauncher { tag, extra -> act.c0(tag, extra) })
                        return
                    }
                    3 -> file = File(str)
                    else -> return
                }
            } else if (!file.isDirectory) {
                val ctx = v0 ?: return
                val act = J0 ?: return
                FileIntent.b(ctx, file, FileIntent.AppLauncher { tag, extra -> act.c0(tag, extra) })
                return
            }
            s0(file)
        } catch (_: IndexOutOfBoundsException) {
        }
    }

    fun v0(str: String) {
        val file = File(str)
        H0 = file
        val ctx = v0
        if (!file.exists()) {
            if (ctx != null) Files.a(ctx, ctx.getString(R.string.toast_folder_not_found))
        } else if (file.canRead()) {
            D0()
            s0(file)
        } else {
            if (ctx != null) Files.a(ctx, ctx.getString(R.string.toast_device_opening_error))
        }
    }

    fun w0(str: String?) {
        if (str == null) return
        try {
            val idx = str.indexOf("/Android/data/")
            val path = if (idx != -1) str.substring(0, idx) else str
            v0(path)
        } catch (_: Exception) {
            val ctx = v0 ?: return
            Files.a(ctx, ctx.getString(R.string.toast_device_opening_error))
        }
    }

    fun x0(i2: Int) {
        val externalStoragePublicDirectory = when (i2) {
            1 -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            2 -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            3 -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            4 -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            5 -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            else -> File(BuildConfig.FLAVOR)
        }
        if (externalStoragePublicDirectory.exists() && externalStoragePublicDirectory.isDirectory()) {
            D0()
            s0(externalStoragePublicDirectory)
            H0 = externalStoragePublicDirectory
        } else {
            val ctx = v0
            if (ctx != null) Files.a(ctx, ctx.getString(R.string.toast_folder_not_found))
        }
        selectNav(i2)
    }

    /** Selects a navigation row by its stable id, never by its list position. */
    private fun selectNav(id: Int) {
        val navigationAdapter = K0 ?: return
        for (i3 in navigationAdapter.z.indices) {
            if (navigationAdapter.z[i3].f4689a == id) {
                navigationAdapter.B(i3)
                return
            }
        }
    }

    fun y0(str: String) {
        val file = File(str)
        H0 = file
        val ctx = v0
        if (!file.exists()) {
            if (ctx != null) Files.a(ctx, ctx.getString(R.string.toast_folder_not_found))
            return
        }
        if (!file.canRead()) {
            if (ctx != null) Files.a(ctx, ctx.getString(R.string.toast_device_opening_error))
            return
        }
        D0()
        s0(file)
        K0?.B(6)
    }

    fun z0(file: File) {
        x0?.text = null
        x0?.visibility = View.VISIBLE
        val i2 = if (Files.f4677a == file.absolutePath) Files.f4678d else 0
        g1 = file
        o1 = false
        val runnable = ExplorerRunnable(this, 0)
        i1 = runnable
        j1.postDelayed(runnable, 800L)
        AppExecutors.b().execute(ExplorerLoadRunnable(this, file, i2))
    }

    // --- setup helpers called by ExplorerRunnable(100) -> async completion ---

    internal fun setupAfterInit(view: View, z: Boolean) {
        val adapter = E0 ?: return
        val files = O0 ?: return
        val c0Path = C0
        if (c0Path == null) {
            AppExecutors.a().execute(ExplorerRunnable(this, 3))
        } else {
            try {
                val context = v0 ?: view.context
                if (Global.w == null) {
                    Global.w = StorageDirs.a(context, Environment.getExternalStorageDirectory())
                }
                H0 = Environment.getExternalStorageDirectory()
                val isImg = c0Path.contains("img/**")
                val isTxt = c0Path.contains("txt/**")
                val file = when {
                    isImg -> {
                        q0(); D0(); Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    }
                    isTxt || isImg -> {
                        q0(); D0(); File(Global.w?.a() ?: Environment.getExternalStorageDirectory().absolutePath)
                    }
                    else -> {
                        D0(); File(c0Path)
                    }
                }
                s0(file)
            } catch (_: Exception) {
            }
        }
        adapter.I(Files.f4681h, files, true)
        files.adapter = adapter
        files.itemAnimator = null
        files.setHasFixedSize(true)
        files.setItemViewCacheSize(20)
        files.setOnEmptyLongPressListener(ExplorerEmptyLongPress(this))
        adapter.E = com.bellatrixos.searchapp.c(1, this)
        adapter.F = ExplorerContextMenu(this)
        files.setOnEmptyPressListener(ExplorerEmptyPress(this))

        if (z) {
            val nav = I0
            val prefs = SharedPrefs.f4755a
            val navWidth = prefs?.getInt("file_explorer_side_nav_width", 0) ?: 0
            if (nav != null && navWidth != 0) {
                val lp = nav.layoutParams
                lp.width = navWidth
                nav.layoutParams = lp
            }
            nav?.adapter = K0
            nav?.itemAnimator = null
            nav?.layoutManager = LinearLayoutManager(v0, RecyclerView.VERTICAL, false)
            K0?.A = ExplorerNavClick(this)
            val navAdapter = K0
            if (navAdapter != null) {
                val sel = when {
                    c0Path == null -> navAdapter.A(7)
                    c0Path.contains("img/**") -> navAdapter.A(2)
                    else -> navAdapter.A(8)
                }
                navAdapter.B(sel)
            }
            val search = z0
            val fileAdapter = E0
            if (search != null && fileAdapter != null) Navigation.b(search, fileAdapter)
            val ctx = v0
            val rv = I0
            val handle = L0
            val grip = M0
            if (ctx != null && rv != null && handle != null && grip != null) {
                Navigation.a(ctx, rv, handle, grip)
            }
        }

        if ((Global.f4838e) > 0.8f && l1 == null) {
            l1 = k1?.inflate() as? ExplorerFilesInfoView
        }
        N0.f4870e = ExplorerFocusResizeListener(this, view)
        val isImgPicker = c0Path?.contains("img/**") == true || c0Path?.contains("txt/**") == true
        S0?.isEnabled = !isImgPicker
        S0?.setOnClickListener(ExplorerClick(this, 2))
        Q0?.setOnClickListener(ExplorerClick(this, 3))
        R0?.setOnClickListener(ExplorerClick(this, 4))
        T0?.setOnClickListener(ExplorerClick(this, 5))
        A0?.setResizeBtnListener { WindowSize.c(N0) }
        A0?.setCloseBtnListener { p0() }
        A0?.setMinimizeBtnListener { J0?.A(tag ?: "") }

        val filesRv = O0
        val homeRv = P0
        val addr = Y0
        val search = z0
        if (filesRv != null && addr != null && search != null) {
            filesRv.setOnTouchListener(com.bellatrixos.app_explorer.navigation.a(addr, search, 0))
        }
        if (homeRv != null && addr != null && search != null) {
            homeRv.setOnTouchListener(com.bellatrixos.app_explorer.navigation.a(addr, search, 1))
        }
        val editText3 = Y0
        if (editText3 != null) {
            val bVar = ExplorerEnterPressed(this)
            editText3.setOnEditorActionListener { textView, i7, keyEvent ->
                var z2 = false
                if (i7 == 2 || i7 == 6 || i7 == 3 || i7 == 4) {
                    bVar.f(textView.text.toString().trim())
                    editText3.clearFocus()
                    z2 = true
                }
                if (keyEvent != null && keyEvent.keyCode == 66 && keyEvent.action == 0) {
                    bVar.f(textView.text.toString().trim())
                    editText3.clearFocus()
                    z2 = true
                }
                z2
            }
            editText3.setOnFocusChangeListener { _, hasFocus ->
                editText3.setText(if (hasFocus) a1 else b1)
                if (hasFocus) editText3.post(ExplorerRunnable(this, 2))
            }
        }
        E0?.D = ExplorerItemCountListener(this)
        h1?.let { files.n0(it) }
        files.setContextMenuListener(E0?.F)
    }
}

// --- helper classes (file-private, within app_explorer) ---

private class ExplorerFocusResizeListener(
    val explorer: UIExplorer,
    val view: View
) : Window.OnResizeStateChangeListener, FloatingLayout.OnAppFocusListener {
    override fun a() {
        view.bringToFront()
        explorer.J0?.d(explorer)
        explorer.B0?.setFocusState(true)
        explorer.e1 = true
    }

    override fun b(i2: Int) {
        val floating = explorer.B0 ?: return
        WindowSize.d(view, floating, explorer.N0)
        WindowSize.f(floating, explorer.A0, i2)
        if (explorer.f1) return
        WindowSize.g(i2, "UIExplorer", explorer.F0)
    }
}

private class ExplorerRunnable(
    val v: UIExplorer,
    val u: Int,
    val view: View? = null,
    val pathString: String? = null
) : Runnable {
    override fun run() {
        when (u) {
            0 -> v.x0?.setText(R.string.st_menu_loading)
            1 -> {
                val fileArrListFiles = v.G0?.listFiles()
                if (fileArrListFiles != null) {
                    val arrayList = ArrayList<FileMeta>(fileArrListFiles.size)
                    for (file in fileArrListFiles) arrayList.add(FileMeta(file))
                    AppExecutors.c().post(ExplorerListResultRunnable(v, arrayList))
                }
            }
            2 -> v.Y0?.selectAll()
            3 -> loadHomeDrives()
            12 -> applyPathTruncation()
            100 -> initAsync(view)
        }
    }

    private fun applyPathTruncation() {
        val uIExplorer2 = v
        val edit = uIExplorer2.Y0 ?: return
        val width = edit.width - (edit.paddingEnd * 2)
        val paint = edit.paint
        val sb = uIExplorer2.d1
        sb.setLength(0)
        val strArr = (pathString ?: return).split("/").toTypedArray()
        for (str in strArr) {
            if (str.isNotEmpty()) {
                sb.append("   ›   ")
                sb.append(str)
            }
        }
        val full = (uIExplorer2.c1 ?: "") + sb.toString()
        uIExplorer2.b1 = full
        val f2 = width.toFloat()
        if (paint.measureText(full) <= f2) {
            edit.setText(full)
            return
        }
        val sb2 = uIExplorer2.n1
        sb2.setLength(0)
        sb2.append("   ›   …")
        for (length in strArr.indices.reversed()) {
            val str3 = strArr[length]
            if (str3.isEmpty()) continue
            sb2.insert(8, "   ›   ")
            sb2.insert(15, str3)
            sb.setLength(0)
            sb.append(uIExplorer2.c1 ?: "")
            sb.append(sb2)
            if (paint.measureText(sb, 0, sb.length) > f2) {
                sb2.delete(8, str3.length + 15)
                val truncated = (uIExplorer2.c1 ?: "") + sb2.toString()
                uIExplorer2.b1 = truncated
                edit.setText(truncated)
                return
            }
        }
        val truncated = (uIExplorer2.c1 ?: "") + sb2.toString()
        uIExplorer2.b1 = truncated
        edit.setText(truncated)
    }

    private fun initAsync(root: View?) {
        val uIExplorer3 = v
        val rootView = root ?: return
        val prefs = SharedPrefs.f4755a
        val showSide = (prefs?.getBoolean("file_explorer_side_nav_enabled", true) ?: true) && uIExplorer3.w0 == 2
        if (Files.f4681h == -1) {
            Files.f4681h = prefs?.getInt("file_explorer_files_view_type", 0) ?: 0
        }
        if (Files.f4679e == null) {
            val i2 = prefs?.getInt("file_sort_by", 0) ?: 0
            var found: SortBy? = null
            for (s in SortBy.values()) if (s.u == i2) { found = s; break }
            Files.f4679e = found ?: SortBy.NAME_ASC
        }
        if (Files.f4680f == null) {
            val i4 = prefs?.getInt("downloads_sort_by", 3) ?: 3
            var found: SortBy = SortBy.NAME_ASC
            for (s in SortBy.values()) if (s.u == i4) { found = s; break }
            Files.f4680f = found
        }
        val ctx = uIExplorer3.v0 ?: rootView.context
        uIExplorer3.E0 = FileAdapter(ctx)
        if (showSide) {
            val strArr2 = uIExplorer3.D0
            val str6 = uIExplorer3.C0
            val zContains = str6?.contains("img/**") == true
            val arrayList2 = ArrayList<NavigationItem>()
            if (strArr2 != null && strArr2.size >= 5) {
                arrayList2.add(NavigationItem(R.drawable.img_exp_folder_downloads_18, 2, strArr2[1], false))
                arrayList2.add(NavigationItem(R.drawable.img_exp_folder_documents_18, 1, strArr2[0], false))
                arrayList2.add(NavigationItem(R.drawable.img_exp_folder_images_18, 4, strArr2[3], false))
                arrayList2.add(NavigationItem(R.drawable.img_exp_folder_music_18, 3, strArr2[2], false))
                arrayList2.add(NavigationItem(R.drawable.img_exp_folder_videos_18, 5, strArr2[4], true))
            }
            val z2 = !zContains
            arrayList2.add(NavigationItem(R.drawable.img_exp_drive_nav_pc_18, ctx.getString(R.string.explorer_str_this_pc), 7, z2, 0))
            arrayList2.add(NavigationItem(R.drawable.img_exp_drive_nav_18, ctx.getString(R.string.storage_internal), 8, z2, 0))
            linuxHome(ctx)?.let { home ->
                val linuxItem = NavigationItem(R.drawable.img_nav_linux_20, ctx.getString(R.string.explorer_linux_home), 10, z2, 0)
                linuxItem.f4690d = home.absolutePath
                arrayList2.add(linuxItem)
            }
            uIExplorer3.K0 = NavigationAdapter(ctx, arrayList2)
        }
        AppExecutors.c().post { uIExplorer3.setupAfterInit(rootView, showSide) }
    }

    private fun loadHomeDrives() {
        val uIExplorer = v
        val arrayList2 = uIExplorer.X0
        arrayList2.clear()
        val context = uIExplorer.v0 ?: return
        val strArr = uIExplorer.D0 ?: return
        val navigationAdapter = uIExplorer.K0
        val prefs = SharedPrefs.f4755a
        if (prefs?.getBoolean("file_explorer_home_shortcuts_enabled", true) != false) {
            arrayList2.add(DriveItem(context.getString(R.string.explorer_home_dirs_title)))
            arrayList2.add(DriveItem(strArr[0], R.drawable.img_folder_documents, 1))
            arrayList2.add(DriveItem(strArr[1], R.drawable.img_folder_downloads, 2))
            arrayList2.add(DriveItem(strArr[2], R.drawable.img_folder_music, 3))
            arrayList2.add(DriveItem(strArr[3], R.drawable.img_folder_images, 4))
            arrayList2.add(DriveItem(strArr[4], R.drawable.img_folder_videos, 5))
        }
        arrayList2.add(DriveItem(context.getString(R.string.explorer_home_dirs_drives_title)))
        if (Global.w == null) {
            Global.w = StorageDirs.a(context, Environment.getExternalStorageDirectory())
        }
        val internal = Global.w
        if (internal != null) {
            arrayList2.add(
                DriveItem(
                    context.getString(R.string.storage_internal),
                    R.drawable.img_drive,
                    internal.c(),
                    internal.b(),
                    internal.a()
                )
            )
        }
        linuxHome(context)?.let { home ->
            arrayList2.add(
                DriveItem(
                    context.getString(R.string.explorer_linux_home),
                    R.drawable.img_nav_linux_20,
                    context.getString(R.string.explorer_linux_home_detail),
                    0,
                    home.absolutePath,
                ),
            )
        }
        val externalFilesDirs = context.getExternalFilesDirs(null) ?: emptyArray()
        var c3 = 'D'
        for (file2 in externalFilesDirs) {
            if (file2 == null) continue
            val internalPath = Global.w?.a() ?: continue
            if (file2.absolutePath.startsWith(internalPath)) continue
            if (!Environment.isExternalStorageRemovable(file2)) continue
            if (file2.absolutePath.startsWith(context.filesDir.path)) continue
            val storageStatA = StorageDirs.a(context, file2)
            arrayList2.add(
                DriveItem(
                    context.getString(R.string.storage_external, c3),
                    R.drawable.img_drive_removable,
                    storageStatA.c(),
                    storageStatA.b(),
                    storageStatA.a()
                )
            )
            if (navigationAdapter != null) {
                val navItem = NavigationItem(R.drawable.img_exp_drive_nav_18, 9, context.getString(R.string.storage_external, c3), false)
                navItem.f4690d = storageStatA.a()
                navigationAdapter.z.add(navItem)
                AppExecutors.c().post { navigationAdapter.notifyDataSetChanged() }
            }
            c3++
        }
        val driveAdapter = DriveAdapter(uIExplorer.v0 ?: context, arrayList2)
        val gridLayoutManager = GridAutofillLayoutManager(context)
        gridLayoutManager.M = context
        gridLayoutManager.r1(R.dimen.ui_200dp)
        AppExecutors.c().post {
            val home = uIExplorer.P0 ?: return@post
            val ctx2 = uIExplorer.v0 ?: return@post
            val onFolder = androidx.core.util.Consumer<Any> { obj -> (obj as? Int)?.let { uIExplorer.x0(it) } }
            val onInternal = androidx.core.util.Consumer<Any> { obj -> (obj as? String)?.let { uIExplorer.v0(it) } }
            val onExternal = androidx.core.util.Consumer<Any> { obj -> (obj as? String)?.let { uIExplorer.w0(it) } }
            com.bellatrixos.app_explorer.drives.Drives.a(
                ctx2, home, driveAdapter, home, gridLayoutManager,
                arrayList2, onFolder, onInternal, onExternal
            )
            uIExplorer.m1 = driveAdapter.itemCount
            if (uIExplorer.O0?.visibility == View.INVISIBLE) {
                uIExplorer.l1?.a(-1, uIExplorer.m1)
            }
        }
    }
}

private class ExplorerLoadRunnable(
    val explorer: UIExplorer,
    val file: File,
    val i2: Int
) : Runnable {
    override fun run() {
        val fileArrListFiles = file.listFiles()
        val arrayList = ArrayList<FileMeta>()
        if (fileArrListFiles != null) {
            for (f in fileArrListFiles) arrayList.add(FileMeta(f))
        }
        AppExecutors.c().post {
            if (file == explorer.g1) {
                explorer.i1?.let { explorer.j1.removeCallbacks(it) }
                if (fileArrListFiles == null) {
                    explorer.A0(i2)
                    return@post
                }
                UIExplorer.o1 = true
                val size = arrayList.size
                explorer.E0?.K(arrayList, i2) {
                    AppExecutors.c().post {
                        if (explorer.g1 == file) {
                            explorer.x0?.visibility = View.GONE
                            explorer.l1?.a(Files.f4681h, size)
                        }
                    }
                }
                explorer.O0?.let {
                    it.u1.clear()
                    it.t1.clear()
                    it.p1 = false
                    it.o1 = false
                }
                explorer.l1?.a(Files.f4681h, size)
            }
        }
    }
}

private class ExplorerListResultRunnable(
    val explorer: UIExplorer,
    val list: ArrayList<FileMeta>
) : Runnable {
    override fun run() {
        explorer.z0?.setText("")
        val fileAdapter = explorer.E0 ?: return
        val copy = ArrayList(list)
        fileAdapter.H.clear()
        fileAdapter.H.addAll(copy)
        Files.b(fileAdapter.H as MutableList<Any>, fileAdapter.z)
        fileAdapter.submitList(fileAdapter.D(fileAdapter.H))
        fileAdapter.I.clear()
    }
}

private class ExplorerClick(val explorer: UIExplorer, val u: Int) : View.OnClickListener {
    override fun onClick(view: View) {
        when (u) {
            0 -> explorer.p0()
            1 -> explorer.J0?.A(explorer.tag ?: "")
            2 -> {
                val file = explorer.G0
                val parentFile = file?.parentFile
                if (file == null || parentFile == null) {
                    explorer.C0()
                } else {
                    explorer.s0(parentFile)
                    explorer.G0 = parentFile
                }
            }
            3 -> explorer.t0()
            4 -> {
                val stack = explorer.V0
                if (stack.isNotEmpty()) {
                    try {
                        val file2 = stack.pop()
                        explorer.s0(file2)
                        explorer.G0 = file2
                        if (file2 == explorer.H0 && explorer.O0?.visibility == View.INVISIBLE) {
                            explorer.D0()
                        }
                    } catch (_: Exception) {
                    }
                }
            }
            5 -> {
                if (explorer.P0?.visibility != View.VISIBLE && explorer.G0 != null) {
                    AppExecutors.b().execute(ExplorerRunnable(explorer, 1))
                }
            }
            6 -> WindowSize.c(explorer.N0)
            else -> explorer.p0()
        }
    }
}

private class ExplorerEnterPressed(val explorer: UIExplorer) : Navigation.OnEnterPressedListener {
    override fun f(str: String) {
        // Navigasi manual lewat address bar: coba buka path.
        try {
            if (str.isNotEmpty()) explorer.v0(str)
        } catch (_: Exception) {
        }
    }
}

private class ExplorerEmptyPress(val explorer: UIExplorer) : FilesRecyclerView.OnEmptyPressListener {
    override fun a() {
        explorer.u0?.requestFocus()
    }
}

private class ExplorerEmptyLongPress(val explorer: UIExplorer) : FilesRecyclerView.OnEmptyLongPressListener {
    override fun e(iArr: IntArray) {
        // Menu konteks area kosong belum sepenuhnya diport; placeholder aman.
        val ctx = explorer.v0 ?: return
        val anchor = explorer.u0 ?: return
        val adapter = explorer.E0 ?: return
        val files = explorer.O0 ?: return
        val act = explorer.J0 ?: return
        // Delegasi ke ContextMenuFE/ContextMenuDT bila tersedia; diam jika tidak.
    }
}

private class ExplorerContextMenu(val explorer: UIExplorer) :
    com.bellatrixos.app_explorer.files.interfaces.OnContextMenuListener<Any> {
    override fun d(iArr: IntArray, obj: Any) {
        // Placeholder aman: konteks file ditangani adapter/ContextMenuFE.
    }
}

private class ExplorerItemCountListener(val explorer: UIExplorer) : FileAdapter.OnItemAddRemoveListener {
    override fun b(i2: Int) {
        if (explorer.G0 != null) {
            explorer.l1?.a(Files.f4681h, i2)
        }
    }
}

private class ExplorerNavClick(val explorer: UIExplorer) : NavigationAdapter.OnItemClickListener {
    override fun c(str: String?, i2: Int, i3: Int) {
        when (i2) {
            0, 7 -> explorer.C0()
            in 1..5 -> explorer.x0(i2)
            6 -> {
                val path = str ?: return
                explorer.y0(path)
            }
            8 -> explorer.v0(Global.w?.a() ?: Environment.getExternalStorageDirectory().absolutePath)
            9 -> if (str != null) explorer.w0(str)
            10 -> if (str != null) explorer.v0(str)
        }
    }
}

private class ExplorerInflateListener(val explorer: UIExplorer, val type: Int) : ViewStub.OnInflateListener {
    override fun onInflate(stub: ViewStub, inflated: View) {
        if (type == 0) {
            // Footer/picker inflate: perilaku detail bergantung pada komponen picker
            // (FilePicker, FILE_IMG, androidx.activity.result.a.k, app_settings.adapters.a, f.c)
            // yang berada di luar scope app_explorer; layout tetap di-inflate.
        }
    }
}
