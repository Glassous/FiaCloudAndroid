package com.glassous.fiacloud.ui.viewer

import android.app.Application
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
        // 实现外部打开逻辑，这里可以调用 S3Repository 或 Activity 逻辑
    }
}
