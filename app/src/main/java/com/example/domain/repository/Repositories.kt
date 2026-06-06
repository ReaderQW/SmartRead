package com.example.domain.repository

import com.example.domain.model.Book
import com.example.domain.model.ChatMessage
import com.example.domain.model.Highlight
import com.example.domain.model.KnowledgeEdge
import com.example.domain.model.KnowledgeNode
import com.example.domain.model.KnowledgeSnippet
import com.example.domain.model.Note
import com.example.domain.model.ReadingReport
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    val allBooks: Flow<List<Book>>
    suspend fun getBookById(id: Int): Book?
    suspend fun updateBook(book: Book)
    suspend fun seedInitialBooks()
    suspend fun searchBooks(query: String): List<Book>
    suspend fun addBook(book: Book): Int
}

interface ReaderRepository {
    fun getHighlightsForBook(bookId: Int): Flow<List<Highlight>>
    suspend fun addHighlight(bookId: Int, pageIndex: Int, text: String, startX: Float, startY: Float, endX: Float, endY: Float, colorHex: String): Long
    suspend fun addHighlightComment(highlightId: Int, comment: String)
    suspend fun deleteHighlight(highlight: Highlight)
}

interface NoteRepository {
    val allNotes: Flow<List<Note>>
    fun getNotesForBook(bookId: Int): Flow<List<Note>>
    suspend fun saveNoteWithAiInsight(bookId: Int, originalText: String, userNote: String): Note
    suspend fun deleteNote(note: Note)
}

interface ChatRepository {
    fun getChatMessagesForBook(bookId: Int): Flow<List<ChatMessage>>
    suspend fun sendSocraticMessage(bookId: Int, bookTitle: String, passage: String, userText: String): String
}

interface ReportRepository {
    suspend fun generateBlindBoxReport(bookId: Int, bookTitle: String): ReadingReport
}

interface KnowledgeRepository {
    val allNodes: Flow<List<KnowledgeNode>>
    val allEdges: Flow<List<KnowledgeEdge>>
    suspend fun addNode(node: KnowledgeNode)
    suspend fun addEdge(edge: KnowledgeEdge)
    suspend fun indexText(bookId: Int, sourceType: String, sourceId: String, text: String)
    suspend fun searchSimilar(bookId: Int, text: String, limit: Int = 5): List<KnowledgeSnippet>
}
