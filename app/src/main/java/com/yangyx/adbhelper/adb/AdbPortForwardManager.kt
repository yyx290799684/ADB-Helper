package com.yangyx.adbhelper.adb

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Immutable
data class AdbForwardRule(
    val id: String = UUID.randomUUID().toString(),
    val localPort: Int,
    val remoteSpec: String, // e.g. "tcp:8080" or "localabstract:scrcpy"
    val createTime: Long = System.currentTimeMillis(),
    val activeConnections: Int = 0,
    val totalConnections: Int = 0,
    val isRunning: Boolean = true,
    val errorMessage: String? = null
)

@Immutable
data class AdbReverseRule(
    val id: String = UUID.randomUUID().toString(),
    val remoteSpec: String, // e.g. "tcp:3000"
    val localSpec: String,  // e.g. "tcp:3000"
    val createTime: Long = System.currentTimeMillis()
)

class ForwardServer(
    var rule: AdbForwardRule,
    private val connectionProvider: () -> AdbConnection?,
    private val onUpdate: (AdbForwardRule) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeStreams = ConcurrentHashMap<AdbStream, Socket>()

    fun start(): Result<Unit> {
        return try {
            val ss = ServerSocket(rule.localPort)
            serverSocket = ss
            rule = rule.copy(isRunning = true, errorMessage = null)
            onUpdate(rule)

            serverJob = scope.launch {
                while (isActive && !ss.isClosed) {
                    try {
                        val client = ss.accept()
                        client.tcpNoDelay = true
                        rule = rule.copy(
                            totalConnections = rule.totalConnections + 1,
                            activeConnections = rule.activeConnections + 1
                        )
                        onUpdate(rule)

                        launch {
                            handleClient(client)
                        }
                    } catch (e: Exception) {
                        if (ss.isClosed) break
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            rule = rule.copy(isRunning = false, errorMessage = e.message ?: "端口绑定失败")
            onUpdate(rule)
            Result.failure(e)
        }
    }

    private suspend fun handleClient(client: Socket) {
        val conn = connectionProvider()
        if (conn == null || !conn.isConnected) {
            try { client.close() } catch (_: Exception) {}
            rule = rule.copy(activeConnections = (rule.activeConnections - 1).coerceAtLeast(0))
            onUpdate(rule)
            return
        }

        try {
            val adbStream = conn.openStream(rule.remoteSpec)
            activeStreams[adbStream] = client

            coroutineScope {
                // Client Socket -> ADB Stream
                launch(Dispatchers.IO) {
                    try {
                        val inStream = client.getInputStream()
                        val buf = ByteArray(16384)
                        while (isActive && !client.isClosed && !adbStream.isClosed) {
                            val n = inStream.read(buf)
                            if (n < 0) break
                            if (n > 0) {
                                val chunk = if (n == buf.size) buf else buf.copyOf(n)
                                adbStream.write(chunk)
                            }
                        }
                    } catch (_: Exception) {}
                    finally {
                        adbStream.close()
                        try { client.close() } catch (_: Exception) {}
                    }
                }

                // ADB Stream -> Client Socket
                launch(Dispatchers.IO) {
                    try {
                        val outStream = client.getOutputStream()
                        while (isActive && !client.isClosed && !adbStream.isClosed) {
                            val data = adbStream.read(5000L)
                            if (data != null && data.isNotEmpty()) {
                                outStream.write(data)
                                outStream.flush()
                            }
                        }
                    } catch (_: Exception) {}
                    finally {
                        try { client.close() } catch (_: Exception) {}
                        adbStream.close()
                    }
                }
            }
        } catch (_: Exception) {
            try { client.close() } catch (_: Exception) {}
        } finally {
            rule = rule.copy(activeConnections = (rule.activeConnections - 1).coerceAtLeast(0))
            onUpdate(rule)
        }
    }

    fun stop() {
        rule = rule.copy(isRunning = false, activeConnections = 0)
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
        activeStreams.forEach { (stream, socket) ->
            try { socket.close() } catch (_: Exception) {}
            stream.close()
        }
        activeStreams.clear()
        onUpdate(rule)
    }
}

class AdbPortForwardManager(
    private val connectionProvider: () -> AdbConnection?,
    private val onRulesChanged: (List<AdbForwardRule>) -> Unit
) {
    private val servers = ConcurrentHashMap<String, ForwardServer>()
    private val rulesMap = ConcurrentHashMap<String, AdbForwardRule>()

    fun startForward(localPort: Int, remoteSpec: String): Result<AdbForwardRule> {
        // Normalize remoteSpec
        val normalizedRemote = if (!remoteSpec.contains(":")) "tcp:$remoteSpec" else remoteSpec
        
        // Check if port already in use by another forward rule
        val existing = rulesMap.values.firstOrNull { it.localPort == localPort }
        if (existing != null && existing.isRunning) {
            return Result.failure(Exception("本地端口 $localPort 已被映射规则占用"))
        }

        val rule = AdbForwardRule(
            localPort = localPort,
            remoteSpec = normalizedRemote
        )
        rulesMap[rule.id] = rule

        val server = ForwardServer(rule, connectionProvider) { updatedRule ->
            rulesMap[updatedRule.id] = updatedRule
            notifyRules()
        }
        servers[rule.id] = server

        val res = server.start()
        notifyRules()
        return if (res.isSuccess) {
            Result.success(rule)
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("启动失败"))
        }
    }

    fun stopForward(ruleId: String) {
        val server = servers.remove(ruleId)
        server?.stop()
        rulesMap.remove(ruleId)
        notifyRules()
    }

    fun stopAllForwards() {
        servers.values.forEach { it.stop() }
        servers.clear()
        rulesMap.clear()
        notifyRules()
    }

    fun getRules(): List<AdbForwardRule> {
        return rulesMap.values.toList().sortedByDescending { it.createTime }
    }

    private fun notifyRules() {
        onRulesChanged(getRules())
    }
}
