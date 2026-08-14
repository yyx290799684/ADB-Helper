#!/bin/bash
sed -i 's/Intent(ACTION_USB_PERMISSION)/Intent(ACTION_USB_PERMISSION).apply { setPackage(context.packageName) }/g' /app/applet/app/src/main/java/com/yangyx/adbhelper/ui/screens/ConnectScreen.kt
sed -i 's/PendingIntent.FLAG_IMMUTABLE/PendingIntent.FLAG_MUTABLE/g' /app/applet/app/src/main/java/com/yangyx/adbhelper/ui/screens/ConnectScreen.kt
