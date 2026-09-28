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

    data class Paired(
        val ip: String,
        val pairingPort: Int,
        val guid: String,
        val message: String
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
     * When a pairing code is provided, executes the full TLS 1.3 + SPAKE2 pairing handshake.
     */
    suspend fun pairOrProbe(ip: String, port: Int, pairingCode: String, timeoutMs: Int = 10000): PairResult = withContext(Dispatchers.IO) {
        val cleanIp = ip.trim()
        if (cleanIp.isEmpty()) {
            return@withContext PairResult.Failure("请输入目标设备的 IP 地址")
        }
        if (port !in 1..65535) {
            return@withContext PairResult.Failure("端口号不合法，请输入 1~65535 之间的有效端口")
        }

        val cleanCode = pairingCode.trim()

        // If user provided a pairing code, perform standard Android 11+ Wireless Debugging Pairing!
        if (cleanCode.isNotEmpty()) {
            val pairingResult = AdbPairingConnection.pair(cleanIp, port, cleanCode, context, timeoutMs = timeoutMs)
            if (pairingResult.success) {
                // Successfully paired to remote device! The remote device has now added this client to its "Paired devices".
                val guidText = if (pairingResult.guid.isNotBlank()) " [guid=${pairingResult.guid}]" else ""

                // Now attempt a quick probe for the wireless debugging connection port nearby (centerPort +-25 and 5555)
                val foundConnectionPort = probeNearbyAdbPorts(cleanIp, centerPort = port)
                if (foundConnectionPort != null) {
                    return@withContext PairResult.Success(
                        ip = cleanIp,
                        port = foundConnectionPort,
                        message = "Successfully paired to $cleanIp:$port$guidText\n\n" +
                                "【配对成功！】被控端已成功将本机添加至【已配对设备】列表中。\n" +
                                "同时自动检测到被控端的无线调试服务连接端口为 $foundConnectionPort，点击下方【立即连接】即可开启控制！"
                    )
                }

                return@withContext PairResult.Paired(
                    ip = cleanIp,
                    pairingPort = port,
                    guid = pairingResult.guid,
                    message = "Successfully paired to $cleanIp:$port$guidText\n\n" +
                            "【配对成功！】\n" +
                            "被控端手机的【已配对设备】中现已多出一个设备（本机）。\n\n" +
                            "【后续连接指引】\n" +
                            "在被控端手机上返回【无线调试】主设置页面（已配对后无需再开配对码弹窗），查看顶部显示的【IP 地址和端口】（例如 192.168.x.x:4xxxx），输入该端口即可直接连接并永久信任！"
                )
            } else {
                return@withContext PairResult.Failure(
                    message = "配对失败: ${pairingResult.error ?: "握手未通过"}",
                    suggestion = "请确认：\n" +
                            "1. 被控端手机上的【使用配对码配对设备】弹窗保持开启（关闭或重新打开弹窗都会导致配对端口与配对码发生变化）\n" +
                            "2. 6位数字配对码输入无误\n" +
                            "3. 两台手机连接在同一个 Wi-Fi / 局域网下"
                )
            }
        }

        // If no pairing code provided, probe port / nearby ports as fallback
        val isAuthSent = tryProbeAndSendAuth(cleanIp, port, timeoutMs = 1500)
        if (isAuthSent) {
            return@withContext PairResult.Success(
                ip = cleanIp,
                port = port,
                message = "检测到目标端口 $port 为 ADB 调试服务端口，已成功发送 RSA 授权请求！\n\n请查看远端手机屏幕，在弹出的【允许无线调试吗？】对话框中勾选【一律允许】，并点击【允许】。"
            )
        }

        val isPortOpen = isTcpPortOpen(cleanIp, port, timeoutMs = 2500)
        if (!isPortOpen) {
            return@withContext PairResult.Failure(
                message = "无法连接到目标设备 $cleanIp:$port",
                suggestion = "请确认：\n1. 远端手机与本机连接在同一个 Wi-Fi / 局域网下\n2. 远端手机【开发者选项 -> 无线调试】已开启\n3. 若使用的是【使用配对码配对设备】，请确保该弹窗在远端手机上保持打开状态"
            )
        }

        val foundConnectionPort = probeNearbyAdbPorts(cleanIp, centerPort = port)
        if (foundConnectionPort != null) {
            return@withContext PairResult.Success(
                ip = cleanIp,
                port = foundConnectionPort,
                message = "已连通配对服务，并自动检测到远端手机的无线调试连接端口为 $foundConnectionPort！\n\n已成功发送 RSA 授权请求，请查看远端手机屏幕并点击【允许无线调试】。"
            )
        }

        return@withContext PairResult.PairingPortDetected(
            ip = cleanIp,
            pairingPort = port,
            message = "已成功连通远端手机配对端口 ($cleanIp:$port)！\n\n" +
                    "【关键说明】\n" +
                    "在 Android 11+ 中，【使用配对码配对设备】弹窗中显示的端口是临时的配对握手端口，请输入弹窗中的6位数配对码进行配对。\n\n" +
                    "或者直接使用免配对码方式：\n" +
                    "1. 返回远端手机的【无线调试】主页面；\n" +
                    "2. 查看页面顶部显示的【IP 地址和端口】（例如 192.168.x.x:4xxxx）；\n" +
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

