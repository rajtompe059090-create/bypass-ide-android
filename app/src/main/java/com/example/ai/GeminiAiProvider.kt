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
        "gemini-3.6-flash"
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

                for (attempt in 1..2) {

                    try {

                        val result = requestGemini(
                            model = model,
                            messages = messages,
                            context = context
                        )

                        if (result.first) {
                            return@withContext AiResponse(result.second)
                        }

                        lastError = result.second

                        if (!result.second.startsWith("RETRY:")) {
                            break
                        }

                        if (attempt < 2) {
                            delay(1500L)
                        }

                    } catch (e: Exception) {

                        lastError =
                            "Connection error: ${e.message ?: "Unknown error"}"

                        if (attempt < 2) {
                            delay(1500L)
                        }
                    }
                }
            }

            AiResponse(
                "Gemini request failed.\n\n$lastError",
                true
            )
        }
    }

    private fun requestGemini(
        model: String,
        messages: List<AiMessage>,
        context: String
    ): Pair<Boolean, String> {

        val url =
            "https://generativelanguage.googleapis.com/v1/interactions"

        val systemText = """
            You are Bypass IDE AI Assistant.

            You are an expert Android, Kotlin, Java, web and software
            development assistant.

            Help the user create applications and code.

            When the user asks you to create files, use:

            <file path="index.html">
            file content
            </file>

            <mkdir path="css"/>

            <command>
            command
            </command>

            Keep normal answers clear and practical.

            Project context:
            $context
        """.trimIndent()

        val input = JSONArray()

        input.put(
            JSONObject()
                .put("role", "user")
                .put(
                    "content",
                    systemText
                )
        )

        messages.forEach { message ->

            if (!message.isLoading && !message.isError) {

                input.put(
                    JSONObject()
                        .put(
                            "role",
                            if (message.isUser) "user" else "model"
                        )
                        .put(
                            "content",
                            message.text
                        )
                )
            }
        }

        val body = JSONObject()
            .put("model", model)
            .put("input", input)
            .put("store", false)
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

            val raw = response.body?.string().orEmpty()

            if (!response.isSuccessful) {

                val errorMessage = try {

                    JSONObject(raw)
                        .optJSONObject("error")
                        ?.optString("message")
                        ?.takeIf { it.isNotBlank() }
                        ?: raw

                } catch (_: Exception) {
                    raw
                }

                val retry =
                    response.code == 408 ||
                    response.code == 429 ||
                    response.code == 500 ||
                    response.code == 502 ||
                    response.code == 503 ||
                    response.code == 504

                return if (retry) {
                    false to
                        "RETRY: $model HTTP ${response.code}: $errorMessage"
                } else {
                    false to
                        "$model HTTP ${response.code}: $errorMessage"
                }
            }

            return parseResponse(raw, model)
        }
    }

    private fun parseResponse(
        raw: String,
        model: String
    ): Pair<Boolean, String> {

        if (raw.isBlank()) {
            return false to "RETRY: $model returned an empty HTTP response."
        }

        return try {

            val json = JSONObject(raw)

            /*
             * First try the simple output field.
             */
            val directOutput = json.optString("output")

            if (directOutput.isNotBlank()) {
                return true to directOutput.trim()
            }

            /*
             * Interactions API can return output as an array.
             */
            val outputArray = json.optJSONArray("output")

            if (outputArray != null) {

                val text = StringBuilder()

                for (i in 0 until outputArray.length()) {

                    val item = outputArray.optJSONObject(i)
                        ?: continue

                    val type = item.optString("type")

                    if (type == "text" ||
                        type == "output_text" ||
                        type == "model_output"
                    ) {

                        val value =
                            item.optString("text")

                        if (value.isNotBlank()) {
                            text.append(value)
                        }

                        val content =
                            item.optJSONArray("content")

                        if (content != null) {
                            for (j in 0 until content.length()) {

                                val part =
                                    content.optJSONObject(j)
                                        ?: continue

                                val partText =
                                    part.optString("text")

                                if (partText.isNotBlank()) {
                                    text.append(partText)
                                }
                            }
                        }
                    }
                }

                if (text.isNotBlank()) {
                    return true to text.toString().trim()
                }
            }

            /*
             * Try steps[] used by some Interactions responses.
             */
            val steps = json.optJSONArray("steps")

            if (steps != null) {

                val text = StringBuilder()

                for (i in 0 until steps.length()) {

                    val step =
                        steps.optJSONObject(i)
                            ?: continue

                    val content =
                        step.optJSONArray("content")

                    if (content != null) {

                        for (j in 0 until content.length()) {

                            val item =
                                content.optJSONObject(j)
                                    ?: continue

                            val itemText =
                                item.optString("text")

                            if (itemText.isNotBlank()) {
                                text.append(itemText)
                            }
                        }
                    }

                    val stepText =
                        step.optString("text")

                    if (stepText.isNotBlank()) {
                        text.append(stepText)
                    }
                }

                if (text.isNotBlank()) {
                    return true to text.toString().trim()
                }
            }

            /*
             * Debug-safe fallback:
             * do NOT leave the UI stuck on Thinking.
             */
            val status =
                json.optString("status")

            val id =
                json.optString("id")

            false to """
                Gemini returned no readable text.
                Model: $model
                Status: ${if (status.isBlank()) "unknown" else status}
                Response ID: ${if (id.isBlank()) "unknown" else id}
            """.trimIndent()

        } catch (e: Exception) {

            false to
                "Gemini response parsing failed: ${e.message}"
        }
    }
}
