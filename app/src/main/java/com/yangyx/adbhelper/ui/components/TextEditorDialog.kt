package com.yangyx.adbhelper.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yangyx.adbhelper.adb.RemoteFileItem
import com.yangyx.adbhelper.ui.AdbViewModel
import kotlinx.coroutines.launch

fun isTextFile(fileName: String, fileSize: Long = 0L): Boolean {
    val lower = fileName.lowercase()
    val ext = lower.substringAfterLast('.', "")
    val textExtensions = setOf(
        "txt", "log", "json", "xml", "prop", "properties", "sh", "rc", "conf", "cfg", "ini",
        "yaml", "yml", "md", "csv", "tsv", "html", "htm", "css", "js", "ts", "kt", "java",
        "py", "c", "cpp", "h", "sql", "gradle", "bat", "cmd", "env", "toml", "lock", "svg"
    )
    if (ext in textExtensions) return true
    val textNames = setOf(
        "build.prop", "default.prop", "hosts", "resolv.conf", "fstab", "init.rc", "ueventd.rc",
        "license", "readme", "notice", "authors", "manifest", "version"
    )
    if (lower in textNames) return true
    // If size is under 512KB and has no extension, assume it can be opened as text
    return ext.isEmpty() && fileSize in 1..524_288
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorDialog(
    file: RemoteFileItem,
    viewModel: AdbViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var content by remember { mutableStateOf("") }
    var originalContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }

    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    val hasUnsavedChanges by remember(content, originalContent) {
        derivedStateOf { content != originalContent }
    }

    val lines by remember(content) {
        derivedStateOf { content.lines() }
    }

    val lineCount by remember(lines) {
        derivedStateOf { lines.size.coerceAtLeast(1) }
    }

    val matches by remember(content, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) emptyList()
            else {
                val list = mutableListOf<Int>()
                var idx = content.indexOf(searchQuery, ignoreCase = true)
                while (idx != -1) {
                    list.add(idx)
                    idx = content.indexOf(searchQuery, idx + searchQuery.length, ignoreCase = true)
                }
                list
            }
        }
    }

    fun loadFile() {
        isLoading = true
        errorMessage = null
        viewModel.readRemoteTextFile(file.path) { result ->
            isLoading = false
            result.onSuccess { text ->
                content = text
                originalContent = text
            }.onFailure { err ->
                errorMessage = err.message ?: "读取文件失败"
            }
        }
    }

    LaunchedEffect(file.path) {
        loadFile()
    }

    fun attemptDismiss() {
        if (hasUnsavedChanges) {
            showUnsavedChangesDialog = true
        } else {
            onDismiss()
        }
    }

    fun saveContent() {
        if (isSaving) return
        isSaving = true
        viewModel.saveRemoteTextFile(file.path, content) { result ->
            isSaving = false
            result.onSuccess {
                originalContent = content
                Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, "保存失败: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Dialog(
        onDismissRequest = { attemptDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { attemptDismiss() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = file.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (hasUnsavedChanges) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "未保存",
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${file.path} · $lineCount 行 · ${formatBytes(content.toByteArray(Charsets.UTF_8).size.toLong())}",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Search Toggle
                            IconButton(
                                onClick = {
                                    showSearchBar = !showSearchBar
                                    if (!showSearchBar) searchQuery = ""
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (showSearchBar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Copy All Button
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(content))
                                    Toast.makeText(context, "已复制全部文本到剪贴板", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy All",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Save Button
                            Button(
                                onClick = { saveContent() },
                                enabled = !isLoading && (hasUnsavedChanges || isEditMode) && !isSaving,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.height(36.dp)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("保存中", fontSize = 13.sp)
                                } else {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("保存", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Mode Selector Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = !isEditMode,
                                onClick = { isEditMode = false },
                                leadingIcon = {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                label = { Text("只读预览", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )

                            FilterChip(
                                selected = isEditMode,
                                onClick = { isEditMode = true },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                label = { Text("编辑模式", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            IconButton(
                                onClick = { loadFile() },
                                enabled = !isLoading,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload", modifier = Modifier.size(18.dp))
                            }
                        }

                        // Search Bar (if expanded)
                        AnimatedVisibility(visible = showSearchBar) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        currentMatchIndex = 0
                                    },
                                    placeholder = { Text("在文件中查找...", fontSize = 13.sp) },
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 13.sp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                if (searchQuery.isNotBlank()) {
                                    Text(
                                        text = if (matches.isNotEmpty()) "${currentMatchIndex + 1}/${matches.size}" else "0/0",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    IconButton(
                                        onClick = {
                                            if (matches.isNotEmpty()) {
                                                currentMatchIndex = (currentMatchIndex - 1 + matches.size) % matches.size
                                            }
                                        },
                                        enabled = matches.isNotEmpty(),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Prev")
                                    }

                                    IconButton(
                                        onClick = {
                                            if (matches.isNotEmpty()) {
                                                currentMatchIndex = (currentMatchIndex + 1) % matches.size
                                            }
                                        },
                                        enabled = matches.isNotEmpty(),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next")
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        showSearchBar = false
                                        searchQuery = ""
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        HorizontalDivider()
                    }
                }

                // Editor Content Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                ) {
                    if (isLoading) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "正在读取远端文件内容...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (errorMessage != null) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "读取文件失败",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { loadFile() }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("重试加载")
                            }
                        }
                    } else {
                        val verticalScrollState = rememberScrollState()
                        val horizontalScrollState = rememberScrollState()

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(verticalScrollState)
                        ) {
                            // Line Number Column
                            Column(
                                modifier = Modifier
                                    .widthIn(min = 40.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                for (i in 1..lineCount) {
                                    Text(
                                        text = "$i",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    )
                                }
                            }

                            // Subtle Divider between line numbers and code
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )

                            // Main Text Editor / Viewer
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(horizontalScrollState)
                                    .padding(12.dp)
                            ) {
                                if (isEditMode) {
                                    BasicTextField(
                                        value = content,
                                        onValueChange = { content = it },
                                        textStyle = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            lineHeight = 20.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    SelectionContainer {
                                        Text(
                                            text = if (content.isEmpty()) "(文件内容为空)" else content,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            lineHeight = 20.sp,
                                            color = if (content.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Unsaved Changes Confirmation Dialog
    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("未保存的更改", fontWeight = FontWeight.Bold) },
            text = {
                Text("您对【${file.name}】进行了一些修改，退出后这些更改将会丢失。确定要放弃修改吗？", fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedChangesDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("放弃修改并退出")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showUnsavedChangesDialog = false
                        saveContent()
                    }
                ) {
                    Text("保存并退出")
                }
            }
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    return when {
        bytes >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
