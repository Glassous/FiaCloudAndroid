package com.glassous.fiacloud.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.glassous.fiacloud.data.S3Repository

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TextEditorScreen(
    file: S3Repository.S3Object,
    initialContent: String,
    onBack: () -> Unit,
    onSave: (String) -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }
    var isEditing by remember { mutableStateOf(false) }
    var lastSavedContent by remember { mutableStateOf(initialContent) }

    // 当云端内容更新时同步本地显示
    LaunchedEffect(initialContent) {
        lastSavedContent = initialContent
        if (!isEditing) {
            content = initialContent
        }
    }

    val hasChanges = content != lastSavedContent
    val isImeVisible = WindowInsets.isImeVisible
    val density = LocalDensity.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(file.displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(
                            onClick = { onSave(content) },
                            enabled = hasChanges
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "保存")
                        }
                        IconButton(onClick = { 
                            isEditing = false 
                            content = lastSavedContent // 退出时不保存
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "退出编辑")
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "进入编辑")
                        }
                    }
                },
                windowInsets = WindowInsets.statusBars
            )

            SelectionContainer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                BasicTextField(
                    value = content,
                    onValueChange = {
                        if (isEditing) {
                            content = it
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (isImeVisible) with(density) { WindowInsets.ime.getBottom(this).toDp() } else 0.dp),
                    readOnly = !isEditing,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                        ) {
                            if (content.isEmpty() && isEditing) {
                                Text(
                                    text = "在此输入内容...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}
