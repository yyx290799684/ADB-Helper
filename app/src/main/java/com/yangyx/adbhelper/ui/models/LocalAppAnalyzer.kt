package com.yangyx.adbhelper.ui.models

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipFile

object LocalAppAnalyzer {

    suspend fun analyze(
        context: Context,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): AppAnalysisReport = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        onProgress(0.05f, "正在读取系统已安装应用列表...")

        val packages: List<PackageInfo> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(
                    PackageManager.PackageInfoFlags.of(
                        PackageManager.GET_PERMISSIONS.toLong()
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }
        } catch (_: Exception) {
            emptyList()
        }

        val totalCount = packages.size
        val allItems = mutableListOf<AppAnalysisItem>()

        packages.forEachIndexed { index, pkgInfo ->
            val appInfo = pkgInfo.applicationInfo ?: return@forEachIndexed
            val sourceDir = appInfo.sourceDir ?: return@forEachIndexed
            if (sourceDir.isBlank()) return@forEachIndexed

            val baseFile = File(sourceDir)
            if (!baseFile.exists()) return@forEachIndexed

            if (index % 10 == 0 || index == totalCount - 1) {
                val ratio = 0.05f + (index.toFloat() / totalCount.coerceAtLeast(1)) * 0.85f
                onProgress(ratio, "正在分析 (${index + 1}/$totalCount): ${pkgInfo.packageName}")
            }

            // Size calculation
            val splits = appInfo.splitSourceDirs?.filter { File(it).exists() } ?: emptyList()
            var totalSize = baseFile.length()
            for (s in splits) {
                totalSize += File(s).length()
            }

            val isSys = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isDebug = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val allowBackup = (appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0

            val appName = try {
                appInfo.loadLabel(pm).toString()
            } catch (_: Exception) {
                pkgInfo.packageName
            }

            val icon = try {
                appInfo.loadIcon(pm)
            } catch (_: Exception) {
                null
            }

            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }

            val targetSdk = appInfo.targetSdkVersion
            val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                appInfo.minSdkVersion
            } else {
                21
            }

            val compileSdk = try {
                val field = pkgInfo.javaClass.getField("compileSdkVersion")
                field.getInt(pkgInfo)
            } catch (_: Exception) {
                try {
                    val field = appInfo.javaClass.getField("compileSdkVersion")
                    field.getInt(appInfo)
                } catch (_: Exception) {
                    0
                }
            }

            // Native ABI detection
            val (abis, abiCategory) = detectAbiAndCategory(baseFile, appInfo)

            // Installer info
            val installerPkg = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    pm.getInstallSourceInfo(pkgInfo.packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstallerPackageName(pkgInfo.packageName)
                }
            } catch (_: Exception) {
                null
            }
            val installerName = resolveInstallerName(installerPkg)

            // Permissions profiling
            val requestedPermissions = pkgInfo.requestedPermissions?.toList() ?: emptyList()
            val sensitiveCategories = extractSensitiveCategories(requestedPermissions)

            val isSplit = splits.isNotEmpty()
            val splitCount = if (isSplit) splits.size + 1 else 1

            val splitDetails = mutableListOf<ApkSplitDetail>()
            splitDetails.add(
                ApkSplitDetail(
                    fileName = baseFile.name,
                    filePath = baseFile.absolutePath,
                    sizeBytes = baseFile.length(),
                    isBase = true,
                    description = if (isSplit) "主程序基包 (Base APK，核心代码与清单)" else "单体完整 APK (包含全部代码、Native动态库与资源)"
                )
            )
            for (s in splits) {
                val f = File(s)
                splitDetails.add(
                    ApkSplitDetail(
                        fileName = f.name,
                        filePath = f.absolutePath,
                        sizeBytes = f.length(),
                        isBase = false,
                        description = AppAnalysisItem.resolveSplitDescription(f.name, false)
                    )
                )
            }

            allItems.add(
                AppAnalysisItem(
                    packageName = pkgInfo.packageName,
                    appName = appName,
                    versionName = pkgInfo.versionName ?: "",
                    versionCode = vCode,
                    icon = icon,
                    targetSdk = targetSdk,
                    minSdk = minSdk,
                    compileSdk = compileSdk,
                    isSystemApp = isSys,
                    nativeAbis = abis,
                    abiCategory = abiCategory,
                    isSplitApk = isSplit,
                    splitCount = splitCount,
                    splitDetails = splitDetails,
                    isDebuggable = isDebug,
                    allowBackup = allowBackup,
                    totalSizeBytes = totalSize,
                    firstInstallTime = pkgInfo.firstInstallTime,
                    lastUpdateTime = pkgInfo.lastUpdateTime,
                    installerPackage = installerPkg,
                    installerName = installerName,
                    requestedPermissions = requestedPermissions,
                    sensitiveCategories = sensitiveCategories
                )
            )
        }

        onProgress(0.95f, "正在聚合第三方应用分析指标...")

        // Sort apps: by name
        allItems.sortBy { it.appName.lowercase() }

        val thirdParty = allItems.filter { !it.isSystemApp }
        val system = allItems.filter { it.isSystemApp }

        // Compile comprehensive statistics primarily for third-party apps
        val targetSdkMap = mutableMapOf<Int, Int>()
        val minSdkMap = mutableMapOf<Int, Int>()
        val abiMap = mutableMapOf<AppAbiCategory, Int>()
        val installerMap = mutableMapOf<String, Int>()
        val sensitiveCountMap = mutableMapOf<SensitivePermissionCategory, Int>()

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

        for (item in thirdParty) {
            targetSdkMap[item.targetSdk] = (targetSdkMap[item.targetSdk] ?: 0) + 1
            minSdkMap[item.minSdk] = (minSdkMap[item.minSdk] ?: 0) + 1
            abiMap[item.abiCategory] = (abiMap[item.abiCategory] ?: 0) + 1
            installerMap[item.installerName] = (installerMap[item.installerName] ?: 0) + 1

            totalTargetSum += item.targetSdk
            if (item.targetSdk >= 34) modernTargetCount++
            if (item.isOutdatedTargetSdk) outdatedTargetCount++

            when (item.abiCategory) {
                AppAbiCategory.ARM32_ONLY -> pure32Count++
                AppAbiCategory.PURE_JAVA -> pureJavaCount++
                AppAbiCategory.ARM64_ONLY, AppAbiCategory.MULTI_ARCH -> arm64Count++
                else -> {}
            }

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

        val avgTarget = if (thirdParty.isNotEmpty()) {
            totalTargetSum.toFloat() / thirdParty.size
        } else 0f

        val modernRate = if (thirdParty.isNotEmpty()) {
            (modernTargetCount.toFloat() / thirdParty.size) * 100f
        } else 0f

        val avgBytes = if (thirdParty.isNotEmpty()) {
            totalBytes / thirdParty.size
        } else 0L

        val largest = thirdParty.sortedByDescending { it.totalSizeBytes }.take(25)
        val hungry = thirdParty
            .sortedWith(
                compareByDescending<AppAnalysisItem> { it.sensitiveCategories.size }
                    .thenByDescending { it.requestedPermissions.size }
            )
            .take(25)

        onProgress(1.0f, "分析完成！")

        AppAnalysisReport(
            scanTimestamp = System.currentTimeMillis(),
            allScannedApps = allItems,
            thirdPartyApps = thirdParty,
            systemApps = system,
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
            singleApksCount = thirdParty.size - splitApkCount,
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

    private fun detectAbiAndCategory(apkFile: File, appInfo: ApplicationInfo): Pair<List<String>, AppAbiCategory> {
        val abis = mutableSetOf<String>()
        var readFromZip = false

        try {
            ZipFile(apkFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.startsWith("lib/")) {
                        val segments = entry.name.split('/')
                        if (segments.size >= 3 && segments[2].endsWith(".so")) {
                            abis.add(segments[1])
                        }
                    }
                }
                readFromZip = true
            }
        } catch (_: Exception) {
            // Zip reading failed (e.g. permission or security restriction)
        }

        if (!readFromZip || abis.isEmpty()) {
            try {
                val libDir = File(appInfo.nativeLibraryDir ?: "")
                if (libDir.exists() && (libDir.list()?.isNotEmpty() == true)) {
                    val path = libDir.absolutePath
                    when {
                        path.contains("arm64") -> abis.add("arm64-v8a")
                        path.contains("x86_64") -> abis.add("x86_64")
                        path.contains("arm") -> abis.add("armeabi-v7a")
                        path.contains("x86") -> abis.add("x86")
                    }
                }
            } catch (_: Exception) {
            }
        }

        val hasArm64 = abis.any { it.contains("arm64", ignoreCase = true) }
        val hasArm32 = abis.any { it.contains("armeabi", ignoreCase = true) || it == "arm" }
        val hasX86_64 = abis.any { it.contains("x86_64", ignoreCase = true) }

        val category = when {
            abis.isEmpty() -> AppAbiCategory.PURE_JAVA
            hasArm64 && !hasArm32 -> AppAbiCategory.ARM64_ONLY
            hasArm64 && hasArm32 -> AppAbiCategory.MULTI_ARCH
            !hasArm64 && hasArm32 -> AppAbiCategory.ARM32_ONLY
            hasX86_64 -> AppAbiCategory.X86_64
            else -> AppAbiCategory.OTHER
        }

        return Pair(abis.toList().sorted(), category)
    }

    private fun resolveInstallerName(installerPkg: String?): String {
        return when (installerPkg) {
            "com.android.vending" -> "Google Play"
            "com.xiaomi.market" -> "小米应用商店"
            "com.huawei.appmarket" -> "华为应用市场"
            "com.heytap.market", "com.oppo.market" -> "OPPO 软件商店"
            "com.bbk.appstore", "com.vivo.appstore" -> "vivo 应用商店"
            "com.sec.android.app.samsungapps" -> "三星应用商店"
            "com.coolapk.market" -> "酷安"
            "com.tencent.android.qqdownloader" -> "腾讯应用宝"
            "com.baidu.appsearch" -> "百度手机助手"
            "com.android.packageinstaller", "com.google.android.packageinstaller" -> "软件包安装程序 (离线)"
            null, "" -> "未知渠道 / 本地安装"
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
                upper.contains("FOREGROUND_SERVICE") || upper.contains("IGNORE_BATTERY_OPTIMIZATIONS") -> categories.add(SensitivePermissionCategory.BACKGROUND_SERVICE)
                upper.contains("SYSTEM_ALERT_WINDOW") || upper.contains("ALERT_WINDOW") -> categories.add(SensitivePermissionCategory.SYSTEM_ALERT)
            }
        }
        return categories
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return "未知"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Generates a cleanly formatted Markdown report suitable for sharing or saving.
     */
    fun generateMarkdownReport(report: AppAnalysisReport): String {
        val sb = StringBuilder()
        sb.appendLine("# 📱 本机第三方应用深度分析报告")
        sb.appendLine("生成时间：${formatDate(report.scanTimestamp)}")
        sb.appendLine()

        sb.appendLine("## 📊 核心指标概览")
        sb.appendLine("- **第三方应用总数**：${report.thirdPartyApps.size} 款（系统应用 ${report.systemApps.size} 款）")
        sb.appendLine("- **总安装包空间占用**：${formatBytes(report.totalStorageBytes)}（平均每款 ${formatBytes(report.averageStorageBytes)}）")
        sb.appendLine("- **平均目标 API (Target SDK)**：API ${String.format(Locale.getDefault(), "%.1f", report.averageTargetSdk)}")
        sb.appendLine("- **Android 14+ (API 34+) 达标率**：${String.format(Locale.getDefault(), "%.1f", report.modernTargetSdkRate)}%（${report.modernTargetSdkCount} 款）")
        sb.appendLine("- **过时 API (< Android 10 / API 29)**：${report.outdatedTargetSdkCount} 款")
        sb.appendLine("- **64位架构支持情况**：${report.arm64AppsCount} 款支持 64位，纯32位应用 ${report.pure32BitAppsCount} 款，纯 Java/Kotlin ${report.pureJavaAppsCount} 款")
        sb.appendLine("- **调试标记 (Debuggable)**：${report.debuggableAppsCount} 款处于调试状态")
        sb.appendLine("- **允许备份 (Allow Backup)**：${report.backupAllowedAppsCount} 款允许备份")
        sb.appendLine("- **分包 APK (Split APKs)**：${report.splitApksCount} 款（单体 APK ${report.singleApksCount} 款）")
        sb.appendLine("- **维护状态**：超过 1 年未更新 ${report.dormantAppsCount} 款，近 30 天内更新 ${report.recentlyUpdatedAppsCount} 款")
        sb.appendLine()

        sb.appendLine("## 🎯 目标 API (Target SDK) 分布")
        sb.appendLine("| Target SDK | 对应 Android 版本 | 应用数量 | 占比 |")
        sb.appendLine("|:---|:---|:---|:---|")
        val total = report.thirdPartyApps.size.coerceAtLeast(1)
        for ((sdk, count) in report.targetSdkDistribution) {
            val ver = AppAnalysisItem.sdkToAndroidVersion(sdk)
            val pct = (count.toFloat() / total) * 100f
            sb.appendLine("| API $sdk | $ver | $count | ${String.format(Locale.getDefault(), "%.1f", pct)}% |")
        }
        sb.appendLine()

        sb.appendLine("## 🚀 最低 API (Min SDK) 兼容性分布")
        sb.appendLine("| Min SDK | 最低支持系统 | 应用数量 | 占比 |")
        sb.appendLine("|:---|:---|:---|:---|")
        for ((sdk, count) in report.minSdkDistribution) {
            val ver = AppAnalysisItem.sdkToAndroidVersion(sdk)
            val pct = (count.toFloat() / total) * 100f
            sb.appendLine("| API $sdk | $ver | $count | ${String.format(Locale.getDefault(), "%.1f", pct)}% |")
        }
        sb.appendLine()

        if (report.pure32BitAppsCount > 0) {
            sb.appendLine("## ⚠️ 纯 32 位应用警报（可能无法在新一代纯 64 位芯片上运行）")
            val pure32Apps = report.thirdPartyApps.filter { it.abiCategory == AppAbiCategory.ARM32_ONLY }
            for (app in pure32Apps) {
                sb.appendLine("- **${app.appName}** (`${app.packageName}`) v${app.versionName}")
            }
            sb.appendLine()
        }

        if (report.debuggableAppsCount > 0) {
            sb.appendLine("## 🚨 Debuggable 调试模式应用警报（存在被依附调试安全隐患）")
            val debugApps = report.thirdPartyApps.filter { it.isDebuggable }
            for (app in debugApps) {
                sb.appendLine("- **${app.appName}** (`${app.packageName}`) v${app.versionName}")
            }
            sb.appendLine()
        }

        sb.appendLine("## 💾 空间占用 TOP 10 应用")
        sb.appendLine("| 排名 | 应用名称 | 包名 | 体积 | Target SDK |")
        sb.appendLine("|:---|:---|:---|:---|:---|")
        report.largestApps.take(10).forEachIndexed { index, app ->
            sb.appendLine("| #${index + 1} | ${app.appName} | `${app.packageName}` | ${formatBytes(app.totalSizeBytes)} | API ${app.targetSdk} |")
        }
        sb.appendLine()

        sb.appendLine("## 🔐 敏感权限分布")
        for ((cat, count) in report.sensitivePermissionCounts) {
            sb.appendLine("- **${cat.title}** (${cat.description}): $count 款应用申请")
        }

        return sb.toString()
    }
}
