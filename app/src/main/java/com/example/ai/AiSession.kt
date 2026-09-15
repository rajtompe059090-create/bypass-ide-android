package com.example.ai

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class AiSession(context: Context) {

    private val appContext = context.applicationContext

    private val prefs = appContext.getSharedPreferences(
        "BypassIDESettings",
        Context.MODE_PRIVATE
    )

    var apiKey: String =
        prefs.getString("gemini_api_key", "") ?: ""
        set(value) {
            field = value.trim()

            prefs.edit()
                .putString("gemini_api_key", field)
                .apply()

            updateProvider()
        }

    var selectedProviderState: AiProviderState =
        AiProviderState.GEMINI
        set(value) {
            field = value

            prefs.edit()
                .putString("ai_provider", value.name)
                .apply()

            updateProvider()
        }

    private val _provider =
        MutableStateFlow<AiProvider>(MockAiProvider())

    val provider: StateFlow<AiProvider> = _provider

    val chatHistory =
        mutableStateListOf<AiMessage>()

    /**
     * Shared workspace used by AI actions, Files and Preview.
     */
    val workspace: File by lazy {
        File(
            appContext.getExternalFilesDir(null),
            "BypassProjects"
        ).apply {
            mkdirs()
        }
    }

    init {
        val savedProvider =
            prefs.getString(
                "ai_provider",
                AiProviderState.GEMINI.name
            )

        selectedProviderState =
            try {
                AiProviderState.valueOf(
                    savedProvider ?: AiProviderState.GEMINI.name
                )
            } catch (_: Exception) {
                AiProviderState.GEMINI
            }

        updateProvider()
    }

    private fun updateProvider() {
        _provider.value =
            if (
                selectedProviderState == AiProviderState.GEMINI &&
                apiKey.isNotBlank()
            ) {
                GeminiAiProvider(apiKey)
            } else {
                MockAiProvider()
            }
    }

    /**
     * Sends the user's prompt to the active AI provider.
     *
     * Uses the project's actual AiProvider interface:
     * generateResponse(messages, context)
     */
    suspend fun generateResponse(prompt: String): String {

        val cleanPrompt = prompt.trim()

        if (cleanPrompt.isEmpty()) {
            return ""
        }

        val userMessage = AiMessage(
            text = cleanPrompt,
            isUser = true
        )

        chatHistory.add(userMessage)

        return try {

            val result = _provider.value.generateResponse(
                messages = chatHistory.toList(),
                context = buildWorkspaceContext()
            )

            val responseText = result.text

            if (responseText.isNotBlank()) {
                chatHistory.add(
                    AiMessage(
                        text = responseText,
                        isUser = false,
                        isError = result.isError
                    )
                )
            }

            responseText

        } catch (e: Exception) {

            val errorText =
                "Gemini API Error: ${e.message ?: "Unknown error"}"

            chatHistory.add(
                AiMessage(
                    text = errorText,
                    isUser = false,
                    isError = true
                )
            )

            errorText
        }
    }

    /**
     * Gives the AI basic information about the current workspace.
     */
    private fun buildWorkspaceContext(): String {

        return try {

            if (!workspace.exists()) {
                workspace.mkdirs()
            }

            val files = workspace
                .walkTopDown()
                .filter { it.isFile }
                .take(100)
                .map {
                    it.relativeTo(workspace).path
                }
                .toList()

            buildString {
                append("Workspace: ")
                append(workspace.absolutePath)
                append("\n")

                if (files.isEmpty()) {
                    append("Files: (empty)")
                } else {
                    append("Files:\n")
                    files.forEach {
                        append("- ")
                        append(it)
                        append("\n")
                    }
                }
            }

        } catch (_: Exception) {
            "Workspace: ${workspace.absolutePath}"
        }
    }

    fun getWorkspace(): File {
        if (!workspace.exists()) {
            workspace.mkdirs()
        }

        return workspace
    }

    fun activateGemini(key: String) {
        apiKey = key
        selectedProviderState = AiProviderState.GEMINI
    }

    fun activateMock() {
        selectedProviderState = AiProviderState.MOCK
    }
}
