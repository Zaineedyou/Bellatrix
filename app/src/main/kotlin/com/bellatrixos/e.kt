package com.bellatrixos

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.TextClock
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultCallback
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.app_installer.Installer
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.context_menu.UIMenuItem
import com.bellatrixos.desktop.DesktopAdapter
import com.bellatrixos.desktop.DesktopDB
import com.bellatrixos.desktop.DesktopItem
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.searchapp.OnSettingsItemClickListener
import com.bellatrixos.startmenu.StartMenuAdapter
import com.bellatrixos.startmenu.StartMenuItem
import com.bellatrixos.startmenu.listeners.StartMenuTextWatcher
import com.bellatrixos.startmenu.StartMenuViews
import com.bellatrixos.startmenu.interfaces.OnStartMenuItemClickListener
import com.bellatrixos.taskbar.TaskBarAdapter
import com.bellatrixos.taskbar.center_action.ActionCenter
import com.bellatrixos.taskbar.center_more_icons.DialogMoreIcons
import com.bellatrixos.taskbar.utils.ObservableList
import com.bellatrixos.taskbar.TaskBarItem
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.FilesRecyclerView
import com.bellatrixos.widgets.TaskbarBtn
import com.bellatrixos.widgets.UIProgressCircular
import com.bellatrixos.windows.InsetManager
import com.bellatrixos.windows.dialogs.UIDialog
import com.bellatrixos.windows.dialogs.UIDialogNative
import com.bellatrixos.windows.dialogs.UIDialogProperties
import com.bellatrixos.windows.drivers.Printer
import com.bellatrixos.windows.drivers.battery.Battery
import com.bellatrixos.windows.drivers.battery.BatteryReceiver
import com.bellatrixos.windows.drivers.battery.BatteryState
import com.bellatrixos.windows.drivers.network.NetworkMonitor
import com.bellatrixos.windows.files.FileExtensions
import com.bellatrixos.windows.files.FileIntent
import com.bellatrixos.windows.files.clipboard.ClipboardItem
import com.bellatrixos.windows.files.clipboard.Clipboards
import com.bellatrixos.windows.icons.IconEngine
import com.bellatrixos.windows.installed_apps.AppsDatabase
import com.bellatrixos.windows.installed_apps.InstalledAppsDB
import com.bellatrixos.windows.installed_apps.IntentSites
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.io.File

/**
 * Jembatan untuk field/method MainActivity yang belum ada di port Kotlin.
 * Nama tetap dipakai lewat reflect agar file turunan tetap compile mandiri.
 */
internal object MainActivityBridge {
    @Suppress("UNCHECKED_CAST")
    fun <T> get(target: MainActivity, name: String): T? = runCatching {
        val field = target.javaClass.getField(name)
        field.isAccessible = true
        field.get(target) as? T
    }.getOrNull()

    fun set(target: MainActivity, name: String, value: Any?) {
        runCatching {
            val field = target.javaClass.getField(name)
            field.isAccessible = true
            field.set(target, value)
        }
    }

    fun call(target: MainActivity, name: String, vararg args: Any?) {
        runCatching {
            target.javaClass.methods.firstOrNull {
                it.name == name && it.parameterCount == args.size
            }?.invoke(target, *args)
        }
    }

    fun callField(target: MainActivity, fieldName: String, methodName: String, vararg args: Any?) {
        runCatching {
            val field = target.javaClass.getField(fieldName)
            field.isAccessible = true
            val value = field.get(target)
            if (value != null) {
                value.javaClass.methods.firstOrNull {
                    it.name == methodName && it.parameterCount == args.size
                }?.invoke(value, *args)
            }
        }
    }

    fun outer(obj: Any?): MainActivity? = when (obj) {
        is MainActivity -> obj
        null -> null
        else -> runCatching {
            val field = obj.javaClass.getDeclaredField("this$0")
            field.isAccessible = true
            field.get(obj) as? MainActivity
        }.getOrNull()
    }

    /** MainActivity sudah mengimplementasikan MainActivityIn; tidak perlu proxy reflection. */
    fun asIn(target: MainActivity): MainActivityIn = target

    private fun defaultValue(type: Class<*>): Any? = when (type) {
        java.lang.Boolean.TYPE -> false
        java.lang.Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        java.lang.Float.TYPE -> 0.0f
        java.lang.Double.TYPE -> 0.0
        java.lang.Character.TYPE -> '\u0000'
        java.lang.Byte.TYPE -> 0.toByte()
        java.lang.Short.TYPE -> 0.toShort()
        else -> null
    }
}

internal var MainActivity.W: Context
    get() = MainActivityBridge.get(this, "W") ?: this
    set(value) = MainActivityBridge.set(this, "W", value)

internal var MainActivity.V: Int
    get() = MainActivityBridge.get(this, "V") ?: 0
    set(value) = MainActivityBridge.set(this, "V", value)

internal var MainActivity.Q0: View
    get() = MainActivityBridge.get(this, "Q0") ?: View(this)
    set(value) = MainActivityBridge.set(this, "Q0", value)

internal var MainActivity.G0: TextClock?
    get() = MainActivityBridge.get(this, "G0")
    set(value) = MainActivityBridge.set(this, "G0", value)

internal var MainActivity.F0: ImageView
    get() = MainActivityBridge.get(this, "F0") ?: ImageView(this)
    set(value) = MainActivityBridge.set(this, "F0", value)

internal var MainActivity.k0: NetworkMonitor?
    get() = MainActivityBridge.get(this, "k0")
    set(value) = MainActivityBridge.set(this, "k0", value)

internal var MainActivity.Z: View
    get() = MainActivityBridge.get(this, "Z") ?: View(this)
    set(value) = MainActivityBridge.set(this, "Z", value)

internal var MainActivity.i0: UIContextMenu
    get() = MainActivityBridge.get(this, "i0") ?: UIContextMenu(this)
    set(value) = MainActivityBridge.set(this, "i0", value)

internal var MainActivity.j0: UIContextMenu
    get() = MainActivityBridge.get(this, "j0") ?: UIContextMenu(this)
    set(value) = MainActivityBridge.set(this, "j0", value)

internal var MainActivity.p0: DesktopAdapter
    get() = MainActivityBridge.get(this, "p0") ?: DesktopAdapter(this)
    set(value) = MainActivityBridge.set(this, "p0", value)

internal var MainActivity.q0: FilesRecyclerView
    get() = MainActivityBridge.get(this, "q0") ?: FilesRecyclerView(this)
    set(value) = MainActivityBridge.set(this, "q0", value)

internal var MainActivity.t0: TaskBarAdapter
    get() = MainActivityBridge.get(this, "t0") ?: TaskBarAdapter(this, mutableListOf())
    set(value) = MainActivityBridge.set(this, "t0", value)

internal var MainActivity.C0: ObservableList<TaskBarItem>
    get() = MainActivityBridge.get(this, "C0") ?: ObservableList()
    set(value) = MainActivityBridge.set(this, "C0", value)

internal var MainActivity.u0: RecyclerView
    get() = MainActivityBridge.get(this, "u0") ?: RecyclerView(this)
    set(value) = MainActivityBridge.set(this, "u0", value)

internal var MainActivity.A0: LinearLayout
    get() = MainActivityBridge.get(this, "A0") ?: LinearLayout(this)
    set(value) = MainActivityBridge.set(this, "A0", value)

internal var MainActivity.B0: LinearLayout?
    get() = MainActivityBridge.get(this, "B0")
    set(value) = MainActivityBridge.set(this, "B0", value)

internal var MainActivity.I0: CoordinatorLayout
    get() = MainActivityBridge.get(this, "I0") ?: CoordinatorLayout(this)
    set(value) = MainActivityBridge.set(this, "I0", value)

internal var MainActivity.U0: BottomSheetBehavior<*>
    get() = MainActivityBridge.get(this, "U0") ?: error("MainActivity.U0 belum tersedia")
    set(value) = MainActivityBridge.set(this, "U0", value)

internal var MainActivity.S0: StartMenuViews
    get() = MainActivityBridge.get(this, "S0") ?: StartMenuViews(RelativeLayout(this))
    set(value) = MainActivityBridge.set(this, "S0", value)

internal var MainActivity.M0: StartMenuAdapter?
    get() = MainActivityBridge.get(this, "M0")
    set(value) = MainActivityBridge.set(this, "M0", value)

internal var MainActivity.T0: StartMenuTextWatcher?
    get() = MainActivityBridge.get<StartMenuTextWatcher?>(this, "T0")
    set(value) = MainActivityBridge.set(this, "T0", value)

internal var MainActivity.J0: MutableList<StartMenuItem>
    get() = MainActivityBridge.get(this, "J0") ?: mutableListOf()
    set(value) = MainActivityBridge.set(this, "J0", value)

internal var MainActivity.e1: ActionCenter?
    get() = MainActivityBridge.get(this, "e1")
    set(value) = MainActivityBridge.set(this, "e1", value)

internal var MainActivity.f0: View
    get() = MainActivityBridge.get(this, "f0") ?: View(this)
    set(value) = MainActivityBridge.set(this, "f0", value)

internal var MainActivity.g1: DialogMoreIcons?
    get() = MainActivityBridge.get(this, "g1")
    set(value) = MainActivityBridge.set(this, "g1", value)

internal var MainActivity.w0: TaskbarBtn
    get() = MainActivityBridge.get(this, "w0") ?: TaskbarBtn(this)
    set(value) = MainActivityBridge.set(this, "w0", value)

internal var MainActivity.v0: TaskbarBtn
    get() = MainActivityBridge.get(this, "v0") ?: TaskbarBtn(this)
    set(value) = MainActivityBridge.set(this, "v0", value)

internal var MainActivity.s0: View
    get() = MainActivityBridge.get(this, "s0") ?: View(this)
    set(value) = MainActivityBridge.set(this, "s0", value)

internal var MainActivity.r0: RelativeLayout
    get() = MainActivityBridge.get(this, "r0") ?: RelativeLayout(this)
    set(value) = MainActivityBridge.set(this, "r0", value)

internal var MainActivity.d0: ImageView
    get() = MainActivityBridge.get(this, "d0") ?: ImageView(this)
    set(value) = MainActivityBridge.set(this, "d0", value)

internal var MainActivity.g0: ViewGroup
    get() = MainActivityBridge.get(this, "g0") ?: LinearLayout(this)
    set(value) = MainActivityBridge.set(this, "g0", value)

internal var MainActivity.l0: View
    get() = MainActivityBridge.get(this, "l0") ?: View(this)
    set(value) = MainActivityBridge.set(this, "l0", value)

internal var MainActivity.m0: View
    get() = MainActivityBridge.get(this, "m0") ?: View(this)
    set(value) = MainActivityBridge.set(this, "m0", value)

internal var MainActivity.n0: View
    get() = MainActivityBridge.get(this, "n0") ?: View(this)
    set(value) = MainActivityBridge.set(this, "n0", value)

internal var MainActivity.o0: View
    get() = MainActivityBridge.get(this, "o0") ?: View(this)
    set(value) = MainActivityBridge.set(this, "o0", value)

internal var MainActivity.z0: ImageView?
    get() = MainActivityBridge.get(this, "z0")
    set(value) = MainActivityBridge.set(this, "z0", value)

internal var MainActivity.U: View
    get() = MainActivityBridge.get(this, "U") ?: View(this)
    set(value) = MainActivityBridge.set(this, "U", value)

internal var MainActivity.P0: View
    get() = MainActivityBridge.get(this, "P0") ?: View(this)
    set(value) = MainActivityBridge.set(this, "P0", value)

internal var MainActivity.O0: UIProgressCircular
    get() = MainActivityBridge.get(this, "O0") ?: UIProgressCircular(this)
    set(value) = MainActivityBridge.set(this, "O0", value)

internal var MainActivity.Y0: ImageView
    get() = MainActivityBridge.get(this, "Y0") ?: ImageView(this)
    set(value) = MainActivityBridge.set(this, "Y0", value)

internal var MainActivity.d1: TextView
    get() = MainActivityBridge.get(this, "d1") ?: TextView(this)
    set(value) = MainActivityBridge.set(this, "d1", value)

internal var MainActivity.L0: RelativeLayout
    get() = MainActivityBridge.get(this, "L0") ?: RelativeLayout(this)
    set(value) = MainActivityBridge.set(this, "L0", value)

internal var MainActivity.R0: LinearLayout
    get() = MainActivityBridge.get(this, "R0") ?: LinearLayout(this)
    set(value) = MainActivityBridge.set(this, "R0", value)

internal var MainActivity.V0: TextView
    get() = MainActivityBridge.get(this, "V0") ?: TextView(this)
    set(value) = MainActivityBridge.set(this, "V0", value)

internal var MainActivity.W0: TextView
    get() = MainActivityBridge.get(this, "W0") ?: TextView(this)
    set(value) = MainActivityBridge.set(this, "W0", value)

internal var MainActivity.a1: ImageView
    get() = MainActivityBridge.get(this, "a1") ?: ImageView(this)
    set(value) = MainActivityBridge.set(this, "a1", value)

internal var MainActivity.b1: TextView
    get() = MainActivityBridge.get(this, "b1") ?: TextView(this)
    set(value) = MainActivityBridge.set(this, "b1", value)

internal var MainActivity.c1: TextView
    get() = MainActivityBridge.get(this, "c1") ?: TextView(this)
    set(value) = MainActivityBridge.set(this, "c1", value)

internal var MainActivity.b0: Boolean
    get() = MainActivityBridge.get(this, "b0") ?: false
    set(value) = MainActivityBridge.set(this, "b0", value)

internal var MainActivity.c0: Boolean
    get() = MainActivityBridge.get(this, "c0") ?: false
    set(value) = MainActivityBridge.set(this, "c0", value)

internal var MainActivity.j1: OnBackPressedCallback?
    get() = MainActivityBridge.get(this, "j1")
    set(value) = MainActivityBridge.set(this, "j1", value)

internal fun MainActivity.c0Call(str: String, str2: String?) {
    MainActivityBridge.call(this, "c0", str, str2)
}

internal fun MainActivity.c0(str: String, str2: String?) {
    c0Call(str, str2)
}

internal fun MainActivity.t(view: View?, i2: Int) {
    MainActivityBridge.call(this, "t", view, i2)
}

internal fun MainActivity.n0(item: DesktopItem?) {
    MainActivityBridge.call(this, "n0", item)
}

internal fun MainActivity.s(str: String) {
    MainActivityBridge.call(this, "s", str)
}

internal fun MainActivity.A0(z: Boolean) {
    MainActivityBridge.call(this, "A0", z)
}

internal fun MainActivity.y0(): Boolean =
    runCatching { javaClass.getMethod("y0").invoke(this) as? Boolean }.getOrNull() == true

internal fun MainActivity.M(str: String?, item: StartMenuItem?, str2: String?) {
    MainActivityBridge.call(this, "M", str, item, str2)
}

internal fun MainActivity.z0(i2: Int) {
    MainActivityBridge.call(this, "z0", i2)
}

internal fun MainActivity.f(): Context? =
    runCatching { javaClass.getMethod("f").invoke(this) as? Context }.getOrNull()

internal fun MainActivity.p0Call(btn: TaskbarBtn?) {
    MainActivityBridge.call(this, "p0", btn)
}

internal fun MainActivity.p0(btn: TaskbarBtn?) {
    p0Call(btn)
}

/** Kumpulan callback synthetic untuk desktop, Start Menu, taskbar dan sistem. */
class e(@JvmField val u: MainActivity) :
    DesktopAdapter.OnContextMenuListener,
    DesktopAdapter.OnItemsMovedListener,
    OnStartMenuItemClickListener,
    OnSettingsItemClickListener,
    StartMenuAdapter.OnContextMenuListener,
    OnApplyWindowInsetsListener,
    BatteryReceiver.Callback,
    ActivityResultCallback<Any?>,
    AppInstallReceiver.AppChangeCallback,
    TaskBarAdapter.OnItemClickListener {

    private fun isImageFile(str: String?): Boolean {
        if (str == null) return false
        val length = str.length
        val start = FileExtensions.b(str, length)
        if (start == -1) return false
        return FileExtensions.a(start, length, str, "jpg") ||
            FileExtensions.a(start, length, str, "jpeg") ||
            FileExtensions.a(start, length, str, "png") ||
            FileExtensions.a(start, length, str, "gif") ||
            FileExtensions.a(start, length, str, "webp")
    }

    private fun isPrintableFile(str: String?): Boolean {
        if (str == null) return false
        val length = str.length
        val start = FileExtensions.b(str, length)
        return FileExtensions.a(start, length, str, "txt") ||
            FileExtensions.a(start, length, str, "jpg") ||
            FileExtensions.a(start, length, str, "jpeg") ||
            FileExtensions.a(start, length, str, "png") ||
            FileExtensions.a(start, length, str, "webp")
    }

    private fun selectRenameText(editText: EditText?, name: String?) {
        if (editText == null) return
        val value = name.orEmpty()
        editText.setText(value)
        val selectName = Runnable {
            editText.setSelection(0, FileExtensions.b(value, value.length).coerceAtLeast(0))
        }
        editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) editText.post(selectName)
        }
        editText.setOnLongClickListener(com.bellatrixos.startmenu.b(1, selectName))
    }

    override fun a(view: View, i2: Int) {
        u.t(view, i2)
    }

    override fun b(str: String) {
        if (str == "linux" || str == "9") {
            u.startActivity(Intent(u, LinuxSettingsActivity::class.java))
            return
        }
        u.c0Call("UISettings", str)
    }

    override fun c(iArr: IntArray, startMenuItem: StartMenuItem) {
        val mainActivity = u
        val mainActivity2: MainActivity = mainActivity.W ?: mainActivity
        val view = mainActivity.Z ?: return
        val taskBarAdapter: TaskBarAdapter = mainActivity.t0 ?: return
        view.x = iArr[0].toFloat()
        view.y = iArr[1].toFloat()
        val strC = startMenuItem.c().orEmpty()
        val uIContextMenu = mainActivity.i0 ?: return
        val arrayList = uIContextMenu.f4745d
        arrayList.clear()
        val zContainsKey = AppsDatabase.b.containsKey(strC)

        val showMenu = {
            uIContextMenu.c = com.bellatrixos.app_explorer.drives.d(
                mainActivity2, strC, startMenuItem, MainActivityBridge.asIn(mainActivity), taskBarAdapter
            )
            uIContextMenu.setOnDismissListener(com.bellatrixos.desktop.b(view, 0))
            uIContextMenu.a(0)
            uIContextMenu.f(view)
        }

        if (strC != mainActivity2.packageName) {
            arrayList.add(
                UIMenuItem(0, 0, R.drawable.menu_ic_blank_20_regular, mainActivity2.getString(R.string.context_menu_open))
            )
            if (!zContainsKey) {
                arrayList.add(
                    UIMenuItem(0, 23, R.drawable.menu_ic_edit_20_regular, mainActivity2.getString(R.string.context_menu_edit_app))
                )
            }
            arrayList.add(
                if (taskBarAdapter.c(strC)) {
                    UIMenuItem(
                        0, 17, R.drawable.menu_ic_pin_off_20_regular,
                        mainActivity2.getString(R.string.context_menu_unpin_from_taskbar)
                    )
                } else {
                    UIMenuItem(
                        0, 16, R.drawable.menu_ic_pin_20_regular,
                        mainActivity2.getString(R.string.context_menu_pin_to_taskbar)
                    )
                }
            )
            arrayList.add(
                UIMenuItem(
                    0, 4, R.drawable.menu_ic_add_desktop_20_regular,
                    mainActivity2.getString(R.string.context_menu_add_to_desktop)
                )
            )
            if (!zContainsKey) {
                arrayList.add(UIMenuItem(1, 0, 0, ""))
                if (!InstalledAppsDB.b.containsKey(strC)) {
                    UIMenuItem(
                        0, 15, R.drawable.menu_ic_settings_20_regular,
                        mainActivity2.getString(R.string.context_menu_app_settings)
                    )
                } else {
                    UIMenuItem(
                        0, 24, R.drawable.menu_ic_delete_20_regular,
                        mainActivity2.getString(R.string.context_menu_uninstall)
                    )
                }
            }
            showMenu()
        }
        arrayList.add(
            UIMenuItem(
                0, 15, R.drawable.menu_ic_settings_20_regular,
                mainActivity2.getString(R.string.context_menu_app_settings)
            )
        )
        showMenu()
    }

    override fun d(startMenuItem: StartMenuItem, i2: Int) {
        val mainActivity = u
        MainActivityBridge.callField(mainActivity, "U0", "B", 5)
        mainActivity.M(startMenuItem.c(), startMenuItem, null)
    }

    /** Nama untuk hasil ActivityResultCallback. */
    fun e(obj: Any?) {
        // androidx.profileinstaller.b(this.u.W, 10) tidak tersedia di port Kotlin.
        AppExecutors.c().postDelayed(Runnable { }, 880L)
    }

    override fun onActivityResult(result: Any?) = e(result)

    override fun f(str: String, str2: String, z: Boolean) {
        val mainActivity = u
        var i2 = 0
        if (str == "android.intent.action.PACKAGE_REMOVED") {
            if (z) return
            Installer.a(str2, mainActivity.M0, mainActivity.t0, mainActivity.p0, false)
            return
        }
        if (str == "android.intent.action.PACKAGE_ADDED") {
            val startMenuAdapter = mainActivity.M0 ?: return
            val context: Context = startMenuAdapter.B
            if (!z) {
                val packageManager: PackageManager = context.packageManager
                val string = try {
                    packageManager.getApplicationLabel(packageManager.getApplicationInfo(str2, 0)).toString()
                } catch (_: PackageManager.NameNotFoundException) {
                    str2
                }
                startMenuAdapter.A(0, str2, string)
                return
            }
            try {
                val packageManager2: PackageManager = context.packageManager
                val string2 =
                    packageManager2.getApplicationLabel(packageManager2.getApplicationInfo(str2, 0)).toString()
                for (index in startMenuAdapter.x.indices) {
                    val startMenuItem = startMenuAdapter.x[index]
                    if (startMenuItem.c() == str2) {
                        startMenuItem.d(string2)
                        StartMenuAdapter.G[str2] = string2
                        IconEngine.b(context).f(str2)
                        while (true) {
                            val arrayList = startMenuAdapter.z
                            if (i2 >= arrayList.size) return
                            if (arrayList[i2].c() == str2) {
                                startMenuAdapter.notifyItemChanged(i2)
                                return
                            }
                            i2++
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun g() {
        val imageView: ImageView? = u.z0
        val infoA = BatteryState.a() ?: return
        if (imageView == null) return
        imageView.setImageResource(Battery.a(infoA.level, infoA.charging))
    }

    /** Nama untuk OnApplyWindowInsetsListener. */
    fun h(view: View, windowInsetsCompat: WindowInsetsCompat): WindowInsetsCompat {
        val mainActivity = u
        val i2 = mainActivity.V
        val view2 = mainActivity.l0
        val view3 = mainActivity.m0
        val view4 = mainActivity.n0
        val view5 = mainActivity.o0
        val viewGroup = mainActivity.g0
        if (i2 == 2) {
            val insetsD: Insets = windowInsetsCompat.getInsets(2)
            InsetManager.f4864a = if (Global.f4840h) 0 else windowInsetsCompat.getInsets(1).top
            val z = Global.g
            val i3 = if (z) 0 else insetsD.bottom
            InsetManager.b = i3
            InsetManager.c = if (z) 0 else insetsD.left
            InsetManager.f4865d = if (z) 0 else insetsD.right
            view3?.let { v ->
                val layoutParams = v.layoutParams
                if (layoutParams != null && layoutParams.height != i3) {
                    layoutParams.height = i3
                    v.layoutParams = layoutParams
                }
            }
            val i4 = InsetManager.c
            view4?.let { v ->
                val layoutParams2 = v.layoutParams
                if (layoutParams2 != null && layoutParams2.width != i4) {
                    layoutParams2.width = i4
                    v.layoutParams = layoutParams2
                }
            }
            val i5 = InsetManager.f4865d
            view5?.let { v ->
                val layoutParams3 = v.layoutParams
                if (layoutParams3 != null && layoutParams3.width != i5) {
                    layoutParams3.width = i5
                    v.layoutParams = layoutParams3
                }
            }
            val i6 = InsetManager.f4864a
            view2?.let { v ->
                val layoutParams4 = v.layoutParams
                if (layoutParams4 != null && layoutParams4.height != i6) {
                    layoutParams4.height = i6
                    v.layoutParams = layoutParams4
                }
            }
        } else {
            val insetsD2: Insets = windowInsetsCompat.getInsets(7)
            viewGroup?.setPadding(0, insetsD2.top, 0, insetsD2.bottom)
        }
        return windowInsetsCompat
    }

    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat = h(v, insets)

    /** Listener menu konteks desktop; blok ini dipakai di dua cabang. */
    private fun desktopMenuListener(
        mainActivityIn: MainActivityIn,
        mainActivity2: Context,
        desktopItem: DesktopItem,
        desktopAdapter: DesktopAdapter,
        taskBarAdapter: TaskBarAdapter,
        iC: Int,
        strA: String,
        strB: String
    ): UIContextMenu.OnMenuItemClickListener = UIContextMenu.OnMenuItemClickListener { _, i4 ->
        val desktopItem2 = desktopItem
        val strA2 = desktopItem2.a()
        val iIntValue = if (strA2 == null) -1 else (desktopAdapter.F[strA2] as? Int) ?: -1
        var zA = false
        val z = strA2 != null && strA2.contains("Bellatrix/Desktop")
        if (i4 == 0) {
            mainActivityIn.n0(desktopItem2)
            return@OnMenuItemClickListener
        }
        if (i4 == 1) {
            if (strA2 != null) FileIntent.a(mainActivity2, File(strA2))
            return@OnMenuItemClickListener
        }
        if (i4 == 2) {
            IntentSites.d(mainActivity2, File(strA))
            return@OnMenuItemClickListener
        }
        if (i4 != 3) {
            val str3 = strB
            when (i4) {
                5 -> {
                    if (isImageFile(str3)) {
                        Printer.a(mainActivityIn.f() ?: mainActivity2, File(strA))
                        return@OnMenuItemClickListener
                    }
                    if (str3 != null) {
                        val length = str3.length
                        zA = FileExtensions.a(FileExtensions.b(str3, length), length, str3, "txt")
                    }
                    if (zA) {
                        Printer.c(mainActivityIn.f() ?: mainActivity2, File(strA))
                    }
                    return@OnMenuItemClickListener
                }
                7 -> {
                    Clipboards.f4922a.clear()
                    Clipboards.f4922a.add(ClipboardItem(iC, strA, str3))
                    Clipboards.b = true
                    return@OnMenuItemClickListener
                }
                26 -> {
                    if (z) {
                        val file = File(strA2)
                        val uIDialog = UIDialog(mainActivity2, mainActivity2.getString(R.string.context_menu_rename), null)
                        uIDialog.z = mainActivity2.getString(R.string.dialog_app_editor_btn_update)
                        uIDialog.y = file.name
                        uIDialog.x = mainActivity2.getString(R.string.files_file_name)
                        uIDialog.G = false
                        uIDialog.C = com.bellatrixos.windows.files.operations.a(
                            mainActivity2, file, uIDialog, desktopAdapter, iIntValue
                        )
                        uIDialog.setCanceledOnTouchOutside(true)
                        uIDialog.show()
                        selectRenameText(uIDialog.w, file.name)
                        return@OnMenuItemClickListener
                    }
                    val strB2 = desktopItem2.b()
                    val uIDialog2 = UIDialog(mainActivity2, mainActivity2.getString(R.string.context_menu_rename), null)
                    uIDialog2.z = mainActivity2.getString(R.string.dialog_app_editor_btn_update)
                    uIDialog2.y = strB2
                    uIDialog2.x = mainActivity2.getString(R.string.files_file_name)
                    uIDialog2.G = false
                    uIDialog2.C = DialogInterface.OnClickListener { _, _ ->
                        val string2 = uIDialog2.w?.text?.toString().orEmpty()
                        val desktopItem3 = desktopAdapter.E[iIntValue]
                        desktopItem3.e(string2)
                        if (iIntValue >= 0) {
                            desktopAdapter.notifyItemChanged(iIntValue)
                        }
                        DesktopDB.b(iIntValue, desktopItem3)
                        uIDialog2.dismiss()
                    }
                    uIDialog2.setCanceledOnTouchOutside(true)
                    uIDialog2.show()
                    selectRenameText(uIDialog2.w, strB2)
                    return@OnMenuItemClickListener
                }
                1099 -> {
                    // Cabang ini dipakai untuk "properties" dari shortcut.
                }
                9 -> {
                    val uIDialogNative = UIDialogNative(
                        mainActivity2,
                        mainActivity2.getString(
                            if (z) R.string.context_menu_delete_folder
                            else R.string.context_menu_delete_shortcut_title
                        ),
                        mainActivity2.getString(
                            if (z) R.string.context_menu_delete_folder_permanent_subtitle
                            else R.string.context_menu_delete_shortcut_subtitle
                        ),
                        str3,
                        if (desktopItem2.c() == 3) R.drawable.img_folder_default else FileExtensions.e(str3)
                    )
                    uIDialogNative.D = com.bellatrixos.app_settings.fragments.DialogInterfaceOnClickListenerC0055d(
                        mainActivityIn, 1, desktopItem2
                    )
                    uIDialogNative.setCanceledOnTouchOutside(false)
                    uIDialogNative.show()
                    IconEngine.b(mainActivity2).c(
                        strA,
                        object : IconEngine.Callback {
                            override fun i(bitmap: android.graphics.Bitmap?) {}
                        }
                    )
                    return@OnMenuItemClickListener
                }
                10 -> {
                    val uIDialogProperties = UIDialogProperties(mainActivity2, File(strA))
                    uIDialogProperties.setCanceledOnTouchOutside(false)
                    uIDialogProperties.show()
                    return@OnMenuItemClickListener
                }
                else -> {
                    when (i4) {
                        15 -> {
                            if (!InstalledAppsDB.b.containsKey(strA)) {
                                IntentSites.a(mainActivity2, strA)
                            } else {
                                mainActivityIn.s(strA)
                            }
                        }
                        16 -> taskBarAdapter.a(strA)
                        17 -> taskBarAdapter.e(strA)
                    }
                    return@OnMenuItemClickListener
                }
            }
            if (i4 == 1099) {
                mainActivityIn.c0("UISettings", "15")
                return@OnMenuItemClickListener
            }
        } else {
            val iLastIndexOf = strA.lastIndexOf("/")
            if (iLastIndexOf == -1) return@OnMenuItemClickListener
            mainActivityIn.c0("UIExplorer", strA.substring(0, iLastIndexOf))
            return@OnMenuItemClickListener
        }
        mainActivityIn.c0("UISettings", "15")
    }

    override fun i(iArr: IntArray, desktopItem: DesktopItem) {
        val mainActivity = u
        val mainActivity2: MainActivity = mainActivity.W ?: mainActivity
        val view = mainActivity.Z ?: return
        val desktopAdapter: DesktopAdapter = mainActivity.p0 ?: return
        val taskBarAdapter: TaskBarAdapter = mainActivity.t0 ?: return
        if (mainActivity.b0) return
        view.x = iArr[0].toFloat()
        view.y = iArr[1].toFloat()
        val uIContextMenu = mainActivity.i0 ?: return
        val arrayList = uIContextMenu.f4745d
        arrayList.clear()
        val iC = desktopItem.c()
        val strA = desktopItem.a().orEmpty()
        val strB = desktopItem.b().orEmpty()

        val openItem = UIMenuItem(
            0, 0, R.drawable.menu_ic_blank_20_regular, mainActivity2.getString(R.string.context_menu_open)
        )
        openItem.f4750f = true
        arrayList.add(openItem)
        if (isImageFile(strB)) {
            arrayList.add(
                UIMenuItem(
                    0, 1, R.drawable.img_icon_pack_default,
                    mainActivity2.getString(R.string.context_menu_open_with_default)
                )
            )
        }
        if (FileExtensions.f(strB)) {
            arrayList.add(
                UIMenuItem(
                    0, 1, R.drawable.ic_statusbar_24_regular,
                    mainActivity2.getString(R.string.context_menu_open_with_default)
                )
            )
        }
        if (iC == 2) {
            val item = UIMenuItem(
                0, 2, R.drawable.menu_ic_share_20_regular, mainActivity2.getString(R.string.context_menu_open_with)
            )
            item.f4752i = true
            arrayList.add(item)
        }
        if (iC == 2 || iC == 3) {
            arrayList.add(
                UIMenuItem(
                    0, 3, R.drawable.menu_ic_folder_open_20_regular,
                    mainActivity2.getString(R.string.context_menu_open_file_location)
                )
            )
        }
        if (iC != 0) arrayList.add(UIMenuItem(1, 0, 0, ""))
        if (iC == 1) {
            arrayList.add(
                if (taskBarAdapter.c(strA)) {
                    UIMenuItem(
                        0, 17, R.drawable.menu_ic_pin_off_20_regular,
                        mainActivity2.getString(R.string.context_menu_unpin_from_taskbar)
                    )
                } else {
                    UIMenuItem(
                        0, 16, R.drawable.menu_ic_pin_20_regular,
                        mainActivity2.getString(R.string.context_menu_pin_to_taskbar)
                    )
                }
            )
        }
        if (isPrintableFile(strB)) {
            arrayList.add(
                UIMenuItem(0, 5, R.drawable.menu_ic_blank_20_regular, mainActivity2.getString(R.string.context_menu_print))
            )
        }
        if (iC == 1 || iC == 2 || (iC == 3 && !strA.contains("Bellatrix/Desktop"))) {
            arrayList.add(
                UIMenuItem(0, 7, R.drawable.menu_ic_blank_20_regular, mainActivity2.getString(R.string.context_menu_copy))
            )
        }
        arrayList.add(UIMenuItem(1, 0, 0, ""))
        if (iC == 3 || iC == 2) {
            arrayList.add(
                UIMenuItem(0, 26, R.drawable.menu_ic_blank_20_regular, mainActivity2.getString(R.string.context_menu_rename))
            )
        }
        if (iC != 0) {
            arrayList.add(
                UIMenuItem(0, 9, R.drawable.menu_ic_blank_20_regular, mainActivity2.getString(R.string.context_menu_delete))
            )
        } else {
            arrayList.add(
                UIMenuItem(0, 1099, R.drawable.menu_ic_blank_20_regular, mainActivity2.getString(R.string.context_menu_properties))
            )
        }

        val listener = desktopMenuListener(
            MainActivityBridge.asIn(mainActivity), mainActivity2, desktopItem, desktopAdapter, taskBarAdapter,
            iC, strA, strB
        )
        val showMenu = {
            uIContextMenu.c = listener
            uIContextMenu.setOnDismissListener(com.bellatrixos.desktop.b(view, 0))
            uIContextMenu.a(0)
            uIContextMenu.f(view)
        }

        if (iC != 1) {
            if (iC == 2 || iC == 3) {
                arrayList.add(UIMenuItem(1, 0, 0, ""))
                arrayList.add(
                    UIMenuItem(
                        0, 10, R.drawable.menu_ic_blank_20_regular,
                        mainActivity2.getString(R.string.context_menu_properties)
                    )
                )
            }
            showMenu()
        }
        arrayList.add(UIMenuItem(1, 0, 0, ""))
        arrayList.add(
            UIMenuItem(
                0, 15, R.drawable.menu_ic_blank_20_regular,
                mainActivity2.getString(R.string.context_menu_app_settings)
            )
        )
        showMenu()
    }

    override fun j() {
        u.A0(true)
    }
}
