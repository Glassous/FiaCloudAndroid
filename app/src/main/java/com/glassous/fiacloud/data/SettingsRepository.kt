package com.glassous.fiacloud.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val ENDPOINT = stringPreferencesKey("endpoint")
        val ACCESS_KEY = stringPreferencesKey("access_key")
        val SECRET_KEY = stringPreferencesKey("secret_key")
        val BUCKET_NAME = stringPreferencesKey("bucket_name")
        val REGION = stringPreferencesKey("region")
    }

    val endpoint: Flow<String> = context.dataStore.data.map { it[ENDPOINT] ?: "" }
    val accessKey: Flow<String> = context.dataStore.data.map { it[ACCESS_KEY] ?: "" }
    val secretKey: Flow<String> = context.dataStore.data.map { it[SECRET_KEY] ?: "" }
    val bucketName: Flow<String> = context.dataStore.data.map { it[BUCKET_NAME] ?: "" }
    val region: Flow<String> = context.dataStore.data.map { it[REGION] ?: "us-east-1" }

    suspend fun saveSettings(endpoint: String, accessKey: String, secretKey: String, bucketName: String, region: String) {
        context.dataStore.edit { preferences ->
            preferences[ENDPOINT] = endpoint
            preferences[ACCESS_KEY] = accessKey
            preferences[SECRET_KEY] = secretKey
            preferences[BUCKET_NAME] = bucketName
            preferences[REGION] = region
        }
    }
}
