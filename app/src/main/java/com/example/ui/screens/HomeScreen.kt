package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ai.ActionParser
import com.example.ai.AiSession
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    aiSession: AiSession,
    onOpenPreview: (() -> Unit)? = null
) {
    var prompt by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<Pair<String, Boolean>>() }
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
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.first,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (isThinking) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Thinking...",
                            modifier = Modifier.padding(12.dp)
                        )
                    }
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
                maxLines = 4
            )

            Button(
                onClick = {
                    val userPrompt = prompt.trim()

                    if (userPrompt.isEmpty() || isThinking) {
                        return@Button
                    }

                    prompt = ""
                    messages.add(userPrompt to true)
                    isThinking = true

                    scope.launch {
                        try {
                            val response = aiSession.generateResponse(userPrompt)

                            messages.add(response to false)

                            val actions = ActionParser.parse(response)

                            if (actions.isNotEmpty()) {
                                val workspace = aiSession.getWorkspace()

                                val results = ActionParser.execute(
                                    actions = actions,
                                    workspace = workspace
                                )

                                if (results.isNotEmpty()) {
                                    messages.add(
                                        results.joinToString("\n") to false
                                    )

                                    if (
                                        results.any {
                                            it.startsWith("PREVIEW_READY:")
                                        }
                                    ) {
                                        onOpenPreview?.invoke()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            messages.add(
                                "Error: ${e.message ?: "Unknown error"}" to false
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
