package com.bellatrixos.windows.drivers.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.ImageView
import com.bellatrixos.AppExecutors
import com.bellatrixos.R

/** Mengamati jaringan aktif dan memperbarui ikon di taskbar. */
class NetworkMonitor(context: Context, private val imageView: ImageView?) {
    @JvmField val f4909a: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @JvmField val b: ConnectivityManager.NetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
            super.onAvailable(network)
            f4907d = true
            val view = imageView ?: return
            val type = typeOf(network)
            f4908e = type == TYPE_WIFI
            AppExecutors.c().post { updateIcon(view, type) }
        }

        override fun onCapabilitiesChanged(network: android.net.Network, capabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, capabilities)
            val view = imageView ?: return
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                val type = typeOf(network)
                f4908e = type == TYPE_WIFI
                AppExecutors.c().post { updateIcon(view, type) }
            }
        }

        override fun onLost(network: android.net.Network) {
            super.onLost(network)
            f4907d = false
            f4908e = false
            imageView?.let { view ->
                AppExecutors.c().post { view.setImageResource(R.drawable.ui_tb_globe_prohibited_24_regular) }
            }
        }
    }

    @JvmField var c: Boolean = false

    fun register() {
        f4909a.registerDefaultNetworkCallback(b)
        c = true
    }

    fun unregister() {
        runCatching { f4909a.unregisterNetworkCallback(b) }
        c = false
    }

    private fun typeOf(network: android.net.Network): Int {
        val capabilities = f4909a.getNetworkCapabilities(network) ?: return TYPE_OTHER
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> TYPE_WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> TYPE_CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> TYPE_ETHERNET
            else -> TYPE_OTHER
        }
    }

    companion object {
        const val TYPE_CELLULAR = 1
        const val TYPE_WIFI = 2
        const val TYPE_ETHERNET = 3
        const val TYPE_OTHER = 2

        @JvmField var f4907d: Boolean = false
        @JvmField var f4908e: Boolean = false

        @JvmStatic
        fun a(monitor: NetworkMonitor, network: android.net.Network): Int = monitor.typeOf(network)

        private fun updateIcon(view: ImageView, type: Int) {
            val res = when (type) {
                TYPE_CELLULAR -> R.drawable.ic_cellular_data_1_24_filled
                TYPE_ETHERNET -> R.drawable.ui_tb_ethernet_24_regular
                else -> R.drawable.ui_tb_wifi_4_24_regular
            }
            view.setImageResource(res)
        }
    }
}
