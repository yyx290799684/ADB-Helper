package com.yangyx.adbhelper.service

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yangyx.adbhelper.ui.AdbViewModel

class MirrorNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val app = context.applicationContext as? Application ?: return
        val vm = AdbViewModel.getOrCreate(app)

        when (action) {
            MirrorLiveNotificationManager.ACTION_STOP_MIRROR_FROM_NOTIFICATION -> {
                val sessionId = intent.getStringExtra(MirrorLiveNotificationManager.EXTRA_SESSION_ID).orEmpty()
                vm.stopMirroringFromNotification(sessionId)
            }
            MirrorLiveNotificationManager.ACTION_DISMISS_MIRROR_NOTIFICATION -> {
                vm.onLiveMirrorNotificationDismissed()
            }
        }
    }
}
