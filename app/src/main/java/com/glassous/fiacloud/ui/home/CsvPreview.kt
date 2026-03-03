package com.glassous.fiacloud.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CsvPreview(content: String) {
    val rows = remember(content) {
        content.lines()
            .filter { it.isNotBlank() }
            .map { line ->
                // Simple CSV parsing, might need a library for complex cases
                line.split(",").map { it.trim() }
            }
    }

    if (rows.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("CSV 内容为空")
        }
        return
    }

    val horizontalScrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
                .padding(16.dp)
        ) {
            items(rows) { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEachIndexed { index, cell ->
                        val isHeader = rows.indexOf(row) == 0
                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = cell,
                                style = if (isHeader) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) 
                                        else MaterialTheme.typography.bodyMedium,
                                color = if (isHeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
