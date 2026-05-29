package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String,
    val coverResName: String,
    val summaryText: String,
    val category: String,
    val progress: Float = 0f,
    val type: String = "DEMO_TEXT",
    val fileUri: String? = null,
    val coverUri: String? = null,
    val totalPages: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "book_pages",
    indices = [Index(value = ["bookId", "pageIndex"], unique = true)]
)
data class BookPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val pageIndex: Int,
    val imageUri: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val extractedText: String? = null
)

@Entity(
    tableName = "highlights",
    indices = [Index(value = ["bookId", "pageIndex"])]
)
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val pageIndex: Int,
    val text: String,
    val comment: String? = null,
    val startX: Float = 0f,
    val startY: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val colorHex: String = "#FFEB3B",
    val rectJson: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notes",
    indices = [Index(value = ["bookId"]), Index(value = ["highlightId"])]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val pageIndex: Int? = null,
    val highlightId: Int? = null,
    val originalText: String,
    val userNote: String,
    val aiSummary: String? = null,
    val tags: String = "",
    val screenshotUri: String? = null,
    val aiStatus: String = "READY",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_sessions",
    indices = [Index(value = ["bookId"])]
)
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["bookId"]), Index(value = ["sessionId"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int? = null,
    val bookId: Int,
    val sender: String,
    val content: String,
    val selectedText: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reading_reports",
    indices = [Index(value = ["bookId"], unique = true)]
)
data class ReadingReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val bookTitle: String,
    val logic: Int,
    val empathy: Int,
    val critical: Int,
    val width: Int,
    val innovation: Int,
    val cognitiveIncrement: String,
    val motto: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "embeddings",
    indices = [Index(value = ["bookId"]), Index(value = ["sourceType", "sourceId"])]
)
data class EmbeddingEntity(
    @PrimaryKey val id: String,
    val bookId: Int,
    val sourceType: String,
    val sourceId: String,
    val text: String,
    val vectorJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "knowledge_nodes",
    indices = [Index(value = ["bookId"])]
)
data class KnowledgeNodeEntity(
    @PrimaryKey val id: String,
    val bookId: Int? = null,
    val label: String,
    val category: String,
    val size: Float = 1.0f
)

@Entity(
    tableName = "knowledge_edges",
    indices = [Index(value = ["bookId"])]
)
data class KnowledgeEdgeEntity(
    @PrimaryKey val id: String,
    val bookId: Int? = null,
    val source: String,
    val target: String,
    val relation: String,
    val weight: Float = 1.0f
)
