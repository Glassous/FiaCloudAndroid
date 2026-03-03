package com.glassous.fiacloud.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.google.gson.*

@Composable
fun JsonPreview(content: String) {
    val jsonElement = try {
        JsonParser.parseString(content)
    } catch (e: Exception) {
        null
    }

    if (jsonElement == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("JSON 解析失败", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            JsonNode(element = jsonElement, name = null)
        }
    }
}

@Composable
fun JsonNode(element: JsonElement, name: String?, depth: Int = 0) {
    var expanded by remember { mutableStateOf(true) }

    val indentation = depth * 16

    Column(modifier = Modifier.padding(start = indentation.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = element.isJsonObject || element.isJsonArray) {
                    expanded = !expanded
                }
                .padding(vertical = 4.dp)
        ) {
            if (element.isJsonObject || element.isJsonArray) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(modifier = Modifier.size(16.dp))
            }

            if (name != null) {
                Text(
                    text = "\"$name\": ",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (element.isJsonPrimitive) {
                val primitive = element.asJsonPrimitive
                val color = when {
                    primitive.isString -> MaterialTheme.colorScheme.tertiary
                    primitive.isNumber -> MaterialTheme.colorScheme.primary
                    primitive.isBoolean -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Text(
                    text = element.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = color
                )
            } else if (element.isJsonNull) {
                Text(
                    text = "null",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.outline
                )
            } else if (element.isJsonObject) {
                Text(
                    text = if (expanded) "{" else "{ ... }",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else if (element.isJsonArray) {
                Text(
                    text = if (expanded) "[" else "[ ... ]",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (expanded) {
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                obj.entrySet().forEach { entry ->
                    JsonNode(element = entry.value, name = entry.key, depth = 1)
                }
                Text(
                    text = "}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else if (element.isJsonArray) {
                val array = element.asJsonArray
                array.forEachIndexed { index, item ->
                    JsonNode(element = item, name = null, depth = 1)
                }
                Text(
                    text = "]",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
