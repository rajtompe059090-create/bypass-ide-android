package com.example.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAiProvider(
    private val apiKey: String
) : AiProvider {

    override val name = "Gemini 3.7 Flash"
    override val isConfigured = apiKey.isNotBlank()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val models = listOf(
        "gemini-3.7-flash",
        "gemini-3.6-flash",
        "gemini-3.5-flash"
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

            val conversation = buildConversation(messages, context)

            var lastError = "Unknown Gemini error"

            for ((index, model) in models.withIndex()) {

                var attempt = 0

                while (attempt < 2) {
                    attempt++

                    try {
                        val result = callGemini(
                            model = model,
                            input = conversation
                        )

                        if (result.success) {
                            return@withContext AiResponse(
                                result.text
                            )
                        }

                        lastError = result.text

                        /*
                         * Retry temporary server/capacity errors.
                         */
                        if (isRetryable(result.code)) {
                            delay(1500L * attempt)
                            continue
                        }

                        break

                    } catch (e: Exception) {

                        lastError =
                            e.message ?: "Unknown connection error"

                        if (attempt < 2) {
                            delay(1500L * attempt)
                        }
                    }
                }

                /*
                 * Move to the next model after temporary failure.
                 */
                if (index < models.lastIndex) {
                    continue
                }
            }

            AiResponse(
                "Gemini API Error: $lastError",
                true
            )
        }
    }

    private suspend fun callGemini(
        model: String,
        input: String
    ): GeminiResult {

        val url =
            "https://generativelanguage.googleapis.com/v1beta/interactions"

        val body = JSONObject()
            .put("model", model)
            .put("input", input)
            .put("store", false)
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

                val message = try {

                    JSONObject(responseText)
                        .optJSONObject("error")
                        ?.optString("message")
                        ?.takeIf { it.isNotBlank() }
                        ?: responseText

                } catch (_: Exception) {
                    responseText
                }

                return GeminiResult(
                    success = false,
                    text = "HTTP ${response.code}: $message",
                    code = response.code
                )
            }

            val json = JSONObject(responseText)

            /*
             * Interactions API response:
             *
             * {
             *   "steps": [
             *     {
             *       "type": "model_output",
             *       "content": [
             *         {
             *           "type": "text",
             *           "text": "..."
             *         }
             *       ]
             *     }
             *   ]
             * }
             */

            val steps =
                json.optJSONArray("steps")

            if (steps == null || steps.length() == 0) {
                return GeminiResult(
                    success = false,
                    text = "Gemini returned no output steps.",
                    code = 200
                )
            }

            val answer = StringBuilder()

            for (i in 0 until steps.length()) {

                val step =
                    steps.optJSONObject(i)
                        ?: continue

                if (
                    step.optString("type") !=
                    "model_output"
                ) {
                    continue
                }

                val content =
                    step.optJSONArray("content")
                        ?: continue

                for (j in 0 until content.length()) {

                    val item =
                        content.optJSONObject(j)
                            ?: continue

                    if (
                        item.optString("type") ==
                        "text"
                    ) {

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
                return GeminiResult(
                    success = false,
                    text = "Gemini returned an empty response.",
                    code = 200
                )
            }

            return GeminiResult(
                success = true,
                text = finalAnswer,
                code = 200
            )
        }
    }

    private fun buildConversation(
        messages: List<AiMessage>,
        context: String
    ): String {

        val systemText = """
            You are Bypass IDE AI Assistant.

            Help the user build Android applications,
            websites, software projects and code.

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

            When the user asks you to create or modify
            a project, return the required action tags
            so Bypass IDE can execute them.

            Project context:
            $context
        """.trimIndent()

        return buildString {

            append("SYSTEM:\n")
            append(systemText)
            append("\n\n")

            messages.forEach { message ->

                if (
                    !message.isLoading &&
                    !message.isError
                ) {

                    append(
                        if (message.isUser)
                            "USER:\n"
                        else
                            "ASSISTANT:\n"
                    )

                    append(message.text)
                    append("\n\n")
                }
            }

            append("ASSISTANT:\n")
        }
    }

    private fun isRetryable(code: Int): Boolean {
        return code == 408 ||
               code == 429 ||
               code == 500 ||
               code == 502 ||
               code == 503 ||
               code == 504
    }

    private data class GeminiResult(
        val success: Boolean,
        val text: String,
        val code: Int
    )
}
