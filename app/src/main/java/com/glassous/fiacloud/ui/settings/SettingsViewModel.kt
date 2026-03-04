package com.glassous.fiacloud.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glassous.fiacloud.data.S3Config
import com.glassous.fiacloud.data.SettingsRepository
import com.glassous.fiacloud.data.UpdateInfo
import com.glassous.fiacloud.data.UpdateManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)
    private val updateManager = UpdateManager(application)

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    val s3Configs: StateFlow<List<S3Config>> = settingsRepo.s3Configs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeS3ConfigId: StateFlow<String?> = settingsRepo.activeS3ConfigId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeS3Config: StateFlow<S3Config?> = settingsRepo.activeS3Config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val themeMode: StateFlow<String> = settingsRepo.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val autoUpdateCheck: StateFlow<Boolean> = settingsRepo.autoUpdateCheck
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        viewModelScope.launch {
            settingsRepo.migrateIfNecessary()
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepo.setThemeMode(mode)
        }
    }

    fun setAutoUpdateCheck(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setAutoUpdateCheck(enabled)
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            _updateInfo.value = updateManager.checkForUpdate()
            _isCheckingUpdate.value = false
        }
    }

    fun clearUpdateInfo() {
        _updateInfo.value = null
    }

    fun openDownloadUrl(url: String) {
        updateManager.openDownloadUrl(url)
    }

    fun addS3Config(name: String) {
        viewModelScope.launch {
            val newConfig = S3Config(name = name)
            settingsRepo.addS3Config(newConfig)
        }
    }

    fun updateS3Config(config: S3Config) {
        viewModelScope.launch {
            settingsRepo.updateS3Config(config)
        }
    }

    fun deleteS3Config(id: String) {
        viewModelScope.launch {
            settingsRepo.deleteS3Config(id)
        }
    }

    fun setActiveS3Config(id: String) {
        viewModelScope.launch {
            settingsRepo.setActiveS3Config(id)
        }
    }
}
