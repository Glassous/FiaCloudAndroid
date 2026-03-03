package com.glassous.fiacloud.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.glassous.fiacloud.data.S3Config

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun S3ListScreen(
    viewModel: SettingsViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onAddNewConfig: () -> Unit
) {
    val configs by viewModel.s3Configs.collectAsState()
    val activeId by viewModel.activeS3ConfigId.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("S3 配置列表") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    var showHelpDialog by remember { mutableStateOf(false) }
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "配置帮助")
                    }
                    IconButton(onClick = onAddNewConfig) {
                        Icon(Icons.Default.Add, "新增")
                    }

                    if (showHelpDialog) {
                        S3HelpDialog(onDismiss = { showHelpDialog = false })
                    }
                },
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        if (configs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()), contentAlignment = Alignment.Center) {
                Text("暂无配置，请点击右上角新增")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(configs) { config ->
                    S3ConfigItem(
                        config = config,
                        isActive = config.id == activeId,
                        onSelect = { viewModel.setActiveS3Config(config.id) },
                        onEdit = { onNavigateToDetail(config.id) },
                        onDelete = { showDeleteDialog = config.id }
                    )
                }
                item {
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除配置") },
            text = { Text("确定要删除这个配置吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog?.let { viewModel.deleteS3Config(it) }
                    showDeleteDialog = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun S3ConfigItem(
    config: S3Config,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        colors = if (isActive) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isActive, onClick = onSelect)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(config.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(config.endpoint, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
            }
            Icon(Icons.Default.ChevronRight, "详情")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun S3DetailScreen(
    configId: String,
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val configs by viewModel.s3Configs.collectAsState()
    val config = configs.find { it.id == configId } ?: return

    var name by remember(config) { mutableStateOf(config.name) }
    var endpoint by remember(config) { mutableStateOf(config.endpoint) }
    var accessKey by remember(config) { mutableStateOf(config.accessKey) }
    var secretKey by remember(config) { mutableStateOf(config.secretKey) }
    var bucketName by remember(config) { mutableStateOf(config.bucketName) }
    var region by remember(config) { mutableStateOf(config.region) }

    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("配置详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = {
                        viewModel.updateS3Config(config.copy(
                            name = name,
                            endpoint = endpoint,
                            accessKey = accessKey,
                            secretKey = secretKey,
                            bucketName = bucketName,
                            region = region
                        ))
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("保存配置")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("配置名称") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = endpoint,
                onValueChange = { endpoint = it },
                label = { Text("服务端点 (例如 s3.amazonaws.com)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = accessKey,
                onValueChange = { accessKey = it },
                label = { Text("访问密钥 (Access Key)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = secretKey,
                onValueChange = { secretKey = it },
                label = { Text("私有密钥 (Secret Key)") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "隐藏密钥" else "显示密钥"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bucketName,
                onValueChange = { bucketName = it },
                label = { Text("存储桶名称 (Bucket)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = region,
                onValueChange = { region = it },
                label = { Text("区域 (可选，默认为 us-east-1)") },
                placeholder = { Text("例如 us-east-1") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun S3HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("S3 配置指南") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HelpSection(
                    title = "Amazon S3",
                    content = "• 端点: s3.amazonaws.com\n• 区域: 根据你的 Bucket 所在区域填写 (如 us-east-1)\n• 密钥: 在 AWS IAM 控制台获取\n• 注意: AWS 必须正确填写区域以进行请求签名"
                )
                HelpSection(
                    title = "Cloudflare R2",
                    content = "• 端点: <account_id>.r2.cloudflarestorage.com\n• 区域: 可不填或填 auto\n• 密钥: 在 R2 仪表盘创建 API 令牌获取"
                )
                HelpSection(
                    title = "阿里云 OSS (S3 兼容)",
                    content = "• 端点: <bucket>.<region>-internal.aliyuncs.com (外网去掉 -internal)\n• 区域: 可不填，默认为 us-east-1 或根据端点自动识别\n• 密钥: 阿里云 RAM 控制台获取"
                )
                HelpSection(
                    title = "腾讯云 COS (S3 兼容)",
                    content = "• 端点: cos.<region>.myqcloud.com\n• 区域: 根据 COS 区域填写 (如 ap-shanghai)\n• 密钥: 腾讯云控制台获取 API 密钥"
                )
                HelpSection(
                    title = "MinIO (私有部署)",
                    content = "• 端点: 你的 MinIO 服务器地址 (如 192.168.1.100:9000)\n• 区域: us-east-1 (默认)\n• 密钥: MinIO 管理后台创建"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("我知道了")
            }
        }
    )
}

@Composable
fun HelpSection(title: String, content: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}
