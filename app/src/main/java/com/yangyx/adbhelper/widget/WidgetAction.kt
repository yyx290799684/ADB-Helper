package com.yangyx.adbhelper.widget

import android.content.Context
import com.yangyx.adbhelper.R

/**
 * 小部件支持的快捷操作动作类型枚举定义
 */
enum class WidgetActionType(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconResId: Int,
    val description: String
) {
    LOCK("LOCK", "锁屏", "关屏休眠", R.drawable.ic_widget_power, "发送电源/锁屏键"),
    SCREENSHOT("SCREENSHOT", "截屏", "捕获画面", R.drawable.ic_widget_screenshot, "远端截屏并保存相册"),
    MIRROR("MIRROR", "投屏", "启动镜像", R.drawable.ic_widget_screen, "启动Scrcpy高清屏幕投屏"),
    HOME("HOME", "主页", "桌面直达", R.drawable.ic_widget_home, "发送返回桌面Home键"),
    MUTE("MUTE", "静音", "静音切换", R.drawable.ic_widget_mute, "远端一键静音切换"),
    BACK("BACK", "返回", "返回上级", R.drawable.ic_widget_back, "发送返回Back键"),
    RECENTS("RECENTS", "多任务", "后台切换", R.drawable.ic_widget_recents, "呼出多任务后台"),
    NOTIFICATIONS("NOTIFICATIONS", "通知", "下拉通知", R.drawable.ic_widget_notification, "展开下拉系统通知栏"),
    FILE_EXPLORER("FILE_EXPLORER", "文件", "浏览传输", R.drawable.ic_widget_folder, "进入App文件管理器"),
    TERMINAL("TERMINAL", "终端", "命令行", R.drawable.ic_widget_terminal, "进入App终端命令行"),
    VOLUME_DOWN("VOLUME_DOWN", "音量-", "减小音量", R.drawable.ic_widget_volume_down, "降低远端媒体音量"),
    VOLUME_UP("VOLUME_UP", "音量+", "增大音量", R.drawable.ic_widget_volume_up, "提高远端媒体音量"),
    SCREEN_OFF("SCREEN_OFF", "息屏", "关闭屏幕", R.drawable.ic_widget_power, "熄灭远端屏幕"),
    WAKEUP("WAKEUP", "唤醒", "点亮屏幕", R.drawable.ic_widget_bolt, "点亮唤醒远端屏幕");

    companion object {
        fun fromId(id: String?, default: WidgetActionType = LOCK): WidgetActionType {
            if (id.isNullOrBlank()) return default
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: default
        }
    }
}

/**
 * 小部件快捷按键配置持久化管理类
 */
object WidgetPreferences {
    private const val PREFS_NAME = "adb_widget_action_preferences"

    private const val KEY_4X2_SLOT_1 = "widget_4x2_slot_1"
    private const val KEY_4X2_SLOT_2 = "widget_4x2_slot_2"
    private const val KEY_4X2_SLOT_3 = "widget_4x2_slot_3"
    private const val KEY_4X2_SLOT_4 = "widget_4x2_slot_4"

    private const val KEY_2X2_SLOT_1 = "widget_2x2_slot_1"
    private const val KEY_2X2_SLOT_2 = "widget_2x2_slot_2"

    fun get4x2Actions(context: Context): List<WidgetActionType> {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return listOf(
            WidgetActionType.fromId(sp.getString(KEY_4X2_SLOT_1, WidgetActionType.LOCK.id), WidgetActionType.LOCK),
            WidgetActionType.fromId(sp.getString(KEY_4X2_SLOT_2, WidgetActionType.SCREENSHOT.id), WidgetActionType.SCREENSHOT),
            WidgetActionType.fromId(sp.getString(KEY_4X2_SLOT_3, WidgetActionType.MIRROR.id), WidgetActionType.MIRROR),
            WidgetActionType.fromId(sp.getString(KEY_4X2_SLOT_4, WidgetActionType.HOME.id), WidgetActionType.HOME)
        )
    }

    fun set4x2Action(context: Context, slotIndex: Int, action: WidgetActionType) {
        val key = when (slotIndex) {
            0 -> KEY_4X2_SLOT_1
            1 -> KEY_4X2_SLOT_2
            2 -> KEY_4X2_SLOT_3
            else -> KEY_4X2_SLOT_4
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key, action.id)
            .apply()
    }

    fun get2x2Actions(context: Context): List<WidgetActionType> {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return listOf(
            WidgetActionType.fromId(sp.getString(KEY_2X2_SLOT_1, WidgetActionType.LOCK.id), WidgetActionType.LOCK),
            WidgetActionType.fromId(sp.getString(KEY_2X2_SLOT_2, WidgetActionType.SCREENSHOT.id), WidgetActionType.SCREENSHOT)
        )
    }

    fun set2x2Action(context: Context, slotIndex: Int, action: WidgetActionType) {
        val key = if (slotIndex == 0) KEY_2X2_SLOT_1 else KEY_2X2_SLOT_2
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key, action.id)
            .apply()
    }

    fun resetToDefaults(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
