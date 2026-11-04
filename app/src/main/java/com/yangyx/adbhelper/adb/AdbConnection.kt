package com.yangyx.adbhelper.adb

import android.content.Context
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLSocket

class AdbConnection(
    val ip: String,
    val port: Int = 5555,
    private val context: Context
) {
    var isUsb = false
    var usbConnection: android.hardware.usb.UsbDeviceConnection? = null
    private var socket: Socket? = null
    constructor(
        usbConnection: android.hardware.usb.UsbDeviceConnection,
        usbIn: android.hardware.usb.UsbEndpoint,
        usbOut: android.hardware.usb.UsbEndpoint,
        context: android.content.Context
    ) : this("USB", 0, context) {
        this.isUsb = true
        this.usbConnection = usbConnection
        this.input = UsbAdbInputStream(usbConnection, usbIn)
        this.output = UsbAdbOutputStream(usbConnection, usbOut)
    }
    private var input: InputStream? = null
    private var output: OutputStream? = null

    @Volatile
    var isConnected: Boolean = false
        private set

    var deviceBanner: String = ""
        private set

    var maxPayloadSize: Int = 262144
        private set

    private val writeLock = Any()
    private val localIdCounter = AtomicInteger(1)
    private val streams = ConcurrentHashMap<Int, AdbStream>()
    private val openWaitLocks = ConcurrentHashMap<Int, java.lang.Object>()

    private var readerThread: Thread? = null

    private fun safeWriteMessage(msg: AdbMessage) {
        val out = output ?: return
        synchronized(writeLock) {
            msg.write(out)
        }
    }

    fun connect(timeoutMs: Int = 10000, onStatusUpdate: ((String) -> Unit)? = null) {
        if (!isUsb) {
            val s = Socket()
            s.connect(java.net.InetSocketAddress(ip, port), timeoutMs)
            s.tcpNoDelay = true
            s.keepAlive = true
            s.receiveBufferSize = 8 * 1024 * 1024 // 8MB receive buffer for high bitrate streaming and sync
            s.sendBufferSize = 8 * 1024 * 1024    // 8MB send buffer for high throughput file transfer
            s.soTimeout = 10000
            this.socket = s
            this.input = s.getInputStream()
            this.output = s.getOutputStream()
        }

        val crypto = AdbCrypto.getOrCreate(context)

        // Send CNXN with maxdata = 1048576 (1MB) for maximum throughput
        val cnxnPayload = "host::ADB_Helper_Client\u0000".toByteArray(Charsets.UTF_8)
        val msgCnxn = AdbMessage(AdbMessage.CMD_CNXN, 0x01000000, 1048576, cnxnPayload)
        safeWriteMessage(msgCnxn)

        var authenticated = false
        var sentSignature = false

        while (!authenticated) {
            val response = try {
                AdbMessage.read(input!!)
            } catch (e: java.net.SocketTimeoutException) {
                if (sentSignature) {
                    throw Exception("等待设备 ADB 授权超时：请确保目标设备屏幕已解锁，并在屏幕上点击【允许 USB/无线调试授权】。如果未显示弹窗，请在设备上重启【USB/无线调试】选项。")
                } else {
                    throw Exception("读取 ADB 响应超时：目标设备可能未响应或拒绝了连接。")
                }
            }

            when (response.command) {
                AdbMessage.CMD_STLS -> {
                    onStatusUpdate?.invoke("目标设备请求 TLS 安全连接，正在建立 TLS 1.3 握手...")
                    // 1. Send STLS acknowledgement packet back to device
                    val stlsMsg = AdbMessage(AdbMessage.CMD_STLS, AdbMessage.A_STLS_VERSION, 0)
                    safeWriteMessage(stlsMsg)

                    // 2. Upgrade socket to TLS 1.3
                    val rawSocket = this.socket ?: throw Exception("Socket is closed")
                    val sslContext = crypto.createSslContext()
                    val sslSocket = sslContext.socketFactory.createSocket(rawSocket, ip, port, true) as SSLSocket
                    sslSocket.enabledProtocols = arrayOf("TLSv1.3")
                    sslSocket.useClientMode = true
                    sslSocket.soTimeout = 15000
                    sslSocket.startHandshake()

                    // 3. Switch stream references to TLS socket
                    this.socket = sslSocket
                    this.input = sslSocket.inputStream
                    this.output = sslSocket.outputStream
                    onStatusUpdate?.invoke("TLS 1.3 握手成功，正在验证设备授权...")
                }
                AdbMessage.CMD_CNXN -> {
                    deviceBanner = String(response.payload, Charsets.UTF_8)
                    val devMaxData = response.arg1
                    if (devMaxData in 4096..1048576) {
                        maxPayloadSize = devMaxData
                    }
                    authenticated = true
                    isConnected = true
                }
                AdbMessage.CMD_AUTH -> {
                    if (response.arg0 == AdbMessage.AUTH_TYPE_TOKEN) {
                        if (!sentSignature) {
                            // Try signed token with private key first
                            val signedToken = crypto.signToken(response.payload)
                            val authMsg = AdbMessage(AdbMessage.CMD_AUTH, AdbMessage.AUTH_TYPE_SIGNATURE, 0, signedToken)
                            safeWriteMessage(authMsg)
                            sentSignature = true
                        } else {
                            // Signature failed/unknown key on remote device. Send RSA Public Key to prompt authorization dialog on target device.
                            onStatusUpdate?.invoke("正在请求设备授权，请在远端设备屏幕上点击【允许调试授权】...")
                            val pubKeyBytes = crypto.getAdbPublicKeyPayload()
                            val pubMsg = AdbMessage(AdbMessage.CMD_AUTH, AdbMessage.AUTH_TYPE_RSAPUBLICKEY, 0, pubKeyBytes)
                            safeWriteMessage(pubMsg)

                            // Give user up to 60 seconds to tap "Allow" on physical device screen
                            socket?.soTimeout = 60000
                        }
                    } else {
                        // Directly requesting public key
                        onStatusUpdate?.invoke("正在发送公钥，请在远端设备屏幕上确认【允许授权】...")
                        val pubKeyBytes = crypto.getAdbPublicKeyPayload()
                        val pubMsg = AdbMessage(AdbMessage.CMD_AUTH, AdbMessage.AUTH_TYPE_RSAPUBLICKEY, 0, pubKeyBytes)
                        safeWriteMessage(pubMsg)

                        socket?.soTimeout = 60000
                    }
                }
                else -> {
                    throw Exception("Unexpected ADB command during handshake: 0x${Integer.toHexString(response.command)}")
                }
            }
        }

        socket?.soTimeout = 0
        // Wrap streams in high throughput buffers for scrcpy, file transfers and sync
        input?.let { this.input = java.io.BufferedInputStream(it, 524288) }
        output?.let { this.output = java.io.BufferedOutputStream(it, 524288) }


        // Start background reader loop
        readerThread = Thread({
            try {
                readerLoop()
            } catch (e: Exception) {
                disconnect()
            }
        }, "AdbReaderThread-$ip")
        readerThread?.start()
    }

    private fun readerLoop() {
        val inStream = input ?: return
        while (isConnected) {
            val msg = AdbMessage.read(inStream)
            when (msg.command) {
                AdbMessage.CMD_OKAY -> {
                    // In ADB protocol: msg.arg0 = remoteId (server stream ID), msg.arg1 = localId (client stream ID)
                    val localId = msg.arg1
                    val remoteId = msg.arg0
                    val stream = streams[localId]
                    if (stream != null) {
                        stream.remoteId = remoteId
                        val lock = openWaitLocks[localId]
                        if (lock != null) {
                            synchronized(lock) {
                                lock.notifyAll()
                            }
                        }
                        stream.onWriteAck()
                    }
                }
                AdbMessage.CMD_WRTE -> {
                    val localId = msg.arg1
                    val stream = streams[localId]
                    if (stream != null) {
                        stream.onData(msg.payload)
                        // Send OKAY ack
                        sendOkay(localId, stream.remoteId)
                    }
                }
                AdbMessage.CMD_CLSE -> {
                    val localId = msg.arg1
                    val stream = streams.remove(localId)
                    if (stream != null) {
                        stream.markClosedLocally()
                    }
                    val lock = openWaitLocks.remove(localId)
                    if (lock != null) {
                        synchronized(lock) {
                            lock.notifyAll()
                        }
                    }
                }
                AdbMessage.CMD_OPEN -> {
                    val remoteId = msg.arg0
                    val destination = String(msg.payload, Charsets.UTF_8).trimEnd('\u0000')
                    handleIncomingOpen(remoteId, destination)
                }
            }
        }
    }

    private fun handleIncomingOpen(remoteId: Int, destination: String) {
        val localId = localIdCounter.getAndIncrement()
        val localPort = destination.removePrefix("tcp:").toIntOrNull()
        if (localPort != null && localPort > 0) {
            Thread({
                try {
                    val localSocket = Socket("127.0.0.1", localPort)
                    localSocket.tcpNoDelay = true
                    val stream = AdbStream(localId, this)
                    stream.remoteId = remoteId
                    streams[localId] = stream
                    sendOkay(localId, remoteId)

                    val t1 = Thread({
                        try {
                            val inStream = localSocket.getInputStream()
                            val buf = ByteArray(16384)
                            while (isConnected && !localSocket.isClosed && !stream.isClosed) {
                                val n = inStream.read(buf)
                                if (n < 0) break
                                if (n > 0) {
                                    val chunk = if (n == buf.size) buf else buf.copyOf(n)
                                    writeStream(stream, chunk)
                                }
                            }
                        } catch (_: Exception) {}
                        finally {
                            closeStream(stream)
                            try { localSocket.close() } catch (_: Exception) {}
                        }
                    }, "ReversePipe-Out-$localId")
                    t1.isDaemon = true
                    t1.start()

                    try {
                        val outStream = localSocket.getOutputStream()
                        while (isConnected && !localSocket.isClosed && !stream.isClosed) {
                            val data = stream.read(5000L)
                            if (data != null && data.isNotEmpty()) {
                                outStream.write(data)
                                outStream.flush()
                            }
                        }
                    } catch (_: Exception) {}
                    finally {
                        closeStream(stream)
                        try { localSocket.close() } catch (_: Exception) {}
                    }
                } catch (_: Exception) {
                    safeWriteMessage(AdbMessage(AdbMessage.CMD_CLSE, 0, remoteId))
                }
            }, "ReverseConnect-$localId").apply {
                isDaemon = true
                start()
            }
        } else {
            safeWriteMessage(AdbMessage(AdbMessage.CMD_CLSE, 0, remoteId))
        }
    }

    fun reverseForward(remoteSpec: String, localSpec: String): Result<Unit> {
        val remote = if (!remoteSpec.contains(":")) "tcp:$remoteSpec" else remoteSpec
        val local = if (!localSpec.contains(":")) "tcp:$localSpec" else localSpec
        return executeReverseCommand("forward:$remote;$local").map { }
    }

    fun reverseKillForward(remoteSpec: String): Result<Unit> {
        val remote = if (!remoteSpec.contains(":")) "tcp:$remoteSpec" else remoteSpec
        return executeReverseCommand("killforward:$remote").map { }
    }

    fun reverseKillForwardAll(): Result<Unit> {
        return executeReverseCommand("killforward-all").map { }
    }

    fun reverseListForward(): List<AdbReverseRule> {
        val res = executeReverseCommand("list-forward")
        val text = res.getOrNull() ?: return emptyList()
        if (text.isEmpty() || text.startsWith("FAIL")) return emptyList()

        val list = mutableListOf<AdbReverseRule>()
        text.lines().forEach { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size >= 3) {
                list.add(AdbReverseRule(remoteSpec = parts[1], localSpec = parts[2]))
            } else if (parts.size == 2) {
                list.add(AdbReverseRule(remoteSpec = parts[0], localSpec = parts[1]))
            }
        }
        return list
    }

    private fun executeReverseCommand(command: String): Result<String> {
        return try {
            val stream = openStream("reverse:$command")
            val data = stream.readAllText(3000L).trim()
            stream.close()
            if (data.startsWith("FAIL")) {
                val err = data.removePrefix("FAIL").drop(4).trim()
                Result.failure(Exception(if (err.isNotBlank()) err else "反向代理执行失败"))
            } else {
                Result.success(data.removePrefix("OKAY").trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sendOkay(localId: Int, remoteId: Int) {
        val msg = AdbMessage(AdbMessage.CMD_OKAY, localId, remoteId)
        safeWriteMessage(msg)
    }

    @Synchronized
    fun ensureConnected(timeoutMs: Int = 5000): Boolean {
        if (isConnected && socket?.isConnected == true && socket?.isClosed == false) {
            return true
        }
        if (isUsb) return false
        if (ip.isBlank() || port <= 0) return false

        return try {
            disconnect()
            connect(timeoutMs)
            isConnected
        } catch (_: Exception) {
            false
        }
    }

    fun isConnectionAlive(timeoutMs: Long = 3000): Boolean {
        if (!isConnected) return false
        if (isUsb) return usbConnection != null
        val s = socket ?: return false
        return s.isConnected && !s.isClosed && !s.isInputShutdown && !s.isOutputShutdown
    }

    fun isRemotePortReachable(timeoutMs: Int = 2000): Boolean {
        if (isUsb) return true
        if (ip.isBlank() || port <= 0) return false
        return try {
            Socket().use { testSocket ->
                testSocket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openStream(destination: String): AdbStream {
        if (!isConnected || socket?.isClosed == true) {
            val reconnected = ensureConnected(5000)
            if (!reconnected) {
                throw IllegalStateException("ADB connection is not active")
            }
        }

        val localId = localIdCounter.getAndIncrement()
        val stream = AdbStream(localId, this)
        streams[localId] = stream

        val lock = java.lang.Object()
        openWaitLocks[localId] = lock

        val destBytes = "$destination\u0000".toByteArray(Charsets.UTF_8)
        val msg = AdbMessage(AdbMessage.CMD_OPEN, localId, 0, destBytes)

        synchronized(lock) {
            safeWriteMessage(msg)
            lock.wait(5000)
        }

        openWaitLocks.remove(localId)

        if (stream.remoteId == 0) {
            streams.remove(localId)
            throw Exception("Failed to open ADB stream to $destination (Timeout or Rejected)")
        }

        return stream
    }

    fun writeStream(stream: AdbStream, data: ByteArray) {
        val msg = AdbMessage(AdbMessage.CMD_WRTE, stream.localId, stream.remoteId, data)
        safeWriteMessage(msg)
    }

    fun closeStream(stream: AdbStream) {
        streams.remove(stream.localId)
        if (isConnected) {
            try {
                val msg = AdbMessage(AdbMessage.CMD_CLSE, stream.localId, stream.remoteId)
                safeWriteMessage(msg)
            } catch (e: Exception) {
                // Ignore closing errors
            }
        }
    }

    fun executeShell(command: String, timeoutMs: Long = 10000): String {
        val stream = openStream("shell:$command")
        val result = stream.readAllText(timeoutMs)
        stream.close()
        return result
    }

    fun executeShellStream(command: String, onLine: (String) -> Unit) {
        val stream = openStream("shell:$command")
        try {
            val reader = java.io.BufferedReader(java.io.InputStreamReader(stream.asInputStream(), Charsets.UTF_8))
            reader.forEachLine { line ->
                onLine(line)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            stream.close()
        }
    }

    fun reboot(mode: String = "") {
        val modeLower = mode.trim().lowercase()
        try {
            val dest = if (modeLower.isBlank()) "reboot:" else "reboot:$modeLower"
            val stream = openStream(dest)
            try {
                stream.close()
            } catch (_: Exception) {}
        } catch (_: Exception) {
            val shellCmd = when (modeLower) {
                "fastboot" -> "reboot fastboot 2>/dev/null || reboot bootloader 2>/dev/null || setprop sys.powerctl reboot,fastboot"
                "bootloader" -> "reboot bootloader 2>/dev/null || setprop sys.powerctl reboot,bootloader"
                "recovery" -> "reboot recovery 2>/dev/null || setprop sys.powerctl reboot,recovery"
                "poweroff", "shutdown" -> "reboot -p 2>/dev/null || svc power shutdown 2>/dev/null || setprop sys.powerctl shutdown"
                else -> "svc power reboot 2>/dev/null || reboot 2>/dev/null || setprop sys.powerctl reboot"
            }
            try {
                executeShell(shellCmd, 3000)
            } catch (_: Exception) {}
        }
    }

    fun disconnect() {
        isConnected = false
        try {
            socket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        socket = null
        input = null
        output = null
        streams.values.forEach { it.markClosedLocally() }
        streams.clear()
    }
}
