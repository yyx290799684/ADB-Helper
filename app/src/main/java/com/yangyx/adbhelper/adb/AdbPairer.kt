package com.yangyx.adbhelper.adb

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.InetSocketAddress
import java.net.Socket

sealed class PairResult {
    data class Success(
        val ip: String,
        val port: Int,
        val message: String,
        val isConnectionPort: Boolean = true
    ) : PairResult()

    data class PairingPortDetected(
        val ip: String,
        val pairingPort: Int,
        val message: String
    ) : PairResult()

    data class Failure(
        val message: String,
        val suggestion: String? = null
    ) : PairResult()
}

class AdbPairer(private val context: Context) {

    /**
     * Intelligently probe and pair with Android 11+ Wireless Debugging.
     */
    suspend fun pairOrProbe(ip: String, port: Int, pairingCode: String, timeoutMs: Int = 3000): PairResult = withContext(Dispatchers.IO) {
        val cleanIp = ip.trim()
        if (cleanIp.isEmpty()) {
            return@withContext PairResult.Failure("请输入目标设备的 IP 地址")
        }
        if (port !in 1..65535) {
            return@withContext PairResult.Failure("端口号不合法，请输入 1~65535 之间的有效端口")
        }

        // Step 1: Check if the specified port is directly an ADB connection port
        val isAuthSent = tryProbeAndSendAuth(cleanIp, port, timeoutMs = 1500)
        if (isAuthSent) {
            return@withContext PairResult.Success(
                ip = cleanIp,
                port = port,
                message = "检测到目标端口 $port 为 ADB 调试服务端口，已成功发送 RSA 授权请求！\n\n请查看远端手机屏幕，在弹出的【允许无线调试吗？】对话框中勾选【一律允许】，并点击【允许】。"
            )
        }

        // Step 2: Check if the specified port is open (e.g. Android 11+ TLS Pairing Port)
        val isPortOpen = isTcpPortOpen(cleanIp, port, timeoutMs = 2500)
        if (!isPortOpen) {
            return@withContext PairResult.Failure(
                message = "无法连接到目标设备 $cleanIp:$port",
                suggestion = "请确认：\n1. 远端手机与本机连接在同一个 Wi-Fi / 局域网下\n2. 远端手机【开发者选项 -> 无线调试】已开启\n3. 若使用的是【使用配对码配对设备】，请确保该弹窗在远端手机上保持打开状态（关闭弹窗会导致配对端口和配对码立即失效）"
            )
        }

        // Step 3: The port is open! It is the Android 11+ Pairing Port.
        // Let's do a fast nearby search for the actual connection port (checking 5555 and nearby ports +-25)
        val foundConnectionPort = probeNearbyAdbPorts(cleanIp, centerPort = port)
        if (foundConnectionPort != null) {
            return@withContext PairResult.Success(
                ip = cleanIp,
                port = foundConnectionPort,
                message = "已连通配对服务，并自动检测到远端手机的无线调试连接端口为 $foundConnectionPort！\n\n已成功发送 RSA 授权请求，请查看远端手机屏幕并点击【允许无线调试】。"
            )
        }

        // If no nearby port responded, explain clearly how to connect using the connection port
        return@withContext PairResult.PairingPortDetected(
            ip = cleanIp,
            pairingPort = port,
            message = "已成功连通远端手机配对端口 ($cleanIp:$port)！\n\n" +
                    "【关键说明】\n" +
                    "在 Android 11+ 中，【使用配对码配对设备】弹窗中显示的端口是临时的配对握手端口，并非实际连接端口。\n\n" +
                    "最稳定便捷的连接方式（免配对码）：\n" +
                    "1. 返回远端手机的【无线调试】主页面（关闭配对码弹窗）；\n" +
                    "2. 查看页面顶部显示的【IP 地址和端口】（例如 192.168.x.x:4xxxx，注意不是刚才弹窗里的端口）；\n" +
                    "3. 返回本应用主页面，直接输入该端口点击【连接】，远端手机屏幕即可直接弹出【允许无线调试】授权确认框！"
        )
    }

    private fun isTcpPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun tryProbeAndSendAuth(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                socket.soTimeout = 1500
                val input = BufferedInputStream(socket.getInputStream())
                val output = BufferedOutputStream(socket.getOutputStream())

                val cnxnPayload = "host::ADB_Helper\u0000".toByteArray(Charsets.UTF_8)
                val msgCnxn = AdbMessage(AdbMessage.CMD_CNXN, 0x01000000, 1048576, cnxnPayload)
                msgCnxn.write(output)

                val response = AdbMessage.read(input)
                if (response.command == AdbMessage.CMD_AUTH) {
                    // Send RSA public key to trigger authorization popup on the remote device!
                    val crypto = AdbCrypto.getOrCreate(context)
                    val pubKeyBytes = crypto.getAdbPublicKeyPayload()
                    val authMsg = AdbMessage(AdbMessage.CMD_AUTH, AdbMessage.AUTH_TYPE_RSAPUBLICKEY, 0, pubKeyBytes)
                    authMsg.write(output)
                    true
                } else if (response.command == AdbMessage.CMD_CNXN) {
                    true
                } else {
                    false
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun probeNearbyAdbPorts(ip: String, centerPort: Int): Int? = withContext(Dispatchers.IO) {
        val candidatePorts = mutableListOf<Int>()
        candidatePorts.add(5555)

        for (offset in 1..25) {
            val pPlus = centerPort + offset
            if (pPlus in 1..65535) candidatePorts.add(pPlus)
            val pMinus = centerPort - offset
            if (pMinus in 1..65535) candidatePorts.add(pMinus)
        }

        val deferreds = candidatePorts.map { candidatePort ->
            async {
                if (tryProbeAndSendAuth(ip, candidatePort, timeoutMs = 400)) {
                    candidatePort
                } else {
                    null
                }
            }
        }

        val results = deferreds.awaitAll()
        results.firstOrNull { it != null }
    }
}

