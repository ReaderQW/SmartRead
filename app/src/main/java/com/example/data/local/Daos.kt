package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY id ASC")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Int): Book?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book)

    @Update
    suspend fun updateBook(book: Book)

    @Delete
    suspend fun deleteBook(book: Book)
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY timestamp DESC")
    fun getHighlightsForBook(bookId: Int): Flow<List<Highlight>>

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY timestamp DESC")
    suspend fun getHighlightsForBookSync(bookId: Int): List<Highlight>

    @Query("SELECT * FROM highlights WHERE id = :id")
    suspend fun getHighlightById(id: Int): Highlight?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: Highlight): Long

    @Delete
    suspend fun deleteHighlight(highlight: Highlight)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY timestamp DESC")
    fun getNotesForBook(bookId: Int): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY timestamp DESC")
    suspend fun getNotesForBookSync(bookId: Int): List<Note>

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Delete
    suspend fun deleteNote(note: Note)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE bookId = :bookId ORDER BY timestamp ASC")
    fun getChatMessagesForBook(bookId: Int): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE bookId = :bookId ORDER BY timestamp ASC")
    suspend fun getChatMessagesForBookSync(bookId: Int): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE bookId = :bookId")
    suspend fun clearChatMessagesForBook(bookId: Int)
}

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_nodes")
    fun getAllNodes(): Flow<List<KnowledgeNode>>

    @Query("SELECT * FROM knowledge_edges")
    fun getAllEdges(): Flow<List<KnowledgeEdge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: KnowledgeNode)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdge(edge: KnowledgeEdge)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<KnowledgeNode>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdges(edges: List<KnowledgeEdge>)

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
