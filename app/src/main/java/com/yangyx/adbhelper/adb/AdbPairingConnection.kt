package com.yangyx.adbhelper.adb

import android.content.Context
import android.os.Build
import android.util.Log
import com.flyfish233.crypto.spake2.Spake2Context
import com.flyfish233.crypto.spake2.Spake2Role
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.KeyParameter
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.Principal
import java.security.PrivateKey
import java.security.Provider
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager

/**
 * Implements Android 11+ Wireless Debugging Pairing Protocol (TLS 1.3 + SPAKE2 + AES-128-GCM).
 * Fully compatible with AOSP adb pairing_connection / adb pair command.
 */
object AdbPairingConnection {

    private const val TAG = "AdbPairing"

    private const val EXPORT_KEY_SIZE = 64
    private const val EXPORTED_KEY_LABEL = "adb-label\u0000"
    private const val MAX_PEER_INFO_SIZE = 8192

    private val CLIENT_NAME = "adb pair client\u0000".toByteArray(Charsets.UTF_8)
    private val SERVER_NAME = "adb pair server\u0000".toByteArray(Charsets.UTF_8)
    private val HKDF_INFO = "adb pairing_auth aes-128-gcm key".toByteArray(Charsets.UTF_8)

    data class PairingResult(
        val success: Boolean,
        val guid: String = "",
        val error: String? = null
    )

    private data class PacketHeader(
        val version: Byte,
        val type: Byte,
        val payloadSize: Int
    ) {
        companion object {
            const val VERSION: Byte = 1
            const val TYPE_SPAKE2: Byte = 0
            const val TYPE_PEER_INFO: Byte = 1

            fun read(input: DataInputStream): PacketHeader {
                val version = input.readByte()
                val type = input.readByte()
                val payloadSize = input.readInt()
                return PacketHeader(version, type, payloadSize)
            }
        }

        fun write(output: DataOutputStream) {
            output.writeByte(version.toInt())
            output.writeByte(type.toInt())
            output.writeInt(payloadSize)
        }
    }

    /**
     * Executes the wireless debugging pairing protocol against the remote device.
     */
    fun pair(
        ip: String,
        port: Int,
        pairingCode: String,
        context: Context,
        timeoutMs: Int = 10000
    ): PairingResult {
        var rawSocket: Socket? = null
        var sslSocket: SSLSocket? = null

        try {
            val cleanCode = pairingCode.trim()
            if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
                return PairingResult(false, error = "配对码必须为 6 位数字")
            }

            val crypto = AdbCrypto.getOrCreate(context)

            // 1. Initialize TLS 1.3 Context
            val sslContext = crypto.createSslContext()

            // 2. Establish TCP connection
            rawSocket = Socket()
            rawSocket.connect(InetSocketAddress(ip, port), timeoutMs)
            rawSocket.tcpNoDelay = true
            rawSocket.soTimeout = timeoutMs

            // 3. Upgrade to TLS 1.3
            sslSocket = sslContext.socketFactory.createSocket(rawSocket, ip, port, true) as SSLSocket
            sslSocket.enabledProtocols = arrayOf("TLSv1.3")
            sslSocket.useClientMode = true
            sslSocket.soTimeout = timeoutMs
            sslSocket.startHandshake()

            Log.d(TAG, "TLS 1.3 handshake completed with $ip:$port")

            // 4. Export keying material (RFC 5705)
            val keyMaterial = exportKeyingMaterial(sslSocket, EXPORT_KEY_SIZE)
            Log.d(TAG, "Exported ${keyMaterial.size} bytes key material")

            // 5. Setup SPAKE2 (Alice / Client role)
            val codeBytes = cleanCode.toByteArray(Charsets.UTF_8)
            val passwordBytes = ByteArray(codeBytes.size + keyMaterial.size)
            System.arraycopy(codeBytes, 0, passwordBytes, 0, codeBytes.size)
            System.arraycopy(keyMaterial, 0, passwordBytes, codeBytes.size, keyMaterial.size)

            val spake2 = Spake2Context(Spake2Role.Alice, CLIENT_NAME, SERVER_NAME)
            val ourSpakeMsg = spake2.generateMessage(passwordBytes)

            val dataOut = DataOutputStream(sslSocket.outputStream)
            val dataIn = DataInputStream(sslSocket.inputStream)

            // 6. Send our SPAKE2 message
            val spakeHeaderOut = PacketHeader(
                version = PacketHeader.VERSION,
                type = PacketHeader.TYPE_SPAKE2,
                payloadSize = ourSpakeMsg.size
            )
            spakeHeaderOut.write(dataOut)
            dataOut.write(ourSpakeMsg)
            dataOut.flush()
            Log.d(TAG, "Sent SPAKE2 client message (${ourSpakeMsg.size} bytes)")

            // 7. Receive server's SPAKE2 message
            val spakeHeaderIn = PacketHeader.read(dataIn)
            if (spakeHeaderIn.version != PacketHeader.VERSION || spakeHeaderIn.type != PacketHeader.TYPE_SPAKE2) {
                return PairingResult(false, error = "无效的 SPAKE2 握手响应包 (ver=${spakeHeaderIn.version}, type=${spakeHeaderIn.type})")
            }

            val theirSpakeMsg = ByteArray(spakeHeaderIn.payloadSize)
            dataIn.readFully(theirSpakeMsg)
            Log.d(TAG, "Received SPAKE2 server message (${theirSpakeMsg.size} bytes)")

            // 8. Process server's SPAKE2 message to derive shared secret
            val sharedSecret = spake2.processMessage(theirSpakeMsg)
                ?: return PairingResult(false, error = "配对验证失败：配对码错误或握手不匹配")

            // 9. Derive AES-128 key using HKDF-SHA256
            val hkdf = HKDFBytesGenerator(SHA256Digest())
            hkdf.init(HKDFParameters(sharedSecret, null, HKDF_INFO))
            val aesKey = ByteArray(16)
            hkdf.generateBytes(aesKey, 0, 16)

            // 10. Prepare Client PeerInfo (RSA public key)
            val clientPeerInfo = ByteArray(MAX_PEER_INFO_SIZE)
            clientPeerInfo[0] = 0 // ADB_RSA_PUB_KEY
            val pubKeyPayload = crypto.getAdbPublicKeyPayload(deviceName = "ADB_Helper")
            val copyLen = pubKeyPayload.size.coerceAtMost(MAX_PEER_INFO_SIZE - 1)
            System.arraycopy(pubKeyPayload, 0, clientPeerInfo, 1, copyLen)

            // 11. Encrypt and send Client PeerInfo (encSequence = 0L)
            var encSequence = 0L
            val encIv = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putLong(encSequence++).array()
            val encCipher = GCMBlockCipher.newInstance(AESEngine.newInstance())
            encCipher.init(true, AEADParameters(KeyParameter(aesKey), 128, encIv))
            val encryptedPeerInfo = ByteArray(encCipher.getOutputSize(clientPeerInfo.size))
            val encLen = encCipher.processBytes(clientPeerInfo, 0, clientPeerInfo.size, encryptedPeerInfo, 0)
            encCipher.doFinal(encryptedPeerInfo, encLen)

            val peerInfoHeaderOut = PacketHeader(
                version = PacketHeader.VERSION,
                type = PacketHeader.TYPE_PEER_INFO,
                payloadSize = encryptedPeerInfo.size
            )
            peerInfoHeaderOut.write(dataOut)
            dataOut.write(encryptedPeerInfo)
            dataOut.flush()
            Log.d(TAG, "Sent encrypted PeerInfo (${encryptedPeerInfo.size} bytes)")

            // 12. Receive and decrypt Server PeerInfo (decSequence = 0L)
            val peerInfoHeaderIn = PacketHeader.read(dataIn)
            if (peerInfoHeaderIn.version != PacketHeader.VERSION || peerInfoHeaderIn.type != PacketHeader.TYPE_PEER_INFO) {
                return PairingResult(false, error = "无效的 PeerInfo 响应包 (type=${peerInfoHeaderIn.type})")
            }

            val serverEncrypted = ByteArray(peerInfoHeaderIn.payloadSize)
            dataIn.readFully(serverEncrypted)

            var decSequence = 0L
            val decIv = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putLong(decSequence++).array()
            val decCipher = GCMBlockCipher.newInstance(AESEngine.newInstance())
            decCipher.init(false, AEADParameters(KeyParameter(aesKey), 128, decIv))
            val serverPeerInfo = ByteArray(decCipher.getOutputSize(serverEncrypted.size))
            val decLen = decCipher.processBytes(serverEncrypted, 0, serverEncrypted.size, serverPeerInfo, 0)
            decCipher.doFinal(serverPeerInfo, decLen)

            // 13. Parse Server GUID
            val serverType = serverPeerInfo[0].toInt()
            if (serverType != 1) { // ADB_DEVICE_GUID
                Log.w(TAG, "Expected ADB_DEVICE_GUID (1), got $serverType")
            }

            var guidEnd = 1
            while (guidEnd < serverPeerInfo.size && serverPeerInfo[guidEnd] != 0.toByte()) {
                guidEnd++
            }
            val guid = String(serverPeerInfo, 1, guidEnd - 1, Charsets.UTF_8).trim()
            Log.d(TAG, "Pairing succeeded! Device GUID: $guid")

            return PairingResult(true, guid = guid)

        } catch (e: Exception) {
            Log.e(TAG, "Pairing failed with exception", e)
            val msg = e.localizedMessage ?: e.message ?: "未知异常"
            return PairingResult(false, error = msg)
        } finally {
            try {
                sslSocket?.close()
            } catch (_: Throwable) {}
            try {
                rawSocket?.close()
            } catch (_: Throwable) {}
        }
    }

    private fun exportKeyingMaterial(sslSocket: SSLSocket, length: Int): ByteArray {
        // 1. Try org.conscrypt.Conscrypt
        try {
            val conscryptClass = Class.forName("org.conscrypt.Conscrypt")
            val method = conscryptClass.getMethod(
                "exportKeyingMaterial",
                SSLSocket::class.java,
                String::class.java,
                ByteArray::class.java,
                Int::class.javaPrimitiveType
            )
            val result = method.invoke(null, sslSocket, EXPORTED_KEY_LABEL, null, length) as? ByteArray
            if (result != null && result.size == length) return result
        } catch (_: Throwable) {}

        // 2. Try com.android.org.conscrypt.Conscrypt (Android internal)
        try {
            val conscryptClass = Class.forName("com.android.org.conscrypt.Conscrypt")
            val method = conscryptClass.getMethod(
                "exportKeyingMaterial",
                SSLSocket::class.java,
                String::class.java,
                ByteArray::class.java,
                Int::class.javaPrimitiveType
            )
            val result = method.invoke(null, sslSocket, EXPORTED_KEY_LABEL, null, length) as? ByteArray
            if (result != null && result.size == length) return result
        } catch (_: Throwable) {}

        // 3. Try method on sslSocket directly
        try {
            val method = sslSocket.javaClass.getMethod(
                "exportKeyingMaterial",
                String::class.java,
                ByteArray::class.java,
                Int::class.javaPrimitiveType
            )
            val result = method.invoke(sslSocket, EXPORTED_KEY_LABEL, null, length) as? ByteArray
            if (result != null && result.size == length) return result
        } catch (_: Throwable) {}

        throw SSLException("TLSv1.3 exportKeyingMaterial not supported on this platform")
    }
}
