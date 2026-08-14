package com.yangyx.adbhelper.adb

import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class AdbStream(
    val localId: Int,
    private val connection: AdbConnection
) {
    var remoteId: Int = 0
    @Volatile
    var isClosed: Boolean = false
        private set

    private val readQueue = LinkedBlockingQueue<ByteArray>()

    fun onData(data: ByteArray) {
        if (data.isNotEmpty()) {
            readQueue.offer(data)
        }
    }

    fun read(timeoutMs: Long = 5000): ByteArray? {
        if (isClosed && readQueue.isEmpty()) return null
        return try {
            readQueue.poll(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            null
        }
    }

    private val writeAckLock = java.lang.Object()

    fun onWriteAck() {
        synchronized(writeAckLock) {
            writeAckLock.notifyAll()
        }
    }

    fun markClosedLocally() {
        isClosed = true
        synchronized(writeAckLock) {
            writeAckLock.notifyAll()
        }
    }

    fun close() {
        if (!isClosed) {
            isClosed = true
            connection.closeStream(this)
            synchronized(writeAckLock) {
                writeAckLock.notifyAll()
            }
        }
    }

    fun write(data: ByteArray) {
        if (isClosed) throw IllegalStateException("Stream $localId is closed")
        
        val maxChunkSize = 16384
        var offset = 0
        while (offset < data.size) {
            if (isClosed) throw IllegalStateException("Stream $localId was closed during write")
            val len = minOf(maxChunkSize, data.size - offset)
            val chunk = if (offset == 0 && len == data.size) data else data.copyOfRange(offset, offset + len)

            synchronized(writeAckLock) {
                connection.writeStream(this, chunk)
                writeAckLock.wait(10000)
            }
            if (isClosed) {
                throw IllegalStateException("Stream $localId was closed by remote peer while sending data")
            }
            offset += len
        }
    }

    fun readAllText(timeoutMs: Long = 3000): String {
        val sb = StringBuilder()
        val startTime = System.currentTimeMillis()
        while (!isClosed && (System.currentTimeMillis() - startTime) < timeoutMs) {
            val chunk = read(200) ?: if (isClosed) break else continue
            sb.append(String(chunk, Charsets.UTF_8))
            if (isClosed) break
        }
        // Drain any remaining in queue
        while (readQueue.isNotEmpty()) {
            val chunk = readQueue.poll() ?: break
            sb.append(String(chunk, Charsets.UTF_8))
        }
        return sb.toString()
    }

    private var readResidualBuffer: ByteArray? = null
    private var readResidualOffset: Int = 0

    @Synchronized
    fun readFully(buffer: ByteArray, offset: Int, length: Int): Boolean {
        var total = 0
        while (total < length) {
            val resBuf = readResidualBuffer
            if (resBuf != null && readResidualOffset < resBuf.size) {
                val available = resBuf.size - readResidualOffset
                val toCopy = minOf(available, length - total)
                System.arraycopy(resBuf, readResidualOffset, buffer, offset + total, toCopy)
                total += toCopy
                readResidualOffset += toCopy
                if (readResidualOffset >= resBuf.size) {
                    readResidualBuffer = null
                    readResidualOffset = 0
                }
                continue
            }

            if (isClosed && readQueue.isEmpty()) return false
            val chunk = read(3000) ?: if (isClosed) return false else continue
            if (chunk.isEmpty()) continue

            val toCopy = minOf(chunk.size, length - total)
            System.arraycopy(chunk, 0, buffer, offset + total, toCopy)
            total += toCopy

            if (toCopy < chunk.size) {
                readResidualBuffer = chunk
                readResidualOffset = toCopy
            }
        }
        return true
    }

    fun asInputStream(): InputStream {
        return object : InputStream() {
            private var currentBuffer: ByteArray? = null
            private var currentOffset = 0

            override fun read(): Int {
                val buf = getBuffer() ?: return -1
                val byteVal = buf[currentOffset].toInt() and 0xFF
                currentOffset++
                if (currentOffset >= buf.size) {
                    currentBuffer = null
                    currentOffset = 0
                }
                return byteVal
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (len == 0) return 0
                val buf = getBuffer() ?: return -1
                val bytesToRead = minOf(len, buf.size - currentOffset)
                System.arraycopy(buf, currentOffset, b, off, bytesToRead)
                currentOffset += bytesToRead
                if (currentOffset >= buf.size) {
                    currentBuffer = null
                    currentOffset = 0
                }
                return bytesToRead
            }

            private fun getBuffer(): ByteArray? {
                while (currentBuffer == null || currentOffset >= currentBuffer!!.size) {
                    val nextChunk = this@AdbStream.read(500)
                    if (nextChunk != null && nextChunk.isNotEmpty()) {
                        currentBuffer = nextChunk
                        currentOffset = 0
                        return currentBuffer
                    }
                    if (this@AdbStream.isClosed) return null
                }
                return currentBuffer
            }
        }
    }
}
