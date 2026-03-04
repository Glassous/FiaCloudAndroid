package com.glassous.fiacloud.ui.home

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Base64

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb

@Composable
fun MarkdownPreview(content: String) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val accentColor = MaterialTheme.colorScheme.primary
    val bgInt = backgroundColor.toArgb()

    fun colorToHex(color: androidx.compose.ui.graphics.Color): String {
        return String.format("#%06X", (0xFFFFFF and color.toArgb()))
    }

    val bgHex = colorToHex(backgroundColor)
    val textHex = colorToHex(textColor)
    val accentHex = colorToHex(accentColor)

    val escapedContent = remember(content) {
        content
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")
    }

    val html = remember(escapedContent, bgHex, textHex, accentHex) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css">
            <script src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js"></script>
            <script src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js"></script>
            <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
            <style>
                body { 
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    padding: 16px;
                    line-height: 1.6;
                    word-wrap: break-word;
                    background-color: $bgHex;
                    color: $textHex;
                }
                pre { background: #f6f8fa; padding: 16px; border-radius: 6px; overflow-x: auto; color: #24292e; }
                code { font-family: ui-monospace, SFMono-Regular, SF Mono, Menlo, Consolas, Liberation Mono, monospace; font-size: 85%; }
                img { max-width: 100%; }
                blockquote { border-left: 4px solid #dfe2e5; color: #6a737d; padding-left: 16px; margin: 0; }
                table { border-collapse: collapse; width: 100%; margin-bottom: 16px; }
                table th, table td { border: 1px solid #dfe2e5; padding: 6px 13px; }
                table tr:nth-child(2n) { background-color: #f6f8fa; }
                a { color: $accentHex; }
            </style>
        </head>
        <body>
            <div id="content"></div>
            <script>
                document.getElementById('content').innerHTML = marked.parse(`${escapedContent}`);
                renderMathInElement(document.body, {
                    delimiters: [
                        {left: "$$", right: "$$", display: true},
                        {left: "$", right: "$", display: false},
                        {left: "\\(", right: "\\)", display: false},
                        {left: "\\[", right: "\\]", display: true}
                    ],
                    throwOnError : false
                });
            </script>
        </body>
        </html>
    """.trimIndent()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(bgInt) // 立即设置背景色，防止白屏
            }
        },
        update = { webView ->
            // 仅在 HTML 内容发生变化时加载，减少闪烁
            val currentHtml = webView.tag as? String
            if (currentHtml != html) {
                val encodedHtml = Base64.getEncoder().encodeToString(html.toByteArray())
                webView.loadData(encodedHtml, "text/html", "base64")
                webView.tag = html
            }
        }
    )
}
