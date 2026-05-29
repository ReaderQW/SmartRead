package com.example.domain.model

data class Book(
    val id: Int = 0,
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

data class BookPage(
    val id: Int = 0,
    val bookId: Int,
    val pageIndex: Int,
    val imageUri: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val extractedText: String? = null
)

data class Highlight(
    val id: Int = 0,
    val bookId: Int,
    val pageIndex: Int,
    val text: String,
    val comment: String? = null,
    val startX: Float = 0f,
    val startY: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val colorHex: String = "#FFEB3B",
    val timestamp: Long = System.currentTimeMillis()
)

data class Note(
    val id: Int = 0,
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

data class ChatSession(
    val id: Int = 0,
    val bookId: Int,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: Int = 0,
    val sessionId: Int? = null,
    val bookId: Int,
    val sender: String,
    val content: String,
    val selectedText: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class KnowledgeNode(
    val id: String,
    val bookId: Int? = null,
    val label: String,
    val category: String,
    val size: Float = 1.0f
)

data class KnowledgeEdge(
    val id: String,
    val bookId: Int? = null,
    val source: String,
    val target: String,
    val relation: String,
    val weight: Float = 1.0f
)

data class ReadingReport(
    val bookId: Int,
    val bookTitle: String,
    val logic: Int,
    val empathy: Int,
    val critical: Int,
    val width: Int,
    val innovation: Int,
    val cognitiveIncrement: String,
    val motto: String,
    val timestamp: Long
)

data class KnowledgeSnippet(
    val bookId: Int,
    val sourceType: String,
    val sourceId: String,
    val text: String,
    val score: Float
)
