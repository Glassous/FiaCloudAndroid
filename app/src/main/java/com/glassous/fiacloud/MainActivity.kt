package com.glassous.fiacloud

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glassous.fiacloud.ui.home.HomeScreen
import com.glassous.fiacloud.ui.home.HomeViewModel
import com.glassous.fiacloud.ui.theme.FiaCloudTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: HomeViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val updateInfo by viewModel.updateInfo.collectAsState()

            FiaCloudTheme(themeMode = themeMode) {
                if (updateInfo != null) {
                    AlertDialog(
                        onDismissRequest = { viewModel.clearUpdateInfo() },
                        title = { Text("发现新版本") },
                        text = {
                            Column {
                                Text("版本: ${updateInfo!!.latestVersion}")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(updateInfo!!.releaseNotes)
                            }
                        },
                        confirmButton = {
                            if (updateInfo!!.downloadUrl != null) {
                                Button(onClick = {
                                    viewModel.openDownloadUrl(updateInfo!!.downloadUrl!!)
                                    viewModel.clearUpdateInfo()
                                }) {
                                    Text("前往下载")
                                }
                            } else {
                                TextButton(onClick = { viewModel.clearUpdateInfo() }) {
                                    Text("确定")
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.clearUpdateInfo() }) {
                                Text("以后再说")
                            }
                        }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = {
                            startActivity(Intent(this, SettingsActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}
