package com.example.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

data class BlueLmChatRequest(
    val model: String,
    val messages: List<BlueLmMessage>,
    val temperature: Float? = null,
    val top_p: Float? = null,
    val max_tokens: Int? = null,
    val stream: Boolean = false,
    val reasoning_effort: String? = "minimal"
)

data class BlueLmMessage(
    val role: String,
    val content: String
)

data class BlueLmChatResponse(
    val id: String?,
    val choices: List<BlueLmChoice>?,
    val usage: BlueLmUsage?,
    val error: BlueLmError?
)

data class BlueLmChoice(
    val index: Int,
    val message: BlueLmMessage,
    val finish_reason: String?
)

data class BlueLmUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

data class BlueLmError(
    val code: String?,
    val message: String?
)

interface BlueLmApi {
    @POST("v1/chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Query("request_id") requestId: String,
        @Body request: BlueLmChatRequest
    ): BlueLmChatResponse
}
