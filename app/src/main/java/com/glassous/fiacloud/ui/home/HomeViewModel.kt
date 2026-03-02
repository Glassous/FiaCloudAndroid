package com.glassous.fiacloud.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glassous.fiacloud.data.S3Repository
import com.glassous.fiacloud.data.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class S3Config(
    val endpoint: String,
    val accessKey: String,
    val secretKey: String,
    val region: String,
    val bucketName: String
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)
    
    private val _files = MutableStateFlow<List<String>>(emptyList())
    val files: StateFlow<List<String>> = _files.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentBucketName: String = ""

    init {
        viewModelScope.launch {
            combine(
                settingsRepo.endpoint,
                settingsRepo.accessKey,
                settingsRepo.secretKey,
                settingsRepo.region,
                settingsRepo.bucketName
            ) { args: Array<String> ->
                S3Config(
                    endpoint = args[0],
                    accessKey = args[1],
                    secretKey = args[2],
                    region = args[3],
                    bucketName = args[4]
                )
            }.collectLatest { config ->
                S3Repository.updateConfig(
                    config.endpoint, 
                    config.accessKey, 
                    config.secretKey, 
                    config.region
                )
                currentBucketName = config.bucketName
                
                // 即使配置为空，也尝试进行连接，由SDK抛出具体异常
                if (config.bucketName.isNotEmpty()) {
                    loadFiles(config.bucketName)
                } else {
                    // 尝试传空字符串给 listObjects
                    loadFiles("")
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            loadFiles(currentBucketName)
        }
    }

    private suspend fun loadFiles(bucketName: String) {
        _isLoading.value = true
        _error.value = null
        try {
            // 如果 bucketName 为空，SDK 可能会抛错，符合用户预期
            _files.value = S3Repository.listObjects(bucketName)
        } catch (e: Exception) {
            // 忽略 HttpClientEngine closed 异常，因为这通常发生在配置快速更新时
            if (e.message?.contains("HttpClientEngine already closed", ignoreCase = true) == true) {
                return
            }
            _error.value = getErrorMessage(e)
        } finally {
            _isLoading.value = false
        }
    }

    private fun getErrorMessage(e: Exception): String {
        val message = e.message ?: "未知错误"
        return when {
            message.contains("Unable to resolve host", ignoreCase = true) -> "无法连接到服务器，请检查网络连接或服务端点地址。"
            message.contains("Access Denied", ignoreCase = true) -> "访问被拒绝 (Access Denied)，请检查您的访问密钥和权限。"
            message.contains("The specified bucket does not exist", ignoreCase = true) -> "指定的存储桶不存在，请检查存储桶名称。"
            message.contains("SignatureDoesNotMatch", ignoreCase = true) -> "签名不匹配，请检查您的 Access Key 和 Secret Key 是否正确。"
            message.contains("Forbidden", ignoreCase = true) -> "禁止访问 (Forbidden)，请检查权限设置。"
            message.contains("SocketTimeout", ignoreCase = true) -> "连接超时，请检查网络状况。"
            message.contains("HttpClientEngine already closed", ignoreCase = true) -> "" // Ignore this error
            else -> "发生错误: $message"
        }
    }
}
