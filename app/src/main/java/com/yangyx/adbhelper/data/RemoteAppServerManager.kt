package com.yangyx.adbhelper.data

import android.content.Context
import com.yangyx.adbhelper.adb.AdbConnection
import com.yangyx.adbhelper.adb.AdbSyncClient
import com.yangyx.adbhelper.ui.models.RemoteAppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayInputStream

/**
 * 负责将内置的 adbhelper_app_server.jar 推送到对端 /data/local/tmp/
 * 并通过 app_process 免安装运行，一次性采集全部应用元数据与 PNG 图标。
 */
object RemoteAppServerManager {

    const val ASSET_SERVER_JAR = "adbhelper_app_server.jar"
    const val REMOTE_SERVER_JAR_PATH = "/data/local/tmp/adbhelper_app_server.jar"
    const val REMOTE_ICONS_DIR = "/data/local/tmp/adbhelper_icons"
    private const val SERVER_MAIN_CLASS = "com.yangyx.adbhelper.server.AppInfoServer"

    suspend fun ensureServerDeployed(
        context: Context,
        conn: AdbConnection,
        log: (String) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val jarBytes = context.assets.open(ASSET_SERVER_JAR).use { it.readBytes() }
            if (jarBytes.isEmpty()) {
                log("✗ 内置 $ASSET_SERVER_JAR 为空，无法部署")
                return@withContext false
            }

            val expectedSize = jarBytes.size.toString()
            val lsOut = conn.executeShell("ls -l $REMOTE_SERVER_JAR_PATH 2>/dev/null", 5000).trim()
            val sizeMatches = lsOut.contains("adbhelper_app_server.jar") &&
                Regex("""\b${Regex.escape(expectedSize)}\b""").containsMatchIn(lsOut)

            if (sizeMatches) {
                log("✓ 远端 $REMOTE_SERVER_JAR_PATH 已就绪 ($expectedSize 字节)")
                return@withContext true
            }

            log("正在向对端推送 $ASSET_SERVER_JAR ($expectedSize 字节) -> $REMOTE_SERVER_JAR_PATH...")
            val syncClient = AdbSyncClient(conn)
            ByteArrayInputStream(jarBytes).use { input ->
                syncClient.pushFile(input, REMOTE_SERVER_JAR_PATH)
            }
            conn.executeShell("chmod 644 $REMOTE_SERVER_JAR_PATH 2>/dev/null || chmod 755 $REMOTE_SERVER_JAR_PATH 2>/dev/null", 5000)

            val verifyOut = conn.executeShell("ls -l $REMOTE_SERVER_JAR_PATH 2>/dev/null", 5000).trim()
            val verified = verifyOut.contains("adbhelper_app_server.jar") &&
                Regex("""\b${Regex.escape(expectedSize)}\b""").containsMatchIn(verifyOut)

            if (verified) {
                log("✓ $ASSET_SERVER_JAR 推送并校验成功")
                true
            } else {
                log("✗ $ASSET_SERVER_JAR 推送后校验失败: $verifyOut")
                false
            }
        } catch (e: Exception) {
            log("✗ 部署 $ASSET_SERVER_JAR 异常: ${e.message}")
            false
        }
    }

    /**
     * 通过 app_process 运行远端 AppInfoServer，返回全量应用列表（并在对端导出 PNG 图标）；
     * 若远端运行失败则返回 null，交由上层回退到 pm + aapt2 兼容流程。
     */
    suspend fun collectRemoteApps(
        context: Context,
        conn: AdbConnection,
        forceRefresh: Boolean,
        parseFallbackName: (String) -> String,
        onProgressCount: (Int) -> Unit = {},
        log: (String) -> Unit = {}
    ): List<RemoteAppItem>? = withContext(Dispatchers.IO) {
        if (!ensureServerDeployed(context, conn, log)) {
            return@withContext null
        }

        val refreshFlag = if (forceRefresh) " --refresh" else ""
        val cmd = "CLASSPATH=$REMOTE_SERVER_JAR_PATH app_process / $SERVER_MAIN_CLASS $REMOTE_ICONS_DIR$refreshFlag 2>&1"
        log("启动远端 AppInfoServer: $cmd")

        val collected = mutableListOf<RemoteAppItem>()
        val errorLines = mutableListOf<String>()
        var doneCount = -1

        try {
            conn.executeShellStream(cmd) { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty()) return@executeShellStream

                when {
                    line.startsWith("APP_JSON:") -> {
                        val jsonStr = line.removePrefix("APP_JSON:")
                        try {
                            val obj = JSONObject(jsonStr)
                            val pkg = obj.optString("pkg", "").trim()
                            if (pkg.isNotEmpty()) {
                                val rawLabel = obj.optString("label", "").trim()
                                val displayName = if (rawLabel.isNotEmpty() && rawLabel != pkg) {
                                    rawLabel
                                } else {
                                    parseFallbackName(pkg)
                                }
                                val verName = obj.optString("verName", "")
                                val verCode = obj.optLong("verCode", 0L)
                                val isSystem = obj.optBoolean("sys", false)
                                val isDisabled = obj.optBoolean("disabled", false)
                                val apkPath = obj.optString("apk", "")
                                val hasIcon = obj.optBoolean("icon", false)
                                val iconPath = obj.optString("iconPath", "")

                                collected.add(
                                    RemoteAppItem(
                                        packageName = pkg,
                                        appName = displayName,
                                        isSystemApp = isSystem,
                                        isDisabled = isDisabled,
                                        apkPath = apkPath,
                                        versionName = verName,
                                        versionCode = verCode,
                                        hasIcon = hasIcon,
                                        iconRemotePath = iconPath
                                    )
                                )
                                if (collected.size % 10 == 0) {
                                    onProgressCount(collected.size)
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                    line.startsWith("APP_DONE:") -> {
                        doneCount = line.removePrefix("APP_DONE:").trim().toIntOrNull() ?: collected.size
                    }
                    else -> {
                        if (errorLines.size < 15) {
                            errorLines.add(line)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log("✗ 执行 AppInfoServer 异常: ${e.message}")
            return@withContext null
        }

        if (collected.isEmpty() || (doneCount < 0 && errorLines.any { it.contains("APP_ERROR:") })) {
            log("⚠️ AppInfoServer 未返回有效应用列表 (done=$doneCount, 错误摘要: ${errorLines.joinToString(" | ")})，将回退至 pm + aapt2 兼容流程")
            return@withContext null
        }

        onProgressCount(collected.size)
        val iconCount = collected.count { it.hasIcon }
        log("✓ AppInfoServer 采集完成: 共 ${collected.size} 个应用，已准备 $iconCount 个远端图标 PNG")
        collected
    }
}
