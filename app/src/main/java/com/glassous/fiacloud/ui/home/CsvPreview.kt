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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

import androidx.compose.foundation.interaction.MutableInteractionSource

@Composable
fun CsvPreview(
    content: String,
    onCellSelected: (String?) -> Unit
) {
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                selectedCell = null
                onCellSelected(null)
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
                .padding(16.dp)
        ) {
            itemsIndexed(rows) { rowIndex, row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEachIndexed { colIndex, cell ->
                        val isHeader = rowIndex == 0
                        val isSelected = selectedCell?.first == rowIndex && selectedCell?.second == colIndex
                        
                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                    else Color.Transparent
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (isSelected) {
                                        selectedCell = null
                                        onCellSelected(null)
                                    } else {
                                        selectedCell = rowIndex to colIndex
                                        onCellSelected(cell)
                                    }
                                }
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary 
                                            else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = cell,
                                style = if (isHeader) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) 
                                        else MaterialTheme.typography.bodyMedium,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isHeader -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
