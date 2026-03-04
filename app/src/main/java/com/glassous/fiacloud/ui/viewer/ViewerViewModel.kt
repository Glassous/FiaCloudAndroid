package com.glassous.fiacloud.ui.viewer

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glassous.fiacloud.data.S3Repository
import com.glassous.fiacloud.data.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class ViewerViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow<String?>(null)
    val downloadProgress: StateFlow<String?> = _downloadProgress.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _fileContent = MutableStateFlow("")
    val fileContent: StateFlow<String> = _fileContent.asStateFlow()

    private val _isPreviewMode = MutableStateFlow(true)
    val isPreviewMode: StateFlow<Boolean> = _isPreviewMode.asStateFlow()

    private val _mediaFile = MutableStateFlow<File?>(null)
    val mediaFile: StateFlow<File?> = _mediaFile.asStateFlow()

    val themeMode: StateFlow<String> = settingsRepo.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    private var currentBucketName: String = ""
    private var isInitialized = false

    init {
        viewModelScope.launch {
            settingsRepo.activeS3Config.collectLatest { config ->
                if (config != null) {
                    S3Repository.updateConfig(config)
                    currentBucketName = config.bucketName
                    isInitialized = true
                }
            }
        }
    }

    fun loadFile(item: S3Repository.S3Object, viewerType: String) {
        viewModelScope.launch {
            // 等待初始化完成（确保 S3Repository 配置已更新）
            while (!isInitialized) {
                kotlinx.coroutines.delay(100)
            }

            if (currentBucketName.isEmpty()) {
                _error.value = "未配置存储桶"
                return@launch
            }

            _isLoading.value = true
            _error.value = null

            try {
                when (viewerType) {
                    "text" -> {
                        val extension = item.displayName.substringAfterLast(".", "").lowercase()
                        _isPreviewMode.value = extension != "txt"
                        val content = S3Repository.getObjectContent(currentBucketName, item.key)
                        _fileContent.value = content
                    }
                    "media" -> {
                        val file = File(getApplication<Application>().cacheDir, item.displayName)
                        if (!file.exists()) {
                            S3Repository.downloadFile(currentBucketName, item.key, file)
                        }
                        _mediaFile.value = file
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "加载失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun togglePreviewMode() {
        _isPreviewMode.value = !_isPreviewMode.value
    }

    fun saveFileContent(item: S3Repository.S3Object, content: String) {
        viewModelScope.launch {
            if (currentBucketName.isEmpty()) return@launch
            _isLoading.value = true
            try {
                S3Repository.putObjectContent(currentBucketName, item.key, content)
                _fileContent.value = content
            } catch (e: Exception) {
                _error.value = e.message ?: "保存失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openExternal(item: S3Repository.S3Object) {
        viewModelScope.launch {
            if (currentBucketName.isEmpty()) {
                _error.value = "未配置存储桶"
                return@launch
            }

            _isDownloading.value = true
            _error.value = null
            _downloadProgress.value = "正在下载文件以供外部查看..."

            try {
                val context = getApplication<Application>()
                val extension = item.displayName.substringAfterLast(".", "").lowercase()
                val tempFile = File(context.cacheDir, "temp_view_${item.displayName}")
                
                // 下载文件
                S3Repository.downloadFile(currentBucketName, item.key, tempFile)

                // 获取 MIME 类型
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

                // 使用 FileProvider 获取 Content URI
                val contentUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    tempFile
                )

                // 创建并启动 Intent
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(Intent.createChooser(intent, "选择应用打开").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })

            } catch (e: Exception) {
                _error.value = "下载失败: ${e.message}"
            } finally {
                _isDownloading.value = false
                _downloadProgress.value = null
            }
        }
    }
}
