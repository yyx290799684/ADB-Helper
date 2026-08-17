package com.yangyx.adbhelper.scrcpy

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class ProxyOption(
    val name: String,
    val prefix: String,
    val description: String = ""
)

data class ScrcpyServerState(
    val isReady: Boolean = false,
    val localVersion: String? = null,
    val localFileSize: Long = 0L,
    val latestVersion: String? = null,
    val latestDownloadUrl: String? = null,
    val isCheckingUpdate: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadSpeedText: String = "",
    val selectedProxyName: String = "ghproxy.net",
    val statusMessage: String? = null,
    val isError: Boolean = false
)

object ScrcpyServerManager {

    private const val PREFS_NAME = "scrcpy_server_prefs"
    private const val KEY_LOCAL_VERSION = "local_scrcpy_version"
    private const val KEY_SELECTED_PROXY = "selected_proxy_name"

    val PROXY_OPTIONS = listOf(
        ProxyOption("ghproxy.net", "https://ghproxy.net/", "国内高速镜像加速"),
        ProxyOption("gh-proxy.com", "https://gh-proxy.com/", "常用 GitHub Proxy 加速"),
        ProxyOption("gh.dpik.top", "https://gh.dpik.top/", "DPIK 代理加速节点"),
        ProxyOption("github.tbap.top", "https://github.tbap.top/", "TBAP 代理加速节点"),
        ProxyOption("github.dpik.top", "https://github.dpik.top/", "DPIK GitHub 镜像"),
        ProxyOption("ghfile.geekertao.top", "https://ghfile.geekertao.top/", "GeekerTao 文件加速"),
        ProxyOption("直连 (GitHub 官方)", "", "直接连接 GitHub 官方节点")
    )

    private val _state = MutableStateFlow(ScrcpyServerState())
    val state: StateFlow<ScrcpyServerState> = _state.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getServerFile(context: Context): File {
        val dir = File(context.filesDir, "scrcpy")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "scrcpy-server.jar")
    }

    fun isServerReady(context: Context): Boolean {
        val file = getServerFile(context)
        return file.exists() && file.length() > 0
    }

    fun init(context: Context) {
        val prefs = getPrefs(context)
        val savedProxy = prefs.getString(KEY_SELECTED_PROXY, "ghproxy.net") ?: "ghproxy.net"
        val savedVersion = prefs.getString(KEY_LOCAL_VERSION, null)
        val file = getServerFile(context)
        val exists = file.exists() && file.length() > 0

        _state.value = _state.value.copy(
            isReady = exists,
            localVersion = if (exists) (savedVersion ?: "已就绪") else null,
            localFileSize = if (exists) file.length() else 0L,
            selectedProxyName = savedProxy
        )
    }

    fun setProxy(context: Context, proxyName: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_PROXY, proxyName).apply()
        _state.value = _state.value.copy(selectedProxyName = proxyName)
    }

    fun getSelectedProxyPrefix(): String {
        val currentName = _state.value.selectedProxyName
        return PROXY_OPTIONS.find { it.name == currentName }?.prefix ?: "https://ghproxy.net/"
    }

    /**
     * 动态从 GitHub 获取 scrcpy 的最新 Release 版本号及下载链接
     * 包含策略：
     * 1. GitHub REST API (/releases/latest)
     * 2. GitHub Web 重定向解析 (/releases/latest -> 302 Location tag)
     * 3. GitHub Releases 页面 HTML 解析
     */
    suspend fun checkUpdate(context: Context): Pair<String, String>? = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(
            isCheckingUpdate = true,
            statusMessage = "正在动态获取 GitHub 最新 Release 版本...",
            isError = false
        )

        var releaseVersion: String? = null
        var downloadUrl: String? = null

        val proxyPrefix = getSelectedProxyPrefix()

        // 尝试策略 1: GitHub API (通过代理或直连)
        val apiUrls = listOfNotNull(
            if (proxyPrefix.isNotEmpty()) "${proxyPrefix}https://api.github.com/repos/Genymobile/scrcpy/releases/latest" else null,
            "https://api.github.com/repos/Genymobile/scrcpy/releases/latest"
        ).distinct()

        for (apiUrl in apiUrls) {
            try {
                val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; ADBHelper)")
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    instanceFollowRedirects = true
                }
                if (conn.responseCode in 200..299) {
                    val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonStr)
                    val tagName = json.optString("tag_name", "").trim()
                    if (tagName.isNotEmpty()) {
                        releaseVersion = tagName
                        val assets = json.optJSONArray("assets")
                        if (assets != null) {
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                val name = asset.optString("name", "")
                                val url = asset.optString("browser_download_url", "")
                                if (name.startsWith("scrcpy-server") && !name.endsWith(".tar.gz") && !name.endsWith(".zip")) {
                                    downloadUrl = url
                                    break
                                }
                            }
                        }
                    }
                }
                conn.disconnect()
                if (!releaseVersion.isNullOrEmpty()) break
            } catch (_: Exception) {}
        }

        // 尝试策略 2: 请求 releases/latest 检测重定向 Location
        if (releaseVersion == null) {
            val redirectUrls = listOfNotNull(
                if (proxyPrefix.isNotEmpty()) "${proxyPrefix}https://github.com/Genymobile/scrcpy/releases/latest" else null,
                "https://github.com/Genymobile/scrcpy/releases/latest"
            ).distinct()

            for (rUrl in redirectUrls) {
                try {
                    val conn = (URL(rUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Android; ADBHelper)")
                        instanceFollowRedirects = false
                    }
                    val loc = conn.getHeaderField("Location") ?: ""
                    conn.disconnect()
                    if (loc.isNotEmpty()) {
                        val tagMatch = Regex("""releases/tag/([^/?#]+)""").find(loc)
                        if (tagMatch != null) {
                            releaseVersion = tagMatch.groupValues[1]
                            break
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // 尝试策略 3: 解析 releases 页面 HTML
        if (releaseVersion == null) {
            val htmlUrls = listOfNotNull(
                if (proxyPrefix.isNotEmpty()) "${proxyPrefix}https://github.com/Genymobile/scrcpy/releases" else null,
                "https://github.com/Genymobile/scrcpy/releases"
            ).distinct()

            for (hUrl in htmlUrls) {
                try {
                    val conn = (URL(hUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 10000
                        readTimeout = 10000
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Android; ADBHelper)")
                        instanceFollowRedirects = true
                    }
                    if (conn.responseCode in 200..299) {
                        val html = conn.inputStream.bufferedReader().use { it.readText() }
                        val tagMatch = Regex("""releases/tag/([vV]?[0-9]+(?:\.[0-9]+)+)""").find(html)
                        if (tagMatch != null) {
                            releaseVersion = tagMatch.groupValues[1]
                        }
                    }
                    conn.disconnect()
                    if (!releaseVersion.isNullOrEmpty()) break
                } catch (_: Exception) {}
            }
        }

        if (releaseVersion != null) {
            if (downloadUrl == null) {
                downloadUrl = "https://github.com/Genymobile/scrcpy/releases/download/$releaseVersion/scrcpy-server-$releaseVersion"
            }

            _state.value = _state.value.copy(
                isCheckingUpdate = false,
                latestVersion = releaseVersion,
                latestDownloadUrl = downloadUrl,
                statusMessage = "成功获取 GitHub 最新版本: $releaseVersion",
                isError = false
            )
            Pair(releaseVersion, downloadUrl)
        } else {
            _state.value = _state.value.copy(
                isCheckingUpdate = false,
                statusMessage = "获取最新版本失败，请检查网络或切换代理源",
                isError = true
            )
            null
        }
    }

    suspend fun downloadServer(
        context: Context,
        version: String? = null,
        targetUrl: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        var ver = version ?: _state.value.latestVersion
        var rawUrl = targetUrl ?: _state.value.latestDownloadUrl

        // 如果尚未获取最新版本，先动态拉取一次
        if (ver == null || rawUrl == null) {
            val result = checkUpdate(context)
            if (result != null) {
                ver = result.first
                rawUrl = result.second
            } else {
                _state.value = _state.value.copy(
                    isDownloading = false,
                    statusMessage = "无法获取 GitHub 最新版本号，请更换代理后重试",
                    isError = true
                )
                return@withContext false
            }
        }

        val proxyPrefix = getSelectedProxyPrefix()
        val finalDownloadUrl = if (proxyPrefix.isNotEmpty() && !rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            "$proxyPrefix$rawUrl"
        } else if (proxyPrefix.isNotEmpty() && !rawUrl.startsWith(proxyPrefix)) {
            "$proxyPrefix$rawUrl"
        } else {
            rawUrl
        }

        _state.value = _state.value.copy(
            isDownloading = true,
            downloadProgress = 0f,
            downloadedBytes = 0L,
            totalBytes = 0L,
            downloadSpeedText = "准备下载...",
            statusMessage = "正在连接 GitHub Releases 节点 ($ver)...",
            isError = false
        )

        val targetFile = getServerFile(context)
        val tempFile = File(targetFile.parentFile, "scrcpy-server.tmp")
        if (tempFile.exists()) tempFile.delete()

        try {
            var currentUrl = finalDownloadUrl
            var connection: HttpURLConnection
            var redirectCount = 0
            val maxRedirects = 6

            while (true) {
                val urlObj = URL(currentUrl)
                connection = (urlObj.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; ADBHelper)")
                    instanceFollowRedirects = true
                }

                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!location.isNullOrEmpty() && redirectCount < maxRedirects) {
                        redirectCount++
                        currentUrl = if (location.startsWith("http")) {
                            if (proxyPrefix.isNotEmpty() && !location.startsWith(proxyPrefix)) {
                                "$proxyPrefix$location"
                            } else {
                                location
                            }
                        } else {
                            URL(urlObj, location).toString()
                        }
                        continue
                    }
                }
                break
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw Exception("HTTP 请求失败: 状态码 $responseCode")
            }

            val totalLength = connection.contentLength.toLong().let { if (it > 0) it else 95000L }
            var downloaded = 0L
            var lastTime = System.currentTimeMillis()
            var bytesSinceLastTime = 0L

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        bytesSinceLastTime += read

                        val now = System.currentTimeMillis()
                        if (now - lastTime >= 300) {
                            val timeDiffSec = (now - lastTime) / 1000f
                            val speedBytesPerSec = if (timeDiffSec > 0) (bytesSinceLastTime / timeDiffSec).toLong() else 0L
                            val speedStr = formatSpeed(speedBytesPerSec)
                            val prog = (downloaded.toFloat() / totalLength).coerceIn(0f, 1f)

                            _state.value = _state.value.copy(
                                downloadProgress = prog,
                                downloadedBytes = downloaded,
                                totalBytes = totalLength,
                                downloadSpeedText = speedStr,
                                statusMessage = "正在下载: ${(prog * 100).toInt()}% ($speedStr)"
                            )

                            lastTime = now
                            bytesSinceLastTime = 0
                        }
                    }
                    output.flush()
                }
            }
            connection.disconnect()

            if (!tempFile.exists() || tempFile.length() < 10000) {
                throw Exception("下载的文件大小异常 (${tempFile.length()} 字节)，可能下载失败")
            }

            // Move temp to target
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            getPrefs(context).edit()
                .putString(KEY_LOCAL_VERSION, ver)
                .apply()

            _state.value = _state.value.copy(
                isReady = true,
                localVersion = ver,
                localFileSize = targetFile.length(),
                isDownloading = false,
                downloadProgress = 1f,
                downloadSpeedText = "",
                statusMessage = "下载完成！已就绪 ($ver - ${formatSize(targetFile.length())})",
                isError = false
            )
            true
        } catch (e: Exception) {
            tempFile.delete()
            _state.value = _state.value.copy(
                isDownloading = false,
                statusMessage = "下载失败: ${e.message}",
                isError = true
            )
            false
        }
    }

    fun deleteServer(context: Context) {
        val file = getServerFile(context)
        if (file.exists()) {
            file.delete()
        }
        getPrefs(context).edit().remove(KEY_LOCAL_VERSION).apply()
        _state.value = _state.value.copy(
            isReady = false,
            localVersion = null,
            localFileSize = 0L,
            statusMessage = "已删除本地组件"
        )
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB/s", bytesPerSec / (1024f * 1024f))
            bytesPerSec >= 1024 -> String.format(Locale.getDefault(), "%d KB/s", bytesPerSec / 1024)
            else -> "$bytesPerSec B/s"
        }
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f MB", bytes / (1024f * 1024f))
            bytes >= 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024f)
            else -> "$bytes B"
        }
    }
}
