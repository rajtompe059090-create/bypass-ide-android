package com.example.ui.screens

import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.BgColor
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.MutedTextColor
import com.example.ui.theme.SurfaceColor
import java.io.File

@Composable
fun PreviewScreen(
    workspace: File
) {

    val context = LocalContext.current

    val htmlFile = remember(workspace.absolutePath) {
        File(workspace, "RajTest/index.html")
    }

    var webViewRef by remember {
        mutableStateOf<WebView?>(null)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = "LIVE PREVIEW",
                color = CyanAccent,
                fontSize = 14.sp
            )

            IconButton(
                onClick = {
                    webViewRef?.reload()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = CyanAccent
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            if (!htmlFile.exists()) {

                Text(
                    text = """
                        No HTML File Found.

                        Expected:
                        ${htmlFile.absolutePath}
                    """.trimIndent(),
                    color = MutedTextColor,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )

            } else {

                AndroidView(
                    modifier = Modifier.fillMaxSize(),

                    factory = { ctx ->

                        WebView(ctx).apply {

                            webViewRef = this

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true

                            settings.allowFileAccessFromFileURLs = true
                            settings.allowUniversalAccessFromFileURLs = true

                            settings.mixedContentMode =
                                WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                            webViewClient =
                                WebViewClient()

                            webChromeClient =
                                WebChromeClient()

                            loadUrl(
                                "file://${htmlFile.absolutePath}"
                            )
                        }
                    },

                    update = { view ->

                        webViewRef = view

                        if (
                            htmlFile.exists() &&
                            view.url !=
                            "file://${htmlFile.absolutePath}"
                        ) {
                            view.loadUrl(
                                "file://${htmlFile.absolutePath}"
                            )
                        }
                    }
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                destroy()
            }
            webViewRef = null
        }
    }
}
