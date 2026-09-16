package com.example.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
fun PreviewScreen() {
    val context = LocalContext.current
    val bypassProjectsRoot = remember { File(context.filesDir, "BypassProjects").apply { mkdirs() } }
    val currentFile = AppState.currentFile
    
    // Find the project directory by traversing up from the current file until the parent is BypassProjects
    val projectDir = remember(currentFile) {
        var dir = currentFile?.parentFile
        while (dir != null && dir.parentFile?.absolutePath != bypassProjectsRoot.absolutePath) {
            dir = dir.parentFile
        }
        dir ?: bypassProjectsRoot
    }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().background(SurfaceColor).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("LIVE PREVIEW", color = CyanAccent, fontSize = 14.sp)
            var webViewRef by remember { mutableStateOf<WebView?>(null) }
            IconButton(onClick = { webViewRef?.reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = CyanAccent)
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val htmlFile = File(projectDir, "index.html")
            if (projectDir == bypassProjectsRoot && !htmlFile.exists() && currentFile?.extension != "html") {
                Text("Open an HTML file or a project folder in Files to preview", color = MutedTextColor, modifier = Modifier.align(Alignment.Center))
            } else {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            settings.allowFileAccessFromFileURLs = true
                            settings.allowUniversalAccessFromFileURLs = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                            
                            if (htmlFile.exists()) {
                                loadUrl("file://${htmlFile.absolutePath}")
                            } else if (currentFile != null && currentFile.extension == "html") {
                                loadUrl("file://${currentFile.absolutePath}")
                            } else {
                                loadDataWithBaseURL(null, "<html><body style='background-color:#0b1114;color:#a0aab0;text-align:center;padding:50px;font-family:sans-serif;'><h3>No index.html found in project.</h3></body></html>", "text/html", "UTF-8", null)
                            }
                        }
                    },
                    update = { view ->
                        // Re-load if needed when projectDir changes
                        if (htmlFile.exists()) {
                            if (view.url != "file://${htmlFile.absolutePath}") {
                                view.loadUrl("file://${htmlFile.absolutePath}")
                            }
                        } else if (currentFile != null && currentFile.extension == "html") {
                            if (view.url != "file://${currentFile.absolutePath}") {
                                view.loadUrl("file://${currentFile.absolutePath}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
