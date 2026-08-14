#!/bin/bash
sed -i 's/                            Text("OTG直连", fontWeight = FontWeight.Bold)/                            Text("OTG直连", fontWeight = FontWeight.Bold)\n                            }/g' /app/applet/app/src/main/java/com/yangyx/adbhelper/ui/screens/ConnectScreen.kt
sed -i '309,310d' /app/applet/app/src/main/java/com/yangyx/adbhelper/ui/screens/ConnectScreen.kt
