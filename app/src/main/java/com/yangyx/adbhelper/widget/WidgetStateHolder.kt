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

    const val SCAN_EXPIRATION_MILLIS = 10 * 60 * 1000L // 10分钟扫描有效期

    @Volatile
    var lastScanTimestamp: Long = 0L
        private set

    @Volatile
    var lastConnectedCheckTimestamp: Long = 0L
        private set

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
        lastConnectedCheckTimestamp = System.currentTimeMillis()
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
        lastConnectedCheckTimestamp = 0L
    }

    fun setFallbackDeviceInfo(name: String, ip: String) {
        if (currentState == State.DISCONNECTED) {
            deviceName = name
            deviceIp = ip
        }
    }

    fun clearFallbackDeviceInfo() {
        if (currentState == State.DISCONNECTED) {
            deviceName = ""
            deviceIp = ""
        }
    }

    fun removeDeletedDevice(serialNo: String, ips: Set<String>) {
        recentDevices = recentDevices.filterNot { d ->
            (serialNo.isNotBlank() && d.serialNo == serialNo) || ips.contains(d.ip)
        }
        scannedDevices = scannedDevices.filterNot { d ->
            (serialNo.isNotBlank() && d.serialNo == serialNo) || ips.contains(d.ip)
        }
        if (currentState == State.DISCONNECTED) {
            val first = recentDevices.firstOrNull()
            if (first != null) {
                deviceName = first.name
                deviceIp = "${first.ip}:${first.port}"
            } else {
                deviceName = ""
                deviceIp = ""
            }
        }
    }

    fun onScanStarted() {
        scanState = ScanState.SCANNING
        scanProgressText = "正在扫描局域网..."
    }

    fun onScanFinished(devices: List<WidgetDeviceInfo>, progressText: String, timestamp: Long = System.currentTimeMillis()) {
        scanState = ScanState.FINISHED
        scannedDevices = devices
        scanProgressText = progressText
        lastScanTimestamp = timestamp
    }

    fun isScanExpired(): Boolean {
        if (scanState == ScanState.IDLE) return false
        if (lastScanTimestamp <= 0L) return false
        return (System.currentTimeMillis() - lastScanTimestamp) >= SCAN_EXPIRATION_MILLIS
    }

    fun checkAndExpireScan(): Boolean {
        if (isScanExpired()) {
            resetScanState()
            return true
        }
        return false
    }

    fun resetScanState() {
        scanState = ScanState.IDLE
        scanProgressText = ""
        scannedDevices = emptyList()
        lastScanTimestamp = 0L
    }
}
