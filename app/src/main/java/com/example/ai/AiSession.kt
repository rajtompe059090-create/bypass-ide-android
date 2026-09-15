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
     * Shared workspace used by Files, Agent actions and Preview.
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
                AiProviderState.valueOf(savedProvider ?: AiProviderState.GEMINI.name)
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
     * Send a prompt to the currently selected AI provider.
     */
    suspend fun generateResponse(prompt: String): String {
        val cleanPrompt = prompt.trim()

        if (cleanPrompt.isEmpty()) {
            return ""
        }

        val userMessage = AiMessage(
            role = "user",
            content = cleanPrompt
        )

        chatHistory.add(userMessage)

        return try {
            val result = _provider.value.generate(
                messages = chatHistory.toList()
            )

            chatHistory.add(
                AiMessage(
                    role = "assistant",
                    content = result
                )
            )

            result
        } catch (e: Exception) {
            val error =
                e.message ?: "Unknown AI error"

            val message =
                "Gemini API Error: $error"

            chatHistory.add(
                AiMessage(
                    role = "assistant",
                    content = message
                )
            )

            message
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
