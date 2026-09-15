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

    override val name = "Gemini 3.6 Flash"
    override val isConfigured = apiKey.isNotBlank()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
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
                    "https://generativelanguage.googleapis.com/v1beta/interactions"

                val systemText = """
                    You are Bypass IDE AI Assistant.

                    Help the user build Android applications, websites,
                    software projects and code.

                    You can create project actions using:

                    <file path="index.html">
                    file content
                    </file>

                    <edit_file path="index.html">
                    file content
                    </edit_file>

                    <append_file path="index.html">
                    content to append
                    </append_file>

                    <mkdir path="css"/>

                    <command>
                    command
                    </command>

                    Be practical and concise.

                    When the user asks you to create or modify a project,
                    return the required action tags so Bypass IDE can execute them.

                    Project context:
                    $context
                """.trimIndent()

                /*
                 * Interactions API accepts a single input string.
                 * We serialize the local conversation ourselves so that
                 * existing AiSession/HomeScreen code remains unchanged.
                 */
                val conversation = buildString {

                    append("SYSTEM INSTRUCTIONS:\n")
                    append(systemText)
                    append("\n\n")

                    messages.forEach { message ->

                        if (!message.isLoading && !message.isError) {

                            val role =
                                if (message.isUser) {
                                    "USER"
                                } else {
                                    "ASSISTANT"
                                }

                            append(role)
                            append(":\n")
                            append(message.text)
                            append("\n\n")
                        }
                    }

                    append("ASSISTANT:\n")
                }

                val body = JSONObject()
                    .put(
                        "model",
                        "gemini-3.6-flash"
                    )
                    .put(
                        "input",
                        conversation
                    )
                    .put(
                        "store",
                        false
                    )
                    .put(
                        "generation_config",
                        JSONObject()
                            .put("temperature", 0.7)
                            .put("max_output_tokens", 8192)
                    )
                    .toString()

                val request = Request.Builder()
                    .url(url)
                    .addHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .addHeader(
                        "x-goog-api-key",
                        apiKey
                    )
                    .addHeader(
                        "Api-Revision",
                        "2026-05-20"
                    )
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

                        val errorMessage = try {

                            val json =
                                JSONObject(responseText)

                            json.optJSONObject("error")
                                ?.optString("message")
                                ?.takeIf { it.isNotBlank() }
                                ?: responseText

                        } catch (_: Exception) {
                            responseText
                        }

                        return@withContext AiResponse(
                            "Gemini API Error ${response.code}: $errorMessage",
                            true
                        )
                    }

                    val json =
                        JSONObject(responseText)

                    /*
                     * Interactions API returns model-generated steps.
                     * Find model_output and collect text content.
                     */
                    val output =
                        json.optJSONArray("output")

                    if (output == null || output.length() == 0) {
                        return@withContext AiResponse(
                            "Gemini returned no output.",
                            true
                        )
                    }

                    val answer = StringBuilder()

                    for (i in 0 until output.length()) {

                        val item =
                            output.optJSONObject(i)
                                ?: continue

                        val type =
                            item.optString("type")

                        if (type == "text") {

                            val text =
                                item.optString("text")

                            if (text.isNotBlank()) {
                                answer.append(text)
                            }

                        } else if (type == "model_output") {

                            val content =
                                item.optJSONArray("content")

                            if (content != null) {

                                for (j in 0 until content.length()) {

                                    val contentItem =
                                        content.optJSONObject(j)
                                            ?: continue

                                    val text =
                                        contentItem.optString("text")

                                    if (text.isNotBlank()) {
                                        answer.append(text)
                                    }
                                }
                            }
                        }
                    }

                    val finalAnswer =
                        answer.toString().trim()

                    if (finalAnswer.isBlank()) {

                        /*
                         * Some response variants expose the text
                         * through output_text.
                         */
                        val fallback =
                            json.optString("output_text")
                                .trim()

                        if (fallback.isNotBlank()) {
                            return@withContext AiResponse(
                                fallback
                            )
                        }

                        return@withContext AiResponse(
                            "Gemini returned an empty response.",
                            true
                        )
                    }

                    AiResponse(finalAnswer)
                }

            } catch (e: Exception) {

                AiResponse(
                    "Gemini connection failed: ${
                        e.message ?: "Unknown error"
                    }",
                    true
                )
            }
        }
    }
}
