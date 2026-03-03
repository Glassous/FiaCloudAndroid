package com.glassous.fiacloud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.glassous.fiacloud.ui.settings.S3DetailScreen
import com.glassous.fiacloud.ui.settings.S3ListScreen
import com.glassous.fiacloud.ui.settings.SettingsScreen
import com.glassous.fiacloud.ui.settings.SettingsViewModel
import com.glassous.fiacloud.ui.theme.FiaCloudTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            val viewModel: SettingsViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()

            FiaCloudTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var showAddDialog by remember { mutableStateOf(false) }
                    var newConfigName by remember { mutableStateOf("") }

                    NavHost(navController = navController, startDestination = "settings") {
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateToS3Configs = { navController.navigate("s3_list") },
                                onNavigateBack = { finish() }
                            )
                        }
                        composable("s3_list") {
                            S3ListScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { id -> navController.navigate("s3_detail/$id") },
                                onNavigateBack = { navController.popBackStack() },
                                onAddNewConfig = { showAddDialog = true }
                            )
                        }
                        composable(
                            "s3_detail/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id") ?: ""
                            S3DetailScreen(
                                configId = id,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    if (showAddDialog) {
                        AlertDialog(
                            onDismissRequest = { showAddDialog = false },
                            title = { Text("新增 S3 配置") },
                            text = {
                                OutlinedTextField(
                                    value = newConfigName,
                                    onValueChange = { newConfigName = it },
                                    label = { Text("配置名称") },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (newConfigName.isNotBlank()) {
                                            viewModel.addS3Config(newConfigName)
                                            newConfigName = ""
                                            showAddDialog = false
                                        }
                                    }
                                ) {
                                    Text("确定")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddDialog = false }) {
                                    Text("取消")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
