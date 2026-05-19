package com.example.smartread.data.repository

import com.example.smartread.data.local.dao.ChatDao
import com.example.smartread.data.mapper.toDomain
import com.example.smartread.data.mapper.toEntity
import com.example.smartread.data.remote.AigcRemoteDataSource
import com.example.smartread.data.remote.SocraticChatRequest
import com.example.smartread.data.vector.VectorStore
import com.example.smartread.domain.model.AiMessageChunk
import com.example.smartread.domain.model.ChatMessage
import com.example.smartread.domain.model.ChatSession
import com.example.smartread.domain.model.KnowledgeSource
import com.example.smartread.domain.model.KnowledgeSourceType
import com.example.smartread.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val aigcRemoteDataSource: AigcRemoteDataSource,
    private val vectorStore: VectorStore
) : ChatRepository {
    override fun observeSessions(bookId: String): Flow<List<ChatSession>> =
        chatDao.observeSessions(bookId).map { sessions -> sessions.map { it.toDomain() } }

    override fun observeMessages(sessionId: String): Flow<List<ChatMessage>> =
        chatDao.observeMessages(sessionId).map { messages -> messages.map { it.toDomain() } }

    override suspend fun saveSession(session: ChatSession) {
        chatDao.upsertSession(session.toEntity())
    }

    override suspend fun saveMessage(message: ChatMessage) {
        chatDao.upsertMessage(message.toEntity())
        val bookId = chatDao.getSession(message.sessionId)?.bookId ?: return
        runCatching {
            vectorStore.indexText(
                source = KnowledgeSource(KnowledgeSourceType.CHAT, message.id, bookId = bookId),
                text = message.content
            )
        }
    }

    override fun sendSocraticMessage(
        bookId: String,
        selectedText: String,
        context: List<String>
    ): Flow<AiMessageChunk> = aigcRemoteDataSource.socraticChat(
        SocraticChatRequest(bookId = bookId, selectedText = selectedText, context = context)
    )
}
