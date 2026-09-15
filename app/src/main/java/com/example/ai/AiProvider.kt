package com.example.ai

interface AiProvider {
    suspend fun generateResponse(messages: List<AiMessage>, context: String = ""): AiResponse
    val name: String
    val isConfigured: Boolean
}

data class AiMessage(
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val isLoading: Boolean = false
)

data class AiResponse(
    val text: String,
    val isError: Boolean = false
)

enum class AiProviderState {
    MOCK, GEMINI
}
