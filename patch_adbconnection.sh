#!/bin/bash
sed -i 's/    private var socket: Socket? = null/    var isUsb = false\n    var usbConnection: android.hardware.usb.UsbDeviceConnection? = null\n    private var socket: Socket? = null/g' /app/applet/app/src/main/java/com/yangyx/adbhelper/adb/AdbConnection.kt

sed -i '/    private var socket: Socket? = null/a\
    constructor(\
        usbConnection: android.hardware.usb.UsbDeviceConnection,\
        usbIn: android.hardware.usb.UsbEndpoint,\
        usbOut: android.hardware.usb.UsbEndpoint,\
        context: android.content.Context\
    ) : this("USB", 0, context) {\
        this.isUsb = true\
        this.usbConnection = usbConnection\
        this.input = UsbAdbInputStream(usbConnection, usbIn)\
        this.output = UsbAdbOutputStream(usbConnection, usbOut)\
    }' /app/applet/app/src/main/java/com/yangyx/adbhelper/adb/AdbConnection.kt

sed -i 's/        val s = Socket()/        if (!isUsb) {\n            val s = Socket()\n            s.connect(java.net.InetSocketAddress(ip, port), timeoutMs)\n            s.tcpNoDelay = true\n            s.soTimeout = 10000\n            this.socket = s\n            this.input = s.getInputStream()\n            this.output = s.getOutputStream()\n        }/g' /app/applet/app/src/main/java/com/yangyx/adbhelper/adb/AdbConnection.kt

sed -i '/        s.connect(InetSocketAddress(ip, port), timeoutMs)/d' /app/applet/app/src/main/java/com/yangyx/adbhelper/adb/AdbConnection.kt
sed -i '/        s.tcpNoDelay = true \/\/ optimize for low latency/d' /app/applet/app/src/main/java/com/yangyx/adbhelper/adb/AdbConnection.kt
sed -i '/        s.soTimeout = 10000 \/\/ 10s read timeout during initial handshake/d' /app/applet/app/src/main/java/com/yangyx/adbhelper/adb/AdbConnection.kt
sed -i '/        this.socket = s/d' /app/applet/app/src/main/java/com/yangyx/adbhelper/adb/AdbConnection.kt
sed -i '/        this.input = s.getInputStream()/d' /app/applet/app/src/main/java/com/yangyx/adbhelper/adb/AdbConnection.kt
sed -i '/        this.output = s.getOutputStream()/d' /app/applet/app/src/main/java/com/yangyx/adbhelper/adb/AdbConnection.kt

sed -i 's/                            s.soTimeout = 60000/                            socket?.soTimeout = 60000/g' /app/applet/app/src/main/java/com/yangyx/adbhelper/adb/AdbConnection.kt
sed -i 's/                        s.soTimeout = 60000/                        socket?.soTimeout = 60000/g' /app/applet/app/src/main/java/com/yangyx/adbhelper/adb/AdbConnection.kt
sed -i 's/        \/\/ Reset soTimeout to 0/        socket?.soTimeout = 0\n        \/\/ Reset soTimeout to 0/g' /app/applet/app/src/main/java/com/yangyx/adbhelper/adb/AdbConnection.kt
sed -i 's/        s.soTimeout = 0//g' /app/applet/app/src/main/java/com/yangyx/adbhelper/adb/AdbConnection.kt
