#!/bin/bash
sed -i 's/                fetchDeviceInfo()/                refreshFiles()\n                refreshSystemInfo()\n                refreshApps()\n                refreshProcesses()/g' /app/applet/app/src/main/java/com/yangyx/adbhelper/ui/AdbViewModel.kt
