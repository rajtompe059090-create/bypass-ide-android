package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ai.ActionParser
import com.example.ai.AiMessage
import com.example.ai.AiSession
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    aiSession: AiSession,
    onOpenPreview: (() -> Unit)? = null
) {
    var prompt by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    val messages = aiSession.chatHistory
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "BYPASS IDE",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Advanced Root AI Engine",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Standard mode active",
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = "Grant root for full POSIX & Termux execution.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (isThinking) {
                item {
                    Text(
                        text = "Thinking...",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Ask Bypass AI anything...")
                },
                enabled = !isThinking,
                maxLines = 4
            )

            Button(
                onClick = {
                    val userPrompt = prompt.trim()

                    if (userPrompt.isEmpty() || isThinking) {
                        return@Button
                    }

                    prompt = ""
                    isThinking = true

                    scope.launch {
                        try {
                            val response =
                                aiSession.generateResponse(userPrompt)

                            val actions =
                                ActionParser.parse(response)

                            if (actions.isNotEmpty()) {
                                val results =
                                    ActionParser.execute(
                                        actions = actions,
                                        workspace = aiSession.workspace
                                    )

                                if (results.isNotEmpty()) {
                                    aiSession.chatHistory.add(
                                        AiMessage(
                                            text = results.joinToString("\n"),
                                            isUser = false
                                        )
                                    )
                                }

                                if (
                                    results.any {
                                        it.startsWith("PREVIEW_READY:")
                                    }
                                ) {
                                    onOpenPreview?.invoke()
                                }
                            }

                            val lowerPrompt = userPrompt.lowercase()

                            if (
                                lowerPrompt.contains("preview") ||
                                lowerPrompt.contains("live preview")
                            ) {
                                onOpenPreview?.invoke()
                            }

                        } catch (e: Exception) {
                            aiSession.chatHistory.add(
                                AiMessage(
                                    text = "Error: ${e.message ?: "Unknown error"}",
                                    isUser = false,
                                    isError = true
                                )
                            )
                        } finally {
                            isThinking = false
                        }
                    }
                },
                enabled = !isThinking
            ) {
                Text("Send")
            }
        }
    }
}
