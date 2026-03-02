package com.glassous.fiacloud.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glassous.fiacloud.data.S3Repository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToSettings: () -> Unit
) {
    val files by viewModel.files.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentPrefix by viewModel.currentPrefix.collectAsState()
    val editingFile by viewModel.editingFile.collectAsState()
    val viewingUnsupportedFile by viewModel.viewingUnsupportedFile.collectAsState()
    val fileContent by viewModel.fileContent.collectAsState()

    BackHandler(enabled = currentPrefix.isNotEmpty() || editingFile != null || viewingUnsupportedFile != null) {
        viewModel.navigateBack()
    }

    if (editingFile != null) {
        TextEditorScreen(
            file = editingFile!!,
            initialContent = fileContent,
            onBack = { viewModel.closeFile() },
            onSave = { viewModel.saveFileContent(it) }
        )
    } else if (viewingUnsupportedFile != null) {
        UnsupportedFileScreen(
            file = viewingUnsupportedFile!!,
            onBack = { viewModel.closeFile() }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text("FiaCloud")
                            if (currentPrefix.isNotEmpty()) {
                                Text(
                                    text = currentPrefix,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (currentPrefix.isNotEmpty()) {
                            IconButton(onClick = { viewModel.navigateBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                    },
                    windowInsets = WindowInsets.statusBars
                )
            },
            contentWindowInsets = WindowInsets.navigationBars
        ) { padding ->
            Box(modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (error != null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onNavigateToSettings) {
                            Text("去设置")
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            bottom = padding.calculateBottomPadding()
                        )
                    ) {
                        items(files) { item ->
                            S3ItemRow(
                                item = item,
                                onFolderClick = { viewModel.navigateInto(item.key) },
                                onFileClick = { viewModel.openFile(item) },
                                onDelete = { viewModel.deleteItem(item) },
                                onRename = { newName -> viewModel.renameItem(item, newName) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun S3ItemRow(
    item: S3Repository.S3Object,
    onFolderClick: () -> Unit,
    onFileClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        RenameDialog(
            initialName = item.displayName,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                onRename(newName)
                showRenameDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog) {
        DeleteConfirmDialog(
            itemName = item.displayName,
            isFolder = item.isFolder,
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                onDelete()
                showDeleteConfirmDialog = false
            }
        )
    }

    ListItem(
        headlineContent = { Text(item.displayName) },
        leadingContent = {
            Icon(
                imageVector = if (item.isFolder) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
                tint = if (item.isFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("重命名") },
                        onClick = {
                            showMenu = false
                            showRenameDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            showDeleteConfirmDialog = true
                        }
                    )
                }
            }
        },
        modifier = Modifier.clickable {
            if (item.isFolder) {
                onFolderClick()
            } else {
                onFileClick()
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    itemName: String,
    isFolder: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = {
            Text("确定要删除${if (isFolder) "文件夹" else "文件"} \"$itemName\" 吗？${if (isFolder) "\n注意：文件夹内的所有内容也将被删除。" else ""}")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun RenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank() && text != initialName
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
