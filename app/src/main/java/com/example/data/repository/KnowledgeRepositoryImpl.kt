package com.example.data.repository

import com.example.data.local.KnowledgeDao
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.data.vector.VectorStore
import com.example.domain.model.KnowledgeEdge
import com.example.domain.model.KnowledgeNode
import com.example.domain.model.KnowledgeSnippet
import com.example.domain.repository.KnowledgeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class KnowledgeRepositoryImpl(
    private val knowledgeDao: KnowledgeDao,
    private val vectorStore: VectorStore
) : KnowledgeRepository {
    override val allNodes: Flow<List<KnowledgeNode>> = knowledgeDao.observeAllNodes().map { nodes ->
        nodes.map { it.toDomain() }
    }

    override val allEdges: Flow<List<KnowledgeEdge>> = knowledgeDao.observeAllEdges().map { edges ->
        edges.map { it.toDomain() }
    }

    override suspend fun addNode(node: KnowledgeNode) = withContext(Dispatchers.IO) {
        knowledgeDao.insertNode(node.toEntity())
    }

    override suspend fun addEdge(edge: KnowledgeEdge) = withContext(Dispatchers.IO) {
        knowledgeDao.insertEdge(edge.toEntity())
    }

    override suspend fun indexText(bookId: Int, sourceType: String, sourceId: String, text: String) = withContext(Dispatchers.IO) {
        vectorStore.indexText(bookId, sourceType, sourceId, text)
    }

    override suspend fun searchSimilar(bookId: Int, text: String, limit: Int): List<KnowledgeSnippet> = withContext(Dispatchers.IO) {
        vectorStore.searchSimilar(bookId, text, limit)
    }
}
