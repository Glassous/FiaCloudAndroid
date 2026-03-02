package com.glassous.fiacloud.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glassous.fiacloud.data.S3Repository
import com.glassous.fiacloud.data.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)
    
    private val _files = MutableStateFlow<List<S3Repository.S3Object>>(emptyList())
    val files: StateFlow<List<S3Repository.S3Object>> = _files.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentPrefix = MutableStateFlow("")
    val currentPrefix: StateFlow<String> = _currentPrefix.asStateFlow()

    private val _editingFile = MutableStateFlow<S3Repository.S3Object?>(null)
    val editingFile: StateFlow<S3Repository.S3Object?> = _editingFile.asStateFlow()

    private val _viewingUnsupportedFile = MutableStateFlow<S3Repository.S3Object?>(null)
    val viewingUnsupportedFile: StateFlow<S3Repository.S3Object?> = _viewingUnsupportedFile.asStateFlow()

    private val _fileContent = MutableStateFlow<String>("")
    val fileContent: StateFlow<String> = _fileContent.asStateFlow()

    private var currentBucketName: String = ""

    init {
        viewModelScope.launch {
            settingsRepo.activeS3Config.collectLatest { config ->
                if (config != null) {
                    S3Repository.updateConfig(config)
                    currentBucketName = config.bucketName
                    _currentPrefix.value = "" // 重置路径
                    _editingFile.value = null // 重置编辑状态
                    _viewingUnsupportedFile.value = null // 重置预览状态
                    
                    if (config.bucketName.isNotEmpty()) {
                        loadFiles(config.bucketName, "")
                    } else {
                        loadFiles("", "")
                    }
                } else {
                    _files.value = emptyList()
                    _error.value = "S3 客户端未配置，请前往设置页面进行配置。"
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            loadFiles(currentBucketName, _currentPrefix.value)
        }
    }

    fun navigateInto(prefix: String) {
        viewModelScope.launch {
            _currentPrefix.value = prefix
            loadFiles(currentBucketName, prefix)
        }
    }

    fun navigateBack(): Boolean {
        if (_editingFile.value != null) {
            _editingFile.value = null
            return true
        }

        if (_viewingUnsupportedFile.value != null) {
            _viewingUnsupportedFile.value = null
            return true
        }

        val current = _currentPrefix.value
        if (current.isEmpty()) return false
        
        val parent = current.removeSuffix("/").substringBeforeLast("/", "").let {
            if (it.isEmpty()) "" else "$it/"
        }
        
        _currentPrefix.value = parent
        viewModelScope.launch {
            loadFiles(currentBucketName, parent)
        }
        return true
    }

    fun openFile(item: S3Repository.S3Object) {
        if (item.displayName.endsWith(".txt", ignoreCase = true)) {
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                try {
                    val content = S3Repository.getObjectContent(currentBucketName, item.key)
                    _fileContent.value = content
                    _editingFile.value = item
                } catch (e: Exception) {
                    _error.value = getErrorMessage(e)
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            // 非 .txt 文件，进入不支持页面
            _viewingUnsupportedFile.value = item
        }
    }

    fun saveFileContent(content: String) {
        val file = _editingFile.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                S3Repository.putObjectContent(currentBucketName, file.key, content)
                _fileContent.value = content
                // 保存后不再自动退出编辑，符合用户新需求
                loadFiles(currentBucketName, _currentPrefix.value)
            } catch (e: Exception) {
                _error.value = getErrorMessage(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun closeFile() {
        _editingFile.value = null
        _viewingUnsupportedFile.value = null
        _fileContent.value = ""
    }

    fun deleteItem(item: S3Repository.S3Object) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                S3Repository.deleteObject(currentBucketName, item.key, item.isFolder)
                loadFiles(currentBucketName, _currentPrefix.value)
            } catch (e: Exception) {
                _error.value = getErrorMessage(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun renameItem(item: S3Repository.S3Object, newName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val oldKey = item.key
                val newKey = if (item.isFolder) {
                    val parent = oldKey.removeSuffix("/").substringBeforeLast("/", "").let {
                        if (it.isEmpty()) "" else "$it/"
                    }
                    "$parent$newName/"
                } else {
                    val parent = oldKey.substringBeforeLast("/", "").let {
                        if (it.isEmpty()) "" else "$it/"
                    }
                    "$parent$newName"
                }
                
                if (newKey != oldKey) {
                    S3Repository.renameObject(currentBucketName, oldKey, newKey, item.isFolder)
                    loadFiles(currentBucketName, _currentPrefix.value)
                }
            } catch (e: Exception) {
                _error.value = getErrorMessage(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadFiles(bucketName: String, prefix: String) {
        _isLoading.value = true
        _error.value = null
        try {
            _files.value = S3Repository.listObjects(bucketName, prefix)
        } catch (e: Exception) {
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
