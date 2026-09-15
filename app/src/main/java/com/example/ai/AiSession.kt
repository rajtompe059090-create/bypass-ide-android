package com.example.ai

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AiSession(context: Context) {

    private val prefs = context.getSharedPreferences(
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

    val provider: StateFlow<AiProvider> =
        _provider

    val chatHistory =
        mutableStateListOf<AiMessage>()

    init {
        val savedProvider =
            prefs.getString(
                "ai_provider",
                AiProviderState.GEMINI.name
            )

        selectedProviderState =
            try {
                AiProviderState.valueOf(savedProvider!!)
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

    fun activateGemini(key: String) {
        apiKey = key
        selectedProviderState = AiProviderState.GEMINI
    }

    fun activateMock() {
        selectedProviderState = AiProviderState.MOCK
    }
}
