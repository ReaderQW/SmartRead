package com.example.data.vector

import com.example.data.local.EmbeddingDao
import com.example.data.local.EmbeddingEntity
import com.example.data.remote.EmbeddingRemoteDataSource
import com.example.domain.model.KnowledgeSnippet
import org.json.JSONArray
import java.util.UUID
import kotlin.math.sqrt

class VectorStore(
    private val embeddingDao: EmbeddingDao,
    private val embeddingRemoteDataSource: EmbeddingRemoteDataSource = EmbeddingRemoteDataSource()
) {
    suspend fun indexText(bookId: Int, sourceType: String, sourceId: String, text: String) {
        if (text.isBlank()) return
        val vector = embeddingRemoteDataSource.embedLocal(text)
        embeddingDao.insertEmbedding(
            EmbeddingEntity(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                sourceType = sourceType,
                sourceId = sourceId,
                text = text,
                vectorJson = vector.toJson()
            )
        )
    }

    suspend fun searchSimilar(bookId: Int, text: String, limit: Int): List<KnowledgeSnippet> {
        val queryVector = embeddingRemoteDataSource.embedLocal(text)
        return embeddingDao.getEmbeddings(bookId)
            .map {
                KnowledgeSnippet(
                    bookId = it.bookId,
                    sourceType = it.sourceType,
                    sourceId = it.sourceId,
                    text = it.text,
                    score = cosineSimilarity(it.vectorJson.toFloatList(), queryVector)
                )
            }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun List<Float>.toJson(): String = JSONArray(this).toString()

    private fun String.toFloatList(): List<Float> {
        val array = JSONArray(this)
        return List(array.length()) { index -> array.optDouble(index, 0.0).toFloat() }
    }

    private fun cosineSimilarity(left: List<Float>, right: List<Float>): Float {
        if (left.isEmpty() || right.isEmpty() || left.size != right.size) return 0f
        var dot = 0f
        var leftNorm = 0f
        var rightNorm = 0f
        left.indices.forEach { index ->
            dot += left[index] * right[index]
            leftNorm += left[index] * left[index]
            rightNorm += right[index] * right[index]
        }
        if (leftNorm == 0f || rightNorm == 0f) return 0f
        return dot / sqrt(leftNorm * rightNorm)
    }
}
