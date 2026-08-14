#!/bin/bash
sed -i 's/            s.soTimeout = 10000/            s.soTimeout = 10000\n            this.socket = s\n            this.input = s.getInputStream()\n            this.output = s.getOutputStream()/g' /app/applet/app/src/main/java/com/yangyx/adbhelper/adb/AdbConnection.kt
