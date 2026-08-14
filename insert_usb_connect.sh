#!/bin/bash
awk '
/fun disconnect/ {
    print "    fun connectUsb(usbConnection: android.hardware.usb.UsbDeviceConnection, usbIn: android.hardware.usb.UsbEndpoint, usbOut: android.hardware.usb.UsbEndpoint) {"
    print "        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {"
    print "            _connectionState.value = ConnectionState.Connecting(\"正在通过 USB 连接...\")"
    print "            try {"
    print "                val conn = com.yangyx.adbhelper.adb.AdbConnection(usbConnection, usbIn, usbOut, getApplication())"
    print "                conn.connect(10000) { status ->"
    print "                    _connectionState.value = ConnectionState.Connecting(status)"
    print "                }"
    print "                activeConnection = conn"
    print "                scrcpyController = com.yangyx.adbhelper.scrcpy.ScrcpyController(conn, viewModelScope, getApplication())"
    print ""
    print "                val model = conn.executeShell(\"getprop ro.product.model\").trim()"
    print "                val brand = conn.executeShell(\"getprop ro.product.brand\").trim()"
    print "                val deviceName = if (model.isNotEmpty()) \"$brand $model\" else conn.deviceBanner"
    print ""
    print "                _connectionState.value = ConnectionState.Connected(\"USB OTG 设备\", deviceName)"
    print "                fetchDeviceInfo()"
    print "            } catch (e: Exception) {"
    print "                _connectionState.value = ConnectionState.Error(e.message ?: \"Unknown error\")"
    print "                usbConnection.close()"
    print "            }"
    print "        }"
    print "    }"
    print ""
}
{ print }
' /app/applet/app/src/main/java/com/yangyx/adbhelper/ui/AdbViewModel.kt > /tmp/AdbViewModel.kt
mv /tmp/AdbViewModel.kt /app/applet/app/src/main/java/com/yangyx/adbhelper/ui/AdbViewModel.kt
