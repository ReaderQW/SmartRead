package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.data.remote.AigcRemoteDataSource
import com.example.domain.model.ChatMessage
import com.example.domain.model.KnowledgeEdge
import com.example.domain.model.KnowledgeNode
import com.example.domain.repository.ChatRepository
import com.example.domain.repository.KnowledgeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatRepositoryImpl(
    private val chatDao: ChatDao,
    private val aigc: AigcRemoteDataSource,
    private val knowledgeRepository: KnowledgeRepository
) : ChatRepository {
    override fun getChatMessagesForBook(bookId: Int): Flow<List<ChatMessage>> =
        chatDao.observeMessagesForBook(bookId).map { messages -> messages.map { it.toDomain() } }

    override suspend fun sendSocraticMessage(
        bookId: Int,
        bookTitle: String,
        passage: String,
        userText: String
    ): String = withContext(Dispatchers.IO) {
        val userId = chatDao.insertMessage(
            ChatMessage(bookId = bookId, sender = "USER", content = userText, selectedText = passage).toEntity()
        )
        val historyPrompt = chatDao.getMessagesForBook(bookId).takeLast(10).joinToString("\n") {
            "${it.sender}: ${it.content}"
        }
        val reply = aigc.socraticReply(bookTitle, passage, userText, historyPrompt)
        val aiId = chatDao.insertMessage(
            ChatMessage(bookId = bookId, sender = "AI", content = reply, selectedText = passage).toEntity()
        )

        knowledgeRepository.indexText(bookId, "CHAT", userId.toString(), userText)
        knowledgeRepository.indexText(bookId, "CHAT", aiId.toString(), reply)

        val topic = listOf("批判性思维", "论证结构", "概念反思", "实证方法", "自我审视").random()
        val topicId = "concept_$topic"
        knowledgeRepository.addNode(KnowledgeNode(topicId, bookId, topic, "Concept", 1.5f))
        knowledgeRepository.addEdge(KnowledgeEdge("edge_msg_${aiId}_$topicId", bookId, "book_$bookId", topicId, "启发"))
        reply
    }
}
