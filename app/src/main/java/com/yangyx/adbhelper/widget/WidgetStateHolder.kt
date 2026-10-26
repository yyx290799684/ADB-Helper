package com.yangyx.adbhelper.widget

import com.yangyx.adbhelper.adb.AdbConnection
import com.yangyx.adbhelper.ui.models.AdbDeviceSession
import java.lang.ref.WeakReference

data class WidgetDeviceInfo(
    val name: String,
    val ip: String,
    val port: Int = 5555,
    val serialNo: String = "",
    val isConnected: Boolean = false,
    val lastConnectedTime: Long = 0L,
    val isFavorite: Boolean = false,
    val ipCount: Int = 1
)

/**
 * 集中管理桌面小部件状态与当前活跃 ADB 会话引用的单例持有类。
 * 供小部件提供者 (AppWidgetProvider) 与动作接收器 (WidgetActionReceiver) 快速读取与交互。
 */
object WidgetStateHolder {

    enum class State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    enum class ScanState {
        IDLE,
        SCANNING,
        FINISHED
    }

    @Volatile
    var currentState: State = State.DISCONNECTED
        private set

    @Volatile
    var scanState: ScanState = ScanState.IDLE
        private set

    @Volatile
    var scanProgressText: String = ""
        private set

    @Volatile
    var scannedDevices: List<WidgetDeviceInfo> = emptyList()

    @Volatile
    var deviceName: String = ""
        private set

    @Volatile
    var deviceIp: String = ""
        private set

    @Volatile
    var connectedDeviceCount: Int = 0
        private set

    @Volatile
    var recentDevices: List<WidgetDeviceInfo> = emptyList()

    private var activeSessionRef: WeakReference<AdbDeviceSession>? = null

    val activeSession: AdbDeviceSession?
        get() = activeSessionRef?.get()

    val activeConnection: AdbConnection?
        get() = activeSession?.connection

    fun onConnected(session: AdbDeviceSession?, name: String, ip: String, count: Int = 1) {
        currentState = State.CONNECTED
        deviceName = name
        deviceIp = ip
        connectedDeviceCount = count
        activeSessionRef = session?.let { WeakReference(it) }
    }

    fun onConnecting(name: String = "", ip: String = "") {
        currentState = State.CONNECTING
        if (name.isNotBlank()) deviceName = name
        if (ip.isNotBlank()) deviceIp = ip
    }

    fun onDisconnected() {
        currentState = State.DISCONNECTED
        connectedDeviceCount = 0
        activeSessionRef = null
    }

    fun setFallbackDeviceInfo(name: String, ip: String) {
        if (currentState == State.DISCONNECTED) {
            deviceName = name
            deviceIp = ip
        }
    }

    fun onScanStarted() {
        scanState = ScanState.SCANNING
        scanProgressText = "正在扫描局域网..."
    }

    fun onScanFinished(devices: List<WidgetDeviceInfo>, progressText: String) {
        scanState = ScanState.FINISHED
        scannedDevices = devices
        scanProgressText = progressText
    }

    fun resetScanState() {
        scanState = ScanState.IDLE
        scanProgressText = ""
        scannedDevices = emptyList()
    }
}
