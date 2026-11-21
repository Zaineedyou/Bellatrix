package com.bellatrixos

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.ColorStateList
import android.graphics.drawable.InsetDrawable
import android.media.SoundPool
import android.net.Uri
import android.os.Handler
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextClock
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.interfaces.BackPressHandler
import com.bellatrixos.startmenu.StartMenu
import com.bellatrixos.startmenu.listeners.StartMenuBG
import com.bellatrixos.taskbar.TaskBar
import com.bellatrixos.taskbar.TaskBarAdapter
import com.bellatrixos.taskbar.utils.ObservableList
import com.bellatrixos.taskbar.utils.TaskBarItemAnimator
import com.bellatrixos.variables.Global
import com.bellatrixos.widgets.TaskbarBtn
import com.bellatrixos.widgets.UINotification
import com.bellatrixos.windows.Updates
import com.bellatrixos.windows.WinLogoDrawable
import com.bellatrixos.windows.WindowSize
import com.bellatrixos.windows.Windows
import com.bellatrixos.windows.blur.UIBlur
import com.bellatrixos.windows.drivers.Display
import com.bellatrixos.windows.drivers.Sounds
import com.bellatrixos.windows.installed_apps.InstalledAppsDB
import com.bellatrixos.windows.installed_apps.models.InstalledApp

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import java.io.File
import java.util.Locale

/** Runnable synthetic yang merakit Start Menu, taskbar dan callback back. */
class l(@JvmField val v: Any, @JvmField val u: Int) : Runnable {

    private val mainActivity: MainActivity
        get() = MainActivityBridge.outer(v) ?: error("MainActivity tidak ditemukan")

    override fun run() {
        when (u) {
            0 -> {
                val anonymousClass1 = v
                val mainActivity2: MainActivity = mainActivity.W ?: mainActivity
                val i5 = mainActivity.V
                val coordinatorLayout = mainActivity.I0 ?: return
                val bottomSheetBehavior: BottomSheetBehavior<*> = mainActivity.U0 ?: return
                val startMenuViews = mainActivity.S0 ?: return
                startMenuViews.f4790a.clipToOutline = true

                val iMax: Int
                if (i5 == 2) {
                    var columns = SharedPrefs.f4755a?.getInt("start_menu_app_columns", 5) ?: 5
                    if (columns == 5) {
                        val dimension = mainActivity2.resources.getDimension(R.dimen.ui_84dp).toInt()
                        val view = startMenuViews.g
                        if ((view?.height ?: 0) > dimension) columns = 6
                        view?.visibility = View.GONE
                    }
                    iMax = columns
                } else {
                    val width = mainActivity2.resources.displayMetrics.widthPixels
                    val itemWidth = mainActivity2.resources.getDimensionPixelSize(R.dimen.start_menu_app_layout_width)
                    iMax = Math.max(1, width / itemWidth)
                }

                val recyclerView: RecyclerView = startMenuViews.f4792e
                if (i5 == 2) {
                    val layoutParams = coordinatorLayout.layoutParams as ConstraintLayout.LayoutParams
                    (layoutParams as ViewGroup.MarginLayoutParams).width =
                        mainActivity2.resources.getDimensionPixelSize(R.dimen.start_menu_padding_edges) +
                            mainActivity2.resources.getDimensionPixelSize(R.dimen.start_menu_app_layout_width) * iMax
                    layoutParams.matchConstraintMaxHeight =
                        mainActivity2.resources.getDimensionPixelSize(R.dimen.start_menu_max_height)
                    coordinatorLayout.layoutParams = layoutParams
                } else {
                    val resources = mainActivity2.resources
                    val itemWidth = resources.getDimensionPixelSize(R.dimen.start_menu_app_layout_width)
                    val rowWidth =
                        ((WindowSize.b - resources.getDimensionPixelSize(R.dimen.start_menu_recommended_padding)) /
                            itemWidth) * itemWidth
                    val layoutParams2 = recyclerView.layoutParams
                    layoutParams2.height = rowWidth
                    recyclerView.layoutParams = layoutParams2
                }
                recyclerView.layoutManager = GridLayoutManager(mainActivity2, iMax)
                bottomSheetBehavior.peekHeight = coordinatorLayout.height
                StartMenu.c =
                    WindowSize.b >= mainActivity2.resources.getDimensionPixelSize(R.dimen.start_menu_min_height) || i5 != 2

                val mainActivity3: MainActivity = mainActivity.W ?: mainActivity
                val startMenuViews2 = mainActivity.S0 ?: return
                if (SharedPrefs.b()) {
                    startMenuViews2.f4790a.backgroundTintList = ColorStateList.valueOf(Global.c)
                }
                startMenuViews2.b.text =
                    SharedPrefs.f4755a?.getString("system_user_name", "This PC") ?: "This PC"
                val imageView: ImageView = startMenuViews2.c
                imageView.clipToOutline = true
                val file = File(mainActivity3.filesDir, "user_image.jpg")
                if (file.exists()) {
                    try {
                        imageView.setImageURI(Uri.fromFile(file))
                        imageView.clipToOutline = true
                        // `break` ditempatkan di sini; setup di bawah tetap dijalankan.
                    } catch (_: Exception) {
                    }
                }
                Global.m = SharedPrefs.f4755a?.getBoolean("start_menu_search_auto_focus", false) ?: false
                val relativeLayout: RelativeLayout = startMenuViews2.f4790a
                val imageButton = relativeLayout.findViewById<ImageButton>(R.id.btnPower)
                if (imageButton != null) {
                    imageButton.setOnClickListener(
                        com.bellatrixos.app_settings.adapters.a(mainActivity3, 3, imageButton)
                    )
                }
                relativeLayout.setOnGenericMotionListener(com.bellatrixos.startmenu.f())
                val imageView2: ImageView = mainActivity.S0?.f4791d ?: return
                val startMenuBG = StartMenuBG()
                startMenuBG.f4800a = false
                StartMenu.f4780e = startMenuBG
                startMenuBG.b = com.bellatrixos.startmenu.g(imageView2)
                val mainActivity4: MainActivity = mainActivity.W ?: mainActivity
                if (SharedPrefs.f4755a?.getBoolean("is_policy_read", false) != true) {
                    Handler().postDelayed(com.bellatrixos.windows.dialogs.d(0, mainActivity4), 60L)
                }
                val mainActivity5: MainActivity = mainActivity.W ?: mainActivity
                val packageManager: PackageManager = mainActivity5.packageManager
                val intent = Intent("android.intent.action.MAIN")
                intent.addCategory("android.intent.category.HOME")
                intent.setPackage(null)
                val resolveInfo: ResolveInfo? = packageManager.resolveActivity(intent, 65536)
                val activityInfo: ActivityInfo? = resolveInfo?.activityInfo
                Global.j = activityInfo != null && mainActivity5.packageName == activityInfo.packageName
                val mainActivity6: MainActivity = mainActivity.W ?: mainActivity
                if (Sounds.f4902a == null) {
                    Sounds.f4903d = SharedPrefs.f4755a?.getFloat("system_sound_volume", 1.0f) ?: 1.0f
                    val soundPoolBuild: SoundPool = SoundPool.Builder().setMaxStreams(1).build()
                    Sounds.f4902a = soundPoolBuild
                    Sounds.b = soundPoolBuild.load(mainActivity6, R.raw.alert, 1)
                    Sounds.c = soundPoolBuild.load(mainActivity6, R.raw.notify, 1)
                }
                Display.f4898a = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == 1
                val applicationContext = mainActivity.applicationContext
                val mVar = m(anonymousClass1)
                if (Updates.f4866a == null) {
                    Updates.f4866a = AppUpdateManagerFactory.create(applicationContext)
                }
                Updates.f4866a?.appUpdateInfo?.addOnSuccessListener { mVar.b() }
            }
            1 -> {
                val anonymousClass2 = v
                val mainActivity7 = mainActivity
                val act7: MainActivity = mainActivity7.W ?: mainActivity7
                val mainActivity8 = act7
                val i6 = mainActivity7.V
                val linearLayout: LinearLayout = mainActivity7.A0 ?: return
                val linearLayout2: LinearLayout? = mainActivity7.B0
                val coordinatorLayout2 = mainActivity7.I0 ?: return
                val taskbarBtn: TaskbarBtn = mainActivity7.w0 ?: return
                if (i6 == 2) {
                    val i7 = SharedPrefs.f4755a?.getInt("taskbar_position", 0) ?: 0
                    Global.f4843n = i7
                    var i3 = Windows.f4884h
                    if (i7 != 0) {
                        if (i7 == 1) {
                            Windows.g = R.anim.app_anim_maximize_left
                            i3 = R.anim.app_anim_minimize_left
                        }
                        if (i7 == 1) {
                            taskbarBtn.visibility = View.GONE
                            val layoutParams3 = linearLayout.layoutParams as RelativeLayout.LayoutParams
                            layoutParams3.addRule(14, 0)
                            linearLayout.layoutParams = layoutParams3
                            val layoutParams4 = coordinatorLayout2.layoutParams as ConstraintLayout.LayoutParams
                            layoutParams4.topToTop = ConstraintLayout.LayoutParams.UNSET
                            coordinatorLayout2.layoutParams = layoutParams4
                        }
                        val width = linearLayout2?.width ?: 0
                        val dimensionPixelSize3 =
                            mainActivity8.resources.getDimensionPixelSize(R.dimen.ui_3dp)
                        val i4 = if (i7 == 0) width else 0
                        linearLayout.setPadding(i4, dimensionPixelSize3, width, dimensionPixelSize3)
                    } else {
                        Windows.g = R.anim.app_anim_maximize_right
                        i3 = R.anim.app_anim_minimize_right
                    }
                    Windows.f4884h = i3
                    if (i7 == 1) {
                        taskbarBtn.visibility = View.GONE
                        val layoutParams5 = linearLayout.layoutParams as RelativeLayout.LayoutParams
                        layoutParams5.addRule(14, 0)
                        linearLayout.layoutParams = layoutParams5
                        val layoutParams6 = coordinatorLayout2.layoutParams as ConstraintLayout.LayoutParams
                        layoutParams6.topToTop = ConstraintLayout.LayoutParams.UNSET
                        coordinatorLayout2.layoutParams = layoutParams6
                    }
                    val width = linearLayout2?.width ?: 0
                    val dimensionPixelSize4 = mainActivity8.resources.getDimensionPixelSize(R.dimen.ui_3dp)
                    val i4 = if (i7 == 0) width else 0
                    linearLayout.setPadding(i4, dimensionPixelSize4, width, dimensionPixelSize4)
                }

                val mainActivity9 = act7
                val i8 = mainActivity7.V
                val linearLayout3: LinearLayout = mainActivity7.A0 ?: return
                if (i8 == 2) {
                    val layoutInflaterFrom = LayoutInflater.from(mainActivity9)
                    val i9 = SharedPrefs.f4755a?.getInt("taskbar_search_app_type", 3) ?: 3
                    val i2 = when (i9) {
                        1 -> R.layout.taskbar_search_icon
                        2 -> R.layout.taskbar_search_icon_n_label
                        3 -> R.layout.taskbar_search_box
                        else -> -1
                    }
                    val viewInflate =
                        if (i2 != -1) layoutInflaterFrom.inflate(i2, linearLayout3 as ViewGroup, false) else null
                    if (viewInflate != null) {
                        viewInflate.setOnClickListener(
                            com.bellatrixos.searchapp.a(MainActivityBridge.asIn(mainActivity7), 1)
                        )
                        linearLayout3.addView(viewInflate, 1)
                    }
                }

                val mainActivity10 = act7
                val i10 = mainActivity7.V
                val linearLayout4: LinearLayout? = mainActivity7.B0
                val taskbarBtn2: TaskbarBtn = mainActivity7.w0 ?: return
                if (i10 == 2) {
                    val z = SharedPrefs.f4755a?.getBoolean("taskbar_btn_widget_visible", true) ?: true
                    Global.o = z
                    val i11 = Global.f4843n
                    if (i11 == 0) {
                        if (z) {
                            mainActivity7.p0(taskbarBtn2)
                        } else {
                            taskbarBtn2.visibility = View.INVISIBLE
                        }
                    } else if (i11 == 1 && z) {
                        val taskbarBtn3 = TaskbarBtn(mainActivity10)
                        taskbarBtn3.setIcon(R.drawable.img_app_widget)
                        mainActivity7.p0(taskbarBtn3)
                        linearLayout4?.addView(taskbarBtn3, 0)
                    }
                }

                val recyclerView2 = mainActivity7.u0 ?: return
                val taskBarAdapter: TaskBarAdapter = mainActivity7.t0 ?: return
                recyclerView2.layoutManager = LinearLayoutManager(mainActivity7, RecyclerView.HORIZONTAL, false)
                recyclerView2.adapter = taskBarAdapter
                recyclerView2.itemAnimator = TaskBarItemAnimator()
                val textClock: TextClock? = mainActivity7.G0
                if (textClock != null) {
                    TaskBar.c(
                        textClock,
                        SharedPrefs.f4755a?.getString("system_date_format", null),
                        SharedPrefs.f4755a?.getString("system_clock_format", null)
                    )
                }
                val taskBarAdapter2: TaskBarAdapter = mainActivity7.t0 ?: return
                taskBarAdapter2.E = e(mainActivity7)
                taskBarAdapter2.F = m(anonymousClass2)
                val blurRelativeLayout = mainActivity7.r0 ?: return
                val blurImageView = mainActivity7.X0 ?: return
                val blurImageView2 = mainActivity7.d0 ?: return
                UIBlur.a(act7, blurRelativeLayout, blurImageView, blurImageView2)

                val mainActivity11 = act7
                if (InstalledAppsDB.f4955a == null) {
                    InstalledAppsDB.f4955a = mainActivity11.getSharedPreferences("InstalledAppsPrefs", 0)
                }
                val listB = InstalledAppsDB.b().toMutableList()
                if (listB.isEmpty()) {
                    listB.add(InstalledApp("UiExplorer", "UIExplorer", "UIExplorer", 0, false))
                    listB.add(InstalledApp("UiSettings", "UISettings", "UISettings", 0, false))
                    listB.add(InstalledApp("UiTerminal", "UITerminal", "UITerminal", 0, false))
                    InstalledAppsDB.f(listB, false)
                }
                InstalledAppsDB.d(listB)
                val taskbarBtn4: TaskbarBtn = mainActivity7.v0 ?: return
                @Suppress("UNCHECKED_CAST")
                val observableList =
                    mainActivity7.C0 as ObservableList<com.bellatrixos.taskbar.TaskBarItem>
                taskbarBtn4.setOnBtnClickListener(
                    com.bellatrixos.app_settings.adapters.a(mainActivity7, 4, observableList as ObservableList<*>)
                )
                AppExecutors.c().postDelayed(c(mainActivity7, 1), 400L)
                TaskBar.b(taskBarAdapter2, linearLayout3, observableList, recyclerView2)
                mainActivity7.s0?.setOnClickListener(i(7, anonymousClass2))

                val taskbarBtn5: TaskbarBtn = mainActivity7.v0 ?: return
                val context: Context = taskbarBtn5.context
                val resources2 = context.resources
                val iB = ContextCompat.getColor(context, R.color.win_logo_start_normal)
                val iB2 = ContextCompat.getColor(context, R.color.win_logo_end_normal)
                val iB3 = ContextCompat.getColor(context, R.color.win_logo_start_dark)
                val iB4 = ContextCompat.getColor(context, R.color.win_logo_end_dark)
                val dimension2 = resources2.getDimension(R.dimen.win_logo_corner_radius)
                val dimension3 = resources2.getDimension(R.dimen.win_logo_line_thickness)
                val dimension4 = resources2.getDimensionPixelSize(R.dimen.win_logo_corner_radius)
                val winLogo = WinLogoDrawable(dimension2, dimension3, iB, iB2)
                StartMenu.f4782h = winLogo
                val insetDrawable = InsetDrawable(winLogo, dimension4)
                StartMenu.f4781f = insetDrawable
                taskbarBtn5.setIcon(insetDrawable)
                taskbarBtn5.setOnBtnOnTouchListener(object : View.OnTouchListener {
                    override fun onTouch(view2: View, motionEvent: MotionEvent): Boolean {
                        val action = motionEvent.action
                        return when (action) {
                            MotionEvent.ACTION_DOWN -> {
                                StartMenu.a(iB, iB2, iB3, iB4)
                                false
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                StartMenu.a(iB3, iB4, iB, iB2)
                                false
                            }
                            else -> false
                        }
                    }
                })
                // NavigationControl belum di-port; pertahankan perilaku inti back/start-menu/toast.
                val callback = object : OnBackPressedCallback(true) {
                    private var exitArmed = false
                    private val resetExit = Runnable { exitArmed = false }
                    override fun handleOnBackPressed() {
                        if (mainActivity7.v0?.isActivated == true) {
                            mainActivity7.U0?.let { it.state = BottomSheetBehavior.STATE_HIDDEN }
                            return
                        }
                        val fragment = n(0).get() as? Fragment
                        if (fragment is BackPressHandler && fragment.b()) {
                            return
                        }
                        if (!Global.j) {
                            if (!exitArmed) {
                                UINotification.a(
                                    act7,
                                    act7.getString(R.string.toast_app_exit),
                                    null
                                )
                                exitArmed = true
                                AppExecutors.c().removeCallbacks(resetExit)
                                AppExecutors.c().postDelayed(resetExit, 2800L)
                                return
                            }
                            exitArmed = false
                        }
                        isEnabled = false
                        mainActivity7.onBackPressedDispatcher.onBackPressed()
                    }
                }
                mainActivity7.j1 = callback
                mainActivity7.onBackPressedDispatcher.addCallback(callback)
            }
            else -> {
                val z2 = SharedPrefs.f4755a?.getBoolean("system_notify_updates", false) ?: false
                Global.C = z2
                if (mainActivity.V == 2 && z2) {
                    mainActivity.F0?.setImageResource(R.drawable.ic_arrow_sync_24_regular)
                }
            }
        }
    }
}
