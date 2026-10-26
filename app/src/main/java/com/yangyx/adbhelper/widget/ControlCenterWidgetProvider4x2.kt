package com.yangyx.adbhelper.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * 4x2 ADB 全功能控制中心桌面小部件提供者。
 * 兼容原生 Android AppWidget 与 小米澎湃OS (HyperOS / MIUI) 小部件 API。
 */
class ControlCenterWidgetProvider4x2 : AppWidgetProvider() {

    companion object {
        const val ACTION_MIUI_APPWIDGET_UPDATE = "miui.appwidget.action.APPWIDGET_UPDATE"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val views = WidgetUpdateHelper.build4x2Views(context)
        if (appWidgetIds.isNotEmpty()) {
            try {
                appWidgetManager.updateAppWidget(appWidgetIds, views)
            } catch (_: Exception) {}
        }
        try {
            appWidgetManager.updateAppWidget(ComponentName(context, ControlCenterWidgetProvider4x2::class.java), views)
        } catch (_: Exception) {}
        WidgetUpdateHelper.updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_MIUI_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val cn = ComponentName(context, ControlCenterWidgetProvider4x2::class.java)
            val ids = appWidgetManager.getAppWidgetIds(cn)
            val views = WidgetUpdateHelper.build4x2Views(context)
            if (ids != null && ids.isNotEmpty()) {
                try {
                    appWidgetManager.updateAppWidget(ids, views)
                } catch (_: Exception) {}
            }
            try {
                appWidgetManager.updateAppWidget(cn, views)
            } catch (_: Exception) {}
        }
    }
}
