package com.glassous.fiacloud.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val endpoint by viewModel.endpoint.collectAsState()
    val accessKey by viewModel.accessKey.collectAsState()
    val secretKey by viewModel.secretKey.collectAsState()
    val bucketName by viewModel.bucketName.collectAsState()
    val region by viewModel.region.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = endpoint,
                onValueChange = viewModel::updateEndpoint,
                label = { Text("服务端点 (例如 s3.amazonaws.com)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = accessKey,
                onValueChange = viewModel::updateAccessKey,
                label = { Text("访问密钥 (Access Key)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = secretKey,
                onValueChange = viewModel::updateSecretKey,
                label = { Text("私有密钥 (Secret Key)") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    val description = if (passwordVisible) "隐藏密钥" else "显示密钥"

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bucketName,
                onValueChange = viewModel::updateBucketName,
                label = { Text("存储桶名称 (Bucket)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = region,
                onValueChange = viewModel::updateRegion,
                label = { Text("区域 (例如 us-east-1)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    viewModel.saveSettings()
                    scope.launch {
                        snackbarHostState.showSnackbar("配置已保存")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }
    }
}
