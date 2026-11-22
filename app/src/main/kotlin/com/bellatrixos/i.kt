package com.bellatrixos

import android.view.View
import android.view.animation.Animation
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import com.bellatrixos.context_menu.UIContextMenu
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.bellatrixos.context_menu.UIMenuItem
import com.bellatrixos.startmenu.StartMenuAdapter
import com.bellatrixos.taskbar.center_action.ActionCenter
import com.bellatrixos.taskbar.center_more_icons.DialogMoreIcons
import com.bellatrixos.taskbar.center_more_icons.MoreIconsManager
import com.bellatrixos.variables.Global
import com.bellatrixos.windows.InsetManager
import com.bellatrixos.windows.Windows
import com.bellatrixos.windows.dialogs.Dialogs

/** OnClickListener synthetic untuk tombol taskbar dan menu Start. */
class i(@JvmField val u: Int, @JvmField val v: Any?) : View.OnClickListener {

    /** Pemegang objek bisa MainActivity maupun AnonymousClass1 miliknya. */
    private val outerActivity: MainActivity?
        get() = MainActivityBridge.outer(v)

    override fun onClick(view: View) {
        when (u) {
            0 -> {
                val mainActivity = v as MainActivity
                val act: MainActivity = mainActivity.W ?: mainActivity
                val f0 = mainActivity.f0 ?: return
                val q0 = mainActivity.Q0 ?: return
                mainActivity.e1 = ActionCenter.b(act, 0, f0, mainActivity.e1, q0, mainActivity.y0())
            }
            1 -> {
                val mainActivity3 = v as MainActivity
                val act: MainActivity = mainActivity3.W ?: mainActivity3
                val textClock = mainActivity3.G0 as? android.widget.TextClock ?: return
                val f0 = mainActivity3.f0 ?: return
                mainActivity3.e1 = ActionCenter.b(act, 1, f0, mainActivity3.e1, textClock, mainActivity3.y0())
            }
            2 -> {
                val mainActivity5 = v as MainActivity
                val act: MainActivity = mainActivity5.W ?: mainActivity5
                if (Global.B == 1 && Global.C) {
                    mainActivity5.c0("UISettings", "8")
                } else {
                    var dialogMoreIcons = mainActivity5.g1
                    val imageButton = mainActivity5.F0 as? android.widget.ImageButton ?: return
                    if (dialogMoreIcons == null) {
                        dialogMoreIcons = DialogMoreIcons(act, MainActivityBridge.asIn(mainActivity5))
                        dialogMoreIcons.setOnDismissListener(
                            com.bellatrixos.app_video_player.a(2, imageButton)
                        )
                        val iArr = IntArray(2)
                        imageButton.getLocationOnScreen(iArr)
                        DialogMoreIcons.f4815e =
                            (imageButton.width / 2 + iArr[0]) - DialogMoreIcons.f4816f / 2
                    }
                    if (dialogMoreIcons.f4818d != InsetManager.c) {
                        dialogMoreIcons.f4817a.setPadding(0, 0, 0, Windows.f4880a + InsetManager.b)
                        dialogMoreIcons.f4818d = InsetManager.c
                        val iArr2 = IntArray(2)
                        imageButton.getLocationOnScreen(iArr2)
                        DialogMoreIcons.f4815e =
                            (imageButton.width / 2 + iArr2[0]) - DialogMoreIcons.f4816f / 2
                    }
                    val gridLayout: GridLayout? = dialogMoreIcons.b
                    if (gridLayout != null) {
                        Dialogs.f4894e = null
                        val animation: Animation? = Dialogs.f4892a
                        animation?.setAnimationListener(Dialogs.f4895f)
                        animation?.let { gridLayout.startAnimation(it) }
                    }
                    dialogMoreIcons?.showAtLocation(imageButton, 8388691, DialogMoreIcons.f4815e, 0)
                    MoreIconsManager.a(true, imageButton)
                    mainActivity5.g1 = dialogMoreIcons
                }
            }
            3 -> (v as MainActivity).U0?.let { it.state = BottomSheetBehavior.STATE_EXPANDED }
            4 -> {
                val mainActivity7 = v as MainActivity
                mainActivity7.c0("UISettings", "5")
            }
            5 -> {
                val mainActivity8 = v as MainActivity
                val act: MainActivity = mainActivity8.W ?: mainActivity8
                val imageView: ImageView = mainActivity8.Y0 ?: return
                val uIContextMenu = mainActivity8.i0 ?: return
                val arrayList = uIContextMenu.f4745d
                arrayList.clear()
                arrayList.add(
                    UIMenuItem(
                        0, R.id.action_sort_default,
                        if (Global.l == 0) R.drawable.menu_ic_checkmark_20_filled
                        else R.drawable.menu_ic_blank_20_regular,
                        act.getString(R.string.st_menu_apps_order_default)
                    )
                )
                arrayList.add(
                    UIMenuItem(
                        0, R.id.action_sort_asc,
                        if (Global.l == 1) R.drawable.menu_ic_checkmark_20_filled
                        else R.drawable.menu_ic_blank_20_regular,
                        act.getString(R.string.st_menu_apps_order_asc)
                    )
                )
                arrayList.add(
                    UIMenuItem(
                        0, R.id.action_sort_desc,
                        if (Global.l == 2) R.drawable.menu_ic_checkmark_20_filled
                        else R.drawable.menu_ic_blank_20_regular,
                        act.getString(R.string.st_menu_apps_order_desc)
                    )
                )
                uIContextMenu.c = com.bellatrixos.startmenu.a(MainActivityBridge.asIn(mainActivity8), 1)
                uIContextMenu.a(R.style.AnimationDialogFadeDown)
                uIContextMenu.g(imageView, act.resources.getDimensionPixelSize(R.dimen.ui_6dp))
            }
            6 -> {
                val mainActivity10 = v as MainActivity
                val act: MainActivity = mainActivity10.W ?: mainActivity10
                val textView: TextView = mainActivity10.d1 ?: return
                val uIContextMenu2 = mainActivity10.i0 ?: return
                val arrayList2 = uIContextMenu2.f4745d
                arrayList2.clear()
                val string = act.getString(R.string.st_menu_apps_order_all)
                var i6 = R.drawable.menu_ic_dot_6_accent_regular
                arrayList2.add(
                    UIMenuItem(
                        0, R.id.action_list_all,
                        if (Global.f4842k == 0) R.drawable.menu_ic_dot_6_accent_regular
                        else R.drawable.ic_statusbar_24_regular,
                        string
                    )
                )
                arrayList2.add(UIMenuItem(1, 0, 0, ""))
                // App types are selected here, not injected as category rows inside All.
                val selectedDot = R.drawable.menu_ic_dot_6_accent_regular
                val unselectedDot = R.drawable.menu_ic_blank_20_regular
                val kindFilters =
                    listOf(
                        StartMenuAdapter.FILTER_BELLATRIX to R.string.start_filter_bellatrix,
                        StartMenuAdapter.FILTER_ANDROID to R.string.start_filter_android,
                        StartMenuAdapter.FILTER_LINUX to R.string.start_filter_linux,
                    )
                for ((mode, label) in kindFilters) {
                    arrayList2.add(
                        UIMenuItem(
                            0, mode,
                            if (Global.f4842k == mode) selectedDot else unselectedDot,
                            act.getString(label)
                        )
                    )
                }
                arrayList2.add(UIMenuItem(1, 0, 0, ""))
                arrayList2.add(
                    UIMenuItem(
                        0, R.id.action_list_freeform,
                        if (Global.f4842k == StartMenuAdapter.MODE_FREEFORM) R.drawable.menu_ic_dot_6_accent_regular
                        else R.drawable.ic_multitasking_24,
                        act.getString(R.string.st_menu_apps_order_freeform)
                    )
                )
                if (StartMenuAdapter.H) {
                    arrayList2.add(UIMenuItem(1, 0, 0, ""))
                    val string2 = act.getString(R.string.st_menu_apps_order_work)
                    if (Global.f4842k != 3) {
                        i6 = R.drawable.menu_ic_work_24_regular
                    }
                    arrayList2.add(UIMenuItem(0, R.id.action_list_work, i6, string2))
                }
                uIContextMenu2.c = com.bellatrixos.startmenu.a(MainActivityBridge.asIn(mainActivity10), 0)
                uIContextMenu2.a(R.style.AnimationDialogFadeDown)
                uIContextMenu2.g(textView, act.resources.getDimensionPixelSize(R.dimen.ui_6dp))
            }
            else -> outerActivity?.A0(false)
        }
    }
}
