#!/bin/bash
sed -i 's/PendingIntent.FLAG_MUTABLE/PendingIntent.FLAG_IMMUTABLE/g' /app/applet/app/src/main/java/com/yangyx/adbhelper/ui/screens/ConnectScreen.kt

awk '
/                        Button\(/ {
    if (!button1_found) {
        button1_found = 1
    } else if (!button2_found) {
        button2_found = 1
        print "                    }"
        print "                    Spacer(modifier = Modifier.height(12.dp))"
        print "                    Row("
        print "                        modifier = Modifier.fillMaxWidth(),"
        print "                        horizontalArrangement = Arrangement.spacedBy(12.dp)"
        print "                    ) {"
        print "                        Button("
        next
    }
}
{ print }
' /app/applet/app/src/main/java/com/yangyx/adbhelper/ui/screens/ConnectScreen.kt > /tmp/ConnectScreen.kt
mv /tmp/ConnectScreen.kt /app/applet/app/src/main/java/com/yangyx/adbhelper/ui/screens/ConnectScreen.kt
