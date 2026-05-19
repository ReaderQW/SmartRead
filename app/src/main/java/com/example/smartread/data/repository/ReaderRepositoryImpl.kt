package com.example.smartread.data.repository

import com.example.smartread.data.local.dao.HighlightDao
import com.example.smartread.data.local.dao.NoteDao
import com.example.smartread.data.mapper.toDomain
import com.example.smartread.data.mapper.toEntity
import com.example.smartread.data.vector.VectorStore
import com.example.smartread.domain.model.Highlight
import com.example.smartread.domain.model.KnowledgeSource
import com.example.smartread.domain.model.KnowledgeSourceType
import com.example.smartread.domain.model.Note
import com.example.smartread.domain.repository.ReaderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReaderRepositoryImpl @Inject constructor(
    private val highlightDao: HighlightDao,
    private val noteDao: NoteDao,
    private val vectorStore: VectorStore
) : ReaderRepository {
    override fun observeHighlights(bookId: String, pageIndex: Int): Flow<List<Highlight>> =
        highlightDao.observeHighlights(bookId, pageIndex).map { highlights -> highlights.map { it.toDomain() } }

    override fun observeBookHighlights(bookId: String): Flow<List<Highlight>> =
        highlightDao.observeBookHighlights(bookId).map { highlights -> highlights.map { it.toDomain() } }

    override suspend fun saveHighlight(highlight: Highlight) {
        highlightDao.upsertHighlight(highlight.toEntity())
        runCatching {
            vectorStore.indexText(
                source = KnowledgeSource(KnowledgeSourceType.HIGHLIGHT, highlight.id, highlight.bookId),
                text = highlight.selectedText
            )
        }
    }

    override suspend fun deleteHighlight(highlightId: String) {
        highlightDao.deleteHighlight(highlightId)
    }

    override suspend fun saveScreenshotNote(note: Note) {
        noteDao.upsertNote(note.toEntity())
        runCatching {
            vectorStore.indexText(
                source = KnowledgeSource(KnowledgeSourceType.NOTE, note.id, note.bookId),
                text = listOf(note.quoteText, note.userContent).filter { it.isNotBlank() }.joinToString("\n")
            )
        }
    }

    override fun observeReaderNotes(bookId: String): Flow<List<Note>> =
        noteDao.observeNotes(bookId).map { notes -> notes.map { it.toDomain() } }
}
