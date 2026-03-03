package com.glassous.fiacloud.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glassous.fiacloud.data.S3Repository

@Composable
fun MarkdownPreview(content: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Simplified Markdown preview since external library is unavailable
        Column {
            content.lines().forEach { line ->
                when {
                    line.startsWith("# ") -> Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                    line.startsWith("## ") -> Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                    )
                    line.startsWith("### ") -> Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                    line.startsWith("- ") || line.startsWith("* ") -> Text(
                        text = "• ${line.substring(2)}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 2.dp)
                    )
                    line.isBlank() -> Spacer(modifier = Modifier.height(8.dp))
                    else -> Text(
                        text = line,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                    )
                }
            }
        }
    }
}
