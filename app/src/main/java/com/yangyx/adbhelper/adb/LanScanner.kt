package com.yangyx.adbhelper.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

data class DiscoveredAdbDevice(
    val ip: String,
    val port: Int = 5555,
    val deviceName: String = "局域网 ADB 设备",
    val isOnline: Boolean = true
)

class LanScanner {

    /**
     * Get local IPv4 addresses (excluding loopback and p2p/tun).
     */
    fun getLocalIpAddresses(): List<String> {
        val ips = mutableListOf<String>()
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddr = addr.hostAddress ?: continue
                        if (!hostAddr.startsWith("127.")) {
                            ips.add(hostAddr)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ips
    }

    /**
     * Scan local subnets for open ADB port (5555).
     * Returns list of discovered devices.
     */
    suspend fun scanLanForAdbDevices(
        ports: List<Int> = listOf(5555),
        timeoutMs: Int = 300,
        onProgress: (scannedCount: Int, totalCount: Int, currentSubnet: String) -> Unit = { _, _, _ -> }
    ): List<DiscoveredAdbDevice> = withContext(Dispatchers.IO) {
        val localIps = getLocalIpAddresses()
        val targetSubnets = mutableSetOf<String>()

        for (localIp in localIps) {
            val lastDot = localIp.lastIndexOf('.')
            if (lastDot > 0) {
                targetSubnets.add(localIp.substring(0, lastDot + 1))
            }
        }

        // Default fallback subnets if no active wifi IP found
        if (targetSubnets.isEmpty()) {
            targetSubnets.add("192.168.1.")
            targetSubnets.add("192.168.0.")
            targetSubnets.add("192.168.31.")
            targetSubnets.add("192.168.43.")
        }

        val ipsToScan = mutableListOf<String>()
        for (prefix in targetSubnets) {
            for (i in 1..254) {
                val ip = "$prefix$i"
                if (!localIps.contains(ip)) {
                    ipsToScan.add(ip)
                }
            }
        }

        val totalTasks = ipsToScan.size * ports.size
        val scannedCounter = AtomicInteger(0)
        val discoveredList = Collections.synchronizedList(mutableListOf<DiscoveredAdbDevice>())

        val subnetText = targetSubnets.joinToString(", ") { "${it}0/24" }

        coroutineScope {
            // Batch tasks to scan concurrently
            ipsToScan.chunked(32).forEach { batch ->
                if (!isActive) return@coroutineScope
                batch.flatMap { ip ->
                    ports.map { port ->
                        async {
                            if (!isActive) return@async
                            if (isPortOpen(ip, port, timeoutMs)) {
                                val discovered = DiscoveredAdbDevice(
                                    ip = ip,
                                    port = port,
                                    deviceName = "开放 ADB 设备 ($ip)"
                                )
                                if (!discoveredList.any { it.ip == ip && it.port == port }) {
                                    discoveredList.add(discovered)
                                }
                            }
                            val currentScanned = scannedCounter.incrementAndGet()
                            onProgress(currentScanned, totalTasks, subnetText)
                        }
                    }
                }.awaitAll()
            }
        }

        discoveredList.toList()
    }

    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
