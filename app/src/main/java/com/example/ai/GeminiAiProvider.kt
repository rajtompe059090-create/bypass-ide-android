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

class GeminiAiProvider(
    private val apiKey: String
) : AiProvider {

    override val name = "Gemini"
    override val isConfigured = apiKey.isNotBlank()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun generateResponse(
        messages: List<AiMessage>,
        context: String
    ): AiResponse {

        if (apiKey.isBlank()) {
            return AiResponse(
                "Gemini API key is not configured.",
                isError = true
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val url =
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

                val contents = JSONArray()

                for (message in messages) {
                    if (message.isLoading || message.isError) continue

                    val role = if (message.isUser) "user" else "model"

                    val part = JSONObject()
                        .put("text", message.text)

                    val content = JSONObject()
                        .put("role", role)
                        .put(
                            "parts",
                            JSONArray().put(part)
                        )

                    contents.put(content)
                }

                val systemPrompt = """
                    You are Bypass IDE AI Assistant.

                    You are an autonomous Android and software development assistant.

                    Help the user:
                    - Build Android apps
                    - Create websites
                    - Create and edit project files
                    - Debug code
                    - Explain errors
                    - Generate Kotlin, JavaScript, HTML, CSS and other code
                    - Work with project structures

                    When the user asks to create files, you may return commands using these formats:

                    <file path="index.html">
                    file content
                    </file>

                    <mkdir path="css"/>

                    <command>
                    command here
                    </command>

                    Always give useful, practical answers.

                    Project context:
                    $context
                """.trimIndent()

                val systemInstruction = JSONObject()
                    .put(
                        "parts",
                        JSONArray().put(
                            JSONObject().put(
                                "text",
                                systemPrompt
                            )
                        )
                    )

                val requestBody = JSONObject()
                    .put("systemInstruction", systemInstruction)
                    .put("contents", contents)
                    .put(
                        "generationConfig",
                        JSONObject()
                            .put("temperature", 0.7)
                            .put("maxOutputTokens", 8192)
                    )
                    .toString()

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(
                        requestBody.toRequestBody(
                            "application/json".toMediaType()
                        )
                    )
                    .build()

                client.newCall(request).execute().use { response ->

                    val responseBody =
                        response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        val errorMessage = try {
                            val errorJson = JSONObject(responseBody)
                            errorJson
                                .optJSONObject("error")
                                ?.optString("message")
                                ?: responseBody
                        } catch (_: Exception) {
                            responseBody
                        }

                        return@withContext AiResponse(
                            "Gemini API Error (${response.code}): $errorMessage",
                            isError = true
                        )
                    }

                    val json = JSONObject(responseBody)

                    val candidates =
                        json.optJSONArray("candidates")

                    if (candidates == null || candidates.length() == 0) {
                        return@withContext AiResponse(
                            "Gemini returned no response.",
                            isError = true
                        )
                    }

                    val parts = candidates
                        .getJSONObject(0)
                        .optJSONObject("content")
                        ?.optJSONArray("parts")

                    if (parts == null || parts.length() == 0) {
                        return@withContext AiResponse(
                            "Gemini returned an empty response.",
                            isError = true
                        )
                    }

                    val result = buildString {
                        for (i in 0 until parts.length()) {
                            val text = parts
                                .getJSONObject(i)
                                .optString("text")

                            if (text.isNotBlank()) {
                                append(text)
                            }
                        }
                    }

                    if (result.isBlank()) {
                        AiResponse(
                            "Gemini returned an empty response.",
                            isError = true
                        )
                    } else {
                        AiResponse(result)
                    }
                }

            } catch (e: Exception) {
                AiResponse(
                    "Gemini connection failed: ${e.message ?: "Unknown error"}",
                    isError = true
                )
            }
        }
    }
}
