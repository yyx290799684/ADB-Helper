package com.yangyx.adbhelper.input

import com.yangyx.adbhelper.adb.AdbConnection
import com.yangyx.adbhelper.adb.AdbStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * 虚拟硬件鼠标驱动管理器 (Linux UHID / UINPUT 真实外接鼠标模拟)
 *
 * 通过在被控设备后台启动 /system/bin/hid 或 /system/bin/uinput 守护流，
 * 向 Linux 内核注册标准的 USB HID 鼠标设备。
 * Android 系统的 EventHub & InputReader 检测到具备 INPUT_DEVICE_CLASS_CURSOR
 * 的真实硬件设备后，将立即加载并渲染系统的原生硬件小箭头鼠标光标 (Pointer Sprite)！
 */
class VirtualMouseManager(
    private val connectionProvider: () -> AdbConnection?,
    private val scope: CoroutineScope
) {
    private val _status = MutableStateFlow<VirtualMouseStatus>(VirtualMouseStatus.Disconnected)
    val status: StateFlow<VirtualMouseStatus> = _status.asStateFlow()

    private var activeStream: AdbStream? = null
    private var activeDriver: DriverType? = null
    private var workerJob: Job? = null

    // 当前按键位掩码 (HID: bit 0 = Left, bit 1 = Right, bit 2 = Middle)
    private var currentButtonsMask = 0

    // 移动增量原子累加器与唤醒信号 (零延迟事件驱动合并，极速响应手势输入且不压垮 ADB socket)
    private val pendingDx = AtomicInteger(0)
    private val pendingDy = AtomicInteger(0)
    private val moveSignal = Channel<Unit>(Channel.CONFLATED)

    /**
     * 启动连接并向目标设备注册虚拟硬件鼠标
     */
    fun connect() {
        val current = _status.value
        if (current is VirtualMouseStatus.Connected || current is VirtualMouseStatus.Connecting) {
            return
        }

        val conn = connectionProvider()
        if (conn == null) {
            _status.value = VirtualMouseStatus.Error("ADB 未连接，无法注册虚拟硬件鼠标")
            return
        }

        _status.value = VirtualMouseStatus.Connecting

        scope.launch(Dispatchers.IO) {
            try {
                // 1. 尝试优先使用 Android 系统内置的 /system/bin/hid (基于 /dev/uhid，原生度最高)
                val hidSupported = checkBinary(conn, "hid")
                var connected = false

                if (hidSupported) {
                    connected = tryConnectUhid(conn)
                }

                // 2. 若 hid 不可用或失败，尝试回退到 /system/bin/uinput
                if (!connected) {
                    val uinputSupported = checkBinary(conn, "uinput")
                    if (uinputSupported) {
                        connected = tryConnectUinput(conn)
                    }
                }

                if (!connected) {
                    _status.value = VirtualMouseStatus.Error(
                        "无法向设备注册虚拟硬件鼠标：设备缺少 /system/bin/hid 与 uinput 工具，或 Shell 权限受限。\n可使用触控小白点或 ADB 坐标模拟控制。"
                    )
                }
            } catch (e: Exception) {
                _status.value = VirtualMouseStatus.Error("注册虚拟鼠标异常: ${e.message}")
                disconnect()
            }
        }
    }

    private suspend fun checkBinary(conn: AdbConnection, name: String): Boolean {
        return try {
            val res = conn.executeShell("which $name 2>/dev/null").trim()
            res.isNotEmpty() && !res.contains("not found")
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 通过 /system/bin/hid 注册 UHID 鼠标
     */
    private suspend fun tryConnectUhid(conn: AdbConnection): Boolean {
        return try {
            val stream = conn.openStream("shell:hid -")
            // 标准 3 键带滚轮 USB HID 鼠标报告描述符 (Canonical 52 bytes USB HID Mouse Descriptor)
            val registerJson = """{"id":1,"command":"register","name":"Android Virtual USB Mouse","vid":5824,"pid":1159,"bus":"usb","descriptor":[5,1,9,2,161,1,9,1,161,0,5,9,25,1,41,3,21,0,37,1,149,3,117,1,129,2,149,1,117,5,129,3,5,1,9,48,9,49,9,56,21,129,37,127,117,8,149,3,129,6,192,192]}"""
            
            stream.write((registerJson + "\n").toByteArray(Charsets.UTF_8))
            
            // 等待内核驱动与 EventHub 响应创建
            delay(250)

            if (stream.isClosed) {
                return false
            }

            activeStream = stream
            activeDriver = DriverType.UHID
            currentButtonsMask = 0
            _status.value = VirtualMouseStatus.Connected(
                driver = DriverType.UHID,
                description = "已成功注册原生 UHID 鼠标设备，系统小箭头光标已显现！"
            )

            startMoveDispatcher()
            startStreamWatcher(stream)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 通过 /system/bin/uinput 注册 UINPUT 鼠标
     */
    private suspend fun tryConnectUinput(conn: AdbConnection): Boolean {
        return try {
            val stream = conn.openStream("shell:uinput -")
            val registerJson = """{"id":1,"command":"register","name":"Android Virtual Uinput Mouse","vid":5824,"pid":1159,"bus":"usb","configuration":[{"type":100,"data":[1,2,0]},{"type":101,"data":[272,273,274]},{"type":102,"data":[0,1,8]}]}"""
            
            stream.write((registerJson + "\n").toByteArray(Charsets.UTF_8))
            delay(250)

            if (stream.isClosed) {
                return false
            }

            activeStream = stream
            activeDriver = DriverType.UINPUT
            currentButtonsMask = 0
            _status.value = VirtualMouseStatus.Connected(
                driver = DriverType.UINPUT,
                description = "已成功注册原生 UINPUT 鼠标设备，系统小箭头光标已显现！"
            )

            startMoveDispatcher()
            startStreamWatcher(stream)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 监听 ADB stream 生命周期，防止远端进程异常退出导致悬挂
     */
    private fun startStreamWatcher(stream: AdbStream) {
        scope.launch(Dispatchers.IO) {
            try {
                while (isActive && !stream.isClosed) {
                    val data = stream.read(1000)
                    if (data != null && data.isNotEmpty()) {
                        val text = String(data, Charsets.UTF_8)
                        if (text.contains("error", ignoreCase = true) || text.contains("permission denied", ignoreCase = true)) {
                            _status.value = VirtualMouseStatus.Error("远端驱动报错: $text")
                            break
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                if (_status.value is VirtualMouseStatus.Connected) {
                    _status.value = VirtualMouseStatus.Disconnected
                }
            }
        }
    }

    /**
     * 高性能事件驱动移动调度协程 (零延迟唤醒，自动聚合传输中到来的高频手势)
     */
    private fun startMoveDispatcher() {
        workerJob?.cancel()
        workerJob = scope.launch(Dispatchers.IO) {
            while (isActive && activeStream != null && !activeStream!!.isClosed) {
                // 等待手势输入唤醒信号（有事件立即被唤醒，无须死等 16ms 周期）
                moveSignal.receive()
                val dx = pendingDx.getAndSet(0)
                val dy = pendingDy.getAndSet(0)
                if (dx != 0 || dy != 0) {
                    dispatchMoveRaw(dx, dy)
                }
            }
        }
    }

    /**
     * 发送相对移动增量 (dx, dy)
     */
    fun move(dx: Int, dy: Int) {
        if (_status.value !is VirtualMouseStatus.Connected) return
        pendingDx.addAndGet(dx)
        pendingDy.addAndGet(dy)
        moveSignal.trySend(Unit)
    }

    private fun dispatchMoveRaw(dx: Int, dy: Int) {
        val stream = activeStream ?: return
        val driver = activeDriver ?: return
        if (stream.isClosed) return

        try {
            when (driver) {
                DriverType.UHID -> {
                    // UHID 报告：[buttons, dx, dy, wheel] 字节范围 -127..127
                    val xByte = dx.coerceIn(-127, 127).toByte().toInt() and 0xFF
                    val yByte = dy.coerceIn(-127, 127).toByte().toInt() and 0xFF
                    val json = "{\"id\":1,\"command\":\"report\",\"report\":[$currentButtonsMask,$xByte,$yByte,0]}\n"
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                DriverType.UINPUT -> {
                    // UINPUT: EV_REL(2) REL_X(0) dx, EV_REL(2) REL_Y(1) dy, EV_SYN(0) 0 0
                    val json = "{\"id\":1,\"command\":\"inject\",\"events\":[2,0,$dx,2,1,$dy,0,0,0]}\n"
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * 鼠标左键点击 (按下并弹起)
     */
    fun leftClick() {
        scope.launch(Dispatchers.IO) {
            pressButton(1)
            delay(50)
            releaseButton(1)
        }
    }

    /**
     * 鼠标双击
     */
    fun doubleClick() {
        scope.launch(Dispatchers.IO) {
            pressButton(1)
            delay(50)
            releaseButton(1)
            delay(100)
            pressButton(1)
            delay(50)
            releaseButton(1)
        }
    }

    /**
     * 鼠标右键 (在 Android 中默认映射为返回或快捷菜单)
     */
    fun rightClick() {
        scope.launch(Dispatchers.IO) {
            pressButton(2)
            delay(50)
            releaseButton(2)
        }
    }

    /**
     * 滚轮滚动 (正数为向上滚动，负数为向下滚动)
     */
    fun scroll(delta: Int) {
        val stream = activeStream ?: return
        val driver = activeDriver ?: return
        if (stream.isClosed) return

        scope.launch(Dispatchers.IO) {
            try {
                when (driver) {
                    DriverType.UHID -> {
                        val wByte = delta.coerceIn(-127, 127).toByte().toInt() and 0xFF
                        val json = "{\"id\":1,\"command\":\"report\",\"report\":[$currentButtonsMask,0,0,$wByte]}\n"
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    }
                    DriverType.UINPUT -> {
                        // EV_REL(2) REL_WHEEL(8) delta, EV_SYN(0) 0 0
                        val json = "{\"id\":1,\"command\":\"inject\",\"events\":[2,8,$delta,0,0,0]}\n"
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 按下按键 (button: 1=Left, 2=Right, 4=Middle)
     */
    fun pressButton(button: Int) {
        val stream = activeStream ?: return
        val driver = activeDriver ?: return
        if (stream.isClosed) return

        currentButtonsMask = currentButtonsMask or button

        try {
            when (driver) {
                DriverType.UHID -> {
                    val json = "{\"id\":1,\"command\":\"report\",\"report\":[$currentButtonsMask,0,0,0]}\n"
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                DriverType.UINPUT -> {
                    val code = when (button) {
                        1 -> 272 // BTN_LEFT
                        2 -> 273 // BTN_RIGHT
                        4 -> 274 // BTN_MIDDLE
                        else -> 272
                    }
                    val json = "{\"id\":1,\"command\":\"inject\",\"events\":[1,$code,1,0,0,0]}\n"
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * 释放按键
     */
    fun releaseButton(button: Int) {
        val stream = activeStream ?: return
        val driver = activeDriver ?: return
        if (stream.isClosed) return

        currentButtonsMask = currentButtonsMask and button.inv()

        try {
            when (driver) {
                DriverType.UHID -> {
                    val json = "{\"id\":1,\"command\":\"report\",\"report\":[$currentButtonsMask,0,0,0]}\n"
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                DriverType.UINPUT -> {
                    val code = when (button) {
                        1 -> 272 // BTN_LEFT
                        2 -> 273 // BTN_RIGHT
                        4 -> 274 // BTN_MIDDLE
                        else -> 272
                    }
                    val json = "{\"id\":1,\"command\":\"inject\",\"events\":[1,$code,0,0,0,0]}\n"
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * 断开虚拟硬件鼠标，通知内核销毁设备 (光标自动收起)
     */
    fun disconnect() {
        workerJob?.cancel()
        workerJob = null
        try {
            activeStream?.close()
        } catch (_: Exception) {
        }
        activeStream = null
        activeDriver = null
        currentButtonsMask = 0
        _status.value = VirtualMouseStatus.Disconnected
    }

    fun toggle() {
        if (_status.value is VirtualMouseStatus.Connected || _status.value is VirtualMouseStatus.Connecting) {
            disconnect()
        } else {
            connect()
        }
    }
}

sealed class VirtualMouseStatus {
    object Disconnected : VirtualMouseStatus()
    object Connecting : VirtualMouseStatus()
    data class Connected(
        val driver: DriverType,
        val description: String
    ) : VirtualMouseStatus()
    data class Error(val message: String) : VirtualMouseStatus()
}

enum class DriverType(val displayName: String) {
    UHID("Android UHID (/dev/uhid)"),
    UINPUT("Android UInput (/dev/uinput)")
}
