package com.glassous.fiacloud.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRelease(
    val tag_name: String,
    val name: String,
    val body: String,
    val assets: List<GitHubAsset>
)

data class GitHubAsset(
    val name: String,
    val browser_download_url: String
)

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String?
)

class UpdateManager(private val context: Context) {
    private val gson = Gson()
    private val GITHUB_REPO = "Glassous/FiaCloudAndroid" // 示例仓库

    /**
     * 示例说明:
     * APK 名称示例: FiaCloud-v1.1.0-release.apk
     * Tag 示例: v1.1.0
     */

    suspend fun checkForUpdate(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val release = gson.fromJson(responseText, GitHubRelease::class.java)
                
                val currentVersion = context.packageManager.getPackageInfo(
                    context.packageName, 
                    PackageManager.PackageInfoFlags.of(0)
                ).versionName ?: "1.0.0"
                val latestVersion = release.tag_name.removePrefix("v")
                
                val hasUpdate = isVersionNewer(currentVersion, latestVersion)
                
                val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                
                UpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersion = release.tag_name,
                    releaseNotes = release.body,
                    downloadUrl = apkAsset?.browser_download_url
                )
            } else {
                UpdateInfo(false, "", "", null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateInfo(false, "", "", null)
        }
    }

    private fun isVersionNewer(current: String, latest: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        
        val length = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until length) {
            val curr = currentParts.getOrNull(i) ?: 0
            val late = latestParts.getOrNull(i) ?: 0
            if (late > curr) return true
            if (late < curr) return false
        }
        return false
    }

    fun openDownloadUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
