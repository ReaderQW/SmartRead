package com.example.data.repository

import com.example.data.local.FileDataSource
import com.example.domain.model.Highlight
import com.example.domain.model.KnowledgeEdge
import com.example.domain.model.KnowledgeNode
import com.example.domain.repository.KnowledgeRepository
import com.example.domain.repository.ReaderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ReaderRepositoryImpl(
    private val fileDataSource: FileDataSource,
    private val knowledgeRepository: KnowledgeRepository
) : ReaderRepository {
    override fun getHighlightsForBook(bookId: Int): Flow<List<Highlight>> =
        fileDataSource.observeHighlightsForBook(bookId)

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
        val id = fileDataSource.addHighlight(
            Highlight(
                bookId = bookId,
                pageIndex = pageIndex,
                text = text,
                startX = startX,
                startY = startY,
                endX = endX,
                endY = endY,
                colorHex = colorHex
            )
        ).toInt()
        val nodeId = "hl_$id"
        val concept = if (text.length > 8) text.take(8) + "..." else text
        knowledgeRepository.addNode(KnowledgeNode(nodeId, bookId, concept, "Note", 1.2f))
        knowledgeRepository.addEdge(KnowledgeEdge("edge_book_${bookId}_$nodeId", bookId, "book_$bookId", nodeId, "高亮"))
        knowledgeRepository.indexText(bookId, "HIGHLIGHT", id.toString(), text)
        id.toLong()
    }

    override suspend fun addHighlightComment(highlightId: Int, comment: String) = withContext(Dispatchers.IO) {
        val highlight = fileDataSource.getHighlightById(highlightId) ?: return@withContext
        fileDataSource.deleteHighlight(highlight)
        fileDataSource.addHighlight(highlight.copy(comment = comment))
    }

    override suspend fun deleteHighlight(highlight: Highlight) = withContext(Dispatchers.IO) {
        fileDataSource.deleteHighlight(highlight)
    }
}