package com.example.smartread.domain.repository

import com.example.smartread.domain.model.AiMessageChunk
import com.example.smartread.domain.model.Book
import com.example.smartread.domain.model.ChatMessage
import com.example.smartread.domain.model.ChatSession
import com.example.smartread.domain.model.Highlight
import com.example.smartread.domain.model.KnowledgeGraph
import com.example.smartread.domain.model.KnowledgeSnippet
import com.example.smartread.domain.model.KnowledgeSource
import com.example.smartread.domain.model.Note
import com.example.smartread.domain.model.ReadingReport
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun observeBooks(): Flow<List<Book>>
    suspend fun getBook(bookId: String): Book?
    suspend fun upsertBook(book: Book)
    suspend fun deleteBook(bookId: String)
}

interface ReaderRepository {
    fun observeHighlights(bookId: String, pageIndex: Int): Flow<List<Highlight>>
    fun observeBookHighlights(bookId: String): Flow<List<Highlight>>
    suspend fun saveHighlight(highlight: Highlight)
    suspend fun deleteHighlight(highlightId: String)
    suspend fun saveScreenshotNote(note: Note)
    fun observeReaderNotes(bookId: String): Flow<List<Note>>
}

interface NoteRepository {
    fun observeNotes(bookId: String): Flow<List<Note>>
    suspend fun getNote(noteId: String): Note?
    suspend fun saveNote(note: Note)
    suspend fun deleteNote(noteId: String)
}

interface ChatRepository {
    fun observeSessions(bookId: String): Flow<List<ChatSession>>
    fun observeMessages(sessionId: String): Flow<List<ChatMessage>>
    suspend fun saveSession(session: ChatSession)
    suspend fun saveMessage(message: ChatMessage)
    fun sendSocraticMessage(bookId: String, selectedText: String, context: List<String>): Flow<AiMessageChunk>
}

interface KnowledgeRepository {
    suspend fun indexText(source: KnowledgeSource, text: String)
    suspend fun searchSimilar(bookId: String, text: String, limit: Int = 5): List<KnowledgeSnippet>
    fun observeGraph(bookId: String): Flow<KnowledgeGraph>
}

interface ReportRepository {
    suspend fun generateBlindBoxReport(bookId: String): ReadingReport
    fun observeReport(bookId: String): Flow<ReadingReport?>
}
