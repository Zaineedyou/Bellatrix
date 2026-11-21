package com.bellatrixos

import android.content.res.ColorStateList
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.bellatrixos.ads.ConsentManager
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.searchapp.b
import com.bellatrixos.startmenu.StartMenu
import com.bellatrixos.startmenu.StartMenuAdapter
import com.bellatrixos.startmenu.StartMenuItem
import com.bellatrixos.startmenu.StartMenuRecommend
import com.bellatrixos.startmenu.StartMenuViews
import com.bellatrixos.startmenu.listeners.StartMenuBG
import com.bellatrixos.startmenu.listeners.StartMenuTextWatcher
import com.bellatrixos.variables.Global
import com.bellatrixos.wallpaper.models.Bonds
import com.bellatrixos.windows.drivers.network.NetworkMonitor

/** Runnable synthetic yang menyiapkan Start Menu dan taskbar. */
class f(
    @JvmField val v: MainActivity,
    @JvmField val w: Int,
    @JvmField val u: Int
) : Runnable {

    override fun run() {
        when (u) {
            0 -> {
                val mainActivity = v
                if (!mainActivity.c0) {
                    val arrayList = ArrayList<StartMenuItem>()
                    @Suppress("UNCHECKED_CAST")
                    val castList = arrayList as ArrayList<Any?>
                    mainActivity.J0 = castList
                    val mainActivity2: MainActivity = mainActivity.W ?: mainActivity
                    val startMenuViews = mainActivity.S0 ?: return
                    val gVar = g(mainActivity, 1)
                    val gVar2 = g(mainActivity, 2)
                    val eVar = e(mainActivity)
                    val i2 = w
                    val fVar = f(mainActivity, i2, 1)
                    AppExecutors.b().execute(Runnable {
                        val startMenuAdapter = com.bellatrixos.startmenu.StartMenuAdapter(mainActivity2, arrayList)
                        val handlerC = AppExecutors.c()
                        val eVar2: e = eVar
                        val gVar3: g = gVar
                        val gVar4: g = gVar2
                        val fVar2: f = fVar
                        val startMenuViews2 = startMenuViews
                        val i3 = i2
                        handlerC.post {
                            StartMenu.f4780e?.a(false)
                            val recyclerView: RecyclerView = startMenuViews2.f4792e
                            recyclerView.adapter = startMenuAdapter
                            startMenuAdapter.D = com.bellatrixos.searchapp.b(eVar2, 1)
                            val startMenuTextWatcher = StartMenuTextWatcher(i3, startMenuAdapter.getFilter())
                            gVar3.accept(startMenuTextWatcher)
                            startMenuViews2.f4793f.addTextChangedListener(startMenuTextWatcher)
                            gVar4.accept(startMenuAdapter)
                            fVar2.run()
                        }
                    })
                }
                val startMenuViews2 = mainActivity.S0 ?: return
                if (!mainActivity.c0) {
                    val relativeLayout: RelativeLayout = startMenuViews2.f4790a
                    if (Global.b) {
                        // androidx.core.content.res.a (blur helper hasil obfuskasi) belum tersedia.
                        AppExecutors.b().execute(Runnable {
                            Bonds.a(relativeLayout)
                            startMenuViews2.f4791d.invalidate()
                        })
                    }
                    if (SharedPrefs.b()) {
                        relativeLayout.backgroundTintList = ColorStateList.valueOf(Global.c)
                    }
                } else {
                    val relativeLayout2: RelativeLayout = startMenuViews2.f4790a
                    val startMenuBG = StartMenu.f4780e ?: return
                    if (startMenuBG.f4800a && StartMenu.f4778a) {
                        startMenuBG.a(false)
                        AppExecutors.b().execute(Runnable {
                            Bonds.a(relativeLayout2)
                            startMenuViews2.f4791d.invalidate()
                        })
                    }
                }
            }
            else -> {
                val mainActivity3 = v
                val act3: MainActivity = mainActivity3.W ?: mainActivity3
                mainActivity3.c0 = true
                mainActivity3.d1?.setText(act3.getString(StartMenuAdapter.labelRes(Global.f4842k)))
                mainActivity3.z0(w)
                mainActivity3.O0?.b()
                val mainActivity4 = act3
                val linearLayout: LinearLayout = mainActivity3.R0 ?: return
                if (linearLayout.height < mainActivity4.resources.getDimension(R.dimen.ui_94dp).toInt()) {
                    StartMenuRecommend.c = false
                    linearLayout.visibility = View.GONE
                } else {
                    linearLayout.visibility = View.VISIBLE
                    if (Global.f4838e * 58.0f < 32.0f) {
                        StartMenuRecommend.c = false
                    } else if (NetworkMonitor.f4907d) {
                        StartMenuRecommend.c = true
                        if (ConsentManager.c == null) {
                            val consentManager = ConsentManager()
                            consentManager.f4636a = false
                            ConsentManager.c = consentManager
                        }
                        ConsentManager.c?.b(
                            mainActivity3,
                            com.bellatrixos.startmenu.m(1, act3, linearLayout)
                        )
                    } else {
                        StartMenuRecommend.b = false
                        StartMenuRecommend.c = true
                    }
                }
                mainActivity3.U?.setOnClickListener(i(3, mainActivity3))
                mainActivity3.M0!!.E = e(mainActivity3)
                mainActivity3.P0?.setOnClickListener(i(4, mainActivity3))
                mainActivity3.Y0?.setOnClickListener(i(5, mainActivity3))
                mainActivity3.d1?.setOnClickListener(i(6, mainActivity3))
                val relativeLayout3: RelativeLayout = mainActivity3.L0 ?: return
                val cVar = c(mainActivity3, 2)
                relativeLayout3.setOnTouchListener(com.bellatrixos.searchapp.d(1))
                relativeLayout3.setOnLongClickListener(com.bellatrixos.startmenu.b(0, cVar))
                relativeLayout3.setOnGenericMotionListener { _, motionEvent ->
                    var z = false
                    if (motionEvent.action == MotionEvent.ACTION_BUTTON_PRESS &&
                        motionEvent.buttonState == MotionEvent.BUTTON_SECONDARY
                    ) {
                        val iArr = Global.A
                        iArr[0] = motionEvent.rawX.toInt()
                        z = true
                        iArr[1] = motionEvent.rawY.toInt()
                        if (StartMenu.f4779d != 1) {
                            cVar.run()
                        }
                    }
                    z
                }
            }
        }
    }
}
