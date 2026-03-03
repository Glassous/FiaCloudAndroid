package com.glassous.fiacloud.ui.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object SyntaxHighlighter {
    private val keywordColor = Color(0xFFCC7832)
    private val stringColor = Color(0xFF6A8759)
    private val commentColor = Color(0xFF808080)
    private val numberColor = Color(0xFF6897BB)
    private val typeColor = Color(0xFF9876AA)

    private val commonKeywords = setOf(
        "abstract", "as", "break", "case", "catch", "class", "continue", "default", "do", "else", "extends", "false",
        "final", "finally", "for", "if", "implements", "import", "in", "instanceof", "interface", "is", "new", "null",
        "package", "private", "protected", "public", "return", "static", "super", "switch", "this", "throw", "true",
        "try", "val", "var", "when", "while"
    )

    private val cFamilyKeywords = commonKeywords + setOf(
        "auto", "bool", "char", "const", "double", "enum", "extern", "float", "int", "long", "register", "short",
        "signed", "sizeof", "struct", "typedef", "union", "unsigned", "void", "volatile"
    )

    private val pythonKeywords = commonKeywords + setOf(
        "and", "assert", "def", "del", "elif", "except", "from", "global", "lambda", "not", "or", "pass", "raise", "with", "yield"
    )

    fun highlight(text: String, extension: String): AnnotatedString {
        val keywords = when (extension) {
            "py" -> pythonKeywords
            "c", "cpp", "h", "java" -> cFamilyKeywords
            else -> commonKeywords
        }

        return buildAnnotatedString {
            var index = 0
            while (index < text.length) {
                val char = text[index]
                
                when {
                    // String literal
                    char == '"' || char == '\'' -> {
                        val start = index
                        val quote = char
                        index++
                        while (index < text.length && text[index] != quote) {
                            if (text[index] == '\\' && index + 1 < text.length) index++
                            index++
                        }
                        if (index < text.length) index++
                        withStyle(SpanStyle(color = stringColor)) {
                            append(text.substring(start, index))
                        }
                    }
                    // Comment
                    char == '/' && index + 1 < text.length && text[index + 1] == '/' -> {
                        val start = index
                        while (index < text.length && text[index] != '\n') index++
                        withStyle(SpanStyle(color = commentColor)) {
                            append(text.substring(start, index))
                        }
                    }
                    // Python Comment
                    char == '#' && extension == "py" -> {
                        val start = index
                        while (index < text.length && text[index] != '\n') index++
                        withStyle(SpanStyle(color = commentColor)) {
                            append(text.substring(start, index))
                        }
                    }
                    // Keyword or Identifier
                    char.isLetter() || char == '_' -> {
                        val start = index
                        while (index < text.length && (text[index].isLetterOrDigit() || text[index] == '_')) index++
                        val word = text.substring(start, index)
                        if (keywords.contains(word)) {
                            withStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)) {
                                append(word)
                            }
                        } else {
                            append(word)
                        }
                    }
                    // Number
                    char.isDigit() -> {
                        val start = index
                        while (index < text.length && text[index].isDigit()) index++
                        withStyle(SpanStyle(color = numberColor)) {
                            append(text.substring(start, index))
                        }
                    }
                    else -> {
                        append(char)
                        index++
                    }
                }
            }
        }
    }
}
