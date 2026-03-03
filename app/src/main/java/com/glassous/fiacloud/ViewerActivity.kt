package com.glassous.fiacloud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glassous.fiacloud.data.S3Repository
import com.glassous.fiacloud.ui.home.MediaViewerScreen
import com.glassous.fiacloud.ui.home.TextEditorScreen
import com.glassous.fiacloud.ui.home.UnsupportedFileScreen
import com.glassous.fiacloud.ui.theme.FiaCloudTheme
import com.glassous.fiacloud.ui.viewer.ViewerViewModel
import java.io.File

class ViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val key = intent.getStringExtra("key") ?: ""
        val displayName = intent.getStringExtra("displayName") ?: ""
        val isFolder = intent.getBooleanExtra("isFolder", false)
        val viewerType = intent.getStringExtra("viewerType") ?: "text"
        
        val s3Object = S3Repository.S3Object(key, isFolder, displayName)

        setContent {
            val viewModel: ViewerViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            
            FiaCloudTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ViewerContent(
                        viewerType = viewerType,
                        s3Object = s3Object,
                        viewModel = viewModel,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun ViewerContent(
    viewerType: String,
    s3Object: S3Repository.S3Object,
    viewModel: ViewerViewModel,
    onBack: () -> Unit
) {
    val fileContent by viewModel.fileContent.collectAsState()
    val isPreviewMode by viewModel.isPreviewMode.collectAsState()
    val mediaFile by viewModel.mediaFile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // 初始化加载数据
    LaunchedEffect(s3Object) {
        viewModel.loadFile(s3Object, viewerType)
    }

    if (isLoading) {
        LoadingBox()
    } else if (error != null) {
        ErrorBox(error!!, onBack)
    } else {
        when (viewerType) {
            "text" -> {
                TextEditorScreen(
                    file = s3Object,
                    initialContent = fileContent,
                    isPreviewMode = isPreviewMode,
                    onBack = onBack,
                    onSave = { viewModel.saveFileContent(s3Object, it) },
                    onTogglePreview = { viewModel.togglePreviewMode() }
                )
            }
            "media" -> {
                if (mediaFile != null) {
                    MediaViewerScreen(
                        file = mediaFile,
                        item = s3Object,
                        onBack = onBack
                    )
                } else {
                    LoadingBox()
                }
            }
            "unsupported" -> {
                UnsupportedFileScreen(
                    file = s3Object,
                    onBack = onBack,
                    onOpenExternal = { viewModel.openExternal(s3Object) }
                )
            }
        }
    }
}

@Composable
fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorBox(message: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "加载错误", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) {
                Text("返回")
            }
        }
    }
}
