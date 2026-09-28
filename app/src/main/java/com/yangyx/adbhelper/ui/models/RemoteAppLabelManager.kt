package com.yangyx.adbhelper.ui.models

import android.content.Context
import com.yangyx.adbhelper.adb.AdbConnection
import com.yangyx.adbhelper.adb.AdbSyncClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages remote application label resolution using the pushed aapt2 binary
 * and maintains an in-memory persistent cache across screens.
 */
object RemoteAppLabelManager {

    // Persistent in-memory cache: packageName -> resolved application display name
    val labelCache = ConcurrentHashMap<String, String>()

    fun cleanLabel(raw: String): String? {
        val t = raw.trim()
        if (t.isEmpty()) return null
        if (t.startsWith("0x") || t.startsWith("@0x") || t.startsWith("@string/") || t.startsWith("@17") || t.startsWith("@android:")) return null
        if (t.matches(Regex("^@?[0-9]+$"))) return null
        return t
    }

    /**
     * Parses the output lines of `aapt2 dump badging` to extract the most accurate
     * human-readable application label (prioritizing Simplified Chinese / Chinese).
     */
    fun parseAaptDumpBadgingOutput(rawLines: List<String>): Pair<String?, Pair<String, Long>?> {
        var foundZhCnLabel: String? = null
        var foundZhLabel: String? = null
        var foundDefLabel: String? = null
        var foundAppLabel: String? = null
        var foundVerName = ""
        var foundVerCode = 0L

        for (rawLine in rawLines) {
            val trimmed = rawLine.trim()
            if (trimmed.startsWith("package:") && (trimmed.contains("name='") || trimmed.contains("versionName='") || trimmed.contains("versionCode='"))) {
                if (trimmed.contains("versionName='")) {
                    foundVerName = trimmed.substringAfter("versionName='").substringBefore("'")
                }
                if (trimmed.contains("versionCode='")) {
                    foundVerCode = trimmed.substringAfter("versionCode='").substringBefore("'").toLongOrNull() ?: 0L
                }
            }
            if (trimmed.contains("application-label")) {
                val rawKey = trimmed.substringBefore(":").trim()
                val rawVal = trimmed.substringAfter("'").substringBefore("'")
                val label = cleanLabel(rawVal)
                if (label != null) {
                    val keyLower = rawKey.lowercase()
                    if (keyLower == "application-label-zh-cn" ||
                        keyLower == "application-label-zh-hans" ||
                        keyLower == "application-label-zh-sg" ||
                        keyLower == "application-label-zh"
                    ) {
                        foundZhCnLabel = label
                    } else if (keyLower.startsWith("application-label-zh")) {
                        if (foundZhLabel == null) foundZhLabel = label
                    } else if (keyLower == "application-label" || keyLower.startsWith("application-label-en")) {
                        if (foundDefLabel == null) foundDefLabel = label
                    }
                }
            } else if (trimmed.contains("application:") && trimmed.contains("label='")) {
                val label = cleanLabel(trimmed.substringAfter("label='").substringBefore("'"))
                if (label != null && foundAppLabel == null) foundAppLabel = label
            }
        }

        val bestLabel = foundZhCnLabel ?: foundZhLabel ?: foundDefLabel ?: foundAppLabel
        val verInfo = if (foundVerName.isNotEmpty() || foundVerCode > 0L) Pair(foundVerName, foundVerCode) else null
        return Pair(bestLabel, verInfo)
    }

    /**
     * Checks if /data/local/tmp/aapt2 exists and is executable on the remote device.
     * If not, pushes the matching binary (arm64-v8a or armeabi-v7a) from assets and grants executable permission.
     */
    suspend fun ensureRemoteAapt2(
        conn: AdbConnection,
        context: Context? = null,
        log: (String) -> Unit = {}
    ): String? = withContext(Dispatchers.IO) {
        val aaptRemotePath = "/data/local/tmp/aapt2"
        val verCmd = "$aaptRemotePath version"
        val verTestRaw = conn.executeShell("$verCmd 2>&1; echo \"___EC:$?\"").trim()
        val verOutput = verTestRaw.substringBefore("___EC:").trim()
        val verEc = verTestRaw.substringAfter("___EC:", "").trim()

        if (verEc == "0" && (verOutput.contains("Android Asset Packaging Tool") || verOutput.contains("aapt2"))) {
            return@withContext aaptRemotePath
        }

        if (context == null) {
            log("aapt2 未在远端部署且无 Context 可供推送")
            return@withContext null
        }

        try {
            val abiProp = conn.executeShell("getprop ro.product.cpu.abi").trim()
            val abiList = conn.executeShell("getprop ro.product.cpu.abilist").trim()
            val fullAbi = "$abiProp $abiList".lowercase()
            val aapt2Asset = if (fullAbi.contains("64") || fullAbi.contains("aarch64")) {
                "aapt2-arm64-v8a"
            } else {
                "aapt2-armeabi-v7a"
            }
            log("正在向远端推送 $aapt2Asset -> $aaptRemotePath...")
            val syncClient = AdbSyncClient(conn)
            context.assets.open(aapt2Asset).use { inputStream ->
                syncClient.pushFile(inputStream, aaptRemotePath)
            }
            conn.executeShell("chmod 755 $aaptRemotePath 2>&1")

            val retestRaw = conn.executeShell("$verCmd 2>&1; echo \"___EC:$?\"").trim()
            val retestOutput = retestRaw.substringBefore("___EC:").trim()
            val retestEc = retestRaw.substringAfter("___EC:", "").trim()
            if (retestEc == "0" && (retestOutput.contains("Android Asset Packaging Tool") || retestOutput.contains("aapt2"))) {
                log("✓ aapt2 部署并验证成功")
                return@withContext aaptRemotePath
            } else {
                log("✗ aapt2 推送后测试失败: $retestOutput (EC: $retestEc)")
            }
        } catch (e: Exception) {
            log("部署 aapt2 异常: ${e.message}")
        }
        null
    }

    /**
     * Resolves labels using aapt2 dump badging for apps that are missing from labelCache.
     */
    suspend fun resolveMissingLabelsWithAapt2(
        conn: AdbConnection,
        appsToResolve: List<Pair<String, String>>, // (pkg, apkPath)
        aaptRemotePath: String,
        numThreads: Int = 4,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Map<String, String> = coroutineScope {
        val resultMap = ConcurrentHashMap<String, String>()
        val total = appsToResolve.size
        if (total == 0) return@coroutineScope resultMap

        val completed = AtomicInteger(0)
        val workerCount = numThreads.coerceIn(1, 8)
        val partitions = (0 until workerCount).map { threadIdx ->
            appsToResolve.filterIndexed { index, _ -> index % workerCount == threadIdx }
        }.filter { it.isNotEmpty() }

        partitions.map { workerApps ->
            async(Dispatchers.IO) {
                for (batch in workerApps.chunked(25)) {
                    val scriptBuilder = StringBuilder()
                    for ((pkg, apk) in batch) {
                        val escapedApk = apk.replace("'", "'\\''")
                        val escapedPkg = pkg.replace("'", "'\\''")
                        scriptBuilder.append("echo '===PKG:$escapedPkg'; ")
                        scriptBuilder.append("echo '===APK:$escapedApk'; ")
                        scriptBuilder.append("out=$($aaptRemotePath dump badging '$escapedApk' 2>&1); ")
                        scriptBuilder.append("ec=$?; ")
                        scriptBuilder.append("echo '===EC:'\"\$ec\"; ")
                        scriptBuilder.append("echo '===RAW_START'; ")
                        scriptBuilder.append("echo \"\$out\"; ")
                        scriptBuilder.append("echo '===RAW_END'; ")
                    }

                    var currentPkg: String? = null
                    val currentRawLines = mutableListOf<String>()
                    var isInRawBlock = false

                    fun commit() {
                        val pkg = currentPkg ?: return
                        val (label, _) = parseAaptDumpBadgingOutput(currentRawLines)
                        if (!label.isNullOrBlank()) {
                            resultMap[pkg] = label
                            labelCache[pkg] = label
                        }
                        val done = completed.incrementAndGet()
                        onProgress(done, total)
                        currentPkg = null
                        currentRawLines.clear()
                        isInRawBlock = false
                    }

                    conn.executeShellStream(scriptBuilder.toString()) { line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("===PKG:")) {
                            commit()
                            currentPkg = trimmed.substring("===PKG:".length).trim()
                        } else if (trimmed == "===RAW_START") {
                            isInRawBlock = true
                            currentRawLines.clear()
                        } else if (trimmed == "===RAW_END") {
                            isInRawBlock = false
                        } else if (isInRawBlock) {
                            currentRawLines.add(line)
                        }
                    }
                    commit()
                }
            }
        }.awaitAll()

        resultMap
    }
}
