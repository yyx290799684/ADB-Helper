package com.yangyx.adbhelper.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangyx.adbhelper.adb.RemoteFileItem
import com.yangyx.adbhelper.ui.AdbViewModel
import com.yangyx.adbhelper.ui.components.TextEditorDialog
import com.yangyx.adbhelper.ui.components.isTextFile

private fun isHighRiskSystemDir(path: String): Boolean {
    val clean = path.trim().removeSuffix("/")
    return clean.isEmpty() || clean == "/" || clean == "/data" || clean.startsWith("/data/") ||
            clean == "/system" || clean.startsWith("/system/") ||
            clean == "/vendor" || clean.startsWith("/vendor/") ||
            clean == "/root" || clean.startsWith("/root/") ||
            clean == "/etc" || clean.startsWith("/etc/") ||
            clean == "/sbin" || clean.startsWith("/sbin/") ||
            clean == "/apex" || clean.startsWith("/apex/")
}

@Composable
fun FileExplorerScreen(
    viewModel: AdbViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentPath by viewModel.currentRemotePath.collectAsState()
    val remoteFiles by viewModel.remoteFiles.collectAsState()
    val isLoading by viewModel.isFileLoading.collectAsState()
    val clipboard by viewModel.fileClipboard.collectAsState()

    val sortedFiles by remember(remoteFiles) {
        derivedStateOf {
            remoteFiles.sortedWith(
                compareByDescending<RemoteFileItem> { it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
        }
    }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    var fileToDelete by remember { mutableStateOf<RemoteFileItem?>(null) }
    var fileToRename by remember { mutableStateOf<RemoteFileItem?>(null) }
    var renameNewName by remember { mutableStateOf("") }
    var fileToDownload by remember { mutableStateOf<RemoteFileItem?>(null) }
    var fileToEdit by remember { mutableStateOf<RemoteFileItem?>(null) }
    var fileToPromptOpen by remember { mutableStateOf<RemoteFileItem?>(null) }

    var pendingRiskPath by remember { mutableStateOf<String?>(null) }
    var hasConfirmedRootRiskNotice by remember { mutableStateOf(false) }

    fun requestNavigate(targetPath: String) {
        if (isHighRiskSystemDir(targetPath) && !hasConfirmedRootRiskNotice) {
            pendingRiskPath = targetPath
        } else {
            viewModel.navigateToPath(targetPath)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "uploaded_file.bin"
            viewModel.uploadFile(context, uri, fileName)
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val targetFile = fileToDownload
        if (uri != null && targetFile != null) {
            viewModel.downloadRemoteFile(context, targetFile.path, uri, targetFile.name, targetFile.size)
        }
        fileToDownload = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Path Breadcrumb Navigation Header
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentPath != "/" && currentPath.contains("/")) {
                                val parent = currentPath.substringBeforeLast("/").ifEmpty { "/" }
                                requestNavigate(parent)
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Parent Directory")
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isHighRiskSystemDir(currentPath)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = "Root目录",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = currentPath,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.refreshFiles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Path Shortcuts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = currentPath == "/sdcard",
                    onClick = { requestNavigate("/sdcard") },
                    leadingIcon = { Icon(Icons.Default.SdCard, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    label = { Text("内部存储", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = currentPath == "/sdcard/Download",
                    onClick = { requestNavigate("/sdcard/Download") },
                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    label = { Text("下载目录", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = currentPath == "/data" || currentPath.startsWith("/data/"),
                    onClick = { requestNavigate("/data") },
                    leadingIcon = { Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp)) },
                    label = { Text("应用数据 (Root)", fontSize = 12.sp, color = if (currentPath.startsWith("/data")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
                )
                FilterChip(
                    selected = currentPath == "/",
                    onClick = { requestNavigate("/") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp)) },
                    label = { Text("根目录 / (Root)", fontSize = 12.sp, color = if (currentPath == "/") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
                )
                FilterChip(
                    selected = currentPath == "/system" || currentPath.startsWith("/system/"),
                    onClick = { requestNavigate("/system") },
                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    label = { Text("系统核心", fontSize = 12.sp) }
                )
            }

            // Clipboard Action Banner (if active)
            if (clipboard != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (clipboard?.isCut == true) Icons.Default.ContentCut else Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${if (clipboard?.isCut == true) "剪切" else "复制"}: ${clipboard?.file?.name}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { viewModel.pasteFileClipboard() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("粘贴到此处", fontSize = 12.sp)
                        }
                        IconButton(
                            onClick = { viewModel.clearFileClipboard() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("正在以特权模式扫描/加载文件...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            } else if (remoteFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isHighRiskSystemDir(currentPath)) Icons.Default.Lock else Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isHighRiskSystemDir(currentPath)) "当前系统目录为空或需要被控端授予 Root (su) 权限" else "当前目录为空",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = sortedFiles,
                        key = { "${if (it.isDirectory) "d" else "f"}_${it.path}" },
                        contentType = { if (it.isDirectory) "dir" else "file" }
                    ) { file ->
                        FileItemRow(
                            file = file,
                            onClick = {
                                if (file.isDirectory) {
                                    requestNavigate(file.path)
                                } else if (isTextFile(file.name, file.size)) {
                                    fileToEdit = file
                                } else {
                                    fileToPromptOpen = file
                                }
                            },
                            onOpenTextEditor = {
                                fileToEdit = file
                            },
                            onDownload = {
                                fileToDownload = file
                                saveFileLauncher.launch(file.name)
                            },
                            onRename = {
                                fileToRename = file
                                renameNewName = file.name
                            },
                            onCopy = {
                                viewModel.setFileClipboard(file, isCut = false)
                            },
                            onCut = {
                                viewModel.setFileClipboard(file, isCut = true)
                            },
                            onDelete = {
                                fileToDelete = file
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Buttons for Upload & New Folder
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = { showCreateFolderDialog = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
            }

            FloatingActionButton(
                onClick = { filePickerLauncher.launch("*/*") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = "Upload File")
            }
        }

        // High Risk Root System Directory Warning Dialog
        pendingRiskPath?.let { targetPath ->
            AlertDialog(
                onDismissRequest = { pendingRiskPath = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text("高危系统受限目录访问提示", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                },
                text = {
                    Column {
                        Text(
                            text = "您正在尝试切换到系统核心受限目录：",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = targetPath,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "• 权限说明：此类目录（如 /、/data、/system 等）属于 Android 底层核心与应用私有数据区，需要被控端已获得 Root 权限 (su) 并通过授权。\n• 高危警告：系统目录中的文件对系统正常运转至关重要，重命名、覆盖或误删可能导致应用闪退、无法使用甚至设备变砖，请务必谨慎操作！",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            hasConfirmedRootRiskNotice = true
                            val path = pendingRiskPath
                            pendingRiskPath = null
                            if (path != null) {
                                viewModel.navigateToPath(path)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("以 Root 权限读取", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { pendingRiskPath = null }) {
                        Text("取消返回")
                    }
                }
            )
        }

        // Create Folder Dialog
        if (showCreateFolderDialog) {
            AlertDialog(
                onDismissRequest = { showCreateFolderDialog = false },
                title = { Text("新建文件夹") },
                text = {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("文件夹名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createRemoteFolder(newFolderName)
                            newFolderName = ""
                        }
                        showCreateFolderDialog = false
                    }) {
                        Text("创建")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateFolderDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        fileToDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { fileToDelete = null },
                title = { Text("确认删除") },
                text = {
                    Text(
                        text = "确定要删除 ${if (target.isDirectory) "文件夹" else "文件"}【${target.name}】吗？\n\n此操作不可撤销，请谨慎操作。",
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteRemotePath(target.path)
                            fileToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("确定删除")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { fileToDelete = null }) {
                        Text("取消")
                    }
                }
            )
        }

        // Rename Dialog
        fileToRename?.let { target ->
            AlertDialog(
                onDismissRequest = { fileToRename = null },
                title = { Text("重命名") },
                text = {
                    OutlinedTextField(
                        value = renameNewName,
                        onValueChange = { renameNewName = it },
                        label = { Text("新名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (renameNewName.isNotBlank() && renameNewName != target.name) {
                            viewModel.renameRemotePath(target.path, renameNewName)
                        }
                        fileToRename = null
                    }) {
                        Text("确认")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { fileToRename = null }) {
                        Text("取消")
                    }
                }
            )
        }

        // Text Editor Dialog (Preview & Edit text files directly)
        fileToEdit?.let { target ->
            TextEditorDialog(
                file = target,
                viewModel = viewModel,
                onDismiss = { fileToEdit = null }
            )
        }

        // Binary / Large File Open Confirmation Dialog
        fileToPromptOpen?.let { target ->
            AlertDialog(
                onDismissRequest = { fileToPromptOpen = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = { Text("打开文件", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "【${target.name}】(${formatFileSize(target.size)}) 可能包含二进制或非文本格式数据。您可以尝试以文本方式打开预览，或者将其下载到本地查看。",
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val file = target
                        fileToPromptOpen = null
                        fileToEdit = file
                    }) {
                        Text("以纯文本预览")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        val file = target
                        fileToPromptOpen = null
                        fileToDownload = file
                        saveFileLauncher.launch(file.name)
                    }) {
                        Text("下载到本地")
                    }
                }
            )
        }
    }
}

@Composable
fun FileItemRow(
    file: RemoteFileItem,
    onClick: () -> Unit,
    onOpenTextEditor: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isText = remember(file.name, file.size) { isTextFile(file.name, file.size) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (file.isDirectory) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else if (isText) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (file.isDirectory) Icons.Default.Folder
                        else if (isText) Icons.Default.Description
                        else Icons.Default.InsertDriveFile,
                        contentDescription = "File Icon",
                        tint = if (file.isDirectory) MaterialTheme.colorScheme.primary
                        else if (isText) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!file.isDirectory) {
                    val sizeFormatted = formatFileSize(file.size)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sizeFormatted,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isText) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "纯文本",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // More Options Dropdown Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More actions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (showMenu) {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (!file.isDirectory) {
                            DropdownMenuItem(
                                text = { Text("查看/编辑文本") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onOpenTextEditor()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("下载到本地") },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onDownload()
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("重命名") },
                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onRename()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("复制") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onCopy()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("剪切 (移动)") },
                            leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onCut()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    return when {
        bytes >= 1024L * 1024L * 1024L -> String.format(java.util.Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

