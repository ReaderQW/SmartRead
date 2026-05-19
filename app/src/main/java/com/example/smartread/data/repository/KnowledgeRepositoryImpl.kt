package com.example.smartread.data.repository

import com.example.smartread.data.local.dao.KnowledgeGraphDao
import com.example.smartread.data.mapper.toDomain
import com.example.smartread.data.vector.VectorStore
import com.example.smartread.domain.model.KnowledgeGraph
import com.example.smartread.domain.model.KnowledgeSnippet
import com.example.smartread.domain.model.KnowledgeSource
import com.example.smartread.domain.repository.KnowledgeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class KnowledgeRepositoryImpl @Inject constructor(
    private val vectorStore: VectorStore,
    private val graphDao: KnowledgeGraphDao
) : KnowledgeRepository {
    override suspend fun indexText(source: KnowledgeSource, text: String) {
        vectorStore.indexText(source, text)
    }

    override suspend fun searchSimilar(bookId: String, text: String, limit: Int): List<KnowledgeSnippet> =
        vectorStore.searchSimilar(bookId, text, limit)

    override fun observeGraph(bookId: String): Flow<KnowledgeGraph> =
        combine(graphDao.observeNodes(bookId), graphDao.observeEdges(bookId)) { nodes, edges ->
            KnowledgeGraph(
                nodes = nodes.map { it.toDomain() },
                edges = edges.map { it.toDomain() }
            )
        }
}
