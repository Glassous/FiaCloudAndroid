package com.glassous.fiacloud.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object FileIconUtils {
    fun getFileIcon(fileName: String, isFolder: Boolean): ImageVector {
        if (isFolder) return Icons.Default.Folder
        
        val extension = fileName.substringAfterLast(".", "").lowercase()
        return when (extension) {
            // Office
            "doc", "docx" -> Icons.AutoMirrored.Filled.Article
            "xls", "xlsx" -> Icons.Default.TableChart
            "ppt", "pptx" -> Icons.Default.Slideshow
            "pdf" -> Icons.Default.PictureAsPdf
            
            // Text & Data
            "txt", "log", "ini", "conf", "config", "env" -> Icons.AutoMirrored.Filled.Article
            "json", "xml", "yaml", "yml" -> Icons.Default.Code
            "csv" -> Icons.Default.GridOn
            
            // Code
            "cpp", "c", "h", "hpp", "java", "kt", "py", "js", "ts", "html", "css", "sh", "bat", "sql" -> Icons.Default.Code
            "md", "markdown" -> Icons.AutoMirrored.Filled.MenuBook
            
            // Media
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "ico" -> Icons.Default.Image
            "mp4", "mkv", "webm", "avi", "mov", "3gp", "ts" -> Icons.Default.Movie
            "mp3", "wav", "ogg", "m4a", "aac", "flac", "amr", "mid", "midi" -> Icons.Default.AudioFile
            
            // Archives
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> Icons.Default.FolderZip
            
            // Others
            "apk" -> Icons.Default.Android
            "exe", "msi", "bat", "sh", "cmd" -> Icons.Default.Terminal
            "ttf", "otf", "woff", "woff2" -> Icons.Default.FontDownload
            "iso", "img", "dmg" -> Icons.Default.DiscFull
            "key", "crt", "pem", "der" -> Icons.Default.VpnKey
            "db", "sqlite", "sqlite3" -> Icons.Default.Storage
            
            else -> Icons.AutoMirrored.Filled.InsertDriveFile
        }
    }

    @Composable
    fun getFileIconColor(fileName: String, isFolder: Boolean): Color {
        if (isFolder) return MaterialTheme.colorScheme.primary
        
        val extension = fileName.substringAfterLast(".", "").lowercase()
        return when (extension) {
            // Office
            "doc", "docx" -> Color(0xFF2B579A) // Word Blue
            "xls", "xlsx" -> Color(0xFF217346) // Excel Green
            "ppt", "pptx" -> Color(0xFFD24726) // PPT Orange
            "pdf" -> Color(0xFFF40F02) // PDF Red
            
            // Text & Data
            "txt", "log", "ini", "conf", "config", "env" -> Color.Gray
            "json", "xml", "yaml", "yml" -> Color(0xFFE34C26)
            "csv" -> Color(0xFF217346)
            
            // Code
            "cpp", "c", "h", "hpp" -> Color(0xFF90CAF9) // Light Blue 200
            "java" -> Color(0xFFFFB74D) // Orange 300
            "kt" -> Color(0xFFB388FF) // Purple 100
            "py" -> Color(0xFF81D4FA) // Light Blue 200
            "js", "ts" -> Color(0xFFFFF176) // Yellow 300
            "html" -> Color(0xFFFF8A65) // Deep Orange 300
            "css" -> Color(0xFF4FC3F7) // Light Blue 300
            "md", "markdown" -> Color(0xFF80DEEA) // Cyan 200
            
            // Media
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "ico" -> Color(0xFF4CAF50) // Green
            "mp4", "mkv", "webm", "avi", "mov", "3gp", "ts" -> Color(0xFFFF9800) // Orange
            "mp3", "wav", "ogg", "m4a", "aac", "flac", "amr", "mid", "midi" -> Color(0xFF9C27B0) // Purple
            
            // Archives
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> Color(0xFFFFC107) // Amber
            
            // Others
            "apk" -> Color(0xFF3DDC84) // Android Green
            "exe", "msi", "bat", "sh", "cmd" -> Color(0xFF424242)
            "ttf", "otf", "woff", "woff2" -> Color(0xFF795548)
            "iso", "img", "dmg" -> Color(0xFF607D8B)
            "key", "crt", "pem", "der" -> Color(0xFFFF5722)
            "db", "sqlite", "sqlite3" -> Color(0xFF3F51B5)
            
            else -> MaterialTheme.colorScheme.secondary
        }
    }
}
