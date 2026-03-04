package com.glassous.fiacloud.ui.home

import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaPlayer
import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.glassous.fiacloud.data.S3Repository
import com.glassous.fiacloud.ui.home.utils.LockScreenOrientation
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    file: File?,
    item: S3Repository.S3Object,
    onBack: () -> Unit
) {
    if (file == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val extension = file.extension.lowercase()
    val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "ico")
    val videoExtensions = setOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "ts")
    val audioExtensions = setOf("mp3", "wav", "ogg", "m4a", "aac", "flac", "amr", "mid", "midi")

    var isFullscreen by remember { mutableStateOf(false) }
    var rotation by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current
    DisposableEffect(isFullscreen) {
        val window = (context as? android.app.Activity)?.window ?: return@DisposableEffect onDispose {}
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        if (isFullscreen) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = { Text(item.displayName) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (imageExtensions.contains(extension) || videoExtensions.contains(extension)) {
                            IconButton(onClick = { rotation += 90f }) {
                                Icon(Icons.Default.RotateRight, contentDescription = "Rotate")
                            }
                            IconButton(onClick = { isFullscreen = !isFullscreen }) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen"
                                )
                            }
                        }
                    }
                )
            } else {
                // In fullscreen mode, show a small overlay button to exit fullscreen
                Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.TopEnd) {
                    IconButton(
                        onClick = { isFullscreen = false },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit Fullscreen",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(top = if (isFullscreen) 0.dp else padding.calculateTopPadding())
                .fillMaxSize()
                .background(if (audioExtensions.contains(extension)) MaterialTheme.colorScheme.background else Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (imageExtensions.contains(extension)) {
                ZoomableContent(rotation = rotation) {
                    if (extension == "gif" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        AndroidView(
                            factory = { context ->
                                ImageView(context).apply {
                                    try {
                                        val source = ImageDecoder.createSource(file)
                                        val drawable = ImageDecoder.decodeDrawable(source)
                                        if (drawable is android.graphics.drawable.AnimatedImageDrawable) {
                                            drawable.start()
                                        }
                                        setImageDrawable(drawable)
                                    } catch (e: Exception) {
                                        // Fallback to static bitmap if decoding fails
                                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                        setImageBitmap(bitmap)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val bitmap = remember(file) {
                            BitmapFactory.decodeFile(file.absolutePath)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("无法加载图片", color = Color.White)
                        }
                    }
                }
            } else if (audioExtensions.contains(extension)) {
                AudioPlayer(file)
            } else if (videoExtensions.contains(extension)) {
                ZoomableContent(rotation = rotation) {
                    AndroidView(
                        factory = { context ->
                            VideoView(context).apply {
                                setVideoPath(file.absolutePath)
                                setMediaController(MediaController(context))
                                start()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Text("不支持的媒体格式", color = Color.White)
            }
        }
    }
}

@Composable
fun ZoomableContent(
    rotation: Float,
    content: @Composable () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale == 1f) {
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    } else {
                        val maxOffset = (scale - 1) * size.width / 2
                        offset = androidx.compose.ui.geometry.Offset(
                            (offset.x + pan.x * scale).coerceIn(-maxOffset, maxOffset),
                            (offset.y + pan.y * scale).coerceIn(-maxOffset, maxOffset)
                        )
                    }
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y,
                rotationZ = rotation
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun AudioPlayer(file: File) {
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    DisposableEffect(file) {
        val mp = MediaPlayer().apply {
            try {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener { 
                    isPlaying = false 
                    currentPosition = 0f
                    seekTo(0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = mp
        duration = mp.duration.toFloat()
        
        onDispose {
            mp.release()
        }
    }

    LaunchedEffect(mediaPlayer, isPlaying, isDragging) {
        while (isPlaying && !isDragging) {
            mediaPlayer?.let { mp ->
                try {
                    currentPosition = mp.currentPosition.toFloat()
                } catch (e: Exception) {
                    // Ignore errors during playback polling
                }
            }
            delay(100) // Update every 100ms
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = FileIconUtils.getFileIcon(file.name, false),
            contentDescription = null,
            modifier = Modifier.size(128.dp),
            tint = FileIconUtils.getFileIconColor(file.name, false)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = formatTime(currentPosition.toLong()) + " / " + formatTime(duration.toLong()),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Slider(
            value = currentPosition,
            onValueChange = { 
                currentPosition = it 
                isDragging = true
            },
            onValueChangeFinished = {
                mediaPlayer?.seekTo(currentPosition.toInt())
                isDragging = false
            },
            valueRange = 0f..duration,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        IconButton(
            onClick = {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        mp.pause()
                        isPlaying = false
                    } else {
                        mp.start()
                        isPlaying = true
                    }
                }
            },
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

fun formatTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, seconds)
}
