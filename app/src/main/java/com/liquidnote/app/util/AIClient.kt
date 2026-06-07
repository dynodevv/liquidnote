package com.liquidnote.app.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object AIClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun chat(
        endpoint: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        userMessage: String
    ): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val url = if (endpoint.endsWith("/")) endpoint else "$endpoint/"
            val fullUrl = if (url.contains("/chat/completions")) url else "${url}chat/completions"

            val requestBody = ChatRequest(
                model = model.ifBlank { "gpt-4o" },
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = userMessage)
                )
            )

            val body = json.encodeToString(requestBody)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(fullUrl)
                .post(body)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: ${response.body?.string()}")
                    )
                }
                val responseBody = response.body?.string() ?: return@withContext Result.failure(IOException("Empty response"))
                val chatResponse = json.decodeFromString<ChatResponse>(responseBody)
                val content = chatResponse.choices.firstOrNull()?.message?.content
                    ?: return@withContext Result.failure(IOException("No response content"))
                Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Serializable
    data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double = 0.7
    )

    @Serializable
    data class Message(
        val role: String,
        val content: String
    )

    @Serializable
    data class ChatResponse(
        val choices: List<Choice>
    )

    @Serializable
    data class Choice(
        val message: Message
    )
}
