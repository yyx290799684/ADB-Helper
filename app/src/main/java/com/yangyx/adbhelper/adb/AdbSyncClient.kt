package com.yangyx.adbhelper.adb

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class RemoteFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val mode: Int,
    val lastModified: Long
)

class AdbSyncClient(private val connection: AdbConnection) {

    fun listDirectory(remotePath: String): List<RemoteFileItem> {
        val path = if (remotePath.endsWith("/")) remotePath else "$remotePath/"
        // Fallback to shell ls -la parsing if sync LIST protocol is restrictive
        val output = connection.executeShell("ls -la '$path'")
        val items = mutableListOf<RemoteFileItem>()

        val lines = output.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("total")) continue

            val parts = trimmed.split(Regex("\\s+"))
            if (parts.size >= 8) {
                val permissions = parts[0]
                val isDir = permissions.startsWith("d")
                val isLink = permissions.startsWith("l")
                val size = parts[4].toLongOrNull() ?: 0L
                val name = parts.subList(7, parts.size).joinToString(" ")

                if (name == "." || name == "..") continue

                // Clean link name if symlink (e.g. name -> target)
                val cleanName = if (isLink && name.contains(" -> ")) name.substringBefore(" -> ") else name
                val fullPath = if (path.endsWith("/")) "$path$cleanName" else "$path/$cleanName"

                items.add(
                    RemoteFileItem(
                        name = cleanName,
                        path = fullPath,
                        isDirectory = isDir,
                        size = size,
                        mode = 0,
                        lastModified = System.currentTimeMillis()
                    )
                )
            }
        }
        return items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun pushFile(localStream: InputStream, remotePath: String, onProgress: (Long) -> Unit = {}) {
        val stream = connection.openStream("sync:")
        try {
            // Write SEND request: "SEND" (4 bytes), path len (4 bytes), "path,33206"
            val pathBytes = "$remotePath,33206".toByteArray(Charsets.UTF_8)
            val sendHeader = ByteBuffer.allocate(8 + pathBytes.size).order(ByteOrder.LITTLE_ENDIAN)
            sendHeader.put("SEND".toByteArray(Charsets.UTF_8))
            sendHeader.putInt(pathBytes.size)
            sendHeader.put(pathBytes)
            stream.write(sendHeader.array())

            // Write DATA chunks
            val bufferSize = 64 * 1024
            val buffer = ByteArray(bufferSize)
            var totalSent = 0L
            var read: Int

            while (localStream.read(buffer).also { read = it } != -1) {
                val chunkBuf = ByteBuffer.allocate(8 + read).order(ByteOrder.LITTLE_ENDIAN)
                chunkBuf.put("DATA".toByteArray(Charsets.UTF_8))
                chunkBuf.putInt(read)
                chunkBuf.put(buffer, 0, read)
                stream.write(chunkBuf.array())

                totalSent += read
                onProgress(totalSent)
            }

            // Write DONE request
            val doneHeader = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            doneHeader.put("DONE".toByteArray(Charsets.UTF_8))
            doneHeader.putInt((System.currentTimeMillis() / 1000).toInt())
            stream.write(doneHeader.array())

            // Read OKAY or FAIL response from adbd before closing stream
            val respHeader = ByteArray(8)
            if (stream.readFully(respHeader, 0, 8)) {
                val respBb = ByteBuffer.wrap(respHeader).order(ByteOrder.LITTLE_ENDIAN)
                val idBytes = ByteArray(4)
                respBb.get(idBytes)
                val id = String(idBytes, Charsets.UTF_8)
                val lenOrStatus = respBb.int

                if (id == "FAIL") {
                    val errBytes = ByteArray(lenOrStatus)
                    if (lenOrStatus > 0) {
                        stream.readFully(errBytes, 0, lenOrStatus)
                    }
                    val errMsg = String(errBytes, Charsets.UTF_8)
                    throw Exception("传输失败: $errMsg")
                } else if (id != "OKAY") {
                    throw Exception("未知的 Sync 响应: $id")
                }
            } else {
                throw Exception("未收到 Sync 设备确认响应")
            }

        } finally {
            stream.close()
        }
    }

    fun pullFile(remotePath: String, outputStream: OutputStream, onProgress: (Long) -> Unit = {}) {
        val stream = connection.openStream("sync:")
        try {
            // Write RECV request
            val pathBytes = remotePath.toByteArray(Charsets.UTF_8)
            val recvHeader = ByteBuffer.allocate(8 + pathBytes.size).order(ByteOrder.LITTLE_ENDIAN)
            recvHeader.put("RECV".toByteArray(Charsets.UTF_8))
            recvHeader.putInt(pathBytes.size)
            recvHeader.put(pathBytes)
            stream.write(recvHeader.array())

            var totalReceived = 0L
            var done = false

            while (!done) {
                val headerBytes = ByteArray(8)
                if (!stream.readFully(headerBytes, 0, 8)) break

                val bb = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
                val idBytes = ByteArray(4)
                bb.get(idBytes)
                val id = String(idBytes, Charsets.UTF_8)
                val len = bb.int

                if (id == "DATA") {
                    val data = ByteArray(len)
                    if (stream.readFully(data, 0, len)) {
                        outputStream.write(data)
                        totalReceived += len
                        onProgress(totalReceived)
                    } else {
                        throw Exception("拉取文件数据块不完整")
                    }
                } else if (id == "DONE") {
                    done = true
                } else if (id == "FAIL") {
                    val errBytes = ByteArray(len)
                    if (len > 0) stream.readFully(errBytes, 0, len)
                    val errMsg = String(errBytes, Charsets.UTF_8)
                    throw Exception("拉取文件失败: $errMsg")
                }
            }
        } finally {
            stream.close()
        }
    }

    fun deletePath(remotePath: String): Boolean {
        val res = connection.executeShell("rm -rf '$remotePath'")
        return !res.contains("Permission denied") && !res.contains("No such file")
    }

    fun createFolder(remotePath: String): Boolean {
        val res = connection.executeShell("mkdir -p '$remotePath'")
        return !res.contains("Permission denied")
    }
}
