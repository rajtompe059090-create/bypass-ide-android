package com.example.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    override val name = "Gemini 3.8 Flash"
    override val isConfigured = apiKey.isNotBlank()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val models = listOf(
        "gemini-3.8-flash",
        "gemini-3.7-flash",
        "gemini-3.6-flash",
        "gemini-3.5-flash",
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite"
    )

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

            var lastError = "Unknown Gemini error"

            for (model in models) {

                var attempt = 0

                while (attempt < 2) {
                    attempt++

                    try {

                        val result = callGemini(
                            model = model,
                            messages = messages,
                            context = context
                        )

                        if (result.first) {
                            return@withContext AiResponse(result.second)
                        }

                        lastError = result.second

                        val retryable =
                            result.second.startsWith("RETRYABLE:")

                        if (!retryable) {
                            break
                        }

                        delay(1000L * attempt)

                    } catch (e: Exception) {

                        lastError =
                            e.message ?: "Unknown connection error"

                        if (attempt < 2) {
                            delay(1000L * attempt)
                        }
                    }
                }
            }

            AiResponse(
                "Gemini unavailable. Tried all supported models.\n\n$lastError",
                true
            )
        }
    }

    private fun callGemini(
        model: String,
        messages: List<AiMessage>,
        context: String
    ): Pair<Boolean, String> {

        val url =
            "https://generativelanguage.googleapis.com/v1/interactions"

        val systemInstruction = """
            You are Bypass IDE AI Assistant.

            You are an expert Android developer, Kotlin developer,
            web developer and autonomous coding assistant.

            Help the user build applications, websites and software.

            When creating project files, use these action formats:

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

        val input = buildString {

            append(systemInstruction)
            append("\n\nConversation:\n")

            messages.forEach { message ->

                if (!message.isLoading && !message.isError) {

                    val role =
                        if (message.isUser) "User"
                        else "Assistant"

                    append(role)
                    append(": ")
                    append(message.text)
                    append("\n\n")
                }
            }
        }.trim()

        val body = JSONObject()
            .put("model", model)
            .put("input", input)
            .toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", apiKey)
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

                val errorMessage =
                    try {
                        JSONObject(responseText)
                            .optJSONObject("error")
                            ?.optString("message")
                            ?.takeIf { it.isNotBlank() }
                            ?: responseText
                    } catch (_: Exception) {
                        responseText
                    }

                val retryable =
                    response.code == 408 ||
                    response.code == 429 ||
                    response.code == 500 ||
                    response.code == 502 ||
                    response.code == 503 ||
                    response.code == 504

                return if (retryable) {
                    false to
                        "RETRYABLE: $model -> HTTP ${response.code}: $errorMessage"
                } else {
                    false to
                        "$model -> HTTP ${response.code}: $errorMessage"
                }
            }

            val json =
                JSONObject(responseText)

            val steps =
                json.optJSONArray("steps")

            if (steps == null || steps.length() == 0) {
                return false to
                    "RETRYABLE: $model returned no steps."
            }

            val answer = StringBuilder()

            for (i in 0 until steps.length()) {

                val step =
                    steps.optJSONObject(i)
                        ?: continue

                if (step.optString("type") != "model_output") {
                    continue
                }

                val content =
                    step.optJSONArray("content")
                        ?: continue

                for (j in 0 until content.length()) {

                    val item =
                        content.optJSONObject(j)
                            ?: continue

                    if (item.optString("type") == "text") {

                        val text =
                            item.optString("text")

                        if (text.isNotBlank()) {
                            answer.append(text)
                        }
                    }
                }
            }

            val finalAnswer =
                answer.toString().trim()

            if (finalAnswer.isBlank()) {
                return false to
                    "RETRYABLE: $model returned an empty response."
            }

            return true to finalAnswer
        }
    }
}
