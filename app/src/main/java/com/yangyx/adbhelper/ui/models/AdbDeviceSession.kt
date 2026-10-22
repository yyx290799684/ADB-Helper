package com.yangyx.adbhelper.ui.models

import com.yangyx.adbhelper.adb.AdbConnection
import com.yangyx.adbhelper.adb.RemoteFileItem
import com.yangyx.adbhelper.scrcpy.ScrcpyController
import java.util.UUID

/**
 * 表示一个独立的 ADB 设备连接会话。
 * 每个会话包含独立的 ADB 底层连接、Scrcpy 投屏控制器、终端上下文、文件列表、应用及系统状态。
 */
data class AdbDeviceSession(
    val id: String = UUID.randomUUID().toString(),
    val ip: String,
    val port: Int = 5555,
    var deviceName: String,
    var serialNo: String = "",
    var model: String = "",
    var iconType: String = "phone",
    val connection: AdbConnection,
    val scrcpyController: ScrcpyController,
    var selectedNavIndex: Int = 0,
    val createdTime: Long = System.currentTimeMillis(),

    // 独立的终端、文件、系统、应用状态
    var terminalOutput: String = "",
    var terminalPath: String = "/",
    var isRootMode: Boolean = false,
    var terminalHistory: List<String> = emptyList(),
    var currentRemotePath: String = "/sdcard",
    var remoteFiles: List<RemoteFileItem> = emptyList(),
    var systemInfo: SystemInfo = SystemInfo(),
    var deviceDensity: DeviceDensityInfo = DeviceDensityInfo(),
    var quickSettings: DeviceQuickSettingsState = DeviceQuickSettingsState(),
    var mediaPlaybackInfo: MediaPlaybackInfo = MediaPlaybackInfo(),
    var mediaVolume: Int = 7,
    var maxMediaVolume: Int = 15,
    var cachedStaticInfo: SystemInfo? = null,
    var installedApps: List<RemoteAppItem> = emptyList(),
    var appDiagnosisLogs: String = "",
    var runningProcesses: List<RemoteProcessItem> = emptyList(),
    var backgroundTimestamp: Long = 0L,
    var autoResumeMirroringOnSwitch: Boolean = false
) {
    val isUsb: Boolean get() = ip.startsWith("USB")
    val displayName: String get() = deviceName
}
