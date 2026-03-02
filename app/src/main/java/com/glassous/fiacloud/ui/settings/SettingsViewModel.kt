package com.glassous.fiacloud.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glassous.fiacloud.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)

    private val _endpoint = MutableStateFlow("")
    val endpoint: StateFlow<String> = _endpoint.asStateFlow()

    private val _accessKey = MutableStateFlow("")
    val accessKey: StateFlow<String> = _accessKey.asStateFlow()

    private val _secretKey = MutableStateFlow("")
    val secretKey: StateFlow<String> = _secretKey.asStateFlow()

    private val _bucketName = MutableStateFlow("")
    val bucketName: StateFlow<String> = _bucketName.asStateFlow()

    private val _region = MutableStateFlow("us-east-1")
    val region: StateFlow<String> = _region.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepo.endpoint.collectLatest { _endpoint.value = it }
        }
        viewModelScope.launch {
            settingsRepo.accessKey.collectLatest { _accessKey.value = it }
        }
        viewModelScope.launch {
            settingsRepo.secretKey.collectLatest { _secretKey.value = it }
        }
        viewModelScope.launch {
            settingsRepo.bucketName.collectLatest { _bucketName.value = it }
        }
        viewModelScope.launch {
            settingsRepo.region.collectLatest { _region.value = it }
        }
    }

    fun updateEndpoint(value: String) { _endpoint.value = value }
    fun updateAccessKey(value: String) { _accessKey.value = value }
    fun updateSecretKey(value: String) { _secretKey.value = value }
    fun updateBucketName(value: String) { _bucketName.value = value }
    fun updateRegion(value: String) { _region.value = value }

    fun saveSettings() {
        viewModelScope.launch {
            settingsRepo.saveSettings(
                endpoint.value,
                accessKey.value,
                secretKey.value,
                bucketName.value,
                region.value
            )
        }
    }
}
