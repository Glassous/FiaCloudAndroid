package com.glassous.fiacloud.ui.home

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glassous.fiacloud.data.S3Repository
import com.glassous.fiacloud.data.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)
    
    private val _files = MutableStateFlow<List<S3Repository.S3Object>>(emptyList())
    val files: StateFlow<List<S3Repository.S3Object>> = _files.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _currentPrefix = MutableStateFlow("")
    val currentPrefix: StateFlow<String> = _currentPrefix.asStateFlow()

    val themeMode: StateFlow<String> = settingsRepo.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    private var currentBucketName: String = ""

    init {
        viewModelScope.launch {
            settingsRepo.activeS3Config.collectLatest { config ->
                if (config != null) {
                    S3Repository.updateConfig(config)
                    currentBucketName = config.bucketName
                    _currentPrefix.value = "" // 重置路径
                    
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

    fun getViewerType(item: S3Repository.S3Object): String {
        val extension = item.displayName.substringAfterLast(".", "").lowercase()
        val textExtensions = setOf(
            "txt", "md", "markdown", "json", "csv",
            "py", "c", "cpp", "h", "java", "kt", "js", "ts", "html", "css", "xml", "yaml", "yml", "sh"
        )
        val mediaExtensions = setOf(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "ico",
            "mp3", "wav", "ogg", "m4a", "aac", "flac", "amr", "mid", "midi",
            "mp4", "mkv", "webm", "avi", "mov", "3gp", "ts"
        )

        return when {
            textExtensions.contains(extension) -> "text"
            mediaExtensions.contains(extension) -> "media"
            else -> "unsupported"
        }
    }

    fun togglePreviewMode() {
        // This is now handled in ViewerViewModel
    }

    fun saveFileContent(content: String) {
        // This is now handled in ViewerViewModel
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

    fun downloadItem(item: S3Repository.S3Object) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, item.displayName)
                S3Repository.downloadFile(currentBucketName, item.key, file)
                _successMessage.value = "已下载到: ${file.absolutePath}"
            } catch (e: Exception) {
                _error.value = getErrorMessage(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadFile(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val fileName = getFileName(uri) ?: "uploaded_file"
                val tempFile = File(getApplication<Application>().cacheDir, fileName)
                
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                val key = if (_currentPrefix.value.isEmpty()) fileName else "${_currentPrefix.value}$fileName"
                S3Repository.uploadFile(currentBucketName, key, tempFile)
                tempFile.delete()
                loadFiles(currentBucketName, _currentPrefix.value)
                _successMessage.value = "上传成功"
            } catch (e: Exception) {
                _error.value = getErrorMessage(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1 && cut != null) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val fileName = if (name.endsWith(".txt")) name else "$name.txt"
                val key = if (_currentPrefix.value.isEmpty()) fileName else "${_currentPrefix.value}$fileName"
                S3Repository.putObjectContent(currentBucketName, key, "")
                loadFiles(currentBucketName, _currentPrefix.value)
                _successMessage.value = "文件创建成功"
            } catch (e: Exception) {
                _error.value = getErrorMessage(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearSuccessMessage() {
        _successMessage.value = null
    }



    fun openExternal(item: S3Repository.S3Object) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val file = File(getApplication<Application>().cacheDir, item.displayName)
                S3Repository.downloadFile(currentBucketName, item.key, file)
                
                val authority = "${getApplication<Application>().packageName}.provider"
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    getApplication(),
                    authority,
                    file
                )
                
                val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
                
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                getApplication<Application>().startActivity(intent)
            } catch (e: Exception) {
                _error.value = "无法打开文件: ${e.message}"
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
