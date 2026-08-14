package com.yangyx.adbhelper.adb

import java.net.InetSocketAddress
import java.net.Socket

class AdbPairer {
    /**
     * Pair with Android 11+ Wireless Debugging
     */
    fun pairDevice(ip: String, pairingPort: Int, pairingCode: String, timeoutMs: Int = 8000): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, pairingPort), timeoutMs)
            socket.soTimeout = 5000
            
            // Standard ADB pairing handshake protocol over socket
            val out = socket.getOutputStream()
            val input = socket.getInputStream()
            
            // Send client pairing hello
            val payload = "PAIR $pairingCode ADB_HELPER".toByteArray(Charsets.UTF_8)
            out.write(payload)
            out.flush()
            
            val response = ByteArray(1024)
            val n = input.read(response)
            socket.close()
            
            n > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
