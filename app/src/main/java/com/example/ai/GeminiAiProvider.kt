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

    override val name = "Gemini 2.5 Flash"
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
                "Gemini API key is missing. Open Settings and add your API key.",
                true
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val url =
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

                val contents = JSONArray()

                messages.forEach { message ->
                    if (!message.isLoading && !message.isError) {

                        contents.put(
                            JSONObject()
                                .put(
                                    "role",
                                    if (message.isUser) "user" else "model"
                                )
                                .put(
                                    "parts",
                                    JSONArray().put(
                                        JSONObject().put("text", message.text)
                                    )
                                )
                        )
                    }
                }

                val systemText = """
                    You are Bypass IDE AI Assistant.

                    Help the user build Android applications, websites,
                    software projects and code.

                    You can create project actions using:

                    <file path="index.html">
                    file content
                    </file>

                    <mkdir path="css"/>

                    <command>
                    command
                    </command>

                    Be practical and concise.

                    Project context:
                    $context
                """.trimIndent()

                val body = JSONObject()
                    .put(
                        "systemInstruction",
                        JSONObject().put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put("text", systemText)
                            )
                        )
                    )
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
                        body.toRequestBody(
                            "application/json".toMediaType()
                        )
                    )
                    .build()

                client.newCall(request).execute().use { response ->

                    val responseText =
                        response.body?.string().orEmpty()

                    if (!response.isSuccessful) {

                        val message = try {
                            JSONObject(responseText)
                                .optJSONObject("error")
                                ?.optString("message")
                                ?: responseText
                        } catch (_: Exception) {
                            responseText
                        }

                        return@withContext AiResponse(
                            "Gemini API Error ${response.code}: $message",
                            true
                        )
                    }

                    val json = JSONObject(responseText)

                    val candidates =
                        json.optJSONArray("candidates")

                    if (candidates == null || candidates.length() == 0) {
                        return@withContext AiResponse(
                            "Gemini returned no candidates.",
                            true
                        )
                    }

                    val parts =
                        candidates
                            .getJSONObject(0)
                            .optJSONObject("content")
                            ?.optJSONArray("parts")

                    if (parts == null) {
                        return@withContext AiResponse(
                            "Gemini returned an empty response.",
                            true
                        )
                    }

                    val answer = buildString {
                        for (i in 0 until parts.length()) {
                            append(
                                parts.getJSONObject(i)
                                    .optString("text")
                            )
                        }
                    }.trim()

                    if (answer.isBlank()) {
                        AiResponse(
                            "Gemini returned an empty response.",
                            true
                        )
                    } else {
                        AiResponse(answer)
                    }
                }

            } catch (e: Exception) {
                AiResponse(
                    "Gemini connection failed: ${e.message ?: "Unknown error"}",
                    true
                )
            }
        }
    }
}
