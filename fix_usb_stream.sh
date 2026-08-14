#!/bin/bash
cat << 'INNER_EOF' > /app/applet/app/src/main/java/com/yangyx/adbhelper/adb/UsbAdbStream.kt
package com.yangyx.adbhelper.adb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.min

class UsbAdbInputStream(
    private val connection: UsbDeviceConnection,
    private val endpoint: UsbEndpoint
) : InputStream() {
    @Volatile
    var isClosed = false

    override fun read(): Int {
        val buf = ByteArray(1)
        val res = read(buf, 0, 1)
        if (res <= 0) return -1
        return buf[0].toInt() and 0xFF
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (isClosed) return -1
        val buffer = if (off == 0 && len == b.size) {
            b
        } else {
            ByteArray(len)
        }
        
        var failureCount = 0
        while (!isClosed) {
            val startTime = System.currentTimeMillis()
            val transferred = connection.bulkTransfer(endpoint, buffer, len, 5000)
            if (transferred > 0) {
                if (buffer !== b) {
                    System.arraycopy(buffer, 0, b, off, transferred)
                }
                failureCount = 0
                return transferred
            } else if (transferred < 0) {
                val duration = System.currentTimeMillis() - startTime
                if (duration < 100) {
                    // Failed immediately, might be disconnected
                    failureCount++
                    if (failureCount > 10) {
                        return -1
                    }
                    try { Thread.sleep(100) } catch (e: Exception) {}
                } else {
                    // Just a timeout
                    failureCount = 0
                }
            }
        }
        return -1
    }

    override fun close() {
        isClosed = true
    }
}

class UsbAdbOutputStream(
    private val connection: UsbDeviceConnection,
    private val endpoint: UsbEndpoint
) : OutputStream() {
    @Volatile
    var isClosed = false

    override fun write(b: Int) {
        write(byteArrayOf(b.toByte()), 0, 1)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        if (isClosed) throw java.io.IOException("Stream is closed")
        val buffer = if (off == 0 && len == b.size) {
            b
        } else {
            val chunk = ByteArray(len)
            System.arraycopy(b, off, chunk, 0, len)
            chunk
        }
        
        var written = 0
        while (written < len && !isClosed) {
            val chunkLength = min(len - written, 16384)
            val chunk = ByteArray(chunkLength)
            System.arraycopy(buffer, written, chunk, 0, chunkLength)
            val res = connection.bulkTransfer(endpoint, chunk, chunkLength, 5000)
            if (res < 0) {
                throw java.io.IOException("Failed to write to USB endpoint")
            }
            written += res
        }
    }

    override fun close() {
        isClosed = true
    }
}
INNER_EOF
