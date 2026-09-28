package com.yangyx.adbhelper.adb

import com.flyfish233.crypto.spake2.Spake2Context
import com.flyfish233.crypto.spake2.Spake2Role
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Spake2AdbTest {

    @Test
    fun testAliceAndBobKeyExchangeAndAesGcm() {
        val clientName = "adb pair client\u0000".toByteArray(Charsets.UTF_8)
        val serverName = "adb pair server\u0000".toByteArray(Charsets.UTF_8)
        val hkdfInfo = "adb pairing_auth aes-128-gcm key".toByteArray(Charsets.UTF_8)

        // Password + TLS exported key material
        val password = "123456fakeexportedkeyingmaterial64byteslongabcdefghijklmnopqrstuvwxyz123456".toByteArray(Charsets.UTF_8)

        // Alice (Client) and Bob (Server)
        val alice = Spake2Context(Spake2Role.Alice, clientName, serverName)
        val bob = Spake2Context(Spake2Role.Bob, serverName, clientName)

        val aliceMsg = alice.generateMessage(password)
        val bobMsg = bob.generateMessage(password)

        val aliceSecret = alice.processMessage(bobMsg)
        val bobSecret = bob.processMessage(aliceMsg)

        assertNotNull(aliceSecret)
        assertNotNull(bobSecret)
        assertArrayEquals("Alice and Bob should derive the exact same shared secret", bobSecret, aliceSecret)

        // Key derivation
        val hkdfAlice = HKDFBytesGenerator(SHA256Digest())
        hkdfAlice.init(HKDFParameters(aliceSecret, null, hkdfInfo))
        val aliceKey = ByteArray(16)
        hkdfAlice.generateBytes(aliceKey, 0, 16)

        val hkdfBob = HKDFBytesGenerator(SHA256Digest())
        hkdfBob.init(HKDFParameters(bobSecret, null, hkdfInfo))
        val bobKey = ByteArray(16)
        hkdfBob.generateBytes(bobKey, 0, 16)

        assertArrayEquals("Derived AES keys must match", bobKey, aliceKey)

        // Client encrypts PeerInfo, Server decrypts
        val clientIv = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putLong(0L).array()
        val encCipher = GCMBlockCipher.newInstance(AESEngine.newInstance())
        encCipher.init(true, AEADParameters(KeyParameter(aliceKey), 128, clientIv))
        val plainText = "Test ADB Peer Info Payload".toByteArray(Charsets.UTF_8)
        val cipherText = ByteArray(encCipher.getOutputSize(plainText.size))
        val encLen = encCipher.processBytes(plainText, 0, plainText.size, cipherText, 0)
        encCipher.doFinal(cipherText, encLen)

        // Server decrypts with same key and same IV
        val serverIv = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putLong(0L).array()
        val decCipher = GCMBlockCipher.newInstance(AESEngine.newInstance())
        decCipher.init(false, AEADParameters(KeyParameter(bobKey), 128, serverIv))
        val decryptedText = ByteArray(decCipher.getOutputSize(cipherText.size))
        val decLen = decCipher.processBytes(cipherText, 0, cipherText.size, decryptedText, 0)
        decCipher.doFinal(decryptedText, decLen)

        assertArrayEquals(plainText, decryptedText)
    }

    @Test
    fun testAdbStlsPacketSerialization() {
        val stlsMsg = AdbMessage(AdbMessage.CMD_STLS, AdbMessage.A_STLS_VERSION, 0)
        org.junit.Assert.assertEquals(0x534c5453, stlsMsg.command)
        org.junit.Assert.assertEquals(0x01000000, stlsMsg.arg0)

        val baos = java.io.ByteArrayOutputStream()
        stlsMsg.write(baos)

        val bytes = baos.toByteArray()
        org.junit.Assert.assertEquals(24, bytes.size)

        val bais = java.io.ByteArrayInputStream(bytes)
        val parsed = AdbMessage.read(bais)

        org.junit.Assert.assertEquals(AdbMessage.CMD_STLS, parsed.command)
        org.junit.Assert.assertEquals(AdbMessage.A_STLS_VERSION, parsed.arg0)
        org.junit.Assert.assertEquals(0, parsed.arg1)
        org.junit.Assert.assertEquals(0, parsed.payload.size)
    }
}
