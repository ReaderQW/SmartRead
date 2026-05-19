package com.example.smartread.data.remote

import com.example.smartread.domain.model.AiMessageChunk
import com.example.smartread.domain.model.ReadingReport
import com.example.smartread.domain.model.ThinkingRadar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.UUID
import javax.inject.Inject

interface AigcApi {
    @POST("v1/socratic-chat")
    suspend fun socraticChat(@Body request: SocraticChatRequest): SocraticChatResponse

    @POST("v1/reports/reading-blind-box")
    suspend fun generateReport(@Body request: ReportGenerateRequest): ReportDto
}

data class SocraticChatRequest(
    val bookId: String,
    val selectedText: String,
    val context: List<String>,
    val systemPrompt: String = SOCRATIC_SYSTEM_PROMPT
)

data class SocraticChatResponse(val content: String)

data class ReportGenerateRequest(
    val bookId: String,
    val highlights: List<String>,
    val notes: List<String>,
    val conversations: List<String>
)

data class ReportDto(
    val cognitionDelta: String,
    val insightText: String,
    val logic: Int,
    val emotionalResonance: Int,
    val criticalThinking: Int,
    val connectionBreadth: Int,
    val creativity: Int
)

const val SOCRATIC_SYSTEM_PROMPT =
    "你是一个阅读伴读AI，不要直接给出答案或摘要，而是通过提问、挑战、关联来引导用户自主思考。"

class AigcRemoteDataSource @Inject constructor(
    private val api: AigcApi
) {
    fun socraticChat(request: SocraticChatRequest): Flow<AiMessageChunk> = flow {
        val response = api.socraticChat(request)
        emit(AiMessageChunk(content = response.content, isFinal = true))
    }

    suspend fun generateReport(request: ReportGenerateRequest): ReadingReport {
        val dto = api.generateReport(request)
        return ReadingReport(
            id = UUID.randomUUID().toString(),
            bookId = request.bookId,
            radar = ThinkingRadar(
                logic = dto.logic,
                emotionalResonance = dto.emotionalResonance,
                criticalThinking = dto.criticalThinking,
                connectionBreadth = dto.connectionBreadth,
                creativity = dto.creativity
            ),
            cognitionDelta = dto.cognitionDelta,
            insightText = dto.insightText,
            imageUri = null,
            createdAt = System.currentTimeMillis()
        )
    }
}

