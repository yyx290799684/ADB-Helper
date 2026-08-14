package com.yangyx.adbhelper.adb

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class AdbMessage(
    val command: Int,
    val arg0: Int,
    val arg1: Int,
    val payload: ByteArray = ByteArray(0)
) {
    companion object {
        const val CMD_SYNC = 0x434e5953
        const val CMD_CNXN = 0x4e584e43
        const val CMD_AUTH = 0x48545541
        const val CMD_OPEN = 0x4e45504f
        const val CMD_OKAY = 0x59414b4f
        const val CMD_CLSE = 0x45534c43
        const val CMD_WRTE = 0x45545257

        const val AUTH_TYPE_TOKEN = 1
        const val AUTH_TYPE_SIGNATURE = 2
        const val AUTH_TYPE_RSAPUBLICKEY = 3

        fun checksum(data: ByteArray): Int {
            var sum = 0
            for (b in data) {
                sum += (b.toInt() and 0xFF)
            }
            return sum
        }

        fun read(input: InputStream): AdbMessage {
            val header = ByteArray(24)
            var read = 0
            while (read < 24) {
                val n = input.read(header, read, 24 - read)
                if (n < 0) throw Exception("ADB connection EOF while reading packet header")
                read += n
            }

            val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val cmd = bb.int
            val arg0 = bb.int
            val arg1 = bb.int
            val dataLen = bb.int
            val dataCrc = bb.int
            val magic = bb.int

            if (cmd != (magic xor -0x1)) {
                throw Exception("Invalid ADB header magic mismatch: cmd=0x${Integer.toHexString(cmd)} magic=0x${Integer.toHexString(magic)}")
            }

            val payload = ByteArray(dataLen)
            var pRead = 0
            while (pRead < dataLen) {
                val n = input.read(payload, pRead, dataLen - pRead)
                if (n < 0) throw Exception("ADB connection EOF while reading payload")
                pRead += n
            }

            return AdbMessage(cmd, arg0, arg1, payload)
        }
    }

    fun write(output: OutputStream) {
        val bb = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        bb.putInt(command)
        bb.putInt(arg0)
        bb.putInt(arg1)
        bb.putInt(payload.size)
        bb.putInt(checksum(payload))
        bb.putInt(command xor -0x1)

        synchronized(output) {
            output.write(bb.array())
            if (payload.isNotEmpty()) {
                output.write(payload)
            }
            output.flush()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AdbMessage
        if (command != other.command) return false
        if (arg0 != other.arg0) return false
        if (arg1 != other.arg1) return false
        if (!payload.contentEquals(other.payload)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = command
        result = 31 * result + arg0
        result = 31 * result + arg1
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
