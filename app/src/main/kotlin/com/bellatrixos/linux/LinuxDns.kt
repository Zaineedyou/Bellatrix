package com.bellatrixos.linux

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.util.Log
import java.io.File
import java.net.Inet6Address
import java.net.InetAddress

/** Keeps the guest's /etc/resolv.conf on the phone's default-network DNS servers. Ported from Omalauncher. */
class LinuxDns(
    context: Context,
    private val rootfs: File,
) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private var started = false

    private val callback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onLinkPropertiesChanged(
                network: Network,
                properties: LinkProperties,
            ) = write(properties.dnsServers, properties.domains)
        }

    fun start() {
        if (started) return
        started = true
        runCatching { connectivity.registerDefaultNetworkCallback(callback) }
            .onFailure { Log.w(TAG, "can't follow the network's DNS", it) }
    }

    private fun write(
        servers: List<InetAddress>,
        domains: String?,
    ) {
        val file = File(rootfs, "etc/resolv.conf")
        if (file.parentFile?.isDirectory != true) return
        val current = runCatching { file.readText() }.getOrNull()
        if (!managed(current)) return
        val text = resolvConf(servers, domains)
        if (text == current) return
        runCatching { writeAtomically(file, text.toByteArray()) }
            .onFailure { Log.w(TAG, "can't write ${file.path}", it) }
    }

    companion object {
        private const val TAG = "dns"
        const val MARKER = "# Written by Bellatrix from the phone's DNS servers; remove this line to keep your own."
        private const val OLD_DEFAULT = "nameserver 8.8.8.8\nnameserver 1.1.1.1\n"
        private val FALLBACK = listOf("8.8.8.8", "1.1.1.1")

        internal fun managed(current: String?): Boolean = current == null || current == OLD_DEFAULT || current.lines().firstOrNull() == MARKER

        internal fun resolvConf(
            servers: List<InetAddress>,
            domains: String?,
        ): String {
            val usable = servers.filterNot { it is Inet6Address && it.isLinkLocalAddress }.map { it.hostAddress!! }
            val lines = mutableListOf(MARKER)
            (usable.ifEmpty { FALLBACK }).take(3).forEach { lines += "nameserver $it" }
            domains?.split(' ', ',')?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }?.let { lines += "search " + it.joinToString(" ") }
            return lines.joinToString("\n", postfix = "\n")
        }
    }
}
