package com.yangyx.adbhelper.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yangyx.adbhelper.ui.AdbViewModel

/**
 * 兼容性转发：设备主界面已整合为包含【信息】、【控制】与【输入】三大选项卡的 DeviceScreen。
 */
@Composable
fun DeviceInfoScreen(
    viewModel: AdbViewModel,
    modifier: Modifier = Modifier,
    isGlassMode: Boolean = false
) {
    DeviceScreen(viewModel = viewModel, modifier = modifier, isGlassMode = isGlassMode)
}
