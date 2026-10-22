package com.yangyx.adbhelper.scrcpy

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
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

data class ScrcpyVersionInfo(
    val version: String,               // e.g. "v3.1"
    val downloadUrl: String,           // e.g. "https://github.com/Genymobile/scrcpy/releases/download/v3.1/scrcpy-server-v3.1"
    val releaseName: String = "",       // e.g. "scrcpy 3.1"
    val publishedAt: String = "",       // e.g. "2024-11-20"
    val sizeFormatted: String = "",     // e.g. "95 KB"
    val isLatest: Boolean = false,
    val note: String = ""               // e.g. "稳定推荐版"
)

data class ScrcpyServerState(
    val isReady: Boolean = false,
    val localVersion: String? = null,
    val localFileSize: Long = 0L,
    val latestVersion: String? = null,
    val latestDownloadUrl: String? = null,
    val selectedTargetVersion: String? = null,
    val availableVersions: List<ScrcpyVersionInfo> = emptyList(),
    val isCheckingUpdate: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadingVersion: String? = null,
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
    private const val KEY_SELECTED_TARGET_VERSION = "selected_target_version"

    val PROXY_OPTIONS = listOf(
        ProxyOption("ghproxy.net", "https://ghproxy.net/", "国内高速镜像加速"),
        ProxyOption("gh-proxy.com", "https://gh-proxy.com/", "常用 GitHub Proxy 加速"),
        ProxyOption("gh.dpik.top", "https://gh.dpik.top/", "DPIK 代理加速节点"),
        ProxyOption("github.tbap.top", "https://github.tbap.top/", "TBAP 代理加速节点"),
        ProxyOption("github.dpik.top", "https://github.dpik.top/", "DPIK GitHub 镜像"),
        ProxyOption("ghfile.geekertao.top", "https://ghfile.geekertao.top/", "GeekerTao 文件加速"),
        ProxyOption("直连 (GitHub 官方)", "", "直接连接 GitHub 官方节点")
    )

    private val _state = MutableStateFlow(ScrcpyServerState(availableVersions = emptyList()))
    val state: StateFlow<ScrcpyServerState> = _state.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 判断版本字符串是否大于等于 4.1 (例如 "v4.1", "4.1", "v4.2", "v5.0")
     */
    fun isVersionAtLeast4_1(ver: String): Boolean {
        val clean = ver.trim().removePrefix("v").removePrefix("V")
        val parts = clean.split(".")
        if (parts.isEmpty()) return false
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return false
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return when {
            major > 4 -> true
            major == 4 -> minor >= 1
            else -> false
        }
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
        val savedTargetVer = prefs.getString(KEY_SELECTED_TARGET_VERSION, null)
        val file = getServerFile(context)
        val exists = file.exists() && file.length() > 0

        val initialTargetVer = savedTargetVer ?: savedVersion ?: _state.value.availableVersions.firstOrNull()?.version

        _state.value = _state.value.copy(
            isReady = exists,
            localVersion = if (exists) (savedVersion ?: "已就绪") else null,
            localFileSize = if (exists) file.length() else 0L,
            selectedProxyName = savedProxy,
            selectedTargetVersion = initialTargetVer
        )
    }

    fun setProxy(context: Context, proxyName: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_PROXY, proxyName).apply()
        _state.value = _state.value.copy(selectedProxyName = proxyName)
    }

    fun selectTargetVersion(context: Context, version: String) {
        val cleanVer = version.trim()
        if (cleanVer.isEmpty()) return
        getPrefs(context).edit().putString(KEY_SELECTED_TARGET_VERSION, cleanVer).apply()
        val matched = _state.value.availableVersions.find { 
            it.version.equals(cleanVer, ignoreCase = true) || it.version.removePrefix("v").equals(cleanVer.removePrefix("v"), ignoreCase = true)
        }
        _state.value = _state.value.copy(
            selectedTargetVersion = cleanVer,
            statusMessage = "已选择目标下载版本: $cleanVer${if (matched?.note?.isNotEmpty() == true) " (${matched.note})" else ""}"
        )
    }

    fun getSelectedProxyPrefix(): String {
        val currentName = _state.value.selectedProxyName
        return PROXY_OPTIONS.find { it.name == currentName }?.prefix ?: "https://ghproxy.net/"
    }

    /**
     * 动态从 GitHub 获取 scrcpy 的可用 Releases 列表及最新版本号
     */
    suspend fun checkUpdate(context: Context): Pair<String, String>? = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(
            isCheckingUpdate = true,
            statusMessage = "正在连接 GitHub 获取可用 scrcpy-server 版本列表...",
            isError = false
        )

        val proxyPrefix = getSelectedProxyPrefix()
        val fetchedVersions = mutableListOf<ScrcpyVersionInfo>()

        // 尝试策略 1: 请求 GitHub API releases 列表 (/releases?per_page=30)
        val listApiUrls = listOfNotNull(
            if (proxyPrefix.isNotEmpty()) "${proxyPrefix}https://api.github.com/repos/Genymobile/scrcpy/releases?per_page=30" else null,
            "https://api.github.com/repos/Genymobile/scrcpy/releases?per_page=30"
        ).distinct()

        for (apiUrl in listApiUrls) {
            try {
                val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 10000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; ADBHelper)")
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    instanceFollowRedirects = true
                }
                if (conn.responseCode in 200..299) {
                    val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(jsonStr)
                    for (i in 0 until jsonArray.length()) {
                        val releaseObj = jsonArray.getJSONObject(i)
                        val tagName = releaseObj.optString("tag_name", "").trim()
                        if (tagName.isEmpty()) continue
                        val releaseName = releaseObj.optString("name", tagName)
                        val publishedAt = releaseObj.optString("published_at", "").take(10)
                        val isPrerelease = releaseObj.optBoolean("prerelease", false)

                        var assetUrl: String? = null
                        var assetSize = 0L
                        val assets = releaseObj.optJSONArray("assets")
                        if (assets != null) {
                            for (j in 0 until assets.length()) {
                                val asset = assets.getJSONObject(j)
                                val aName = asset.optString("name", "")
                                if (aName.startsWith("scrcpy-server") && !aName.endsWith(".tar.gz") && !aName.endsWith(".zip") && !aName.endsWith(".asc") && !aName.endsWith(".txt")) {
                                    assetUrl = asset.optString("browser_download_url", "")
                                    assetSize = asset.optLong("size", 0L)
                                    break
                                }
                            }
                        }

                        val downloadUrl = assetUrl ?: "https://github.com/Genymobile/scrcpy/releases/download/$tagName/scrcpy-server-$tagName"
                        val note = when {
                            i == 0 && !isPrerelease -> "最新正式版"
                            tagName.startsWith("v3.1") -> "广泛推荐稳定版"
                            tagName.startsWith("v3.0") -> "v3.0 初始版"
                            tagName.startsWith("v2.7") -> "v2 系列最终版"
                            tagName.startsWith("v2.0") -> "首个音频转发版"
                            isPrerelease -> "预发布版"
                            else -> ""
                        }

                        fetchedVersions.add(
                            ScrcpyVersionInfo(
                                version = tagName,
                                downloadUrl = downloadUrl,
                                releaseName = if (releaseName.isNotBlank()) releaseName else "scrcpy $tagName",
                                publishedAt = publishedAt,
                                sizeFormatted = if (assetSize > 0) formatSize(assetSize) else "",
                                isLatest = (i == 0),
                                note = note
                            )
                        )
                    }
                }
                conn.disconnect()
                if (fetchedVersions.isNotEmpty()) break
            } catch (_: Exception) {}
        }

        // 尝试策略 2: 单个 releases/latest API
        var singleLatestVersion: String? = null
        var singleDownloadUrl: String? = null
        if (fetchedVersions.isEmpty()) {
            val latestApiUrls = listOfNotNull(
                if (proxyPrefix.isNotEmpty()) "${proxyPrefix}https://api.github.com/repos/Genymobile/scrcpy/releases/latest" else null,
                "https://api.github.com/repos/Genymobile/scrcpy/releases/latest"
            ).distinct()

            for (apiUrl in latestApiUrls) {
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
                            singleLatestVersion = tagName
                            val assets = json.optJSONArray("assets")
                            if (assets != null) {
                                for (i in 0 until assets.length()) {
                                    val asset = assets.getJSONObject(i)
                                    val name = asset.optString("name", "")
                                    val url = asset.optString("browser_download_url", "")
                                    if (name.startsWith("scrcpy-server") && !name.endsWith(".tar.gz") && !name.endsWith(".zip")) {
                                        singleDownloadUrl = url
                                        break
                                    }
                                }
                            }
                        }
                    }
                    conn.disconnect()
                    if (!singleLatestVersion.isNullOrEmpty()) break
                } catch (_: Exception) {}
            }
        }

        // 尝试策略 3: 请求 releases/latest 检测重定向 Location
        if (fetchedVersions.isEmpty() && singleLatestVersion == null) {
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
                            singleLatestVersion = tagMatch.groupValues[1]
                            break
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // 尝试策略 4: 解析 releases 页面 HTML 标签
        if (fetchedVersions.isEmpty()) {
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
                        val matches = Regex("""releases/tag/([vV]?[0-9]+(?:\.[0-9]+)+)""").findAll(html)
                        val tagSet = mutableSetOf<String>()
                        for (match in matches) {
                            val tag = match.groupValues[1]
                            if (tagSet.add(tag)) {
                                fetchedVersions.add(
                                    ScrcpyVersionInfo(
                                        version = tag,
                                        downloadUrl = "https://github.com/Genymobile/scrcpy/releases/download/$tag/scrcpy-server-$tag",
                                        releaseName = "scrcpy $tag",
                                        isLatest = (tagSet.size == 1)
                                    )
                                )
                            }
                        }
                    }
                    conn.disconnect()
                    if (fetchedVersions.isNotEmpty()) break
                } catch (_: Exception) {}
            }
        }

        if (singleLatestVersion != null && fetchedVersions.isEmpty()) {
            val dl = singleDownloadUrl ?: "https://github.com/Genymobile/scrcpy/releases/download/$singleLatestVersion/scrcpy-server-$singleLatestVersion"
            fetchedVersions.add(
                ScrcpyVersionInfo(
                    version = singleLatestVersion,
                    downloadUrl = dl,
                    releaseName = "scrcpy $singleLatestVersion",
                    isLatest = true,
                    note = "最新版本"
                )
            )
        }

        // 过滤仅保留 >= 4.1 的版本，且完全从网络拉取（不使用任何内置列表）
        val finalVersions = mutableListOf<ScrcpyVersionInfo>()
        val seenVersions = mutableSetOf<String>()

        for (v in fetchedVersions) {
            val norm = v.version.lowercase().removePrefix("v")
            if (isVersionAtLeast4_1(v.version) && seenVersions.add(norm)) {
                finalVersions.add(v)
            }
        }

        val effectiveList = finalVersions
        val releaseVersion = effectiveList.firstOrNull { it.isLatest }?.version ?: effectiveList.firstOrNull()?.version
        val downloadUrl = effectiveList.firstOrNull { it.isLatest }?.downloadUrl ?: effectiveList.firstOrNull()?.downloadUrl

        val targetVer = _state.value.selectedTargetVersion?.takeIf { ver ->
            effectiveList.any { it.version.equals(ver, ignoreCase = true) || it.version.removePrefix("v").equals(ver.removePrefix("v"), ignoreCase = true) }
        } ?: releaseVersion

        if (releaseVersion != null && downloadUrl != null) {
            _state.value = _state.value.copy(
                isCheckingUpdate = false,
                latestVersion = releaseVersion,
                latestDownloadUrl = downloadUrl,
                selectedTargetVersion = targetVer,
                availableVersions = effectiveList,
                statusMessage = "检查完成: 获取到 ${effectiveList.size} 个 >=4.1 版本 (最新: $releaseVersion)",
                isError = false
            )
            Pair(releaseVersion, downloadUrl)
        } else if (effectiveList.isNotEmpty()) {
            _state.value = _state.value.copy(
                isCheckingUpdate = false,
                selectedTargetVersion = targetVer,
                availableVersions = effectiveList,
                statusMessage = "检查完成: 获取到 ${effectiveList.size} 个 >=4.1 版本",
                isError = false
            )
            val first = effectiveList.first()
            Pair(first.version, first.downloadUrl)
        } else {
            _state.value = _state.value.copy(
                isCheckingUpdate = false,
                availableVersions = emptyList(),
                statusMessage = "从网络拉取完成，未发现 >= 4.1 的版本或请求失败",
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
        var ver = version ?: _state.value.selectedTargetVersion ?: _state.value.latestVersion
        var rawUrl = targetUrl

        if (rawUrl == null && ver != null) {
            rawUrl = _state.value.availableVersions.find {
                it.version.equals(ver, ignoreCase = true) || it.version.removePrefix("v").equals(ver.removePrefix("v"), ignoreCase = true)
            }?.downloadUrl
        }

        // 如果尚未获取版本信息，先检查一次更新
        if (ver == null || rawUrl == null) {
            val result = checkUpdate(context)
            if (result != null) {
                ver = ver ?: result.first
                rawUrl = rawUrl ?: result.second
            } else {
                ver = ver ?: "v4.1"
                rawUrl = rawUrl ?: "https://github.com/Genymobile/scrcpy/releases/download/$ver/scrcpy-server-$ver"
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
            downloadingVersion = ver,
            downloadProgress = 0f,
            downloadedBytes = 0L,
            totalBytes = 0L,
            downloadSpeedText = "准备下载...",
            statusMessage = "正在连接 GitHub Releases 节点下载 scrcpy-server $ver...",
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
                .putString(KEY_SELECTED_TARGET_VERSION, ver)
                .apply()

            _state.value = _state.value.copy(
                isReady = true,
                localVersion = ver,
                selectedTargetVersion = ver,
                localFileSize = targetFile.length(),
                isDownloading = false,
                downloadingVersion = null,
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
                downloadingVersion = null,
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
