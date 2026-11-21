package com.bellatrixos

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.Filter
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.app_explorer.UIExplorer
import com.bellatrixos.app_installer.IconManager
import com.bellatrixos.app_installer.Installer
import com.bellatrixos.app_settings.fragments.C
import com.bellatrixos.app_settings.searchbar.SuggestionItem
import com.bellatrixos.context_menu.UIContextMenu
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.desktop.DesktopAdapter
import com.bellatrixos.linux.LINUX_AGENT_TAG_PREFIX
import com.bellatrixos.linux.LINUX_APP_PREFIX
import com.bellatrixos.desktop.DesktopDB
import com.bellatrixos.desktop.DesktopItem
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.interfaces.SaveCallback
import com.bellatrixos.searchapp.SearchApp
import com.bellatrixos.searchapp.SearchAppAdapter
import com.bellatrixos.searchapp.SearchItem
import com.bellatrixos.startmenu.StartMenu
import com.bellatrixos.startmenu.StartMenuAdapter
import com.bellatrixos.startmenu.StartMenuItem
import com.bellatrixos.startmenu.StartMenuRecommend
import com.bellatrixos.startmenu.StartMenuViews
import com.bellatrixos.startmenu.listeners.StartMenuTextWatcher
import com.bellatrixos.taskbar.TaskBar
import com.bellatrixos.taskbar.TaskBarAdapter
import com.bellatrixos.taskbar.TaskBarDB
import com.bellatrixos.taskbar.TaskBarItem
import com.bellatrixos.taskbar.center_action.ActionCenter
import com.bellatrixos.taskbar.center_more_icons.DialogMoreIcons
import com.bellatrixos.taskbar.center_widgets.WidgetCenter
import com.bellatrixos.taskbar.center_widgets.WidgetManager
import com.bellatrixos.taskbar.center_widgets.callbacks.WidgetBindCallback
import com.bellatrixos.taskbar.utils.HorizontalDragCallback
import com.bellatrixos.taskbar.utils.ObservableList
import com.bellatrixos.ui.keyPressed
import com.bellatrixos.ui.keysFocused
import com.bellatrixos.ui.keysStartAgain
import com.bellatrixos.ui.movesFocus
import com.bellatrixos.variables.Global
import com.bellatrixos.wallpaper.Default
import com.bellatrixos.wallpaper.WallpaperVideoManager
import com.bellatrixos.widgets.FilesRecyclerView
import com.bellatrixos.widgets.TaskbarBtn
import com.bellatrixos.widgets.UINotification
import com.bellatrixos.widgets.UIProgressCircular
import com.bellatrixos.windows.InsetManager
import com.bellatrixos.windows.Window
import com.bellatrixos.windows.WindowSize
import com.bellatrixos.windows.Windows
import com.bellatrixos.windows.blur.UIBlur
import com.bellatrixos.windows.blur.UIBlurImage
import com.bellatrixos.windows.config.Languages
import com.bellatrixos.windows.config.LogonController
import com.bellatrixos.windows.config.Themes
import com.bellatrixos.windows.config.UIPermissions
import com.bellatrixos.windows.dialogs.Dialogs
import com.bellatrixos.windows.drivers.battery.BatteryReceiver
import com.bellatrixos.windows.drivers.network.NetworkMonitor
import com.bellatrixos.windows.drivers.volume.OnVolumeChangeListener
import com.bellatrixos.windows.drivers.volume.UIVolume
import com.bellatrixos.windows.drivers.volume.VolumeObserver
import com.bellatrixos.windows.files.FileIntent
import com.bellatrixos.windows.files.clipboard.ClipboardItem
import com.bellatrixos.windows.files.clipboard.Clipboards
import com.bellatrixos.windows.files.operations.FileOperations
import com.bellatrixos.windows.installed_apps.AppsDatabase
import com.bellatrixos.windows.installed_apps.InstalledAppsDB
import com.bellatrixos.windows.installed_apps.IntentApps
import com.bellatrixos.windows.installed_apps.IntentSites
import com.bellatrixos.windows.installed_apps.dialogs.DialogAppEdit
import com.bellatrixos.windows.installed_apps.models.AppDataDBItem
import com.bellatrixos.windows.installed_apps.models.InstalledApp
import com.bellatrixos.windows.navigation.Navigation
import com.bellatrixos.windows.switcher.UIInstanceSwitcher
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import java.util.Locale
import java.util.Objects

class MainActivity : AppCompatActivity(), MainActivityIn, OnVolumeChangeListener {
    @JvmField var A0: LinearLayout? = null
    @JvmField var B0: LinearLayout? = null
    @JvmField var D0: RecyclerView? = null
    @JvmField var E0: EditText? = null
    @JvmField var F0: ImageButton? = null
    @JvmField var G0: TextClock? = null
    @JvmField var H0: FrameLayout? = null
    @JvmField var I0: CoordinatorLayout? = null
    @JvmField var K0: RecyclerView? = null
    @JvmField var L0: RelativeLayout? = null
    @JvmField var M0: StartMenuAdapter? = null
    @JvmField var N0: SearchAppAdapter? = null
    @JvmField var O0: UIProgressCircular? = null
    @JvmField var P0: LinearLayout? = null
    @JvmField var Q0: LinearLayout? = null
    @JvmField var R0: LinearLayout? = null
    private var pendingSettingsPage: String? = null
    @JvmField var S0: StartMenuViews? = null
    @JvmField var T0: StartMenuTextWatcher? = null
    @JvmField var U: View? = null
    @JvmField var U0: BottomSheetBehavior<*>? = null
    @JvmField var V: Int = 0
    @JvmField var V0: TextView? = null
    @JvmField var W: MainActivity? = null
    @JvmField var W0: TextView? = null
    @JvmField var X: Context? = null
    @JvmField var X0: ImageView? = null
    @JvmField var Y: View? = null
    @JvmField var Y0: ImageView? = null
    @JvmField var Z: View? = null
    @JvmField var Z0: BottomSheetBehavior.BottomSheetCallback? = null
    @JvmField var a0: FragmentManager? = null
    @JvmField var a1: ImageView? = null
    @JvmField var b1: TextView? = null
    @JvmField var c1: TextView? = null
    @JvmField var d0: ImageView? = null
    @JvmField var d1: TextView? = null
    @JvmField var e1: ActionCenter? = null
    @JvmField var f0: ViewGroup? = null
    @JvmField var f1: WidgetCenter? = null
    @JvmField var g0: ViewGroup? = null
    @JvmField var g1: DialogMoreIcons? = null
    @JvmField var h0: ViewGroup? = null
    @JvmField var i0: UIContextMenu? = null
    @JvmField var i1: AppInstallReceiver? = null
    @JvmField var j0: UIContextMenu? = null
    @JvmField var j1: OnBackPressedCallback? = null
    @JvmField var k0: NetworkMonitor? = null
    @JvmField var k1: ActivityResultLauncher<Intent>? = null
    @JvmField var l0: View? = null
    @JvmField var l1: WallpaperVideoManager? = null
    @JvmField var m0: View? = null
    @JvmField var m1: PlayerView? = null
    @JvmField var n0: View? = null
    @JvmField var n1: LogonController? = null
    @JvmField var o0: View? = null
    @JvmField var o1: View? = null
    @JvmField var p0: DesktopAdapter? = null
    @JvmField var q0: FilesRecyclerView? = null
    @JvmField var q1: BatteryReceiver? = null
    @JvmField var r0: RelativeLayout? = null
    @JvmField var r1: VolumeObserver? = null
    @JvmField var s1: ActivityResultLauncher<Intent>? = null
    @JvmField var t0: TaskBarAdapter? = null
    @JvmField var t1: Int = -1
    @JvmField var u0: RecyclerView? = null
    @JvmField var v0: TaskbarBtn? = null
    @JvmField var w0: TaskbarBtn? = null
    @JvmField var x0: ImageView? = null
    @JvmField var y0: ImageView? = null
    @JvmField var z0: ImageView? = null

    @JvmField var b0: Boolean = false
    @JvmField var c0: Boolean = false
    @JvmField var e0: Bitmap? = null
    @JvmField val C0: ObservableList<TaskBarItem> = ObservableList()
    @JvmField var J0: ArrayList<Any?>? = null
    @JvmField var h1: Boolean = false
    @JvmField val p1: String = "TARGET_TASKBAR"

    companion object {
        /** Runtime fallback: open Bellatrix Settings at this nav id once home is up. */
        const val EXTRA_SETTINGS_PAGE = "bellatrix_settings_page"

        @JvmField
        val u1: IntentFilter = IntentFilter("android.intent.action.BATTERY_CHANGED")
    }

    /* Inner class MainActivity$1 (nama kelas tidak valid). */
    inner class AnonymousClass1(@JvmField val u: Bundle?) : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            val mainActivity = this@MainActivity
            mainActivity.f0?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
            Themes.a(mainActivity.W ?: mainActivity, mainActivity.G())
            val mainActivity2 = mainActivity.W
            Windows.f4881d = mainActivity.a0
            Windows.f4880a = mainActivity2!!.resources.getDimensionPixelSize(R.dimen.taskbar_height)
            val mainActivity3 = mainActivity.W!!
            val viewGroup = mainActivity.h0!!
            WindowSize.f4872d = Handler(Looper.getMainLooper())
            WindowSize.f4871a = viewGroup.width
            val height = viewGroup.height
            WindowSize.b = height
            val f2 = height.toFloat()
            val f3 = Windows.f4880a.toFloat()
            WindowSize.f4873e = (0.95f * f2) - f3
            WindowSize.f4874f = (f2 * 0.9f) - f3
            WindowSize.g = (WindowSize.f4871a.toDouble() * 0.82).toInt()
            WindowSize.f4875h = (height.toDouble() * 0.86).toInt()
            WindowSize.l = mainActivity3.resources.getDimensionPixelSize(R.dimen.ui_5dp)
            WindowSize.f4877k = mainActivity3.resources.getDimensionPixelSize(R.dimen.app_window_margin)
            mainActivity3.resources.getDimensionPixelSize(R.dimen.ui_12dp)
            WindowSize.m = (WindowSize.b + Windows.f4880a) - WindowSize.l
            if (Global.f4840h && Global.g) {
                ViewCompat.setOnApplyWindowInsetsListener(mainActivity.f0!!, null)
            }
            mainActivity.f0?.post(k(this, this.u, 0))
            if (Global.t == null && Build.VERSION.SDK_INT >= 26) {
                Global.v = DateTimeFormatter.ofPattern("EEEE, d MMMM").withZone(ZoneId.systemDefault())
                Global.t = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a").withZone(ZoneId.systemDefault())
                Global.u = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy").withZone(ZoneId.systemDefault())
            }
            val mainActivity4 = mainActivity.W!!
            if (Dialogs.f4892a == null) {
                Dialogs.f4892a = AnimationUtils.loadAnimation(mainActivity4, R.anim.center_action_open_up)
                Dialogs.c = AnimationUtils.loadAnimation(mainActivity4, R.anim.center_widgets_open)
            }
            if (Dialogs.b == null) {
                Dialogs.b = AnimationUtils.loadAnimation(mainActivity4, R.anim.center_action_close_down)
                Dialogs.f4893d = AnimationUtils.loadAnimation(mainActivity4, R.anim.center_widgets_close)
            }
            val mainActivity5 = mainActivity.W!!
            Languages.f4887a = mainActivity5.resources.getStringArray(R.array.language_codes)
            mainActivity5.resources.getStringArray(R.array.language_names)
            AppExecutors.c().postDelayed(l(this, 0), 500L)
        }
    }

    override fun A(str: String) {
        val fragment = a0?.findFragmentByTag(str) as? UIFragment
        windowsCall("f", fragment, t0)
    }

    fun A0(z: Boolean) {
        this.b0 = z
        if (!z) {
            this.p0?.let { adapter ->
                adapter.y = false
                DesktopDB.c(adapter.E)
            }
        }
        this.A0?.visibility = View.VISIBLE
    }

    override fun C(str: String, str2: String, str3: String) {
        val mainActivity = this.W
        val startMenuAdapter = this.M0
        val desktopAdapter = this.p0
        InstalledAppsDB.a(mainActivity ?: this, str, str2, str3, 1)
        @Suppress("UNCHECKED_CAST")
        (AppsDatabase.f4954d as? MutableMap<String, AppDataDBItem?>)?.put(str, AppDataDBItem(str2, str3))
        startMenuAdapter?.A(1, str, str2)
        desktopAdapter?.A(1, str, str2)
    }

    override fun D(str: String) {
        Installer.a(str, this.M0, this.t0, this.p0, true)
    }

    override fun E(i2: Int) {
        val startMenuAdapter = this.M0
        startMenuAdapter?.E(i2)
        val textView = this.d1!!
        val mainActivity = this.W!!
        textView.text = mainActivity.getString(StartMenuAdapter.labelRes(i2))
    }

    override fun F() {
        if (this.c0) {
            this.O0?.a()
            this.M0?.D(c(this, 3))
        }
    }

    override fun G(): Bitmap? {
        if (this.e0 == null) {
            val mainActivity = this.W ?: this
            var bitmapDecodeResource: Bitmap? = null
            if (SharedPrefs.f4755a?.getBoolean("wallpaper_enabled", true) ?: true) {
                val string = SharedPrefs.f4755a?.getString("wallpaper_mini_path", "theme1") ?: "theme1"
                try {
                    val options = BitmapFactory.Options()
                    options.inPreferredConfig = Bitmap.Config.RGB_565
                    bitmapDecodeResource = BitmapFactory.decodeFile(string, options)
                } catch (_: Exception) {
                    bitmapDecodeResource = BitmapFactory.decodeResource(mainActivity.resources, R.drawable.theme_1_mini)
                }
            } else {
                bitmapDecodeResource = BitmapFactory.decodeResource(mainActivity.resources, R.drawable.theme_2_mini)
            }
            this.e0 = bitmapDecodeResource
        }
        return this.e0
    }

    override fun H(z: Boolean) {
        val desktopAdapter = this.p0 ?: return
        desktopAdapter.z = z
        desktopAdapter.notifyDataSetChanged()
    }

    override fun J() {
        val logonController: LogonController?
        val viewGroup = this.f0
        if (isFinishing || isDestroyed) {
            logonController = null
        } else {
            val handlerC = AppExecutors.c()
            val logonControllerArr = arrayOfNulls<LogonController>(1)
            val aVar = com.bellatrixos.windows.a(logonControllerArr as Any, 1, this)
            val bVar = com.bellatrixos.windows.b(logonControllerArr, this, viewGroup!!, handlerC, aVar, 1)
            logonController = LogonController(handlerC, bVar, aVar)
            logonControllerArr[0] = logonController
            handlerC.postDelayed(bVar, 800L)
        }
        this.n1 = logonController
    }

    override fun K(): Boolean {
        return this.b0
    }

    /** Opens a guest app by its desktop id from the drawer, a pin or a shortcut. */
    private fun openLinuxApp(id: String) {
        val runtime = (application as App).runtime
        val app = runtime.linuxApps.apps.firstOrNull { it.id == id } ?: return
        runtime.launch(app)
    }

    override fun M(str: String, item: StartMenuItem?, str2: String) {
        // The terminal never opens a placeholder tile: focus the one already open, or launch foot.
        if (str == "UITerminal") {
            openOrFocusTerminal()
            return
        }
        // Guest .desktop entries (including installed coding agents) live in the same drawer
        // as every other app. They open through the one runtime, never a separate screen.
        if (str.startsWith(LINUX_APP_PREFIX)) {
            openLinuxApp(str.removePrefix(LINUX_APP_PREFIX))
            return
        }
        // A coding-agent taskbar entry is a window tag, not an installed Android package.
        if (str.startsWith(LINUX_AGENT_TAG_PREFIX)) {
            c0(str, null)
            return
        }
        val callbackA = { s: String -> IntentApps.b(this.W ?: this, s, item, this) }
        val callbackB = { s1: String, s2: String? -> c0(s1, s2) }
        if (AppsDatabase.b.containsKey(str)) {
            callbackB(str, str2)
        } else {
            callbackA(str)
        }
    }

    override fun N() {
        val startMenuAdapter = this.M0
        startMenuAdapter?.D(null)
        val taskBarAdapter = this.t0
        taskBarAdapter?.notifyDataSetChanged()
        val desktopAdapter = this.p0
        desktopAdapter?.notifyDataSetChanged()
    }

    override fun P(fragment: UIFragment) {
        windowsCall("a", fragment, t0)
    }

    override fun closeWindow(fragment: UIFragment) {
        windowsCall("close", fragment, t0)
    }

    override fun Q(i2: Int) {
        if (i2 == -1) return
        AppExecutors.c().post {
            if (!isFinishing && !isDestroyed) recreate()
        }
    }

    override fun R(z: Boolean) {
        val handler: Handler
        val runnable: Runnable
        var i2: Int
        var strA: String
        this.b0 = z
        val taskBarAdapter = this.t0!!
        if (!z || Windows.b == -1) {
            Windows.b = TaskBarDB.a(TaskBarAdapter.J)
        } else {
            i2 = Windows.b
            if (i2 >= 0) {
                val list = taskBarAdapter.D
                strA = if (i2 >= list.size) "" else (list[i2] as TaskBarItem).a() ?: ""
            } else {
                strA = ""
            }
            TaskBarAdapter.J = strA
        }
        val taskBarAdapter2 = this.t0!!
        val recyclerView = this.u0
        if (recyclerView != null) {
            if (z) {
                val itemTouchHelper = ItemTouchHelper(HorizontalDragCallback(taskBarAdapter2))
                taskBarAdapter2.C = itemTouchHelper
                itemTouchHelper.attachToRecyclerView(recyclerView)
            } else {
                val itemTouchHelper2 = taskBarAdapter2.C as? ItemTouchHelper
                if (itemTouchHelper2 != null) {
                    itemTouchHelper2.attachToRecyclerView(null)
                    taskBarAdapter2.C = null
                }
            }
        }
        val mainActivity = this.W!!
        val taskbarBtn = this.v0!!
        if (TaskBar.f4802a == null) {
            TaskBar.f4802a = AnimationUtils.loadAnimation(mainActivity, R.anim.taskbar_rearrange_check)
        }
        if (z) {
            handler = Handler(Looper.getMainLooper())
            runnable = Runnable {
                val animation = TaskBar.f4802a
                taskbarBtn.startAnimation(animation)
                taskbarBtn.setIcon(R.drawable.img_app_start_check)
            }
        } else {
            handler = Handler(Looper.getMainLooper())
            runnable = Runnable {
                val animation2 = TaskBar.f4802a
                taskbarBtn.startAnimation(animation2)
                taskbarBtn.setIcon(StartMenu.f4781f)
            }
        }
        handler.postDelayed(runnable, 500L)
    }

    override fun S() {
        val logonController: LogonController?
        val viewGroup = this.f0
        if (isFinishing || isDestroyed) {
            logonController = null
        } else {
            val handlerC = AppExecutors.c()
            val logonControllerArr = arrayOfNulls<LogonController>(1)
            val aVar = com.bellatrixos.windows.a(logonControllerArr as Any, 0, this)
            val bVar = com.bellatrixos.windows.b(logonControllerArr, this, viewGroup!!, handlerC, aVar, 0)
            logonController = LogonController(handlerC, bVar, aVar)
            logonControllerArr[0] = logonController
            handlerC.postDelayed(bVar, 800L)
        }
        this.n1 = logonController
    }

    override fun U(): Drawable? {
        return this.S0?.c?.drawable
    }

    override fun W(bitmap: Bitmap?) {
        val wallpaperVideoManager = this.l1
        if (wallpaperVideoManager != null) {
            wallpaperVideoManager.f4844a.release()
            wallpaperVideoManager.b.release()
            this.l1 = null
        }
        this.d0?.setImageBitmap(bitmap)
        Themes.a(this.W ?: this, G())
        val cW = this.W ?: this
        val r0v = this.r0
        val x0v = this.X0
        val d0v = this.d0
        if (r0v != null && x0v != null && d0v != null) {
            UIBlur.a(cW, r0v, x0v, d0v)
        }
        StartMenu.f4780e?.a(true)
        val actionCenter = this.e1
        if (actionCenter != null) {
            actionCenter.q = true
            actionCenter.g.setImageBitmap(null)
        }
        val widgetCenter = this.f1
        if (widgetCenter != null) {
            widgetCenter.p = true
            widgetCenter.f4821e.setImageBitmap(null)
        }
    }

    override fun X(i2: Int, i3: Int) {
        val imageView = this.y0 ?: return
        if (i3 == 0) return
        val i5 = ((i2 * 100.0f) / i3).toInt()
        val i4 = if (i5 < 45) {
            R.drawable.ui_tb_speaker_1_24_regular
        } else {
            if (i5 > 50 && i5 < 65) R.drawable.ui_tb_speaker_2_24_regular else R.drawable.ui_tb_speaker_3_24_regular
        }
        imageView.setImageResource(i4)
        UIVolume.f4911a = i5
    }

    override fun Y(i2: Int) {
        val mainActivity = this.W!!
        val i3 = this.V
        val coordinatorLayout = this.I0!!
        val recyclerView = this.D0!!
        if (i3 != 2) {
            Toast.makeText(mainActivity, mainActivity.getString(R.string.toast_supports_only_landscape), 0).show()
            return
        }
        val gridLayoutManager = recyclerView.layoutManager as? GridLayoutManager
        if (gridLayoutManager != null) {
            gridLayoutManager.spanCount = i2
            recyclerView.requestLayout()
        } else {
            recyclerView.layoutManager = GridLayoutManager(mainActivity, i2)
        }
        val layoutParams = coordinatorLayout.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.width = mainActivity.resources.getDimensionPixelSize(R.dimen.start_menu_padding_edges) +
            (mainActivity.resources.getDimensionPixelSize(R.dimen.start_menu_app_layout_width) * i2)
        coordinatorLayout.layoutParams = layoutParams
        StartMenu.f4780e?.a(true)
    }

    override fun Z(z: Boolean) {
        this.r0?.visibility = if (z) 0 else 8
    }

    override fun a(uri: Uri?) {
        this.S0?.c?.setImageURI(uri)
    }

    override fun a0(): View? {
        return this.Z
    }

    override fun attachBaseContext(newBase: Context) {
        var context = newBase
        this.X = context
        var i2 = context.resources.configuration.orientation
        if (SharedPrefs.f4755a == null) {
            SharedPrefs.f4755a = context.applicationContext.getSharedPreferences("SettingsPrefs", 0)
        }
        val sharedPreferences = SharedPrefs.f4755a
        val i3 = sharedPreferences?.getInt("system_color_scheme", 2) ?: 2
        val string = sharedPreferences?.getString("language_code", "default") ?: "default"
        val i4 = sharedPreferences?.getInt("system_display_orientation", 1) ?: 1
        if (i4 == 1) {
            i2 = 2
        } else if (i4 == 2) {
            i2 = 1
        } else if (i4 == 3) {
            i2 = 2
        }
        val configuration = Configuration(context.resources.configuration)
        configuration.orientation = i2
        if (i3 == 0) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else if (i3 == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        if (string != "default" || Global.f4841i) {
            Global.f4841i = false
            val locale: Locale = if (string.contains("-")) {
                val parts = string.split("-")
                Locale(parts[0], if (parts.size > 1) parts[1] else "")
            } else {
                if (Build.VERSION.SDK_INT >= 24) {
                    Locale(string, Resources.getSystem().configuration.locales[0].country)
                } else {
                    @Suppress("DEPRECATION")
                    Locale(string, Resources.getSystem().configuration.locale.country)
                }
            }
            Locale.setDefault(locale)
            configuration.setLocale(locale)
        }
        val resources = context.resources
        if (i2 == 2) {
            val fA = SharedPrefs.a(context)
            Global.f4838e = fA
            if ((fA != 1.0f || Global.s == 1) && Global.s != 0) {
                Global.s = 0
                val displayMetrics = context.resources.displayMetrics
                val f2 = SharedPrefs.f4755a?.getFloat("original_density", displayMetrics.density) ?: displayMetrics.density
                val f3 = SharedPrefs.f4755a?.getFloat("original_scaledDensity", displayMetrics.scaledDensity) ?: displayMetrics.scaledDensity
                val f4 = SharedPrefs.f4755a?.getFloat("original_xdpi", displayMetrics.xdpi) ?: displayMetrics.xdpi
                val f5 = SharedPrefs.f4755a?.getFloat("original_ydpi", displayMetrics.ydpi) ?: displayMetrics.ydpi
                val sharedPreferences2 = SharedPrefs.f4755a
                if (sharedPreferences2 != null && !sharedPreferences2.contains("original_density")) {
                    val editorEdit = sharedPreferences2.edit()
                    editorEdit.putFloat("original_density", displayMetrics.density)
                    editorEdit.putFloat("original_scaledDensity", displayMetrics.scaledDensity)
                    editorEdit.putFloat("original_xdpi", displayMetrics.xdpi)
                    editorEdit.putFloat("original_ydpi", displayMetrics.ydpi)
                    editorEdit.apply()
                }
                displayMetrics.density = f2 * fA
                displayMetrics.scaledDensity = f3 * fA
                displayMetrics.xdpi = f4 * fA
                displayMetrics.ydpi = f5 * fA
                resources.displayMetrics.setTo(displayMetrics)
                if (context.resources.configuration.smallestScreenWidthDp != 0) {
                    try {
                        configuration.densityDpi = (displayMetrics.density * 160.0f).toInt()
                        @Suppress("DEPRECATION")
                        resources.updateConfiguration(configuration, displayMetrics)
                    } catch (_: Exception) {
                    }
                }
            }
        } else {
            Global.s = -1
            Global.f4838e = 1.0f
        }
        super.attachBaseContext(context.createConfigurationContext(configuration))
    }

    override fun b(str: String, drawable: BitmapDrawable?) {
        IconManager.a(this.W ?: this, drawable, str, SaveCallback { str2, z ->
            if (!z) return@SaveCallback
            val startMenuAdapter = this.M0
            val desktopAdapter = this.p0
            val installedAppC = InstalledAppsDB.c(str)
            if (installedAppC != null) {
                installedAppC.i(str2)
                InstalledAppsDB.e(str, installedAppC)
            }
            startMenuAdapter?.G(str)
            desktopAdapter?.E(str)
        })
    }

    override fun b0(i2: Int) {
        val startMenuAdapter = this.M0
        startMenuAdapter?.F(i2, true)
    }

    override fun c0(str: String, str2: String?) {
        // App drawer, taskbar, pinned shortcut and start menu all come through here.
        if (str == "UITerminal") {
            openOrFocusTerminal()
            return
        }
        if (str.startsWith(LINUX_APP_PREFIX)) {
            openLinuxApp(str.removePrefix(LINUX_APP_PREFIX))
            return
        }
        val bottomSheetBehavior = this.U0
        if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
            hideStartMenu()
        }
        val zEquals = "UIExplorer" == str
        val zEquals2 = Objects.equals(str2, this.p1)
        if (!zEquals || UIPermissions.a(this)) {
            windowsCall("h", this.W, str, this.t0, str2, this.o1, zEquals2)
            return
        }
        val packageName = packageName
        if (Build.VERSION.SDK_INT >= 30) {
            UIPermissions.b(this.s1, this, packageName)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf("android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"), 100)
        }
    }

    /** Hides the Start Menu and its touch-consuming overlay. */
    fun hideStartMenu() {
        U0?.state = BottomSheetBehavior.STATE_HIDDEN
        U?.visibility = View.GONE
    }

    override fun d(fragment: UIFragment?) {
        windowsCall("j", 4, this.t0, fragment)
    }

    override fun d0(str: String, str2: String, str3: String, z: Boolean) {
        val mainActivity = this.W
        val startMenuAdapter = this.M0
        val desktopAdapter = this.p0
        InstalledAppsDB.a(mainActivity ?: this, str, str2, str3, 0)
        @Suppress("UNCHECKED_CAST")
        (AppsDatabase.f4954d as? MutableMap<String, AppDataDBItem?>)?.put(str, AppDataDBItem(str2, str3))
        startMenuAdapter?.A(1, str, str2)
        if (desktopAdapter == null || !z) return
        desktopAdapter.A(1, str, str2)
    }

    override fun e(file: File?, file2: File?) {
        if (file == null || file2 == null) return
        if (this.l1 == null) {
            val front = findViewById<PlayerView>(R.id.playerWallpaper)
            val back = findViewById<PlayerView>(R.id.playerWallpaperBlur)
            this.m1 = front
            this.l1 = WallpaperVideoManager(this.W ?: this, front, back)
        }
        val wallpaperVideoManager = this.l1 ?: return
        val uriFromFile = Uri.fromFile(file)
        val uriFromFile2 = Uri.fromFile(file2)
        val mediaItemA = MediaItem.fromUri(uriFromFile)
        val mediaItemA2 = MediaItem.fromUri(uriFromFile2)
        val player = wallpaperVideoManager.f4844a
        val player2 = wallpaperVideoManager.b
        player.setMediaItem(mediaItemA)
        player2.setMediaItem(mediaItemA2)
        player.prepare()
        player2.prepare()
        wallpaperVideoManager.a()
    }

    override fun e0(): Any? {
        return this.t0
    }

    override fun f(): Context? {
        return this.X
    }

    override fun g0() {
        this.U?.visibility = 0
    }

    override fun h(str: String, str2: String) {
        val textClock = this.G0 ?: return
        if (str.isNotEmpty()) {
            SharedPrefs.f4755a?.edit()?.putString("system_date_format", str)?.apply()
        }
        if (str2.isNotEmpty()) {
            SharedPrefs.f4755a?.edit()?.putString("system_clock_format", str2)?.apply()
        }
        var s1 = str
        if (s1.isEmpty()) s1 = Global.z ?: "dd-MM-yyyy"
        var s2 = str2
        if (s2.isEmpty()) s2 = Global.y ?: "hh:mm a"
        TaskBar.c(textClock, s1, s2)
    }

    override fun i(i2: Int) {
        val mainActivity = this.W ?: return
        val linearLayout = this.A0 ?: return
        for (idx in 0 until linearLayout.childCount) {
            val childAt = linearLayout.getChildAt(idx)
            if (childAt.id == R.id.taskbar_search_icon_n_label || childAt.id == R.id.taskbar_search_icon || childAt.id == R.id.taskbar_search_box) {
                linearLayout.removeView(childAt)
                break
            }
        }
        val layoutInflaterFrom = LayoutInflater.from(mainActivity)
        val i3 = when (i2) {
            1 -> R.layout.taskbar_search_icon
            2 -> R.layout.taskbar_search_icon_n_label
            3 -> R.layout.taskbar_search_box
            else -> -1
        }
        val viewInflate = if (i3 != -1) layoutInflaterFrom.inflate(i3, linearLayout, false) else null
        if (viewInflate != null) {
            try {
                val clickListener = com.bellatrixos.searchapp.a(this, 0)
                @Suppress("UNCHECKED_CAST")
                viewInflate.setOnClickListener(clickListener as? View.OnClickListener)
            } catch (_: Throwable) {}
            linearLayout.addView(viewInflate, 1)
        }
    }

    override fun i0(): ViewGroup? {
        return this.h0
    }

    override fun j(i2: Int, str: String, str2: String) {
        this.p0?.A(i2, str, str2)
    }

    override fun j0(z: Boolean) {
        val tbW = this.W ?: this
        val tbB0 = this.B0
        if (tbB0 != null) {
            TaskBar.a(tbW, Global.f4843n, tbB0, this.w0, this)
        }
        SharedPrefs.f4755a?.edit()?.putBoolean("taskbar_btn_widget_visible", z)?.apply()
    }

    override fun k(): Any? {
        return this.j0
    }

    override fun k0(str: String) {
        val fragmentManager = this.a0 ?: return
        val viewGroup = this.h0 ?: return
        val uIExplorer = UIExplorer()
        val bundle = Bundle()
        bundle.putString("path", str)
        uIExplorer.arguments = bundle
        val view = View(viewGroup.context)
        view.layoutParams = FrameLayout.LayoutParams(-1, -1)
        view.setBackgroundColor(0)
        view.isClickable = true
        view.isFocusable = true
        view.tag = "overlay_picker"
        view.setOnClickListener(C(2))
        viewGroup.addView(view)
        val fragmentTransactionD = fragmentManager.beginTransaction()
        if (!uIExplorer.isAdded) {
            fragmentTransactionD.setCustomAnimations(R.anim.app_anim_open, 0, R.anim.app_anim_open, 0)
            fragmentTransactionD.replace(R.id.apps_window, uIExplorer, "UIExplorer::::" + System.currentTimeMillis())
        }
        if (fragmentManager.isStateSaved) {
            fragmentTransactionD.commitAllowingStateLoss()
        } else {
            fragmentTransactionD.commit()
        }
        fragmentManager.executePendingTransactions()
        val view2 = uIExplorer.c0
        view2?.bringToFront()
    }

    override fun l(z: Boolean) {
        val mainActivity = this.W
        val imageView = this.X0
        val relativeLayout = this.r0
        val imageView2 = this.d0
        if (z) {
            try {
                val cls = Class.forName("androidx.media3.exoplayer.audio.h")
                val ctor = cls.constructors.firstOrNull { it.parameterCount == 5 }
                val r = ctor?.newInstance(imageView2, mainActivity, relativeLayout, imageView, 2) as? Runnable
                if (r != null) AppExecutors.d().execute(r)
            } catch (_: Throwable) {}
        } else {
            val bitmap = UIBlurImage.f4886a
            if (bitmap != null) {
                bitmap.recycle()
                UIBlurImage.f4886a = null
            }
            imageView?.let { iv ->
                iv.animate().alpha(0.0f).setDuration(1000L).withEndAction(com.bellatrixos.startmenu.d(iv, 1)).start()
            }
            val uIFragment = Windows.f4882e
            uIFragment?.i0(false)
        }
        Global.b = z
        StartMenu.f4780e?.a(z)
        SharedPrefs.f4755a?.edit()?.putBoolean("system_ui_blur", z)?.apply()
        val actionCenter = this.e1
        if (actionCenter != null) {
            actionCenter.q = true
            actionCenter.g.setImageBitmap(null)
        }
    }

    override fun l0(): Any? {
        return this.i0
    }

    override fun m0(item: DesktopItem?) {
        val desktopItem = item ?: return
        val mainActivity = this.W ?: return
        val desktopAdapter = this.p0 ?: return
        val path = desktopItem.a() ?: ""
        if (!path.contains("Bellatrix/Desktop")) {
            desktopAdapter.C(path)
            return
        }
        val fileOperationsC = FileOperations.c(mainActivity)
        val strA = path
        val strB = desktopItem.b() ?: ""
        val file = File(strA)
        fileOperationsC.f(fileOperationsC.f4924a.getString(R.string.msg_files_title_delete), strB)
        try {
            val cls = Class.forName("androidx.core.content.res.a")
            val ctor = cls.constructors.firstOrNull { it.parameterCount == 3 }
            val r = ctor?.newInstance(fileOperationsC, 28, file) as? Runnable
            if (r != null) FileOperations.f4923i?.execute(r)
        } catch (_: Throwable) {}
        fileOperationsC.c = com.bellatrixos.desktop.e(desktopAdapter, desktopItem)
        fileOperationsC.f4925d = com.bellatrixos.desktop.e(desktopAdapter, desktopItem)
    }

    override fun n() {
        try { Handler(Looper.getMainLooper()).postDelayed(c(this, 0), 400L) } catch (_: Throwable) {}
    }

    override fun n0(item: DesktopItem?) {
        val desktopItem = item ?: return
        val iC = desktopItem.c()
        if (iC == 0) {
            c0("UIExplorer", null)
            return
        }
        if (iC == 1) {
            M(desktopItem.a() ?: "", null, "")
        } else if (iC == 2) {
            FileIntent.b(this.W ?: this, File(desktopItem.a() ?: ""), FileIntent.AppLauncher { tag, extra -> c0(tag, extra) })
        } else if (iC == 3) {
            c0("UIExplorer", desktopItem.a())
        }
    }

    override fun o(i2: Int) {
        this.U?.visibility = 0
        if (this.c0) {
            z0(i2)
            this.T0?.u = i2
        } else {
            this.O0?.a()
            val bottomSheetCallbackB = StartMenu.b(this.U!!, f(this, i2, 0), this.S0!!, this.v0!!)
            this.Z0?.let { this.U0?.removeBottomSheetCallback(it) }
            this.Z0 = bottomSheetCallbackB
            this.U0?.addBottomSheetCallback(bottomSheetCallbackB)
        }
        val taskbarBtn = this.v0 ?: return
        val bottomSheetBehavior = this.U0 ?: return
        val startMenuViews = this.S0 ?: return
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN && StartMenu.f4779d == i2) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            return
        }
        val z = i2 == 0
        startMenuViews.f4795i.visibility = if (z) 4 else 0
        startMenuViews.f4794h.visibility = if (z) 0 else 4
        val frameLayout = startMenuViews.f4796k
        if (z) {
            frameLayout.setBackgroundResource(R.drawable.app_startmenu_dialog_bg_header)
        } else {
            frameLayout.background = null
        }
        val i3 = if (z && !StartMenu.c) 8 else 0
        val editText = startMenuViews.f4793f
        editText.visibility = i3
        taskbarBtn.isActivated = z
        StartMenu.f4778a = y0()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        if (Global.m) {
            editText.requestFocus()
        }
        StartMenu.f4779d = i2
        if (z) {
            val view = startMenuViews.g
            val context = view.context
            if (StartMenuRecommend.c) {
                if (StartMenuRecommend.b && StartMenuRecommend.f4787a != null) {
                    try {
                        val cls = Class.forName("com.bellatrixos.app_browser.files.c")
                        val ctor = cls.constructors.firstOrNull { it.parameterCount == 2 }
                        val r = ctor?.newInstance(view, 3) as? Runnable
                        if (r != null) AppExecutors.c().post(r)
                    } catch (_: Throwable) {}
                } else if (NetworkMonitor.f4907d) {
                    try {
                        val cls = Class.forName("androidx.media3.common.util.f")
                        val ctor = cls.constructors.firstOrNull { it.parameterCount == 4 }
                        val r = ctor?.newInstance(context, view, this, 4) as? Runnable
                        if (r != null) AppExecutors.c().postDelayed(r, 1000L)
                    } catch (_: Throwable) {}
                }
            }
        }
    }

    override fun onCreate(bundle: Bundle?) {
        pendingSettingsPage = intent?.getStringExtra(EXTRA_SETTINGS_PAGE)
        var i2 = 29
        var bByte: Byte = 6
        val i3 = 4
        super.onCreate(bundle)
        // One runtime owns Linux for this process, for both the classic grid and the OWL layout.
        (application as App).runtime.start()

        s1 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            val ctx = this@MainActivity.W
            if (ctx != null) {
                try {
                    val cls = Class.forName("androidx.profileinstaller.b")
                    val m = cls.getMethod("b", Context::class.java, Int::class.javaPrimitiveType)
                    m.invoke(null, ctx, 10)
                } catch (_: Throwable) {
                }
            }
            AppExecutors.c().postDelayed({}, 880L)
        }
        k1 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = WidgetManager.f4828a
            callback?.a(result.resultCode)
        }

        this.W = this
        Themes.f4891a = SharedPrefs.f4755a?.getString("system_primary_color", null)
        val i4 = SharedPrefs.f4755a?.getInt("system_display_orientation", 1) ?: 1
        if (i4 != 0) {
            when (i4) {
                1 -> requestedOrientation = 6
                2 -> requestedOrientation = 1
                3 -> requestedOrientation = 0
            }
        }
        val theme = theme
        val strColor = Themes.f4891a
        var i5 = R.style.ThemeColorDefault
        if (strColor != null) {
            when (strColor) {
                "Red0" -> bByte = 0
                "Red1" -> bByte = 1
                "Red2" -> bByte = 2
                "Red3" -> bByte = 3
                "Red4" -> bByte = 4
                "Blue1" -> bByte = 5
                "Blue2" -> bByte = 6
                "Blue3" -> bByte = 7
                "Cyan0" -> bByte = 8
                "Cyan1" -> bByte = 9
                "Cyan2" -> bByte = 10
                "Cyan3" -> bByte = 11
                "Grey0" -> bByte = 12
                "Grey1" -> bByte = 13
                "Grey2" -> bByte = 14
                "Grey3" -> bByte = 15
                "Grey4" -> bByte = 16
                "Grey5" -> bByte = 17
                "Grey6" -> bByte = 18
                "Grey7" -> bByte = 19
                "Grey8" -> bByte = 20
                "Grey9" -> bByte = 21
                "Pink0" -> bByte = 22
                "Pink1" -> bByte = 23
                "Pink2" -> bByte = 24
                "Pink3" -> bByte = 25
                "Pink4" -> bByte = 26
                "Pink5" -> bByte = 27
                "Yellow0" -> bByte = 28
                "Yellow1" -> bByte = 29
                "Orange0" -> bByte = 30
                "Purple0" -> bByte = 31
                "Purple1" -> bByte = 32
                "Purple2" -> bByte = 33
                "Purple3" -> bByte = 34
                "Brown0" -> bByte = 35
                "Brown1" -> bByte = 36
                "Brown2" -> bByte = 37
                "Brown3" -> bByte = 38
                "Brown4" -> bByte = 39
                "Brown5" -> bByte = 40
                "Green0" -> bByte = 41
                "Green1" -> bByte = 42
                "Green2" -> bByte = 43
                "Green3" -> bByte = 44
                "Green4" -> bByte = 45
                "Green5" -> bByte = 46
                else -> bByte = -1
            }
            i5 = when (bByte.toInt()) {
                0 -> R.style.ThemeColorRed0
                1 -> R.style.ThemeColorRed1
                2 -> R.style.ThemeColorRed2
                3 -> R.style.ThemeColorRed3
                4 -> R.style.ThemeColorRed4
                5 -> R.style.ThemeColorBlue1
                6 -> R.style.ThemeColorBlue2
                7 -> R.style.ThemeColorBlue3
                8 -> R.style.ThemeColorCyan0
                9 -> R.style.ThemeColorCyan1
                10 -> R.style.ThemeColorCyan2
                11 -> R.style.ThemeColorCyan3
                12 -> R.style.ThemeColorGrey0
                13 -> R.style.ThemeColorGrey1
                14 -> R.style.ThemeColorGrey2
                15 -> R.style.ThemeColorGrey3
                16 -> R.style.ThemeColorGrey4
                17 -> R.style.ThemeColorGrey5
                18 -> R.style.ThemeColorGrey6
                19 -> R.style.ThemeColorGrey7
                20 -> R.style.ThemeColorGrey8
                21 -> R.style.ThemeColorGrey9
                22 -> R.style.ThemeColorPink0
                23 -> R.style.ThemeColorPink1
                24 -> R.style.ThemeColorPink2
                25 -> R.style.ThemeColorPink3
                26 -> R.style.ThemeColorPink4
                27 -> R.style.ThemeColorPink5
                28 -> R.style.ThemeColorYellow0
                29 -> R.style.ThemeColorYellow1
                30 -> R.style.ThemeColorOrange0
                31 -> R.style.ThemeColorPurple0
                32 -> R.style.ThemeColorPurple1
                33 -> R.style.ThemeColorPurple2
                34 -> R.style.ThemeColorPurple3
                35 -> R.style.ThemeColorBrown0
                36 -> R.style.ThemeColorBrown1
                37 -> R.style.ThemeColorBrown2
                38 -> R.style.ThemeColorBrown3
                39 -> R.style.ThemeColorBrown4
                40 -> R.style.ThemeColorBrown5
                41 -> R.style.ThemeColorGreen0
                42 -> R.style.ThemeColorGreen1
                43 -> R.style.ThemeColorGreen2
                44 -> R.style.ThemeColorGreen3
                45 -> R.style.ThemeColorGreen4
                46 -> R.style.ThemeColorGreen5
                else -> R.style.ThemeColorDefault
            }
        }
        theme.applyStyle(i5, true)
        val i6 = resources.configuration.orientation
        this.V = i6
        Global.f4836a = i6
        setContentView(R.layout.activity_main)
        val decorView = window.decorView
        this.Y = decorView
        if (this.V == 2) {
            val i7 = SharedPrefs.f4755a?.getInt("device_statusbar_type", 0) ?: 0
            val zNav = (SharedPrefs.f4755a?.getInt("device_navigation_type", 1) ?: 1) == 0
            Navigation.a(i7, decorView, zNav)
            Global.f4837d = i7
            Global.f4840h = i7 == 0
            Global.g = zNav
        }
        val imageView = findViewById<ImageView>(R.id.systemWallpaper)
        this.d0 = imageView
        val mainActivity = this.W!!
        if (SharedPrefs.f4755a?.getBoolean("wallpaper_enabled", true) ?: true) {
            val stringPath = SharedPrefs.f4755a?.getString("wallpaper_path", "theme1") ?: "theme1"
            var bitmapDecodeFile: Bitmap? = null
            try {
                if (Objects.equals(stringPath, "theme1")) {
                    try {
                        val dark = (mainActivity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                        bitmapDecodeFile = if (dark) Default.a(mainActivity, "theme1") else Default.a(mainActivity, "theme2")
                    } catch (_: Exception) {
                    }
                } else {
                    val options = BitmapFactory.Options()
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888
                    bitmapDecodeFile = BitmapFactory.decodeFile(stringPath, options)
                }
            } catch (_: Exception) {
                bitmapDecodeFile = null
            }
            if (bitmapDecodeFile != null) {
                imageView.setImageBitmap(bitmapDecodeFile)
            }
        }
        val viewGroup = findViewById<ViewGroup>(R.id.window)
        this.f0 = viewGroup
        val viewGroup2 = viewGroup.findViewById<ViewGroup>(R.id.window_main)
        this.g0 = viewGroup2
        this.h0 = viewGroup2.findViewById(R.id.apps_window)
        this.l0 = this.f0?.findViewById(R.id.insetBgTop)
        this.m0 = this.f0?.findViewById(R.id.insetBgBottom)
        this.n0 = this.f0?.findViewById(R.id.insetBgLeft)
        this.o0 = this.f0?.findViewById(R.id.insetBgRight)
        this.a0 = supportFragmentManager
        this.Z = findViewById(R.id.context_pos)
        this.U = findViewById(R.id.overlay)
        this.r0 = findViewById(R.id.taskBar)
        this.X0 = findViewById(R.id.taskBarBlur)
        this.u0 = findViewById(R.id.taskBarAppList)
        this.A0 = findViewById(R.id.taskBarList)
        this.L0 = findViewById(R.id.startMenuBg)
        this.H0 = findViewById(R.id.startMenuBehavior)
        this.v0 = findViewById(R.id.btnStartMenu)
        this.D0 = findViewById(R.id.startMenuApps)
        this.K0 = findViewById(R.id.startMenuSuggestedApps)
        this.P0 = findViewById(R.id.startMenuUser)
        this.E0 = findViewById(R.id.startMenuSearch)
        this.I0 = findViewById(R.id.startMenuLayout)
        this.q0 = findViewById(R.id.desktop_icons)
        this.S0 = StartMenuViews(this.L0!!)
        this.R0 = findViewById(R.id.recommendedUi)
        this.Y0 = findViewById(R.id.startmenu_btn_sort)
        this.d1 = findViewById(R.id.startmenu_btn_type)
        this.O0 = findViewById(R.id.start_menu_progress)
        this.a1 = findViewById(R.id.searchAppAppIcon)
        this.b1 = findViewById(R.id.searchAppAppName)
        this.c1 = findViewById(R.id.searchAppResultType)
        this.V0 = findViewById(R.id.searchAppUiOpen)
        this.W0 = findViewById(R.id.searchAppUiSettings)
        if (this.V == 2) {
            this.F0 = findViewById(R.id.btn_more_icons)
            this.Q0 = findViewById(R.id.btn_action_center)
            this.G0 = findViewById(R.id.btn_calendar)
            this.w0 = findViewById(R.id.btn_startMenu_widget)
            this.B0 = findViewById(R.id.taskbar_actions)
            this.x0 = findViewById(R.id.tray_wifi)
            this.y0 = findViewById(R.id.tray_volume)
            this.z0 = findViewById(R.id.tray_battery)
        }
        ViewCompat.setOnApplyWindowInsetsListener(this.f0!!, e(this))
        val bottomSheetBehaviorX = BottomSheetBehavior.from(this.H0!!)
        this.U0 = bottomSheetBehaviorX
        bottomSheetBehaviorX.state = BottomSheetBehavior.STATE_HIDDEN
        val mainActivity2 = this.W!!
        if (TaskBarDB.f4804a == null) {
            TaskBarDB.f4804a = mainActivity2.getSharedPreferences("TaskbarPrefs", 0)
        }
        val i8 = Build.VERSION.SDK_INT
        if (i8 >= 26) {
            this.i1 = AppInstallReceiver(e(this))
            val intentFilter = IntentFilter()
            intentFilter.addAction("android.intent.action.PACKAGE_ADDED")
            intentFilter.addAction("android.intent.action.PACKAGE_REMOVED")
            intentFilter.addAction("android.intent.action.PACKAGE_REPLACED")
            intentFilter.addDataScheme("package")
            val receiver = this.i1
            if (receiver != null) {
                if (i8 >= 33) {
                    registerReceiver(receiver, intentFilter, 4)
                } else {
                    @Suppress("UnspecifiedRegisterReceiverFlag")
                    registerReceiver(receiver, intentFilter)
                }
            }
            this.h1 = true
        }
        try { AppExecutors.b().execute(c(this, i3)) } catch (_: Throwable) {}
        this.f0?.viewTreeObserver?.addOnGlobalLayoutListener(AnonymousClass1(bundle))
    }

    override fun onDestroy() {
        (application as App).runtime.clearHomeHooks()
        val logonController = this.n1
        if (logonController != null) {
            logonController.f4890d = true
            val runnable = logonController.b
            val handler = logonController.f4889a
            handler.removeCallbacks(runnable)
            handler.removeCallbacks(logonController.c)
        }
        super.onDestroy()
        if (Global.s != 1) {
            Global.s = -1
        }
        val desktopAdapter = this.p0
        if (desktopAdapter != null) {
            desktopAdapter.D = null
            desktopAdapter.A = null
        }
        if (this.M0 != null) {
            try {
                StartMenuAdapter.G.clear()
            } catch (_: Throwable) {
            }
        }
        this.E0?.removeTextChangedListener(this.T0)
        val bottomSheetBehavior = this.U0
        val cb = this.Z0
        if (bottomSheetBehavior != null && cb != null) {
            bottomSheetBehavior.removeBottomSheetCallback(cb)
        }
        if (this.h1) {
            try {
                this.i1?.let { unregisterReceiver(it) }
            } catch (_: Throwable) {
            }
            this.h1 = false
        }
        if (UIContextMenu.f4743f != null) {
            UIContextMenu.f4743f = null
        }
        val popupWindow = UINotification.b
        if (popupWindow != null) {
            if (popupWindow.isShowing) {
                popupWindow.dismiss()
            }
            UINotification.b = null
        }
        if (FileOperations.j != null) {
            val fo = FileOperations.j!!
            fo.f4928h = false
            if (fo.f4926e.isShowing) {
                fo.f4926e.dismiss()
            }
            fo.c = null
            FileOperations.j = null
        }
        val uIInstanceSwitcher = UIInstanceSwitcher.c
        if (uIInstanceSwitcher != null) {
            if (uIInstanceSwitcher.isShowing) {
                uIInstanceSwitcher.dismiss()
            }
            UIInstanceSwitcher.c = null
        }
        try {
            val nativeAd = StartMenuRecommend.f4787a
            if (nativeAd != null) {
                nativeAd.destroy()
                StartMenuRecommend.f4787a = null
            }
            StartMenuRecommend.b = false
        } catch (_: Exception) {
        }
        val widgetCenter = this.f1
        if (widgetCenter != null) {
            widgetCenter.j.setOnClickListener(null)
            widgetCenter.f4825k.setOnClickListener(null)
        }
        val wallpaperVideoManager = this.l1
        if (wallpaperVideoManager != null) {
            wallpaperVideoManager.f4844a.release()
            wallpaperVideoManager.b.release()
        }
        val onBackPressedCallback = this.j1
        if (onBackPressedCallback != null) {
            try {
                val field = onBackPressedCallback.javaClass.getDeclaredField("b")
                field.isAccessible = true
                val cbs = field.get(onBackPressedCallback) as? Iterable<*>
                cbs?.forEach { cb ->
                    if (cb != null) {
                        try { cb.javaClass.getMethod("cancel").invoke(cb) } catch (_: Throwable) {}
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    // Lint flags super.dispatchKeyEvent: ComponentActivity marks its override @RestrictTo, but Activity's is public API.
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val down = event.action == KeyEvent.ACTION_DOWN
        if (down) keyPressed(window.decorView)
        if (down && movesFocus(event.keyCode) && keysStartAgain(window.decorView)) return true
        if (down && event.isPrintingKey && !keysFocused(window.decorView)) keysStartAgain(window.decorView)
        return super.dispatchKeyEvent(event) || escapeAsBack(event)
    }

    override fun onKeyShortcut(i2: Int, keyEvent: KeyEvent): Boolean {
        val uIFragment = Windows.f4882e
        if (uIFragment != null && (fragmentBool(uIFragment, "t") || uIFragment.isVisible)) {
            uIFragment.h0(i2, keyEvent)
            return true
        }
        val arrayList = Clipboards.f4922a
        if (arrayList.isEmpty() || i2 != 50 || !keyEvent.isCtrlPressed) {
            return super.onKeyShortcut(i2, keyEvent)
        }
        for (clipboardItem in arrayList) {
            val str = clipboardItem.f4921a
            val str2 = clipboardItem.b
            j(clipboardItem.c, Clipboards.a(str, str2), str2)
        }
        Clipboards.f4922a.clear()
        return true
    }

    override fun onPause() {
        super.onPause()
        try { UINotification.c.removeCallbacksAndMessages(null) } catch (_: Throwable) {}
        val wallpaperVideoManager = this.l1
        if (wallpaperVideoManager != null) {
            wallpaperVideoManager.f4844a.playWhenReady = false
            wallpaperVideoManager.b.playWhenReady = false
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        this.f0?.post(h(0))
    }

    override fun onResume() {
        super.onResume()
        if (Global.c != 0 && this.V == 2) {
            val view = this.Y
            if (this.V == 2) {
                Navigation.a(Global.f4837d, view!!, Global.g)
            }
        }
        val wallpaperVideoManager = this.l1
        wallpaperVideoManager?.a()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList("taskbar_items", ArrayList(this.C0))
    }

    /**
     * Taskbar Terminal behaves like Settings and Explorer: one window, focused on the next tap.
     * A new foot is launched only when no plain terminal window is open, so repeated taps never
     * pile new Terminal entries into Recents.
     */
    private fun openOrFocusTerminal() {
        installRuntimeHooks()
        val runtime = (application as App).runtime
        val existing =
            Windows.c(com.bellatrixos.linux.UILinuxWindow.TAG).firstOrNull { fragment ->
                val handle = fragment.d0() ?: return@firstOrNull false
                val window = runtime.wm.windowByHandle(handle) ?: return@firstOrNull false
                runtime.agentOf(window.appId, runtime.displayName(window.appId, window.title)) == null
            }
        if (existing != null) p(existing) else runtime.terminal(null, "Terminal")
    }

    /** Keeps every guest window routed into a Bellatrix window, including taskbar Terminal taps. */
    private fun installRuntimeHooks() {
        val runtime = (application as App).runtime
        // Every foot window, from any tile, becomes a floating home window. Missing Linux goes
        // to the one launcher Settings screen, never a separate legacy Linux window.
        runtime.openFloatingTerminal = { handle ->
            if (!isFinishing && !isDestroyed) {
                runOnUiThread { this@MainActivity.c0(runtime.floatingWindowTag(handle), handle) }
            }
        }
        // Installing or removing an agent rewrites its .desktop entry; the drawer is one
        // list, so just rebuild that list. No agents button, no separate screen.
        runtime.onAppsChanged = {
            if (!isFinishing && !isDestroyed) runOnUiThread { M0?.D(null) }
        }
        // Guest Files and Editor use Bellatrix's Explorer window, never their old tasks.
        runtime.openExplorer = { guestPath ->
            if (!isFinishing && !isDestroyed) runOnUiThread { c0("UIExplorer", UIExplorer.filesArg(guestPath)) }
        }
        runtime.openBellatrixEditor = { guestPath, done ->
            if (!isFinishing && !isDestroyed) runOnUiThread { c0("UIExplorer", UIExplorer.editorArg(guestPath, done)) }
        }
        runtime.openVoiceWindow = {
            // A non-null marker makes an already hidden UIVoice call k0(): another mic tap
            // is a new conversation, not just a return from the taskbar.
            if (!isFinishing && !isDestroyed) runOnUiThread { c0("UIVoice", "reopen") }
        }
        // Coding Agents and Linux are pages of this one Bellatrix Settings window, never OWL screens.
        runtime.openCodingAgents = { if (!isFinishing && !isDestroyed) runOnUiThread { c0("UISettings", "10") } }
        runtime.openLinuxSettings = { if (!isFinishing && !isDestroyed) runOnUiThread { c0("UISettings", "9") } }
        runtime.onLinuxUnavailable = { if (!isFinishing && !isDestroyed) runOnUiThread { c0("UISettings", "9") } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_SETTINGS_PAGE)?.let { page ->
            pendingSettingsPage = page
            if (!isFinishing && !isDestroyed) runOnUiThread { c0("UISettings", page) }
        }
    }

    override fun onStart() {
        super.onStart()
        val runtime = (application as App).runtime
        installRuntimeHooks()
        pendingSettingsPage?.let { page ->
            pendingSettingsPage = null
            c0("UISettings", page)
        }
        // Agents install .desktop entries in the guest; coming home picks them up in the
        // same drawer, alongside Android apps and the built-in windows.
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { runtime.refreshApps() }
            M0?.D(null)
        }
        val networkMonitor = this.k0
        if (networkMonitor != null && Build.VERSION.SDK_INT >= 24) {
            val connectivityManager = networkMonitor.f4909a
            if (connectivityManager != null && !networkMonitor.c) {
                try {
                    connectivityManager.registerDefaultNetworkCallback(networkMonitor.b)
                    networkMonitor.c = true
                } catch (_: Throwable) {}
            }
        }
        if (this.q1 == null) {
            this.q1 = BatteryReceiver(e(this))
        }
        val intentRegisterReceiver = this.q1?.let { registerReceiver(it, u1) }
        if (intentRegisterReceiver != null) {
            val intExtra = intentRegisterReceiver.getIntExtra("level", -1)
            if (intExtra != this.t1) {
                this.t1 = intExtra
                this.q1?.onReceive(this, intentRegisterReceiver)
            }
        }
        if (this.r1 == null) {
            this.r1 = VolumeObserver(this.W ?: this, this)
        }
        val volumeObserver = this.r1
        if (volumeObserver != null) {
            try {
                volumeObserver.resolver.registerContentObserver(Settings.System.CONTENT_URI, true, volumeObserver)
            } catch (_: Throwable) {}
            val audioManager = volumeObserver.audioManager
            val i2 = volumeObserver.b
            val streamVolume = audioManager.getStreamVolume(i2)
            val streamMaxVolume = audioManager.getStreamMaxVolume(i2)
            if (streamVolume != volumeObserver.f4912a) {
                volumeObserver.f4912a = streamVolume
                val onVolumeChangeListener = volumeObserver.listener
                onVolumeChangeListener?.X(streamVolume, streamMaxVolume)
            }
        }
        (application as App).runtime.screenStarted()
    }

    override fun onStop() {
        (application as App).runtime.clearHomeHooks()
        (application as App).runtime.screenStopped()
        super.onStop()
        val volumeObserver = this.r1
        try {
            volumeObserver?.let { vo -> vo.resolver.unregisterContentObserver(vo) }
        } catch (_: Throwable) {}
        val networkMonitor = this.k0
        if (networkMonitor != null) {
            val connectivityManager = networkMonitor.f4909a
            if (connectivityManager != null) {
                try {
                    connectivityManager.unregisterNetworkCallback(networkMonitor.b)
                    networkMonitor.c = false
                } catch (_: Throwable) {}
            }
        }
        val batteryReceiver = this.q1
        if (batteryReceiver != null) {
            try { unregisterReceiver(batteryReceiver) } catch (_: Throwable) {}
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || Global.c == 0) return
        val z2 = Global.g
        if (z2 || Global.f4840h) {
            val view = this.Y
            if (this.V == 2) {
                Navigation.a(Global.f4837d, view!!, z2)
            }
        }
    }

    override fun p(fragment: UIFragment?) {
        val uIFragment = fragment ?: return
        val taskBarAdapter = this.t0
        val removing = fragmentBool(uIFragment, "G")
        if (!uIFragment.isAdded || removing) {
            return
        }
        val idx = TaskBarDB.a(Windows.taskTag(uIFragment) ?: "")
        val visible = if (fragmentBool(uIFragment, "t")) true else uIFragment.isVisible
        if (Windows.b != idx) {
            if (!visible) {
                windowsCall("e", Windows.f4881d, uIFragment)
            }
            uIFragment.g0()
            windowsCall("j", 4, taskBarAdapter, uIFragment)
            return
        }
        if (visible) {
            uIFragment.g0()
            return
        }
        windowsCall("e", Windows.f4881d, uIFragment)
        uIFragment.g0()
        windowsCall("j", 2, taskBarAdapter, uIFragment)
    }

    override fun p0(btn: TaskbarBtn?) {
        val taskbarBtn = btn ?: return
        taskbarBtn.setOnBtnClickListener {
            val mainActivity = this
            val mainActivity2 = mainActivity.W!!
            val viewGroup = mainActivity.f0!!
            var widgetCenter = mainActivity.f1
            val zY0 = mainActivity.y0()
            val activityResultLauncher = mainActivity.k1
            val taskbarBtn2 = taskbarBtn
            if (widgetCenter == null) {
                widgetCenter = WidgetCenter(mainActivity2, activityResultLauncher!!)
                try {
                    val cls = Class.forName("com.bellatrixos.app_video_player.a")
                    val ctor = cls.constructors.firstOrNull { it.parameterCount == 2 }
                    val listener = ctor?.newInstance(1, taskbarBtn2) as? PopupWindow.OnDismissListener
                    if (listener != null) widgetCenter.setOnDismissListener(listener)
                } catch (_: Throwable) {}
            }
            val i2 = widgetCenter.f4820d
            val i3 = InsetManager.c
            if (i2 != i3) {
                widgetCenter.f4819a.setPadding(i3, InsetManager.f4864a, InsetManager.f4865d, Windows.f4880a + InsetManager.b)
                widgetCenter.f4820d = InsetManager.c
            }
            try {
                val cls = Class.forName("androidx.media3.exoplayer.audio.g")
                val ctor = cls.constructors.firstOrNull { it.parameterCount == 3 }
                val r = ctor?.newInstance(widgetCenter, zY0, 4) as? Runnable
                if (r != null) AppExecutors.c().postDelayed(r, 480L)
            } catch (_: Throwable) {}
            if (!widgetCenter.p && zY0) {
                widgetCenter.f4821e.animate().alpha(0.09f).setDuration(200L).start()
            }
            widgetCenter.showAtLocation(viewGroup, 8388693, 0, 0)
            try {
                val cls = Class.forName("com.bellatrixos.taskbar.center_widgets.a")
                val ctor = cls.constructors.firstOrNull { it.parameterCount == 2 }
                val r = ctor?.newInstance(widgetCenter, 0) as? Runnable
                if (r != null) AppExecutors.c().postDelayed(r, 500L)
            } catch (_: Throwable) {}
            taskbarBtn2.isActivated = true
            mainActivity.f1 = widgetCenter
        }
    }

    override fun q0(z: Boolean) {
        val relativeLayout = this.r0
        val relativeLayout2 = this.L0
        val actionCenter = this.e1
        val dialogMoreIcons = this.g1
        val widgetCenter = this.f1
        relativeLayout?.backgroundTintList = if (z) ColorStateList.valueOf(Global.c) else null
        relativeLayout2?.backgroundTintList = if (z) ColorStateList.valueOf(Global.c) else null
        if (actionCenter != null) {
            val i2 = Global.c
            val frameLayout = actionCenter.f4809h
            if (frameLayout != null) {
                frameLayout.backgroundTintList = if (z) ColorStateList.valueOf(i2) else null
            }
        }
        if (dialogMoreIcons != null) {
            val i3 = Global.c
            val gridLayout = dialogMoreIcons.b
            if (gridLayout != null) {
                gridLayout.backgroundTintList = if (z) ColorStateList.valueOf(i3) else null
            }
        }
        if (widgetCenter != null) {
            val i4 = Global.c
            val frameLayout2 = widgetCenter.c
            if (frameLayout2 != null) {
                frameLayout2.backgroundTintList = if (z) ColorStateList.valueOf(i4) else null
            }
        }
        SharedPrefs.f4755a?.edit()?.putBoolean("start_and_taskbar_has_ui_accent", z)?.apply()
    }

    override fun r(str: String) {
        this.S0?.b?.text = str
    }

    override fun s(str: String) {
        val mainActivity = this.W ?: return
        val fragmentManager = this.a0 ?: return
        val startMenuAdapter = this.M0
        val desktopAdapter = this.p0
        val dialogAppEdit = DialogAppEdit(mainActivity, fragmentManager, str)
        dialogAppEdit.D = DialogAppEdit.OnItemUpdateListener { _: Boolean ->
            startMenuAdapter?.G(str)
            desktopAdapter?.E(str)
        }
        dialogAppEdit.show()
    }

    override fun t(view: View?, i2: Int) {
        val item = this.C0.getOrNull(i2) as? TaskBarItem ?: return
        val strA = item.a() ?: return
        this.o1 = view
        M(strA, null, this.p1)
    }

    override fun v(bitmap: Bitmap?) {
        this.e0 = bitmap
    }

    override fun x(i2: Int) {
        val mainActivity = this.W ?: return
        val linearLayout = this.A0 ?: return
        val linearLayout2 = this.B0
        val coordinatorLayout = this.I0 ?: return
        var width = 0
        if (i2 != 1) {
            if (i2 == 0) {
                val layoutParams2 = linearLayout.layoutParams as RelativeLayout.LayoutParams
                layoutParams2.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE)
                linearLayout.layoutParams = layoutParams2
                val layoutParams = coordinatorLayout.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.startToStart = 0
                layoutParams.endToEnd = 0
            }
        } else {
            val layoutParams3 = linearLayout.layoutParams as RelativeLayout.LayoutParams
            layoutParams3.addRule(RelativeLayout.CENTER_HORIZONTAL, 0)
            linearLayout.layoutParams = layoutParams3
            val layoutParams = coordinatorLayout.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.startToStart = ConstraintLayout.LayoutParams.UNSET
            coordinatorLayout.layoutParams = layoutParams
        }
        width = linearLayout2?.width ?: 0
        val dimensionPixelSize = mainActivity.resources.getDimensionPixelSize(R.dimen.ui_3dp)
        linearLayout.setPadding(if (i2 == 0) width else 0, dimensionPixelSize, width, dimensionPixelSize)
        val taW = this.W ?: this
        val taB0 = this.B0
        if (taB0 != null) {
            TaskBar.a(taW, i2, taB0, this.w0, this)
        }
    }

    fun y0(): Boolean {
        for (fragment in this.a0?.fragments.orEmpty()) {
            if (fragment != null && fragment.isVisible) {
                // keep scanning
            }
        }
        return Global.b
    }

    fun z0(i2: Int) {
        if (i2 == 1 && this.N0 == null) {
            val mainActivity = this.W ?: return
            val arrayList = this.J0 ?: ArrayList<Any?>()
            val recyclerView = this.K0 ?: return
            val startMenuTextWatcher = this.T0 ?: return
            recyclerView.layoutManager = LinearLayoutManager(mainActivity, RecyclerView.VERTICAL, false)
            val searchAppAdapter = SearchAppAdapter(mainActivity, arrayList)
            recyclerView.adapter = searchAppAdapter

            try {
                @Suppress("UNCHECKED_CAST")
                searchAppAdapter.F = com.bellatrixos.searchapp.b(e(this), 0) as? com.bellatrixos.context_menu.OnItemClickListener<Any>
            } catch (_: Throwable) {}
            try {
                @Suppress("UNCHECKED_CAST")
                searchAppAdapter.E = com.bellatrixos.searchapp.c(0, e(this)) as? com.bellatrixos.context_menu.OnItemClickListener<Any>
            } catch (_: Throwable) {}
            try {
                searchAppAdapter.D = com.bellatrixos.app_notepad.a(g(this, 0), 2) as? com.bellatrixos.searchapp.OnFilterCompleteListener
            } catch (_: Throwable) {}

            try {
                val filter = searchAppAdapter.G as? Filter
                startMenuTextWatcher.w = filter
                filter?.filter("")
            } catch (_: Throwable) {}
            try {
                val touchListener = com.bellatrixos.searchapp.d(0)
                @Suppress("UNCHECKED_CAST")
                recyclerView.setOnTouchListener(touchListener as? View.OnTouchListener)
            } catch (_: Throwable) {}
            this.N0 = searchAppAdapter

            val mainActivity2 = this.W!!
            if (SearchApp.c == null) {
                SearchApp.c = ContextCompat.getDrawable(mainActivity2, R.drawable.ui_sm_expand_up_right_20_regular)
            }
            if (SearchApp.f4769d == null) {
                SearchApp.f4769d = ContextCompat.getDrawable(mainActivity2, R.drawable.ic_settings_20_regular)
            }
            if (SearchApp.f4770e == null) {
                SearchApp.f4770e = ContextCompat.getDrawable(mainActivity2, R.drawable.ui_sm_translate_20_regular)
            }
            if (SearchApp.f4771f == null) {
                SearchApp.f4771f = ContextCompat.getDrawable(mainActivity2, R.drawable.ui_sm_history_20_regular)
            }
            if (SearchApp.b == null) {
                @Suppress("UNCHECKED_CAST")
                SearchApp.b = arrayOfNulls<Runnable?>(1) as Array<Runnable>
            }
            if (SearchApp.f4768a == null) {
                SearchApp.f4768a = Handler(Looper.getMainLooper())
            }

            val mainActivity3 = this.W!!
            val searchAppAdapter2 = this.N0!!
            val textView = this.V0!!
            val textView2 = this.W0!!
            val startMenuViews = this.S0!!
            startMenuViews.f4793f.imeOptions = 301989888

            textView.setOnClickListener {
                if (startMenuViews.f4793f.length() < 2) {
                    c0("UIBrowser", "https://www.google.com/search?q=Translate")
                } else {
                    var found: SearchItem? = null
                    for (obj in searchAppAdapter2.C) {
                        val item = obj as? SearchItem ?: continue
                        if (item.f4773a == 0) { found = item; break }
                    }
                    if (found != null) {
                        val sm = found.b
                        if (sm != null) {
                            IntentApps.b(mainActivity3, sm.c() ?: "", null as StartMenuItem?, this)
                        } else {
                            val sug = found.c
                            if (sug != null) {
                                val sc = sug.c
                                if (sc != null) c0("UISettings", sc)
                            }
                        }
                    }
                }
            }

            textView2.setOnClickListener {
                if (startMenuViews.f4793f.length() < 2) {
                    c0("UIBrowser", "https://www.google.com/search?q=Today%20in%20history")
                } else {
                    var found: SearchItem? = null
                    for (obj in searchAppAdapter2.C) {
                        val item = obj as? SearchItem ?: continue
                        if (item.f4773a == 0) { found = item; break }
                    }
                    if (found != null) {
                        val sm = found.b
                        if (sm != null) {
                            IntentSites.a(mainActivity3, sm.c() ?: "")
                        } else {
                            val sug = found.c
                            if (sug != null) {
                                c0("UISettings", null)
                            }
                        }
                    }
                }
            }

            startMenuViews.l.setOnCheckedChangeListener { _, checkedId ->
                val sa = searchAppAdapter2
                when (checkedId) {
                    R.id.filter_apps -> {
                        if (sa.x == 1) return@setOnCheckedChangeListener
                        sa.x = 1
                    }
                    R.id.filter_settings -> {
                        if (sa.x == 2) return@setOnCheckedChangeListener
                        sa.x = 2
                    }
                    else -> {
                        if (sa.x == 0) return@setOnCheckedChangeListener
                        sa.x = 0
                    }
                }
                try {
                    (sa.G as? Filter)?.filter(sa.y)
                } catch (_: Throwable) {}
            }
            try {
                val cl = com.bellatrixos.app_browser.widgets.dialogs.a(2, startMenuViews)
                @Suppress("UNCHECKED_CAST")
                startMenuViews.m.setOnClickListener(cl as? View.OnClickListener)
            } catch (_: Throwable) {}
        }
    }

    private fun windowsCall(name: String, vararg args: Any?) {
        try {
            val methods = Windows::class.java.methods
            val m = methods.firstOrNull { it.name == name && it.parameterCount == args.size } ?: return
            m.invoke(Windows, *args)
        } catch (_: Throwable) {
        }
    }

    private fun fragmentBool(fragment: Fragment, name: String): Boolean {
        return try {
            val m = fragment.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 }
            if (m != null) (m.invoke(fragment) as? Boolean) ?: false
            else {
                val f = fragment.javaClass.getField(name)
                f.getBoolean(fragment)
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun fragmentString(fragment: Fragment, name: String): String? {
        return try {
            val m = fragment.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 }
            if (m != null) m.invoke(fragment) as? String
            else fragment.javaClass.getField(name).get(fragment) as? String
        } catch (_: Throwable) {
            null
        }
    }

}
