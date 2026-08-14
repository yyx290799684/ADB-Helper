#!/bin/bash
sed -i 's/modifier = Modifier.height(50.dp)/modifier = Modifier.weight(1f).height(50.dp)/g' /app/applet/app/src/main/java/com/yangyx/adbhelper/ui/screens/ConnectScreen.kt
