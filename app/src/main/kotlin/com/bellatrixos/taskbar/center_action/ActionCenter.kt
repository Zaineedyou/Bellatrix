package com.bellatrixos.taskbar.center_action

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.database.SharedPrefs
import com.bellatrixos.variables.Global
import com.bellatrixos.windows.InsetManager
import com.bellatrixos.windows.Windows
import com.bellatrixos.windows.config.Dates
import com.bellatrixos.windows.dialogs.Dialogs
import com.bellatrixos.windows.drivers.battery.Battery
import com.bellatrixos.windows.drivers.battery.BatteryState
import com.bellatrixos.windows.drivers.network.Network
import com.bellatrixos.windows.drivers.network.NetworkMonitor
import com.bellatrixos.windows.drivers.volume.UIVolume
import com.bellatrixos.interfaces.MainActivityIn
import com.bellatrixos.ui.nativeui.NativeUiLook

/** Panel cepat akses (wifi, baterai, volume, bluetooth) di atas taskbar. */
@SuppressLint("InflateParams")
class ActionCenter(@JvmField val f4808f: Context) : PopupWindow() {
    @JvmField val f4805a: View
    @JvmField val b: ViewStub
    @JvmField val f4806d: View
    @JvmField val g: ImageView
    @JvmField val f4809h: FrameLayout
    @JvmField val f4810i: SeekBar
    @JvmField val j: TextView
    @JvmField val f4811k: TextView
    @JvmField val l: ImageButton
    @JvmField val m: ImageButton
    @JvmField val f4812n: ImageButton
    @JvmField var f4807e: TextView? = null
    @JvmField var o: Int
    @JvmField var c: View? = null
    @JvmField var p: Boolean = false
    @JvmField var q: Boolean = true

    init {
        val root = LayoutInflater.from(f4808f).inflate(R.layout.taskbar_dialog_action_center, null as ViewGroup?)
        f4806d = root
        contentView = root
        NativeUiLook.popup(root, "action-center")
        width = -2
        height = -1
        isClippingEnabled = false
        f4809h = root.findViewById(R.id.mainView)
        f4805a = root.findViewById(R.id.viewWidgets)
        b = root.findViewById(R.id.stubDate)
        g = root.findViewById(R.id.ACBlur)
        f4811k = root.findViewById(R.id.txtWifi)
        j = root.findViewById(R.id.txtBattery)
        l = root.findViewById(R.id.btnDeviceWifi)
        m = root.findViewById(R.id.btnDeviceInternet)
        f4812n = root.findViewById(R.id.btnDeviceBluetooth)
        f4810i = root.findViewById(R.id.deviceAudioLevel)
        val nearby = root.findViewById<ImageButton>(R.id.btnNearbyShare)
        val theme = root.findViewById<ImageButton>(R.id.btnDeviceTheme)
        val settings = root.findViewById<ImageButton>(R.id.btnDeviceSettings)
        val accessibility = root.findViewById<ImageButton>(R.id.btnDeviceAccessibility)
        root.setPadding(InsetManager.c, InsetManager.f4864a, InsetManager.f4865d, Windows.f4880a + InsetManager.b)
        o = InsetManager.c
        f4810i.thumbTintMode = null
        if (SharedPrefs.b()) {
            f4809h.backgroundTintList = ColorStateList.valueOf(Global.c)
        }
        c(l, settingsRunnable(f4808f, 3))
        c(m, settingsRunnable(f4808f, 4))
        c(settings, settingsRunnable(f4808f, 5))
        c(nearby, settingsRunnable(f4808f, 6))
        c(theme, settingsRunnable(f4808f, 7))
        c(accessibility, settingsRunnable(f4808f, 2))
        val callback = ActionCenterManager.OnStopTrackingTouchCallback { progress ->
            val manager = f4808f.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            val max = manager?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 100
            manager?.setStreamVolume(
                android.media.AudioManager.STREAM_MUSIC,
                Math.round(progress / 100.0f * max),
                0
            )
        }
        f4810i.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.progress?.let { callback.a(it) }
            }
        })
        root.setOnClickListener { dismiss() }
        isFocusable = true
        isOutsideTouchable = true
        setAnimationStyle(0)
        synchronized(BatteryState::class.java) { BatteryState.b = true }
    }

    private fun settingsRunnable(context: Context, kind: Int) = Runnable {
        when (kind) {
            2 -> context.startActivity(Intent("android.settings.ACCESSIBILITY_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            3 -> {
                val action = if (android.os.Build.VERSION.SDK_INT >= 29) "android.settings.panel.action.WIFI" else "android.settings.WIFI_SETTINGS"
                context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            4 -> {
                val action = if (android.os.Build.VERSION.SDK_INT >= 29) "android.settings.panel.action.INTERNET_CONNECTIVITY" else "android.settings.NETWORK_OPERATOR_SETTINGS"
                context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            5 -> (context as? MainActivityIn)?.c0("UISettings", null)
            6 -> {
                val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain" }
                intent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.toast_share))
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.toast_share_via)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            7 -> context.startActivity(Intent("android.settings.DISPLAY_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun c(imageButton: ImageButton, runnable: Runnable) {
        imageButton.setOnClickListener { runnable.run() }
    }

    internal fun dismissNow() {
        super.dismiss()
    }

    override fun dismiss() {
        if (p) return
        p = true
        Dialogs.a(f4809h, r, Runnable { a(this@ActionCenter) })
    }

    companion object {
        @JvmField var r: Int = 0

        @JvmStatic
        fun a(actionCenter: ActionCenter) {
            actionCenter.p = false
            actionCenter.g.animate().alpha(0.0f).setDuration(200L).start()
            actionCenter.dismissNow()
        }

        @JvmStatic
        fun b(
            context: Context,
            i2: Int,
            view: View,
            existing: ActionCenter?,
            view2: View,
            z: Boolean
        ): ActionCenter {
            val actionCenter = existing ?: ActionCenter(context)
            r = i2
            if (actionCenter.o != InsetManager.c) {
                actionCenter.f4806d.setPadding(
                    InsetManager.c,
                    InsetManager.f4864a,
                    InsetManager.f4865d,
                    Windows.f4880a + InsetManager.b
                )
                actionCenter.o = InsetManager.c
            }
            val afterAnimation = Runnable {
                if (actionCenter.q && z) {
                    actionCenter.q = false
                    actionCenter.g.clipToOutline = true
                }
            }
            val frameLayout = actionCenter.f4809h
            Dialogs.f4894e = afterAnimation
            val animation = if (r == 0) Dialogs.f4892a else Dialogs.c
            animation?.setAnimationListener(Dialogs.f4895f)
            animation?.let { frameLayout.startAnimation(it) }
            if (!actionCenter.q && z) {
                actionCenter.g.animate().alpha(0.09f).setDuration(200L).start()
            }
            val widgets = actionCenter.f4805a
            if (r == 0) {
                widgets.visibility = View.VISIBLE
                actionCenter.c?.visibility = View.GONE
            } else if (r == 1) {
                if (actionCenter.c == null) {
                    val inflated = actionCenter.b.inflate()
                    actionCenter.c = inflated
                    actionCenter.f4807e = inflated.findViewById(R.id.date_title)
                    inflated.post(Runnable {
                        val params = actionCenter.c?.layoutParams ?: return@Runnable
                        params.height = ((actionCenter.c?.height ?: 0) * 1.05f).toInt()
                        actionCenter.c?.layoutParams = params
                    })
                }
                widgets.visibility = View.GONE
                actionCenter.c?.visibility = View.VISIBLE
            }
            actionCenter.showAtLocation(view, 8388693, 0, 0)
            if (r == 0) {
                AppExecutors.d().execute(Runnable { refreshStatus(actionCenter) })
            } else {
                actionCenter.f4807e?.text = Dates.a(Global.v, "EEEE, d MMMM")
            }
            view2.isActivated = true
            actionCenter.setOnDismissListener { view2.isActivated = false }
            return actionCenter
        }

        internal fun refreshStatus(actionCenter: ActionCenter) {
            val context = actionCenter.f4808f
            AppExecutors.c().post {
                val info = BatteryState.a()
                if (info != null) {
                    val fresh = synchronized(BatteryState::class.java) {
                        val wasFresh = BatteryState.b
                        BatteryState.b = false
                        wasFresh
                    }
                    if (fresh) {
                        actionCenter.j.text = context.getString(
                            R.string.settings_taskbar_battery_percent,
                            info.level
                        )
                        val drawable = ContextCompat.getDrawable(context, Battery.a(info.level, info.charging))
                        val size = context.resources.getDimensionPixelSize(R.dimen.ui_20dp)
                        drawable?.setBounds(0, 0, size, size)
                        actionCenter.j.setCompoundDrawablesRelative(drawable, null, null, null)
                    }
                }
                actionCenter.l.isActivated = isWifiOn(context)
                actionCenter.m.isActivated = isMobileOn(context)
                actionCenter.f4812n.isActivated = Network.a()
                actionCenter.f4810i.progress = UIVolume.f4911a
                actionCenter.f4811k.text = context.getString(
                    if (NetworkMonitor.f4908e) R.string.action_center_wifi_connected else R.string.action_center_wifi
                )
            }
        }

        private fun isWifiOn(context: Context): Boolean = try {
            val manager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            manager != null && manager.isWifiEnabled
        } catch (ignored: Exception) {
            false
        }

        private fun isMobileOn(context: Context): Boolean = try {
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            @Suppress("DEPRECATION")
            val info = manager?.activeNetworkInfo
            info != null && info.isConnectedOrConnecting && info.type == 0
        } catch (ignored: Exception) {
            false
        }
    }
}
