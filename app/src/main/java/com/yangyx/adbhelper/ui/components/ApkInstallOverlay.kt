package com.yangyx.adbhelper.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yangyx.adbhelper.ui.AdbViewModel
import com.yangyx.adbhelper.ui.InstallPhase
import kotlin.math.roundToInt

@Composable
fun ApkInstallOverlay(
    viewModel: AdbViewModel,
    modifier: Modifier = Modifier
) {
    val installTask by viewModel.currentInstallTask.collectAsState()
    val task = installTask ?: return

    if (!task.isMinimized) {
        // Full Floating Dialog
        Dialog(
            onDismissRequest = {
                // If finished, close; if in progress, minimize instead of dismissing
                if (task.isFinished) {
                    viewModel.dismissInstallTask()
                } else {
                    viewModel.minimizeInstallTask(true)
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        when (task.phase) {
                                            InstallPhase.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                                            InstallPhase.FAILED, InstallPhase.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                                            else -> MaterialTheme.colorScheme.secondaryContainer
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (task.phase) {
                                        InstallPhase.SUCCESS -> Icons.Default.CheckCircle
                                        InstallPhase.FAILED, InstallPhase.CANCELLED -> Icons.Default.Error
                                        InstallPhase.INSTALLING -> Icons.Default.Android
                                        else -> Icons.Default.Upload
                                    },
                                    contentDescription = null,
                                    tint = when (task.phase) {
                                        InstallPhase.SUCCESS -> MaterialTheme.colorScheme.primary
                                        InstallPhase.FAILED, InstallPhase.CANCELLED -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.secondary
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (task.isFinished) {
                                        when (task.phase) {
                                            InstallPhase.SUCCESS -> "安装完成"
                                            InstallPhase.CANCELLED -> "已取消安装"
                                            else -> "安装失败"
                                        }
                                    } else {
                                        when (task.phase) {
                                            InstallPhase.INSTALLING -> "正在远端安装..."
                                            InstallPhase.TRANSFERRING -> "正在推送 APK..."
                                            else -> "准备推送中..."
                                        }
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = task.appName,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (!task.isFinished) {
                            IconButton(
                                onClick = { viewModel.minimizeInstallTask(true) }
                            ) {
                                Icon(
                                    Icons.Default.VisibilityOff,
                                    contentDescription = "后台运行/最小化",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { viewModel.dismissInstallTask() }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Progress Section
                    if (!task.isFinished) {
                        if (task.phase == InstallPhase.INSTALLING) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        } else {
                            LinearProgressIndicator(
                                progress = { task.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (task.phase == InstallPhase.TRANSFERRING) {
                                    "${(task.progress * 100).toInt()}%"
                                } else {
                                    "对端包管理器处理中"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (task.phase == InstallPhase.TRANSFERRING && task.speedMbPerSec > 0.05f) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Speed,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${String.format("%.1f", task.speedMbPerSec)} MB/s",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status detail box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = task.statusText,
                                fontSize = 13.sp,
                                color = if (task.phase == InstallPhase.FAILED) {
                                    MaterialTheme.colorScheme.error
                                } else if (task.phase == InstallPhase.SUCCESS) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (task.fileName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "文件: ${task.fileName}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!task.isFinished) {
                            OutlinedButton(
                                onClick = { viewModel.cancelInstallTask() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("取消推送")
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            FilledTonalButton(
                                onClick = { viewModel.minimizeInstallTask(true) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("后台静默进行")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.dismissInstallTask() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("知道了")
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Minimized Floating Pill/Badge
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Surface(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .clickable { viewModel.minimizeInstallTask(false) }
                    .shadow(8.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = when (task.phase) {
                    InstallPhase.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                    InstallPhase.FAILED, InstallPhase.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!task.isFinished) {
                        if (task.phase == InstallPhase.INSTALLING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            CircularProgressIndicator(
                                progress = { task.progress },
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (task.phase == InstallPhase.SUCCESS) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (task.phase == InstallPhase.SUCCESS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (task.isFinished) {
                            if (task.phase == InstallPhase.SUCCESS) "已安装 ${task.appName}" else "安装失败"
                        } else {
                            if (task.phase == InstallPhase.INSTALLING) "正在安装 ${task.appName}" else "${(task.progress * 100).toInt()}% ${task.appName}"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        Icons.Default.OpenInFull,
                        contentDescription = "展开详情",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
