package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY id ASC")
    fun observeBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Int): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)
}

@Dao
interface BookPageDao {
    @Query("SELECT * FROM book_pages WHERE bookId = :bookId ORDER BY pageIndex ASC")
    fun observePages(bookId: Int): Flow<List<BookPageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: BookPageEntity): Long
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY timestamp DESC")
    fun observeHighlightsForBook(bookId: Int): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY timestamp DESC")
    suspend fun getHighlightsForBook(bookId: Int): List<HighlightEntity>

    @Query("SELECT * FROM highlights WHERE id = :id")
    suspend fun getHighlightById(id: Int): HighlightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity): Long

    @Delete
    suspend fun deleteHighlight(highlight: HighlightEntity)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY timestamp DESC")
    fun observeNotesForBook(bookId: Int): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY timestamp DESC")
    suspend fun getNotesForBook(bookId: Int): List<NoteEntity>

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun observeAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Delete
    suspend fun deleteNote(note: NoteEntity)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions WHERE bookId = :bookId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSessionForBook(bookId: Int): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Query("SELECT * FROM chat_messages WHERE bookId = :bookId ORDER BY timestamp ASC")
    fun observeMessagesForBook(bookId: Int): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE bookId = :bookId ORDER BY timestamp ASC")
    suspend fun getMessagesForBook(bookId: Int): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE bookId = :bookId")
    suspend fun clearMessagesForBook(bookId: Int)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reading_reports WHERE bookId = :bookId")
    fun observeReport(bookId: Int): Flow<ReadingReportEntity?>

    @Query("SELECT * FROM reading_reports WHERE bookId = :bookId")
    suspend fun getReport(bookId: Int): ReadingReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReadingReportEntity): Long
}

@Dao
interface EmbeddingDao {
    @Query("SELECT * FROM embeddings WHERE bookId = :bookId")
    suspend fun getEmbeddings(bookId: Int): List<EmbeddingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: EmbeddingEntity)
}

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_nodes")
    fun observeAllNodes(): Flow<List<KnowledgeNodeEntity>>

    @Query("SELECT * FROM knowledge_edges")
    fun observeAllEdges(): Flow<List<KnowledgeEdgeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: KnowledgeNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdge(edge: KnowledgeEdgeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<KnowledgeNodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdges(edges: List<KnowledgeEdgeEntity>)

    @Query("DELETE FROM knowledge_nodes")
    suspend fun clearNodes()

    @Query("DELETE FROM knowledge_edges")
    suspend fun clearEdges()

    @Transaction
    suspend fun clearGraph() {
        clearNodes()
        clearEdges()
    }
}
