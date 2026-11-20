package com.bellatrixos.taskbar.center_action

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.bellatrixos.AppExecutors
import com.bellatrixos.R
import com.bellatrixos.windows.drivers.battery.Battery
import com.bellatrixos.windows.drivers.battery.BatteryState
import com.bellatrixos.windows.drivers.network.Network
import com.bellatrixos.windows.drivers.network.NetworkMonitor
import com.bellatrixos.windows.drivers.volume.UIVolume

/** Runnable sintetis untuk aksi async pada ActionCenter. */
class a(
    @JvmField val u: Int,
    @JvmField val v: ActionCenter
) : Runnable {
    override fun run() {
        when (u) {
            0 -> ActionCenter.a(v)
            1 -> {
                val view = v.c ?: return
                val layoutParams: ViewGroup.LayoutParams = view.layoutParams ?: return
                layoutParams.height = (view.height * 1.05f).toInt()
                view.layoutParams = layoutParams
            }
            2 -> {
                val actionCenter = v
                val context = actionCenter.f4808f ?: return
                try {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    val wifiEnabled = wifiManager?.isWifiEnabled == true
                    var mobileConnected = false
                    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    if (connectivityManager != null) {
                        val active: NetworkInfo? = connectivityManager.activeNetworkInfo
                        if (active != null && active.isConnectedOrConnecting && active.type == ConnectivityManager.TYPE_MOBILE) {
                            mobileConnected = true
                        }
                    }
                    val bluetooth = Network.a()
                    AppExecutors.c().post {
                        val ctx = actionCenter.f4808f ?: return@post
                        val info = BatteryState.a()
                        if (info != null) {
                            val changed = synchronized(BatteryState::class.java) {
                                val c = BatteryState.b
                                BatteryState.b = false
                                c
                            }
                            if (changed) {
                                actionCenter.j?.text = ctx.getString(R.string.settings_taskbar_battery_percent, info.level)
                                val drawable = ContextCompat.getDrawable(ctx, Battery.a(info.level, info.charging))
                                val size = ctx.resources.getDimensionPixelSize(R.dimen.ui_20dp)
                                drawable?.setBounds(0, 0, size, size)
                                actionCenter.j?.setCompoundDrawablesRelative(drawable, null, null, null)
                            }
                        }
                        actionCenter.l?.isActivated = wifiEnabled
                        actionCenter.m?.isActivated = mobileConnected
                        actionCenter.f4812n?.isActivated = bluetooth
                        actionCenter.f4810i?.progress = UIVolume.f4911a
                        val wifiText = if (NetworkMonitor.f4908e) R.string.action_center_wifi_connected else R.string.action_center_wifi
                        actionCenter.f4811k?.text = ctx.getString(wifiText)
                    }
                } catch (_: Exception) {
                }
            }
            else -> v.g.alpha = 0.09f
        }
    }
}
