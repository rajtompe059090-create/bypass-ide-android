package com.example.ai

import kotlinx.coroutines.delay

class MockAiProvider : AiProvider {
    override val name = "Mock AI Provider (Offline)"
    override val isConfigured = true

    override suspend fun generateResponse(messages: List<AiMessage>, context: String): AiResponse {
        delay(1000)
        
        val userPrompt = messages.lastOrNull { it.isUser }?.text ?: ""
        if (userPrompt.contains("raj test", ignoreCase = true)) {
            return AiResponse(
                "I will create the Raj Test website for you locally.\\n\\n" +
                "<file path=\"RajTest/index.html\">\\n" +
                "<!DOCTYPE html>\\n<html lang=\"en\">\\n<head>\\n<title>Raj Test</title>\\n<link rel=\"stylesheet\" href=\"css/styles.css\">\\n</head>\\n" +
                "<body>\\n<h1>Raj Test (Mock Builder)</h1>\\n<button id=\"test-button\">Test Button</button>\\n<p id=\"message-area\"></p>\\n<script src=\"js/app.js\"></script>\\n</body>\\n</html>\\n" +
                "</file>\\n\\n" +
                "<file path=\"RajTest/css/styles.css\">\\n" +
                "body { background: #0b1114; color: #a0aab0; font-family: sans-serif; text-align: center; margin-top: 50px; }\\nh1 { color: #00e5ff; }\\n" +
                "</file>\\n\\n" +
                "<file path=\"RajTest/js/app.js\">\\n" +
                "document.getElementById('test-button').addEventListener('click', function() { document.getElementById('message-area').innerText = 'Action executed successfully!'; });\\n" +
                "</file>"
            )
        }
        
        return AiResponse("This is a mock AI response. Please connect a real Gemini API key in Settings for full functionality.\n\nYou asked: \$userPrompt")
    }
}
