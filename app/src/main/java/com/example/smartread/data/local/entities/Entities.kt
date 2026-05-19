package com.example.smartread.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.smartread.domain.model.AiStatus
import com.example.smartread.domain.model.BookType
import com.example.smartread.domain.model.ChatRole
import com.example.smartread.domain.model.KnowledgeNodeType
import com.example.smartread.domain.model.KnowledgeSourceType

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val type: BookType,
    val coverUri: String?,
    val fileUri: String,
    val totalPages: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "book_pages",
    indices = [Index(value = ["bookId", "pageIndex"], unique = true)]
)
data class BookPageEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val pageIndex: Int,
    val imageUri: String?,
    val width: Int,
    val height: Int,
    val extractedText: String?
)

@Entity(
    tableName = "highlights",
    indices = [Index(value = ["bookId", "pageIndex"])]
)
data class HighlightEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val pageIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val selectedText: String,
    val rectsJson: String,
    val color: Long,
    val createdAt: Long
)

@Entity(
    tableName = "notes",
    indices = [Index(value = ["bookId"]), Index(value = ["highlightId"])]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val pageIndex: Int?,
    val highlightId: String?,
    val quoteText: String,
    val userContent: String,
    val screenshotUri: String?,
    val aiSummary: String?,
    val aiStatus: AiStatus,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "chat_sessions",
    indices = [Index(value = ["bookId"])]
)
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val title: String,
    val createdAt: Long
)

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["sessionId"])]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: ChatRole,
    val content: String,
    val selectedText: String?,
    val createdAt: Long
)

@Entity(
    tableName = "embeddings",
    indices = [Index(value = ["bookId"]), Index(value = ["sourceType", "sourceId"])]
)
data class EmbeddingEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val sourceType: KnowledgeSourceType,
    val sourceId: String,
    val text: String,
    val vectorJson: String,
    val createdAt: Long
)

@Entity(
    tableName = "knowledge_nodes",
    indices = [Index(value = ["bookId"])]
)
data class KnowledgeNodeEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val label: String,
    val type: KnowledgeNodeType,
    val weight: Float
)

@Entity(
    tableName = "knowledge_edges",
    indices = [Index(value = ["bookId"])]
)
data class KnowledgeEdgeEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val fromNodeId: String,
    val toNodeId: String,
    val relation: String,
    val weight: Float
)

@Entity(
    tableName = "reading_reports",
    indices = [Index(value = ["bookId"], unique = true)]
)
data class ReadingReportEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val radarJson: String,
    val cognitionDelta: String,
    val insightText: String,
    val imageUri: String?,
    val createdAt: Long
)
