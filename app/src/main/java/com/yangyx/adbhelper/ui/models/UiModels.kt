package com.yangyx.adbhelper.ui.models

import androidx.compose.runtime.Immutable

@Immutable
data class SystemInfo(
    val model: String = "Unknown",
    val manufacturer: String = "Unknown",
    val androidVersion: String = "Unknown",
    val sdkVersion: Int = 0,
    val cpuUsage: Float = 0f,
    val cpuArchitecture: String = "aarch64",
    val ramTotalMb: Long = 0,
    val ramUsedMb: Long = 0,
    val batteryLevel: Int = 0,
    val batteryTemperature: Float = 0f,
    val batteryStatus: String = "Unknown",
    val storageTotalGb: Float = 0f,
    val storageUsedGb: Float = 0f,
    val marketName: String? = null,
    val osVersion: String? = null,
    val cpuHardware: String? = null,
    val socModel: String? = null,
    val serialNo: String? = null
)

@Immutable
data class GroupedDevice(
    val key: String,
    val serialNo: String,
    val deviceName: String,
    val model: String,
    val aliasName: String = "",
    val iconType: String = "phone",
    val lastConnectedTime: Long,
    val ipRecords: List<com.yangyx.adbhelper.data.entity.DeviceEntity>
) {
    val displayName: String
        get() = if (aliasName.isNotBlank()) aliasName else deviceName
}

@Immutable
data class RemoteAppItem(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val apkPath: String = "",
    val splitApkPaths: List<String> = emptyList(),
    val versionName: String = "",
    val versionCode: Long = 0L
)

@Immutable
data class RemoteProcessItem(
    val pid: Int,
    val user: String,
    val cpuUsage: String,
    val memUsage: String,
    val name: String,
    val appTitle: String = "",
    val packageName: String = "",
    val isUserApp: Boolean = true,
    val isSystemApp: Boolean = false
)

@Immutable
data class LocalAppItem(
    val packageName: String,
    val appName: String,
    val versionName: String = "",
    val versionCode: Long = 0L,
    val icon: android.graphics.drawable.Drawable? = null,
    val sourceDir: String,
    val splitSourceDirs: List<String> = emptyList(),
    val totalSizeBytes: Long = 0L,
    val isSystemApp: Boolean = false,
    val isSingleApk: Boolean = true
)

enum class RebootOption(
    val title: String,
    val shortName: String,
    val adbArg: String,
    val description: String,
    val warningText: String,
    val isSevere: Boolean
) {
    NORMAL(
        title = "正常重启系统 (adb reboot)",
        shortName = "正常重启",
        adbArg = "",
        description = "重新启动 Android 操作系统并重新加载所有服务。",
        warningText = "确定要重启目标设备吗？重启过程中 ADB 连接将会断开，设备通常需要 30~60 秒重新开机。",
        isSevere = false
    ),
    FASTBOOT(
        title = "重启至 Fastboot 模式 (adb reboot fastboot)",
        shortName = "Fastboot 模式",
        adbArg = "fastboot",
        description = "进入 Fastboot / Userspace 引导线刷模式，用于线刷分区镜像。",
        warningText = "⚠️ 高危警告：目标设备将进入 Fastboot 模式。在此模式下普通无线/USB ADB 将无法继续交互，需要电脑 Fastboot 工具或长按电源键强制退出。确定要执行吗？",
        isSevere = true
    ),
    BOOTLOADER(
        title = "重启至 Bootloader 模式 (adb reboot bootloader)",
        shortName = "Bootloader 模式",
        adbArg = "bootloader",
        description = "进入 Bootloader 引导程序主界面，用于解锁 BL、线刷底包等。",
        warningText = "⚠️ 高危警告：目标设备将进入 Bootloader 模式。当前 ADB 会话将立即终止。确定要继续吗？",
        isSevere = true
    ),
    RECOVERY(
        title = "重启至 Recovery 恢复模式 (adb reboot recovery)",
        shortName = "Recovery 模式",
        adbArg = "recovery",
        description = "进入 Recovery 恢复模式，用于系统双清重置、卡刷更新包或系统救援。",
        warningText = "⚠️ 高危警告：目标设备将进入 Recovery 恢复模式。设备将退出正常系统界面，当前 ADB 远程连接将断开。确定要继续吗？",
        isSevere = true
    ),
    POWER_OFF(
        title = "设备关机 (adb reboot -p)",
        shortName = "关闭电源",
        adbArg = "poweroff",
        description = "安全关闭目标设备电源。",
        warningText = "⚠️ 警告：目标设备将直接关机。关机后无法通过网络远程唤醒，必须手动按下手机物理电源键开机。确定要关机吗？",
        isSevere = true
    )
}



