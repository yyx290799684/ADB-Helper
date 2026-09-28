package com.yangyx.adbhelper.ui.models

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable

/**
 * Represents sensitive permission categories for security & privacy profiling.
 */
enum class SensitivePermissionCategory(
    val title: String,
    val description: String,
    val iconName: String,
    val isHighRisk: Boolean = false
) {
    LOCATION("地理位置", "精确/粗略/后台定位权限", "LocationOn", isHighRisk = true),
    CAMERA("相机拍照", "调用摄像头拍摄照片与视频", "PhotoCamera", isHighRisk = true),
    MICROPHONE("麦克风录音", "录制环境音频与麦克风访问", "Mic", isHighRisk = true),
    STORAGE("存储与媒体", "访问设备照片、媒体或全部文件", "Folder", isHighRisk = false),
    CONTACTS_CALLS("通讯录与通话", "读取联系人或通话记录/拨打电话", "Contacts", isHighRisk = true),
    SMS("短信收发", "发送、接收或读取短信验证码", "Sms", isHighRisk = true),
    NOTIFICATION("通知推送", "向系统发送常驻或弹窗通知", "Notifications", isHighRisk = false),
    BACKGROUND_SERVICE("后台/前台服务", "常驻前台服务或忽略电池优化", "Power", isHighRisk = false),
    SYSTEM_ALERT("系统悬浮窗", "在其他应用上方显示悬浮窗内容", "Layers", isHighRisk = true)
}

/**
 * Represents the native ABI architecture category of an application.
 */
enum class AppAbiCategory(val label: String, val badgeText: String) {
    ARM64_ONLY("64位专用 (ARM64)", "64-bit"),
    MULTI_ARCH("多架构支持 (32/64位)", "Multi-ABI"),
    ARM32_ONLY("仅32位 (遗留架构)", "32-bit Only"),
    PURE_JAVA("纯 Java/Kotlin (无 Native 库)", "Pure Java"),
    X86_64("x86_64 (PC/模拟器)", "x86_64"),
    OTHER("其他架构", "Other")
}

@Immutable
data class ApkSplitDetail(
    val fileName: String,
    val filePath: String = "",
    val sizeBytes: Long = 0L,
    val isBase: Boolean = false,
    val description: String = ""
)

/**
 * Detailed analysis record of a single installed application.
 */
@Immutable
data class AppAnalysisItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Drawable? = null,
    val targetSdk: Int,
    val minSdk: Int,
    val compileSdk: Int = 0,
    val isSystemApp: Boolean,
    val nativeAbis: List<String> = emptyList(),
    val abiCategory: AppAbiCategory = AppAbiCategory.PURE_JAVA,
    val isSplitApk: Boolean = false,
    val splitCount: Int = 0,
    val splitDetails: List<ApkSplitDetail> = emptyList(),
    val isDebuggable: Boolean = false,
    val allowBackup: Boolean = true,
    val totalSizeBytes: Long = 0L,
    val firstInstallTime: Long = 0L,
    val lastUpdateTime: Long = 0L,
    val installerPackage: String? = null,
    val installerName: String = "未知来源",
    val requestedPermissions: List<String> = emptyList(),
    val sensitiveCategories: Set<SensitivePermissionCategory> = emptySet()
) {
    val targetSdkAndroidVersion: String
        get() = sdkToAndroidVersion(targetSdk)

    val minSdkAndroidVersion: String
        get() = sdkToAndroidVersion(minSdk)

    val isOutdatedTargetSdk: Boolean
        get() = targetSdk < 29 // Android 10 以下视为过低

    val is32BitOnly: Boolean
        get() = abiCategory == AppAbiCategory.ARM32_ONLY

    val isDormant: Boolean
        get() = lastUpdateTime > 0 && (System.currentTimeMillis() - lastUpdateTime > 365L * 24 * 3600 * 1000)

    val isRecentlyUpdated: Boolean
        get() = lastUpdateTime > 0 && (System.currentTimeMillis() - lastUpdateTime <= 30L * 24 * 3600 * 1000)

    companion object {
        fun sdkToAndroidVersion(sdk: Int): String = when (sdk) {
            36 -> "Android 16"
            35 -> "Android 15"
            34 -> "Android 14"
            33 -> "Android 13"
            32 -> "Android 12L"
            31 -> "Android 12"
            30 -> "Android 11"
            29 -> "Android 10"
            28 -> "Android 9 (Pie)"
            27 -> "Android 8.1 (Oreo)"
            26 -> "Android 8.0 (Oreo)"
            25 -> "Android 7.1 (Nougat)"
            24 -> "Android 7.0 (Nougat)"
            23 -> "Android 6.0 (Marshmallow)"
            22 -> "Android 5.1 (Lollipop)"
            21 -> "Android 5.0 (Lollipop)"
            in 1..20 -> "Android 4.x/早期 (<21)"
            else -> if (sdk > 36) "Android 16+ (API $sdk)" else "未知 (API $sdk)"
        }

        fun resolveSplitDescription(fileName: String, isBase: Boolean): String {
            if (isBase || fileName.equals("base.apk", ignoreCase = true)) {
                return "主程序基包 (Base APK，核心代码与清单)"
            }
            val lower = fileName.lowercase()
            return when {
                lower.contains("arm64") || lower.contains("v8a") -> "64位 CPU 原生代码与动态库切片"
                lower.contains("armeabi") || lower.contains("v7a") -> "32位 CPU 原生代码与动态库切片"
                lower.contains("x86_64") -> "x86_64 原生架构动态库切片"
                lower.contains("x86") -> "x86 原生架构动态库切片"
                lower.contains("xxxhdpi") || lower.contains("xxhdpi") || lower.contains("xhdpi") || lower.contains("hdpi") || lower.contains("mdpi") || lower.contains("ldpi") -> "屏幕像素密度与高清图形资源分片"
                lower.contains(".zh") || lower.contains("zh_") || lower.contains("lang_zh") || lower.contains("locale_zh") -> "中文语言本地化资源分片"
                lower.contains("lang_") || lower.contains("locale_") -> "多国语言本地化资源分片"
                lower.startsWith("split_config.") -> "设备自适应配置分片 (${fileName.removePrefix("split_config.").removeSuffix(".apk")})"
                lower.startsWith("split_") -> "动态功能交付特性分片 (${fileName.removePrefix("split_").removeSuffix(".apk")})"
                else -> "配置/模块分包 (${fileName.removeSuffix(".apk")})"
            }
        }
    }
}

/**
 * Aggregated report statistics computed from scanned apps.
 */
@Immutable
data class AppAnalysisReport(
    val scanTimestamp: Long = System.currentTimeMillis(),
    val allScannedApps: List<AppAnalysisItem> = emptyList(),
    val thirdPartyApps: List<AppAnalysisItem> = emptyList(),
    val systemApps: List<AppAnalysisItem> = emptyList(),
    // Target metrics
    val targetSdkDistribution: Map<Int, Int> = emptyMap(),
    val minSdkDistribution: Map<Int, Int> = emptyMap(),
    val averageTargetSdk: Float = 0f,
    val modernTargetSdkCount: Int = 0, // Target SDK >= 34
    val modernTargetSdkRate: Float = 0f,
    val outdatedTargetSdkCount: Int = 0, // Target SDK < 29
    // ABI metrics
    val abiCategoryDistribution: Map<AppAbiCategory, Int> = emptyMap(),
    val pure32BitAppsCount: Int = 0,
    val pureJavaAppsCount: Int = 0,
    val arm64AppsCount: Int = 0,
    // Security & Package metrics
    val debuggableAppsCount: Int = 0,
    val backupAllowedAppsCount: Int = 0,
    val splitApksCount: Int = 0,
    val singleApksCount: Int = 0,
    // Size & Storage
    val totalStorageBytes: Long = 0L,
    val averageStorageBytes: Long = 0L,
    val largestApps: List<AppAnalysisItem> = emptyList(),
    // Permissions
    val sensitivePermissionCounts: Map<SensitivePermissionCategory, Int> = emptyMap(),
    val permissionHungryApps: List<AppAnalysisItem> = emptyList(),
    // Installer sources
    val installerDistribution: Map<String, Int> = emptyMap(),
    // Freshness
    val dormantAppsCount: Int = 0,
    val recentlyUpdatedAppsCount: Int = 0
)
