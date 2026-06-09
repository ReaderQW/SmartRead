package com.example.data.repository

import com.example.data.local.FileDataSource
import com.example.data.vector.VectorStore
import com.example.domain.model.KnowledgeEdge
import com.example.domain.model.KnowledgeNode
import com.example.domain.model.KnowledgeSnippet
import com.example.domain.repository.KnowledgeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class KnowledgeRepositoryImpl(
    private val fileDataSource: FileDataSource,
    private val vectorStore: VectorStore
) : KnowledgeRepository {
    override val allNodes: Flow<List<KnowledgeNode>> = fileDataSource.observeNodes

    override val allEdges: Flow<List<KnowledgeEdge>> = fileDataSource.observeEdges

    override suspend fun addNode(node: KnowledgeNode) = withContext(Dispatchers.IO) {
        fileDataSource.addNode(node)
    }

    override suspend fun addEdge(edge: KnowledgeEdge) = withContext(Dispatchers.IO) {
        fileDataSource.addEdge(edge)
    }

    override suspend fun indexText(bookId: Int, sourceType: String, sourceId: String, text: String) = withContext(Dispatchers.IO) {
        vectorStore.indexText(bookId, sourceType, sourceId, text)
    }

    override suspend fun searchSimilar(bookId: Int, text: String, limit: Int): List<KnowledgeSnippet> = withContext(Dispatchers.IO) {
        vectorStore.searchSimilar(bookId, text, limit)
    }
}