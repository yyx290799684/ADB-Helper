package com.yangyx.adbhelper.adb

import android.content.Context
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

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
            s.receiveBufferSize = 2 * 1024 * 1024 // 2MB receive buffer for high bitrate streaming
            s.sendBufferSize = 512 * 1024
            s.soTimeout = 10000
            this.socket = s
            this.input = s.getInputStream()
            this.output = s.getOutputStream()
        }


        val crypto = AdbCrypto.getOrCreate(context)

        // Send CNXN with maxdata = 262144 (256KB) for high-throughput streaming
        val cnxnPayload = "host::ADB_Helper_Client\u0000".toByteArray(Charsets.UTF_8)
        val msgCnxn = AdbMessage(AdbMessage.CMD_CNXN, 0x01000000, 262144, cnxnPayload)
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
                AdbMessage.CMD_CNXN -> {
                    deviceBanner = String(response.payload, Charsets.UTF_8)
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
        // Reset soTimeout to 0 (infinite) for background reader loop


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
            }
        }
    }

    private fun sendOkay(localId: Int, remoteId: Int) {
        val msg = AdbMessage(AdbMessage.CMD_OKAY, localId, remoteId)
        safeWriteMessage(msg)
    }

    fun openStream(destination: String): AdbStream {
        if (!isConnected) throw IllegalStateException("ADB connection is not active")

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
