package com.yangyx.adbhelper.adb

import android.content.Context
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

class AdbCrypto private constructor(val keyPair: KeyPair) {

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
     * Format public key for ADB AUTH_RSAPUBLICKEY message.
     * ADB requires an Android-specific RSA public key struct representation or base64 structure.
     */
    fun getAdbPublicKeyPayload(): ByteArray {
        val pubKey = keyPair.public as RSAPublicKey
        val pubKeyBytes = convertAdbPublicKey(pubKey)
        val base64Key = Base64.encodeToString(pubKeyBytes, Base64.NO_WRAP)
        val fullString = "$base64Key adbhelper@android\u0000"
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
                    return AdbCrypto(KeyPair(pubKey, privKey))
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

            return AdbCrypto(kp)
        }
    }
}
