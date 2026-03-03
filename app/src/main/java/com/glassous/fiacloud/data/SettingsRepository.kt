package com.glassous.fiacloud.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val gson = Gson()

    companion object {
        // Old keys for migration
        val ENDPOINT = stringPreferencesKey("endpoint")
        val ACCESS_KEY = stringPreferencesKey("access_key")
        val SECRET_KEY = stringPreferencesKey("secret_key")
        val BUCKET_NAME = stringPreferencesKey("bucket_name")
        val REGION = stringPreferencesKey("region")

        // New keys
        val S3_CONFIGS = stringPreferencesKey("s3_configs")
        val ACTIVE_S3_CONFIG_ID = stringPreferencesKey("active_s3_config_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "SYSTEM"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    val s3Configs: Flow<List<S3Config>> = context.dataStore.data.map { preferences ->
        val configsJson = preferences[S3_CONFIGS] ?: "[]"
        val type = object : TypeToken<List<S3Config>>() {}.type
        val result: List<S3Config>? = gson.fromJson(configsJson, type)
        result ?: emptyList()
    }

    val activeS3ConfigId: Flow<String?> = context.dataStore.data.map { it[ACTIVE_S3_CONFIG_ID] }

    val activeS3Config: Flow<S3Config?> = context.dataStore.data.map { preferences ->
        val configsJson = preferences[S3_CONFIGS] ?: "[]"
        val activeId = preferences[ACTIVE_S3_CONFIG_ID]
        val type = object : TypeToken<List<S3Config>>() {}.type
        val configs: List<S3Config> = gson.fromJson(configsJson, type) ?: emptyList()
        configs.find { it.id == activeId } ?: configs.firstOrNull()
    }

    suspend fun addS3Config(config: S3Config) {
        context.dataStore.edit { preferences ->
            val configsJson = preferences[S3_CONFIGS] ?: "[]"
            val type = object : TypeToken<MutableList<S3Config>>() {}.type
            val configs: MutableList<S3Config> = gson.fromJson(configsJson, type) ?: mutableListOf()
            configs.add(config)
            preferences[S3_CONFIGS] = gson.toJson(configs)
            if (preferences[ACTIVE_S3_CONFIG_ID] == null) {
                preferences[ACTIVE_S3_CONFIG_ID] = config.id
            }
        }
    }

    suspend fun updateS3Config(config: S3Config) {
        context.dataStore.edit { preferences ->
            val configsJson = preferences[S3_CONFIGS] ?: "[]"
            val type = object : TypeToken<MutableList<S3Config>>() {}.type
            val configs: MutableList<S3Config> = gson.fromJson(configsJson, type) ?: mutableListOf()
            val index = configs.indexOfFirst { it.id == config.id }
            if (index != -1) {
                configs[index] = config
                preferences[S3_CONFIGS] = gson.toJson(configs)
            }
        }
    }

    suspend fun deleteS3Config(id: String) {
        context.dataStore.edit { preferences ->
            val configsJson = preferences[S3_CONFIGS] ?: "[]"
            val type = object : TypeToken<MutableList<S3Config>>() {}.type
            val configs: MutableList<S3Config> = gson.fromJson(configsJson, type) ?: mutableListOf()
            configs.removeAll { it.id == id }
            preferences[S3_CONFIGS] = gson.toJson(configs)
            if (preferences[ACTIVE_S3_CONFIG_ID] == id) {
                val nextConfigId = configs.firstOrNull()?.id
                if (nextConfigId != null) {
                    preferences[ACTIVE_S3_CONFIG_ID] = nextConfigId
                } else {
                    preferences.remove(ACTIVE_S3_CONFIG_ID)
                }
            }
        }
    }

    suspend fun setActiveS3Config(id: String) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_S3_CONFIG_ID] = id
        }
    }

    suspend fun migrateIfNecessary() {
        context.dataStore.edit { preferences ->
            val endpoint = preferences[ENDPOINT]
            if (endpoint != null && preferences[S3_CONFIGS] == null) {
                val config = S3Config(
                    name = "默认配置",
                    endpoint = endpoint,
                    accessKey = preferences[ACCESS_KEY] ?: "",
                    secretKey = preferences[SECRET_KEY] ?: "",
                    bucketName = preferences[BUCKET_NAME] ?: "",
                    region = preferences[REGION] ?: "us-east-1"
                )
                preferences[S3_CONFIGS] = gson.toJson(listOf(config))
                preferences[ACTIVE_S3_CONFIG_ID] = config.id
            }
        }
    }
}
