package com.example.smartread.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.smartread.data.local.entities.BookEntity
import com.example.smartread.data.local.entities.ChatMessageEntity
import com.example.smartread.data.local.entities.ChatSessionEntity
import com.example.smartread.data.local.entities.EmbeddingEntity
import com.example.smartread.data.local.entities.HighlightEntity
import com.example.smartread.data.local.entities.KnowledgeEdgeEntity
import com.example.smartread.data.local.entities.KnowledgeNodeEntity
import com.example.smartread.data.local.entities.NoteEntity
import com.example.smartread.data.local.entities.ReadingReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY updatedAt DESC")
    fun observeBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBook(bookId: String): BookEntity?

    @Upsert
    suspend fun upsertBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBook(bookId: String)
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights WHERE bookId = :bookId AND pageIndex = :pageIndex ORDER BY createdAt ASC")
    fun observeHighlights(bookId: String, pageIndex: Int): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY pageIndex ASC, createdAt ASC")
    fun observeBookHighlights(bookId: String): Flow<List<HighlightEntity>>

    @Upsert
    suspend fun upsertHighlight(highlight: HighlightEntity)

    @Query("DELETE FROM highlights WHERE id = :highlightId")
    suspend fun deleteHighlight(highlightId: String)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY updatedAt DESC")
    fun observeNotes(bookId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNote(noteId: String): NoteEntity?

    @Upsert
    suspend fun upsertNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun observeSessions(bookId: String): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: String): ChatSessionEntity?

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun observeMessages(sessionId: String): Flow<List<ChatMessageEntity>>

    @Upsert
    suspend fun upsertSession(session: ChatSessionEntity)

    @Upsert
    suspend fun upsertMessage(message: ChatMessageEntity)
}

@Dao
interface EmbeddingDao {
    @Query("SELECT * FROM embeddings WHERE bookId = :bookId")
    suspend fun getEmbeddings(bookId: String): List<EmbeddingEntity>

    @Upsert
    suspend fun upsertEmbedding(embedding: EmbeddingEntity)
}

@Dao
interface KnowledgeGraphDao {
    @Query("SELECT * FROM knowledge_nodes WHERE bookId = :bookId")
    fun observeNodes(bookId: String): Flow<List<KnowledgeNodeEntity>>

    @Query("SELECT * FROM knowledge_edges WHERE bookId = :bookId")
    fun observeEdges(bookId: String): Flow<List<KnowledgeEdgeEntity>>

    @Upsert
    suspend fun upsertNode(node: KnowledgeNodeEntity)

    @Upsert
    suspend fun upsertEdge(edge: KnowledgeEdgeEntity)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reading_reports WHERE bookId = :bookId")
    fun observeReport(bookId: String): Flow<ReadingReportEntity?>

    @Query("SELECT * FROM reading_reports WHERE bookId = :bookId")
    suspend fun getReport(bookId: String): ReadingReportEntity?

    @Upsert
    suspend fun upsertReport(report: ReadingReportEntity)
}
