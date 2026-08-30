package com.yangyx.adbhelper.adb

import androidx.compose.runtime.Immutable
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Immutable
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
        val isRootOrSystem = isSystemOrRestrictedPath(path)

        // If it's a known system/root path, we can directly try su or fallback seamlessly
        var output = connection.executeShell("ls -la '$path'")
        if (output.contains("Permission denied") || output.contains("opendir failed") || 
            (isRootOrSystem && (output.trim().isEmpty() || output.contains("ls: ")))
        ) {
            val rootOutput = connection.executeShell("su -c \"ls -la '$path'\"")
            if (rootOutput.isNotEmpty() && !rootOutput.contains("Permission denied") && !rootOutput.contains("not found")) {
                output = rootOutput
            } else {
                val su0Output = connection.executeShell("su 0 ls -la '$path'")
                if (su0Output.isNotEmpty() && !su0Output.contains("Permission denied")) {
                    output = su0Output
                }
            }
        }

        val items = mutableListOf<RemoteFileItem>()
        val lines = output.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("total") || trimmed.startsWith("ls: ")) continue

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
            } else if (parts.size in 4..7 && (parts[0].startsWith("-") || parts[0].startsWith("d") || parts[0].startsWith("l"))) {
                // Short ls output format fallback
                val permissions = parts[0]
                val isDir = permissions.startsWith("d")
                val name = parts.last()
                if (name == "." || name == "..") continue
                val fullPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
                items.add(
                    RemoteFileItem(
                        name = name,
                        path = fullPath,
                        isDirectory = isDir,
                        size = 0L,
                        mode = 0,
                        lastModified = System.currentTimeMillis()
                    )
                )
            }
        }
        return items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    private fun isSystemOrRestrictedPath(path: String): Boolean {
        val clean = path.trim().removeSuffix("/")
        return clean.isEmpty() || clean == "/" || clean == "/data" || clean.startsWith("/data/") ||
                clean == "/system" || clean.startsWith("/system/") ||
                clean == "/vendor" || clean.startsWith("/vendor/") ||
                clean == "/root" || clean.startsWith("/root/") ||
                clean == "/etc" || clean.startsWith("/etc/") ||
                clean == "/sbin" || clean.startsWith("/sbin/") ||
                clean == "/apex" || clean.startsWith("/apex/")
    }

    fun pushFile(
        localStream: InputStream,
        remotePath: String,
        isCancelled: () -> Boolean = { false },
        onProgress: (Long) -> Unit = {}
    ) {
        val stream = connection.openStream("sync:")
        try {
            if (isCancelled()) throw java.util.concurrent.CancellationException("Push cancelled before start")

            // Write SEND request: "SEND" (4 bytes), path len (4 bytes), "path,33206"
            val pathBytes = "$remotePath,33206".toByteArray(Charsets.UTF_8)
            val sendHeader = ByteBuffer.allocate(8 + pathBytes.size).order(ByteOrder.LITTLE_ENDIAN)
            sendHeader.put("SEND".toByteArray(Charsets.UTF_8))
            sendHeader.putInt(pathBytes.size)
            sendHeader.put(pathBytes)
            stream.write(sendHeader.array())

            // High-throughput DATA batching: standard 64KB sub-chunks batched into max payload sizes
            val syncDataMax = 64 * 1024
            val maxBatchSize = connection.maxPayloadSize.coerceIn(65536, 1048576)
            val batchBuffer = java.io.ByteArrayOutputStream(maxBatchSize + syncDataMax + 8)
            val buffer = ByteArray(syncDataMax)
            var totalSent = 0L
            var read: Int

            val headerBuf = ByteArray(8)
            val bbHeader = ByteBuffer.wrap(headerBuf).order(ByteOrder.LITTLE_ENDIAN)
            val dataTag = "DATA".toByteArray(Charsets.UTF_8)
            val bufferedInput = if (localStream is java.io.BufferedInputStream) localStream else java.io.BufferedInputStream(localStream, 524288)

            while (bufferedInput.read(buffer).also { read = it } != -1) {
                if (isCancelled()) {
                    throw java.util.concurrent.CancellationException("Push cancelled during read")
                }
                if (read > 0) {
                    bbHeader.position(0)
                    bbHeader.put(dataTag)
                    bbHeader.putInt(read)
                    batchBuffer.write(headerBuf, 0, 8)
                    batchBuffer.write(buffer, 0, read)

                    if (batchBuffer.size() >= maxBatchSize) {
                        if (isCancelled()) throw java.util.concurrent.CancellationException("Push cancelled before batch write")
                        stream.write(batchBuffer.toByteArray())
                        batchBuffer.reset()
                    }

                    totalSent += read
                    if (!isCancelled()) {
                        onProgress(totalSent)
                    }
                }
            }

            if (isCancelled()) throw java.util.concurrent.CancellationException("Push cancelled before finish")

            if (batchBuffer.size() > 0) {
                stream.write(batchBuffer.toByteArray())
                batchBuffer.reset()
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
            try { stream.close() } catch (_: Exception) {}
        }
    }

    fun pullFile(
        remotePath: String,
        outputStream: OutputStream,
        isCancelled: () -> Boolean = { false },
        onProgress: (Long) -> Unit = {}
    ) {
        try {
            pullFileStandardSync(remotePath, outputStream, isCancelled, onProgress)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("FAIL") || msg.contains("Permission denied") || isSystemOrRestrictedPath(remotePath)) {
                // Attempt Root Cat stream fallback
                pullFileWithRootCat(remotePath, outputStream, isCancelled, onProgress)
            } else {
                throw e
            }
        }
    }

    private fun pullFileStandardSync(
        remotePath: String,
        outputStream: OutputStream,
        isCancelled: () -> Boolean,
        onProgress: (Long) -> Unit
    ) {
        val stream = connection.openStream("sync:")
        try {
            if (isCancelled()) throw java.util.concurrent.CancellationException("Pull cancelled before start")

            val bufferedOutput = if (outputStream is java.io.BufferedOutputStream) outputStream else java.io.BufferedOutputStream(outputStream, 524288)
            // Write RECV request
            val pathBytes = remotePath.toByteArray(Charsets.UTF_8)
            val recvHeader = ByteBuffer.allocate(8 + pathBytes.size).order(ByteOrder.LITTLE_ENDIAN)
            recvHeader.put("RECV".toByteArray(Charsets.UTF_8))
            recvHeader.putInt(pathBytes.size)
            recvHeader.put(pathBytes)
            stream.write(recvHeader.array())

            var totalReceived = 0L
            var done = false

            val headerBytes = ByteArray(8)
            val chunkBuffer = ByteArray(256 * 1024)

            while (!done) {
                if (isCancelled()) throw java.util.concurrent.CancellationException("Pull cancelled during receive")

                if (!stream.readFully(headerBytes, 0, 8)) break

                val bb = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
                val idBytes = ByteArray(4)
                bb.get(idBytes)
                val id = String(idBytes, Charsets.UTF_8)
                val len = bb.int

                if (id == "DATA") {
                    var remaining = len
                    while (remaining > 0) {
                        if (isCancelled()) throw java.util.concurrent.CancellationException("Pull cancelled during data chunk")
                        val toRead = minOf(remaining, chunkBuffer.size)
                        if (stream.readFully(chunkBuffer, 0, toRead)) {
                            bufferedOutput.write(chunkBuffer, 0, toRead)
                            remaining -= toRead
                            totalReceived += toRead
                            if (!isCancelled()) {
                                onProgress(totalReceived)
                            }
                        } else {
                            throw Exception("拉取文件数据块不完整")
                        }
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
            if (isCancelled()) throw java.util.concurrent.CancellationException("Pull cancelled before flush")
            bufferedOutput.flush()
        } finally {
            try { stream.close() } catch (_: Exception) {}
        }
    }

    private fun pullFileWithRootCat(
        remotePath: String,
        outputStream: OutputStream,
        isCancelled: () -> Boolean,
        onProgress: (Long) -> Unit
    ) {
        val stream = connection.openStream("exec:su -c 'cat \"$remotePath\"'")
        try {
            if (isCancelled()) throw java.util.concurrent.CancellationException("Pull cancelled before root start")
            val bufferedOutput = if (outputStream is java.io.BufferedOutputStream) outputStream else java.io.BufferedOutputStream(outputStream, 524288)
            var totalReceived = 0L

            while (true) {
                if (isCancelled()) throw java.util.concurrent.CancellationException("Pull cancelled during root receive")
                val packet = stream.read(5000) ?: break
                bufferedOutput.write(packet)
                totalReceived += packet.size
                if (!isCancelled()) {
                    onProgress(totalReceived)
                }
            }
            bufferedOutput.flush()
        } finally {
            try { stream.close() } catch (_: Exception) {}
        }
    }

    fun deletePath(remotePath: String): Boolean {
        var res = connection.executeShell("rm -rf '$remotePath'")
        if (res.contains("Permission denied") || res.contains("failed")) {
            res = connection.executeShell("su -c \"rm -rf '$remotePath'\"")
        }
        return !res.contains("Permission denied") && !res.contains("No such file")
    }

    fun createFolder(remotePath: String): Boolean {
        var res = connection.executeShell("mkdir -p '$remotePath'")
        if (res.contains("Permission denied") || res.contains("failed")) {
            res = connection.executeShell("su -c \"mkdir -p '$remotePath'\"")
        }
        return !res.contains("Permission denied")
    }

    fun renamePath(oldPath: String, newPath: String): Boolean {
        var res = connection.executeShell("mv '$oldPath' '$newPath'")
        if (res.contains("Permission denied") || res.contains("failed")) {
            res = connection.executeShell("su -c \"mv '$oldPath' '$newPath'\"")
        }
        return !res.contains("Permission denied") && !res.contains("failed") && !res.contains("No such file")
    }

    fun copyPath(sourcePath: String, destPath: String): Boolean {
        var res = connection.executeShell("cp -r '$sourcePath' '$destPath'")
        if (res.contains("Permission denied") || res.contains("failed")) {
            res = connection.executeShell("su -c \"cp -r '$sourcePath' '$destPath'\"")
        }
        return !res.contains("Permission denied") && !res.contains("failed")
    }

    fun movePath(sourcePath: String, destPath: String): Boolean {
        var res = connection.executeShell("mv '$sourcePath' '$destPath'")
        if (res.contains("Permission denied") || res.contains("failed")) {
            res = connection.executeShell("su -c \"mv '$sourcePath' '$destPath'\"")
        }
        return !res.contains("Permission denied") && !res.contains("failed")
    }
}

