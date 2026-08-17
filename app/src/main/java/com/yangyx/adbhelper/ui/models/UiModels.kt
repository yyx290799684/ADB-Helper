package com.yangyx.adbhelper.ui.models

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
    val storageUsedGb: Float = 0f
)

data class RemoteAppItem(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false
)

data class RemoteProcessItem(
    val pid: Int,
    val user: String,
    val cpuUsage: String,
    val memUsage: String,
    val name: String
)

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

