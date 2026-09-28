package com.yangyx.adbhelper.ui.models

import android.content.Context
import com.yangyx.adbhelper.adb.AdbConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * High-performance analyzer that inspects third-party applications installed on the
 * REMOTE connected Android device over ADB shell & dumpsys.
 */
object RemoteAppAnalyzer {

    private val DATE_FORMATS = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    )

    private fun parseDateTime(raw: String): Long {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return 0L
        val asLong = trimmed.toLongOrNull()
        if (asLong != null && asLong > 1000000000L) return asLong

        for (fmt in DATE_FORMATS) {
            try {
                val d = fmt.parse(trimmed)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return 0L
    }

    private fun resolveInstallerName(installerPkg: String?): String {
        return when (installerPkg?.trim()) {
            "com.android.vending" -> "Google Play"
            "com.xiaomi.market" -> "小米应用商店"
            "com.huawei.appmarket" -> "华为应用市场"
            "com.heytap.market", "com.oppo.market" -> "OPPO 软件商店"
            "com.bbk.appstore", "com.vivo.appstore" -> "vivo 应用商店"
            "com.sec.android.app.samsungapps" -> "三星应用商店"
            "com.coolapk.market" -> "酷安"
            "com.tencent.android.qqdownloader" -> "腾讯应用宝"
            "com.baidu.appsearch" -> "百度手机助手"
            "com.qihoo.appstore" -> "360手机助手"
            "com.wandoujia.phoenix2" -> "豌豆荚"
            "com.android.packageinstaller", "com.google.android.packageinstaller" -> "系统安装器 / 旁加载"
            null, "", "null" -> "未知来源 / 手动安装"
            else -> installerPkg
        }
    }

    private fun extractSensitiveCategories(permissions: List<String>): Set<SensitivePermissionCategory> {
        val categories = mutableSetOf<SensitivePermissionCategory>()
        for (p in permissions) {
            val upper = p.uppercase(Locale.ROOT)
            when {
                upper.contains("LOCATION") -> categories.add(SensitivePermissionCategory.LOCATION)
                upper.contains("CAMERA") -> categories.add(SensitivePermissionCategory.CAMERA)
                upper.contains("RECORD_AUDIO") || upper.contains("MICROPHONE") -> categories.add(SensitivePermissionCategory.MICROPHONE)
                upper.contains("STORAGE") || upper.contains("MEDIA") -> categories.add(SensitivePermissionCategory.STORAGE)
                upper.contains("CONTACTS") || upper.contains("CALL_LOG") || upper.contains("CALL_PHONE") || upper.contains("PHONE_STATE") -> categories.add(SensitivePermissionCategory.CONTACTS_CALLS)
                upper.contains("SMS") -> categories.add(SensitivePermissionCategory.SMS)
                upper.contains("POST_NOTIFICATIONS") -> categories.add(SensitivePermissionCategory.NOTIFICATION)
                upper.contains("FOREGROUND_SERVICE") || upper.contains("IGNORE_BATTERY_OPTIMIZATIONS") || upper.contains("BOOT_COMPLETED") -> categories.add(SensitivePermissionCategory.BACKGROUND_SERVICE)
                upper.contains("SYSTEM_ALERT_WINDOW") || upper.contains("ALERT_WINDOW") -> categories.add(SensitivePermissionCategory.SYSTEM_ALERT)
            }
        }
        return categories
    }

    private fun parseFriendlyAppName(pkg: String): String {
        val knownApps = mapOf(
            "com.tencent.mm" to "微信",
            "com.ss.android.ugc.aweme" to "抖音",
            "com.ss.android.ugc.aweme.lite" to "抖音极速版",
            "com.ss.android.ugc.livelite" to "抖音火山版",
            "com.tencent.mobileqq" to "QQ",
            "com.tencent.tim" to "TIM",
            "com.eg.android.AlipayGphone" to "支付宝",
            "com.taobao.taobao" to "淘宝",
            "com.taobao.idlefish" to "闲鱼",
            "com.jingdong.app.mall" to "京东",
            "com.jingdong.app.mall.lite" to "京东极速版",
            "com.xunmeng.pinduoduo" to "拼多多",
            "com.sina.weibo" to "微博",
            "com.sankuai.meituan" to "美团",
            "com.sankuai.meituan.takeout" to "美团外卖",
            "me.ele" to "饿了么",
            "com.bilibili.app.in" to "哔哩哔哩",
            "tv.danmaku.bili" to "哔哩哔哩",
            "com.baidu.netdisk" to "百度网盘",
            "com.autonavi.minimap" to "高德地图",
            "com.baidu.BaiduMap" to "百度地图",
            "com.zhihu.android" to "知乎",
            "com.coolapk.market" to "酷安",
            "com.xingin.xhs" to "小红书",
            "com.kuaishou.nebula" to "快手极速版",
            "com.smile.gifmaker" to "快手",
            "com.netease.cloudmusic" to "网易云音乐",
            "com.tencent.qqmusic" to "QQ音乐",
            "com.kugou.android" to "酷狗音乐",
            "cn.kuwo.player" to "酷我音乐",
            "com.qiyi.video" to "爱奇艺",
            "com.tencent.qqlive" to "腾讯视频",
            "com.youku.phone" to "优酷视频",
            "com.ss.android.article.news" to "今日头条",
            "com.baidu.tieba" to "百度贴吧",
            "com.baidu.searchbox" to "百度",
            "com.quark.browser" to "夸克",
            "com.UCMobile" to "UC浏览器",
            "com.alibaba.android.rimet" to "钉钉",
            "com.ss.android.lark" to "飞书",
            "com.tencent.wework" to "企业微信"
        )
        return knownApps[pkg] ?: run {
            val suffix = pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            if (suffix.length > 2) suffix else pkg
        }
    }

    private class RawRemotePkgData(
        val pkg: String,
        var baseApkPath: String = "",
        var codePath: String = "",
        var versionName: String = "",
        var versionCode: Long = 0L,
        var targetSdk: Int = 0,
        var minSdk: Int = 0,
        var compileSdk: Int = 0,
        var primaryCpuAbi: String = "",
        var secondaryCpuAbi: String = "",
        var isDebuggable: Boolean = false,
        var allowBackup: Boolean = true,
        var installerPackage: String? = null,
        var firstInstallTime: Long = 0L,
        var lastUpdateTime: Long = 0L,
        val splits: MutableList<String> = mutableListOf(),
        val requestedPermissions: LinkedHashSet<String> = LinkedHashSet()
    )

    suspend fun analyzeRemote(
        conn: AdbConnection,
        context: Context? = null,
        knownRemoteApps: List<RemoteAppItem> = emptyList(),
        cachedLabels: Map<String, String> = emptyMap(),
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): AppAnalysisReport = withContext(Dispatchers.IO) {
        onProgress(0.05f, "1/5 正在检索远端已安装第三方应用 (pm list packages -3 -f)...")

        // Step 1: Fetch third party packages and base APK paths
        var pmListOut = conn.executeShell("pm list packages -3 -f 2>&1", 15000).trim()
        if (pmListOut.lines().none { it.trim().startsWith("package:") }) {
            val fallbackOut = conn.executeShell("cmd package list packages -3 -f 2>&1", 15000).trim()
            if (fallbackOut.lines().any { it.trim().startsWith("package:") }) {
                pmListOut = fallbackOut
            }
        }

        val thirdPartyMap = mutableMapOf<String, String>() // packageName -> baseApkPath
        pmListOut.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("package:")) {
                val rest = trimmed.removePrefix("package:")
                if (rest.contains("=")) {
                    val apk = rest.substringBeforeLast("=")
                    val pkg = rest.substringAfterLast("=")
                    if (pkg.isNotBlank() && apk.isNotBlank()) {
                        thirdPartyMap[pkg] = apk
                    }
                } else if (rest.isNotBlank()) {
                    thirdPartyMap[rest.trim()] = ""
                }
            }
        }

        // Also merge knownRemoteApps from the App List to ensure no 3rd-party apps are missed
        knownRemoteApps.forEach { item ->
            if (!item.isSystemApp && item.packageName.isNotBlank()) {
                val existingApk = thirdPartyMap[item.packageName]
                if (existingApk.isNullOrBlank() && item.apkPath.isNotBlank()) {
                    thirdPartyMap[item.packageName] = item.apkPath
                } else if (!thirdPartyMap.containsKey(item.packageName)) {
                    thirdPartyMap[item.packageName] = item.apkPath
                }
            }
        }

        val totalApps = thirdPartyMap.size
        if (totalApps == 0) {
            onProgress(1.0f, "远端未检测到第三方应用")
            return@withContext AppAnalysisReport(
                scanTimestamp = System.currentTimeMillis()
            )
        }

        onProgress(0.20f, "2/4 正在解析远端应用包元数据与权限清单 (dumpsys package)...")

        // Step 2: Query dumpsys package for third-party packages in batches
        // Note: dumpsys package packages on Android 11+ omits requested permissions.
        // Querying dumpsys package <pkg> dumps the full manifest permissions reliably.
        val rawDataMap = mutableMapOf<String, RawRemotePkgData>()
        thirdPartyMap.forEach { (pkg, apk) ->
            rawDataMap[pkg] = RawRemotePkgData(pkg = pkg, baseApkPath = apk)
        }

        val pkgList = thirdPartyMap.keys.toList()
        val batchSize = 15
        val batches = pkgList.chunked(batchSize)
        val totalBatches = batches.size

        batches.forEachIndexed { batchIndex, batch ->
            val progressVal = 0.20f + (batchIndex.toFloat() / totalBatches.coerceAtLeast(1)) * 0.45f
            onProgress(progressVal, "2/4 正在解析远端应用元数据与权限清单 (${batchIndex + 1}/$totalBatches 批)...")

            val pkgsArg = batch.joinToString(" ")
            val script = "for p in $pkgsArg; do echo \"===PKG_START:\$p===\"; dumpsys package \$p 2>/dev/null; echo \"===PKG_END:\$p===\"; done"

            var currentPkgData: RawRemotePkgData? = null
            var inPermSection = false

            conn.executeShellStream(script) { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("===PKG_START:") && trimmed.endsWith("===")) {
                    val pkgName = trimmed.removePrefix("===PKG_START:").removeSuffix("===").trim()
                    currentPkgData = rawDataMap[pkgName]
                    inPermSection = false
                    return@executeShellStream
                }
                if (trimmed.startsWith("===PKG_END:") && trimmed.endsWith("===")) {
                    currentPkgData = null
                    inPermSection = false
                    return@executeShellStream
                }

                val cur = currentPkgData ?: return@executeShellStream

                // Permissions Section Detection (Requested, Install, Runtime)
                val isPermHeader = trimmed == "requested permissions:" || trimmed.startsWith("requested permissions:") ||
                        trimmed == "install permissions:" || trimmed.startsWith("install permissions:") ||
                        trimmed == "runtime permissions:" || trimmed.startsWith("runtime permissions:")
                if (isPermHeader) {
                    inPermSection = true
                    return@executeShellStream
                }

                if (inPermSection) {
                    // Check if exiting permission section
                    if (trimmed.endsWith(":") &&
                        !trimmed.contains("android.permission") &&
                        !trimmed.contains(".permission.") &&
                        (trimmed.startsWith("User ") || trimmed.startsWith("Dexopt ") || trimmed.startsWith("Queries:") ||
                         trimmed.startsWith("Compiler ") || trimmed.startsWith("Commands:") || trimmed.startsWith("Components:"))
                    ) {
                        inPermSection = false
                    } else {
                        val rawPerm = trimmed.substringBefore(":").trim()
                        if (rawPerm.isNotBlank() && !rawPerm.contains(" ") && (rawPerm.contains(".") || rawPerm.startsWith("android.permission"))) {
                            cur.requestedPermissions.add(rawPerm)
                        }
                    }
                }

                // Global regex match for standard permissions on any line of this package
                Regex("""\b(android\.permission\.[A-Za-z0-9_]+)\b""").findAll(trimmed).forEach { m ->
                    cur.requestedPermissions.add(m.groupValues[1])
                }

                // Parse standard package fields
                if (trimmed.startsWith("codePath=")) {
                    cur.codePath = trimmed.substringAfter("codePath=").trim()
                } else if (trimmed.startsWith("versionName=")) {
                    cur.versionName = trimmed.substringAfter("versionName=").trim()
                } else if (trimmed.contains("versionCode=") || trimmed.contains("targetSdk=") || trimmed.contains("minSdk=")) {
                    val vCodeMatch = Regex("""versionCode=(\d+)""").find(trimmed)
                    if (vCodeMatch != null && cur.versionCode == 0L) {
                        cur.versionCode = vCodeMatch.groupValues[1].toLongOrNull() ?: 0L
                    }
                    val targetMatch = Regex("""targetSdk=(\d+)""").find(trimmed)
                    if (targetMatch != null && cur.targetSdk == 0) {
                        cur.targetSdk = targetMatch.groupValues[1].toIntOrNull() ?: 0
                    }
                    val minMatch = Regex("""minSdk=(\d+)""").find(trimmed)
                    if (minMatch != null && cur.minSdk == 0) {
                        cur.minSdk = minMatch.groupValues[1].toIntOrNull() ?: 0
                    }
                } else if (trimmed.startsWith("compileSdkVersion=")) {
                    cur.compileSdk = trimmed.substringAfter("compileSdkVersion=").trim().toIntOrNull() ?: 0
                } else if (trimmed.startsWith("primaryCpuAbi=")) {
                    val abi = trimmed.substringAfter("primaryCpuAbi=").trim()
                    if (abi.isNotEmpty() && abi != "null") {
                        cur.primaryCpuAbi = abi
                    }
                } else if (trimmed.startsWith("secondaryCpuAbi=")) {
                    val abi = trimmed.substringAfter("secondaryCpuAbi=").trim()
                    if (abi.isNotEmpty() && abi != "null") {
                        cur.secondaryCpuAbi = abi
                    }
                } else if (trimmed.contains("flags=[") && trimmed.contains("]")) {
                    val flags = trimmed.substringAfter("flags=[").substringBefore("]")
                    if (flags.contains("DEBUGGABLE")) {
                        cur.isDebuggable = true
                    }
                    if (!flags.contains("ALLOW_BACKUP")) {
                        cur.allowBackup = false
                    }
                } else if (trimmed.startsWith("installerPackageName=")) {
                    val inst = trimmed.substringAfter("installerPackageName=").trim()
                    if (inst.isNotEmpty() && inst != "null") {
                        cur.installerPackage = inst
                    }
                } else if (trimmed.startsWith("firstInstallTime=")) {
                    cur.firstInstallTime = parseDateTime(trimmed.substringAfter("firstInstallTime="))
                } else if (trimmed.startsWith("lastUpdateTime=")) {
                    cur.lastUpdateTime = parseDateTime(trimmed.substringAfter("lastUpdateTime="))
                } else if (trimmed.startsWith("splits=[")) {
                    val spStr = trimmed.substringAfter("splits=[").substringBefore("]")
                    if (spStr.isNotBlank() && spStr != "null") {
                        cur.splits.addAll(spStr.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                    }
                }
            }
        }

        onProgress(0.65f, "3/4 正在统计远端应用体积大小与分包文件...")

        // Step 3: Fast APK sizes and split detections
        // Using stat to get exact bytes for all installed APKs in /data/app
        val apkSizeMap = mutableMapOf<String, Long>() // apk path -> size in bytes
        val statOut = conn.executeShell("stat -c \"%s %n\" /data/app/*/*.apk /data/app/*/*/*.apk 2>/dev/null", 15000).trim()
        if (statOut.isNotBlank()) {
            statOut.lines().forEach { line ->
                val parts = line.trim().split(Regex("\\s+"), limit = 2)
                if (parts.size == 2) {
                    val size = parts[0].toLongOrNull() ?: 0L
                    val path = parts[1]
                    if (size > 0L && path.isNotEmpty()) {
                        apkSizeMap[path] = size
                    }
                }
            }
        }

        // Fallback for missing apps using ls -l
        val missingSizeApps = rawDataMap.values.filter { raw ->
            raw.baseApkPath.isNotEmpty() && !apkSizeMap.containsKey(raw.baseApkPath)
        }
        if (missingSizeApps.isNotEmpty()) {
            val chunked = missingSizeApps.chunked(30)
            for (batch in chunked) {
                val paths = batch.joinToString(" ") { "\"${it.baseApkPath}\"" }
                val lsOut = conn.executeShell("ls -l $paths 2>/dev/null", 10000).trim()
                lsOut.lines().forEach { l ->
                    val tokens = l.trim().split(Regex("\\s+"))
                    if (tokens.size >= 5) {
                        val path = tokens.last()
                        // Size is usually column 4 or 5 depending on ls format
                        val sz = tokens.drop(3).firstOrNull { it.toLongOrNull() != null }?.toLongOrNull() ?: 0L
                        if (sz > 0L) {
                            apkSizeMap[path] = sz
                        }
                    }
                }
            }
        }

        // Step 4: Resolve app display names by reusing the App List's cached aapt2 labels & remote aapt2
        onProgress(0.70f, "4/5 正在复用应用列表与 aapt2 解析应用真实名称...")

        val resolvedLabels = mutableMapOf<String, String>()
        // Seed from global persistent label cache
        resolvedLabels.putAll(RemoteAppLabelManager.labelCache)
        // Seed from passed in cached labels
        resolvedLabels.putAll(cachedLabels)
        // Seed from knownRemoteApps (from App List)
        knownRemoteApps.forEach { item ->
            if (item.appName.isNotBlank() && item.appName != item.packageName) {
                resolvedLabels[item.packageName] = item.appName
                RemoteAppLabelManager.labelCache[item.packageName] = item.appName
            }
        }

        // Detect which apps are still missing a resolved human-readable display name
        val missingApps = thirdPartyMap.entries
            .filter { (pkg, apk) ->
                val current = resolvedLabels[pkg]
                (current == null || current.isBlank() || current == pkg) && apk.isNotBlank()
            }
            .map { it.key to it.value }

        if (missingApps.isNotEmpty()) {
            onProgress(0.72f, "正在使用远端 aapt2 解析 ${missingApps.size} 款未命名应用...")
            val aaptPath = RemoteAppLabelManager.ensureRemoteAapt2(conn, context)
            if (aaptPath != null) {
                val newlyParsed = RemoteAppLabelManager.resolveMissingLabelsWithAapt2(
                    conn = conn,
                    appsToResolve = missingApps,
                    aaptRemotePath = aaptPath,
                    numThreads = 4
                ) { done, total ->
                    onProgress(0.72f + (done.toFloat() / total) * 0.14f, "正在通过 aapt2 提取应用显示名称 ($done/$total)...")
                }
                resolvedLabels.putAll(newlyParsed)
            }
        }

        onProgress(0.88f, "5/5 正在生成多维度综合分析统计画像...")

        // Build AppAnalysisItem list
        val knownAppsMap = knownRemoteApps.associateBy { it.packageName }

        val analysisItems = rawDataMap.values.map { raw ->
            val pkg = raw.pkg
            val known = knownAppsMap[pkg]
            val appName = resolvedLabels[pkg]
                ?: (known?.appName?.takeIf { it.isNotBlank() && it != pkg })
                ?: parseFriendlyAppName(pkg)

            val versionName = when {
                raw.versionName.isNotEmpty() -> raw.versionName
                known != null && known.versionName.isNotEmpty() -> known.versionName
                else -> "1.0"
            }

            val versionCode = when {
                raw.versionCode > 0L -> raw.versionCode
                known != null && known.versionCode > 0L -> known.versionCode
                else -> 1L
            }

            // Target SDK & Min SDK fallback
            val targetSdk = if (raw.targetSdk > 0) raw.targetSdk else 29 // default reasonable
            val minSdk = if (raw.minSdk > 0) raw.minSdk else 21

            // Size calculation & Split APK accurate detection
            val apkDir = if (raw.baseApkPath.contains("/")) raw.baseApkPath.substringBeforeLast("/") else ""
            val siblingApks = if (apkDir.isNotEmpty()) {
                apkSizeMap.filter { it.key.startsWith("$apkDir/") && it.key.endsWith(".apk") }
            } else emptyMap()

            // Filter out "base", "none", "null" tokens from dumpsys package splits=[...]
            // In Android framework, splits=[base] designates a SINGLE APK with only base.apk!
            val nonBaseSplits = raw.splits.filter { splitName ->
                val s = splitName.trim().lowercase(Locale.ROOT)
                s.isNotEmpty() && s != "base" && s != "none" && s != "null"
            }

            // An app is truly a Split APK if multiple APKs exist in its directory,
            // OR if dumpsys package explicitly declares non-base split modules.
            val isSplit = (siblingApks.size > 1) || nonBaseSplits.isNotEmpty()
            val splitCount = when {
                siblingApks.size > 1 -> siblingApks.size
                nonBaseSplits.isNotEmpty() -> nonBaseSplits.size + 1
                else -> 1
            }

            val totalSize = if (siblingApks.isNotEmpty()) {
                siblingApks.values.sum()
            } else {
                apkSizeMap[raw.baseApkPath] ?: 0L
            }

            val splitDetails: List<ApkSplitDetail> = when {
                siblingApks.size > 1 -> {
                    siblingApks.entries.sortedWith(
                        compareByDescending<Map.Entry<String, Long>> { it.key.endsWith("/base.apk") || it.key == raw.baseApkPath }
                            .thenByDescending { it.value }
                    ).map { (path, size) ->
                        val fileName = path.substringAfterLast('/')
                        val isBase = fileName.equals("base.apk", ignoreCase = true) || path == raw.baseApkPath
                        ApkSplitDetail(
                            fileName = fileName,
                            filePath = path,
                            sizeBytes = size,
                            isBase = isBase,
                            description = AppAnalysisItem.resolveSplitDescription(fileName, isBase)
                        )
                    }
                }
                nonBaseSplits.isNotEmpty() -> {
                    val baseDetail = ApkSplitDetail(
                        fileName = raw.baseApkPath.substringAfterLast('/').ifBlank { "base.apk" },
                        filePath = raw.baseApkPath,
                        sizeBytes = apkSizeMap[raw.baseApkPath] ?: 0L,
                        isBase = true,
                        description = "主程序基包 (Base APK，核心代码与清单)"
                    )
                    val otherSplits = nonBaseSplits.map { sName ->
                        val fName = if (sName.endsWith(".apk")) sName else "split_$sName.apk"
                        val fPath = if (apkDir.isNotEmpty()) "$apkDir/$fName" else ""
                        val fSize = apkSizeMap[fPath] ?: 0L
                        ApkSplitDetail(
                            fileName = fName,
                            filePath = fPath,
                            sizeBytes = fSize,
                            isBase = false,
                            description = AppAnalysisItem.resolveSplitDescription(fName, false)
                        )
                    }
                    listOf(baseDetail) + otherSplits
                }
                else -> {
                    listOf(
                        ApkSplitDetail(
                            fileName = raw.baseApkPath.substringAfterLast('/').ifBlank { "base.apk" },
                            filePath = raw.baseApkPath,
                            sizeBytes = if (siblingApks.isNotEmpty()) siblingApks.values.first() else (apkSizeMap[raw.baseApkPath] ?: totalSize),
                            isBase = true,
                            description = "单体完整 APK (包含全部代码、Native动态库与资源)"
                        )
                    )
                }
            }

            // ABI classification
            val abis = mutableListOf<String>()
            if (raw.primaryCpuAbi.isNotEmpty()) abis.add(raw.primaryCpuAbi)
            if (raw.secondaryCpuAbi.isNotEmpty()) abis.add(raw.secondaryCpuAbi)

            val pLower = raw.primaryCpuAbi.lowercase()
            val sLower = raw.secondaryCpuAbi.lowercase()
            val abiCategory = when {
                pLower.contains("arm64") && (sLower.contains("arm") || sLower.contains("v7a")) -> AppAbiCategory.MULTI_ARCH
                pLower.contains("arm64") -> AppAbiCategory.ARM64_ONLY
                pLower.contains("armeabi") || pLower == "arm" -> AppAbiCategory.ARM32_ONLY
                pLower.contains("x86_64") -> AppAbiCategory.X86_64
                pLower.isEmpty() -> AppAbiCategory.PURE_JAVA
                else -> AppAbiCategory.OTHER
            }

            // Permissions
            val permList = raw.requestedPermissions.toList()
            val sensCategories = extractSensitiveCategories(permList)

            AppAnalysisItem(
                packageName = pkg,
                appName = appName,
                versionName = versionName,
                versionCode = versionCode,
                icon = null, // Remote apps don't have local Drawable icons, UI renders dynamic Android badge
                targetSdk = targetSdk,
                minSdk = minSdk,
                compileSdk = raw.compileSdk,
                isSystemApp = false,
                nativeAbis = abis,
                abiCategory = abiCategory,
                isSplitApk = isSplit,
                splitCount = splitCount,
                splitDetails = splitDetails,
                isDebuggable = raw.isDebuggable,
                allowBackup = raw.allowBackup,
                totalSizeBytes = totalSize,
                firstInstallTime = raw.firstInstallTime,
                lastUpdateTime = raw.lastUpdateTime,
                installerPackage = raw.installerPackage,
                installerName = resolveInstallerName(raw.installerPackage),
                requestedPermissions = permList,
                sensitiveCategories = sensCategories
            )
        }

        // Aggregate statistics
        val targetSdkMap = mutableMapOf<Int, Int>()
        val minSdkMap = mutableMapOf<Int, Int>()
        val abiMap = mutableMapOf<AppAbiCategory, Int>()
        val sensitiveCountMap = mutableMapOf<SensitivePermissionCategory, Int>()
        val installerMap = mutableMapOf<String, Int>()

        var totalTargetSum = 0L
        var modernTargetCount = 0
        var outdatedTargetCount = 0

        var pure32Count = 0
        var pureJavaCount = 0
        var arm64Count = 0

        var debuggableCount = 0
        var backupAllowedCount = 0
        var splitApkCount = 0
        var totalBytes = 0L

        var dormantCount = 0
        var recentUpdateCount = 0

        for (item in analysisItems) {
            targetSdkMap[item.targetSdk] = (targetSdkMap[item.targetSdk] ?: 0) + 1
            minSdkMap[item.minSdk] = (minSdkMap[item.minSdk] ?: 0) + 1

            totalTargetSum += item.targetSdk
            if (item.targetSdk >= 34) modernTargetCount++
            if (item.targetSdk < 29) outdatedTargetCount++

            abiMap[item.abiCategory] = (abiMap[item.abiCategory] ?: 0) + 1
            when (item.abiCategory) {
                AppAbiCategory.ARM32_ONLY -> pure32Count++
                AppAbiCategory.PURE_JAVA -> pureJavaCount++
                AppAbiCategory.ARM64_ONLY -> arm64Count++
                else -> {}
            }

            installerMap[item.installerName] = (installerMap[item.installerName] ?: 0) + 1

            if (item.isDebuggable) debuggableCount++
            if (item.allowBackup) backupAllowedCount++
            if (item.isSplitApk) splitApkCount++
            totalBytes += item.totalSizeBytes

            if (item.isDormant) dormantCount++
            if (item.isRecentlyUpdated) recentUpdateCount++

            for (cat in item.sensitiveCategories) {
                sensitiveCountMap[cat] = (sensitiveCountMap[cat] ?: 0) + 1
            }
        }

        val avgTarget = if (analysisItems.isNotEmpty()) totalTargetSum.toFloat() / analysisItems.size else 0f
        val modernRate = if (analysisItems.isNotEmpty()) (modernTargetCount.toFloat() / analysisItems.size) * 100f else 0f
        val avgBytes = if (analysisItems.isNotEmpty()) totalBytes / analysisItems.size else 0L

        val largest = analysisItems.sortedByDescending { it.totalSizeBytes }.take(25)
        val hungry = analysisItems
            .sortedWith(
                compareByDescending<AppAnalysisItem> { it.sensitiveCategories.size }
                    .thenByDescending { it.requestedPermissions.size }
            )
            .take(25)

        onProgress(1.0f, "分析完成！")

        AppAnalysisReport(
            scanTimestamp = System.currentTimeMillis(),
            allScannedApps = analysisItems,
            thirdPartyApps = analysisItems,
            systemApps = emptyList(),
            targetSdkDistribution = targetSdkMap.toSortedMap(Comparator.reverseOrder()),
            minSdkDistribution = minSdkMap.toSortedMap(),
            averageTargetSdk = avgTarget,
            modernTargetSdkCount = modernTargetCount,
            modernTargetSdkRate = modernRate,
            outdatedTargetSdkCount = outdatedTargetCount,
            abiCategoryDistribution = abiMap,
            pure32BitAppsCount = pure32Count,
            pureJavaAppsCount = pureJavaCount,
            arm64AppsCount = arm64Count,
            debuggableAppsCount = debuggableCount,
            backupAllowedAppsCount = backupAllowedCount,
            splitApksCount = splitApkCount,
            singleApksCount = analysisItems.size - splitApkCount,
            totalStorageBytes = totalBytes,
            averageStorageBytes = avgBytes,
            largestApps = largest,
            sensitivePermissionCounts = sensitiveCountMap,
            permissionHungryApps = hungry,
            installerDistribution = installerMap.entries.sortedByDescending { it.value }.associate { it.key to it.value },
            dormantAppsCount = dormantCount,
            recentlyUpdatedAppsCount = recentUpdateCount
        )
    }
}
