package com.example.smartread.data.vector

import com.example.smartread.data.local.dao.EmbeddingDao
import com.example.smartread.data.local.entities.EmbeddingEntity
import com.example.smartread.data.mapper.toSnippet
import com.example.smartread.data.mapper.toVectorJson
import com.example.smartread.domain.model.KnowledgeSnippet
import com.example.smartread.domain.model.KnowledgeSource
import java.util.UUID
import javax.inject.Inject

class VectorStore @Inject constructor(
    private val embeddingDao: EmbeddingDao,
    private val embeddingClient: EmbeddingClient
) {
    suspend fun indexText(source: KnowledgeSource, text: String) {
        val vector = embeddingClient.embed(text)
        embeddingDao.upsertEmbedding(
            EmbeddingEntity(
                id = UUID.randomUUID().toString(),
                bookId = source.bookId,
                sourceType = source.type,
                sourceId = source.id,
                text = text,
                vectorJson = vector.toVectorJson(),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun searchSimilar(bookId: String, text: String, limit: Int): List<KnowledgeSnippet> {
        val queryVector = embeddingClient.embed(text)
        return embeddingDao.getEmbeddings(bookId)
            .map { it.toSnippet(queryVector) }
            .sortedByDescending { it.score }
            .take(limit)
    }
}

