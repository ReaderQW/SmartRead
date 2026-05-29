package com.example.data.repository

import com.example.data.local.HighlightDao
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.Highlight
import com.example.domain.model.KnowledgeEdge
import com.example.domain.model.KnowledgeNode
import com.example.domain.repository.KnowledgeRepository
import com.example.domain.repository.ReaderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ReaderRepositoryImpl(
    private val highlightDao: HighlightDao,
    private val knowledgeRepository: KnowledgeRepository
) : ReaderRepository {
    override fun getHighlightsForBook(bookId: Int): Flow<List<Highlight>> =
        highlightDao.observeHighlightsForBook(bookId).map { highlights -> highlights.map { it.toDomain() } }

    override suspend fun addHighlight(
        bookId: Int,
        pageIndex: Int,
        text: String,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        colorHex: String
    ): Long = withContext(Dispatchers.IO) {
        val id = highlightDao.insertHighlight(
            Highlight(
                bookId = bookId,
                pageIndex = pageIndex,
                text = text,
                startX = startX,
                startY = startY,
                endX = endX,
                endY = endY,
                colorHex = colorHex
            ).toEntity()
        )
        val nodeId = "hl_$id"
        val concept = if (text.length > 8) text.take(8) + "..." else text
        knowledgeRepository.addNode(KnowledgeNode(nodeId, bookId, concept, "Note", 1.2f))
        knowledgeRepository.addEdge(KnowledgeEdge("edge_book_${bookId}_$nodeId", bookId, "book_$bookId", nodeId, "高亮"))
        knowledgeRepository.indexText(bookId, "HIGHLIGHT", id.toString(), text)
        id
    }

    override suspend fun addHighlightComment(highlightId: Int, comment: String) = withContext(Dispatchers.IO) {
        val highlight = highlightDao.getHighlightById(highlightId) ?: return@withContext
        highlightDao.insertHighlight(highlight.copy(comment = comment))
    }

    override suspend fun deleteHighlight(highlight: Highlight) = withContext(Dispatchers.IO) {
        highlightDao.deleteHighlight(highlight.toEntity())
    }
}
