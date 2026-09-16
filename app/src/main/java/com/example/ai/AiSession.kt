package com.example.ai

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AiSession(context: Context) {
    private val prefs = context.getSharedPreferences("BypassIDESettings", Context.MODE_PRIVATE)
    
    var apiKey: String = prefs.getString("gemini_api_key", "") ?: ""
        set(value) {
            field = value
            prefs.edit().putString("gemini_api_key", value).apply()
            updateProvider()
        }
        
    var selectedProviderState: AiProviderState = AiProviderState.valueOf(prefs.getString("ai_provider", AiProviderState.MOCK.name) ?: AiProviderState.MOCK.name)
        set(value) {
            field = value
            prefs.edit().putString("ai_provider", value.name).apply()
            updateProvider()
        }
        
    private val _provider = MutableStateFlow<AiProvider>(MockAiProvider())
    val provider: StateFlow<AiProvider> = _provider
    
    val chatHistory = mutableStateListOf<AiMessage>()

    init {
        updateProvider()
    }

    private fun updateProvider() {
        if (selectedProviderState == AiProviderState.GEMINI && apiKey.isNotBlank()) {
            _provider.value = GeminiAiProvider(apiKey)
        } else {
            _provider.value = MockAiProvider()
        }
    }
}
