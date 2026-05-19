package com.example.smartread.domain.model

data class Book(
    val id: String,
    val title: String,
    val author: String?,
    val type: BookType,
    val coverUri: String?,
    val fileUri: String,
    val totalPages: Int,
    val createdAt: Long,
    val updatedAt: Long
)

enum class BookType {
    IMAGE,
    PDF,
    EPUB
}

data class Highlight(
    val id: String,
    val bookId: String,
    val pageIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val selectedText: String,
    val rects: List<HighlightRect>,
    val color: Long,
    val createdAt: Long
)

data class HighlightRect(
    val pageIndex: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class Note(
    val id: String,
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

enum class AiStatus {
    NONE,
    GENERATING,
    READY,
    FAILED
}

data class ChatSession(
    val id: String,
    val bookId: String,
    val title: String,
    val createdAt: Long
)

data class ChatMessage(
    val id: String,
    val sessionId: String,
    val role: ChatRole,
    val content: String,
    val selectedText: String?,
    val createdAt: Long
)

enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class AiMessageChunk(
    val content: String,
    val isFinal: Boolean = false
)

data class KnowledgeSnippet(
    val sourceType: KnowledgeSourceType,
    val sourceId: String,
    val bookId: String,
    val text: String,
    val score: Float
)

data class KnowledgeSource(
    val type: KnowledgeSourceType,
    val id: String,
    val bookId: String
)

enum class KnowledgeSourceType {
    NOTE,
    HIGHLIGHT,
    CHAT,
    PAGE
}

data class KnowledgeGraph(
    val nodes: List<KnowledgeNode>,
    val edges: List<KnowledgeEdge>
)

data class KnowledgeNode(
    val id: String,
    val bookId: String,
    val label: String,
    val type: KnowledgeNodeType,
    val weight: Float
)

enum class KnowledgeNodeType {
    CONCEPT,
    BOOK,
    NOTE,
    QUESTION
}

data class KnowledgeEdge(
    val id: String,
    val bookId: String,
    val fromNodeId: String,
    val toNodeId: String,
    val relation: String,
    val weight: Float
)

data class ReadingReport(
    val id: String,
    val bookId: String,
    val radar: ThinkingRadar,
    val cognitionDelta: String,
    val insightText: String,
    val imageUri: String?,
    val createdAt: Long
)

data class ThinkingRadar(
    val logic: Int,
    val emotionalResonance: Int,
    val criticalThinking: Int,
    val connectionBreadth: Int,
    val creativity: Int
)
