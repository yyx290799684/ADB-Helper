package com.yangyx.adbhelper.adb

import android.content.Context
import android.util.Base64
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.net.Socket
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Principal
import java.security.PrivateKey
import java.security.Provider
import java.security.PublicKey
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Date
import javax.crypto.Cipher
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager

class AdbCrypto private constructor(val keyPair: KeyPair, private val context: Context) {

    private var cachedCertificate: X509Certificate? = null

    /**
     * Get or generate a self-signed X.509 certificate for TLS 1.3 wireless pairing.
     */
    fun getOrCreateCertificate(): X509Certificate {
        cachedCertificate?.let { return it }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val certBase64 = prefs.getString(KEY_CERT, null)
        if (certBase64 != null) {
            try {
                val cf = CertificateFactory.getInstance("X.509")
                val cert = cf.generateCertificate(ByteArrayInputStream(Base64.decode(certBase64, Base64.DEFAULT))) as X509Certificate
                cachedCertificate = cert
                return cert
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Generate self-signed certificate using BouncyCastle
        val now = System.currentTimeMillis()
        val notBefore = Date(now - 86400000L) // 1 day ago
        val notAfter = Date(now + 10L * 365 * 86400000L) // 10 years validity
        val serialNumber = BigInteger(64, SecureRandom())
        val name = X500Name("CN=ADB Helper, O=Android, C=US")

        val builder = JcaX509v3CertificateBuilder(
            name,
            serialNumber,
            notBefore,
            notAfter,
            name,
            keyPair.public
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val cert = JcaX509CertificateConverter().getCertificate(builder.build(signer))

        prefs.edit()
            .putString(KEY_CERT, Base64.encodeToString(cert.encoded, Base64.DEFAULT))
            .apply()

        cachedCertificate = cert
        return cert
    }

    /**
     * Creates a TLS 1.3 SSLContext with this device's client key & certificate
     * for ADB wireless pairing and wireless connection.
     */
    fun createSslContext(): SSLContext {
        val certificate = getOrCreateCertificate()
        val privateKey = keyPair.private

        val provider: Provider? = try {
            val providerClass = Class.forName("org.conscrypt.OpenSSLProvider")
            providerClass.getDeclaredConstructor().newInstance() as Provider
        } catch (_: Throwable) {
            null
        }

        val sslContext = if (provider != null) {
            SSLContext.getInstance("TLSv1.3", provider)
        } else {
            SSLContext.getInstance("TLSv1.3")
        }

        val keyManager = object : X509ExtendedKeyManager() {
            private val alias = "adb-client-key"

            override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> = arrayOf(alias)

            override fun chooseClientAlias(keyTypes: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?): String = alias

            override fun chooseEngineClientAlias(keyTypes: Array<out String>?, issuers: Array<out Principal>?, engine: SSLEngine?): String = alias

            override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? = null

            override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String? = null

            override fun getCertificateChain(aliasName: String?): Array<X509Certificate> = arrayOf(certificate)

            override fun getPrivateKey(aliasName: String?): PrivateKey = privateKey
        }

        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        sslContext.init(arrayOf(keyManager), arrayOf(trustManager), SecureRandom())
        return sslContext
    }

    /**
     * Sign the AUTH token sent by adbd with RSA PKCS1 v1.5
     */
    private val SIGNATURE_AID = byteArrayOf(
        0x30.toByte(), 0x21.toByte(), 0x30.toByte(), 0x09.toByte(), 0x06.toByte(),
        0x05.toByte(), 0x2b.toByte(), 0x0e.toByte(), 0x03.toByte(), 0x02.toByte(),
        0x1a.toByte(), 0x05.toByte(), 0x00.toByte(), 0x04.toByte(), 0x14.toByte()
    )

    fun signToken(token: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.private)
        val payload = ByteArray(SIGNATURE_AID.size + token.size)
        System.arraycopy(SIGNATURE_AID, 0, payload, 0, SIGNATURE_AID.size)
        System.arraycopy(token, 0, payload, SIGNATURE_AID.size, token.size)
        return cipher.doFinal(payload)
    }

    /**
     * Format public key for ADB AUTH_RSAPUBLICKEY message and Wireless Pairing PeerInfo.
     */
    fun getAdbPublicKeyPayload(deviceName: String = "ADB_Helper"): ByteArray {
        val pubKey = keyPair.public as RSAPublicKey
        val pubKeyBytes = convertAdbPublicKey(pubKey)
        val base64Key = Base64.encodeToString(pubKeyBytes, Base64.NO_WRAP)
        val fullString = "$base64Key $deviceName\u0000"
        return fullString.toByteArray(Charsets.UTF_8)
    }

    private fun convertAdbPublicKey(pubKey: RSAPublicKey): ByteArray {
        val n = pubKey.modulus
        val e = pubKey.publicExponent

        // ADB public key format: 32-bit words
        // N0inv, N[64], RR[64], Exponent
        val r = BigInteger.ONE.shiftLeft(32)
        val r32 = BigInteger.ONE.shiftLeft(2048)
        val rr = r32.multiply(r32).mod(n)
        val rem = n.mod(r)
        val n0inv = rem.modInverse(r).negate().mod(r).toLong()

        val bos = ByteArrayOutputStream()
        write32(bos, 2048 / 32) // len in words
        write32(bos, n0inv)

        // N mod n
        val nWords = bigIntToWords(n, 64)
        for (w in nWords) {
            write32(bos, w)
        }

        // RR mod n
        val rrWords = bigIntToWords(rr, 64)
        for (w in rrWords) {
            write32(bos, w)
        }

        write32(bos, e.toLong())
        return bos.toByteArray()
    }

    private fun bigIntToWords(bigInt: BigInteger, wordCount: Int): LongArray {
        val words = LongArray(wordCount)
        var temp = bigInt
        val mask = BigInteger.valueOf(0xFFFFFFFFL)
        for (i in 0 until wordCount) {
            words[i] = temp.and(mask).longValueExact()
            temp = temp.shiftRight(32)
        }
        return words
    }

    private fun write32(bos: ByteArrayOutputStream, value: Long) {
        bos.write((value and 0xFF).toInt())
        bos.write(((value shr 8) and 0xFF).toInt())
        bos.write(((value shr 16) and 0xFF).toInt())
        bos.write(((value shr 24) and 0xFF).toInt())
    }

    companion object {
        private const val PREFS_NAME = "adb_crypto_keys"
        private const val KEY_PRIV = "private_key"
        private const val KEY_PUB = "public_key"
        private const val KEY_CERT = "x509_certificate"

        fun getOrCreate(context: Context): AdbCrypto {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val privBase64 = prefs.getString(KEY_PRIV, null)
            val pubBase64 = prefs.getString(KEY_PUB, null)

            if (privBase64 != null && pubBase64 != null) {
                try {
                    val keyFactory = KeyFactory.getInstance("RSA")
                    val privSpec = PKCS8EncodedKeySpec(Base64.decode(privBase64, Base64.DEFAULT))
                    val pubSpec = X509EncodedKeySpec(Base64.decode(pubBase64, Base64.DEFAULT))

                    val privKey = keyFactory.generatePrivate(privSpec)
                    val pubKey = keyFactory.generatePublic(pubSpec)
                    return AdbCrypto(KeyPair(pubKey, privKey), context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Generate new 2048 RSA keypair
            val kpg = KeyPairGenerator.getInstance("RSA")
            kpg.initialize(2048)
            val kp = kpg.generateKeyPair()

            prefs.edit()
                .putString(KEY_PRIV, Base64.encodeToString(kp.private.encoded, Base64.DEFAULT))
                .putString(KEY_PUB, Base64.encodeToString(kp.public.encoded, Base64.DEFAULT))
                .apply()

            return AdbCrypto(kp, context)
        }
    }
}
