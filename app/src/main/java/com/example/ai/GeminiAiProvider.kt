package com.example.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAiProvider(private val apiKey: String) : AiProvider {
    override val name = "Gemini 1.5 Pro"
    override val isConfigured = apiKey.isNotBlank()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun generateResponse(messages: List<AiMessage>, context: String): AiResponse {
        if (!isConfigured) {
            return AiResponse("Gemini API key is not configured.", isError = true)
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=\$apiKey"
                
                val contents = JSONArray()
                
                // Add system context if provided
                val fullContext = if (context.isNotBlank()) {
                    "You are the Bypass IDE AI Assistant. You help users build projects. You can return actions like <file path=\"index.html\">content</file>, <mkdir path=\"css\"/>, <command>npm install</command>.\n\nProject Context:\n\$context"
                } else {
                    "You are the Bypass IDE AI Assistant. You help users build projects. You can return actions like <file path=\"index.html\">content</file>, <mkdir path=\"css\"/>, <command>npm install</command>."
                }
                
                // Build history
                for (msg in messages) {
                    if (msg.isLoading || msg.isError) continue
                    val part = JSONObject().put("text", msg.text)
                    val content = JSONObject()
                        .put("role", if (msg.isUser) "user" else "model")
                        .put("parts", JSONArray().put(part))
                    contents.put(content)
                }
                
                val systemInstruction = JSONObject().put("parts", JSONArray().put(JSONObject().put("text", fullContext)))
                
                val requestBody = JSONObject()
                    .put("contents", contents)
                    .put("systemInstruction", systemInstruction)
                    .toString()

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text")
                            return@withContext AiResponse(text)
                        }
                    }
                    AiResponse("Empty response from Gemini API.", isError = true)
                } else {
                    AiResponse("Error: \${response.code} \${response.message}\\n\$responseBody", isError = true)
                }
            } catch (e: Exception) {
                AiResponse("Exception: \${e.message}", isError = true)
            }
        }
    }
}
