package com.glassous.fiacloud.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.glassous.fiacloud.data.S3Repository

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TextEditorScreen(
    file: S3Repository.S3Object,
    initialContent: String,
    isPreviewMode: Boolean,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    onTogglePreview: () -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(initialContent)) }
    var isEditing by remember { mutableStateOf(false) }
    var lastSavedContent by remember { mutableStateOf(initialContent) }

    // 当云端内容更新时同步本地显示
    LaunchedEffect(initialContent) {
        lastSavedContent = initialContent
        if (!isEditing) {
            content = initialContent
            textFieldValue = TextFieldValue(initialContent)
        }
    }

    val hasChanges = content != lastSavedContent
    val isImeVisible = WindowInsets.isImeVisible
    val density = LocalDensity.current
    val extension = file.displayName.substringAfterLast(".", "").lowercase()

    BackHandler {
        if (isEditing) {
            isEditing = false
            content = lastSavedContent
            textFieldValue = TextFieldValue(lastSavedContent)
        } else {
            onBack()
        }
    }

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
                    val showPreviewToggle = setOf("md", "markdown", "json", "csv").contains(extension)
                    if (showPreviewToggle && !isEditing) {
                        IconButton(onClick = onTogglePreview) {
                            Icon(
                                imageVector = if (isPreviewMode) Icons.Default.Code else Icons.Default.Preview,
                                contentDescription = if (isPreviewMode) "源码模式" else "预览模式"
                            )
                        }
                    }

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
                            textFieldValue = TextFieldValue(lastSavedContent)
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "退出编辑")
                        }
                    } else if (!isPreviewMode || !showPreviewToggle) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "进入编辑")
                        }
                    }
                },
                windowInsets = WindowInsets.statusBars
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isPreviewMode && !isEditing) {
                    when (extension) {
                        "md", "markdown" -> MarkdownPreview(content)
                        "json" -> JsonPreview(content)
                        "csv" -> CsvPreview(content)
                        else -> {
                            SourceEditor(
                                textFieldValue = textFieldValue,
                                onValueChange = { textFieldValue = it; content = it.text },
                                isEditing = false,
                                isImeVisible = isImeVisible,
                                density = density,
                                extension = extension
                            )
                        }
                    }
                } else {
                    SourceEditor(
                        textFieldValue = textFieldValue,
                        onValueChange = { 
                            if (isEditing) {
                                textFieldValue = it
                                content = it.text
                            }
                        },
                        isEditing = isEditing,
                        isImeVisible = isImeVisible,
                        density = density,
                        extension = extension
                    )
                }
            }
        }
    }
}

@Composable
fun SourceEditor(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    isEditing: Boolean,
    isImeVisible: Boolean,
    density: androidx.compose.ui.unit.Density,
    extension: String
) {
    SelectionContainer(
        modifier = Modifier.fillMaxSize()
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isImeVisible) with(density) { WindowInsets.ime.getBottom(this).toDp() } else 0.dp),
            readOnly = !isEditing,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = { text ->
                val highlighted = SyntaxHighlighter.highlight(text.text, extension)
                androidx.compose.ui.text.input.TransformedText(
                    highlighted,
                    VisualTransformation.None.filter(text).offsetMapping
                )
            },
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                ) {
                    if (textFieldValue.text.isEmpty() && isEditing) {
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
