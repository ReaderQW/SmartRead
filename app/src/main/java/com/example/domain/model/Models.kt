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
    val size: Float = 1.0f,
    val content: String? = null,        // 原始内容（如原文）
    val notes: String? = null,          // 用户笔记
    val aiAnalysis: String? = null      // AI 深度解析
)

data class KnowledgeEdge(
    val id: String,
    val bookId: Int? = null,
    val source: String,
    val target: String,
    val relation: String,
    val weight: Float = 1.0f
)

/**
 * 三维交互矩阵维度
 */
data class InteractionMatrix(
    val highlightSemantics: HighlightSemantics,  // 划线语义
    val noteDepth: NoteDepth,                    // 笔记深度
    val dialogueFrequency: DialogueFrequency     // 对话频率
)

data class HighlightSemantics(
    val totalCount: Int = 0,
    val keyConcepts: List<String> = emptyList(),  // 高频划线概念
    val emotionalTone: String = "",               // 情感基调
    val representativeExcerpts: List<String> = emptyList()  // 代表性划线摘录
)

data class NoteDepth(
    val totalCount: Int = 0,
    val depthScore: Int = 0,          // 深度评分 0-100
    val insightThemes: List<String> = emptyList(),  // 洞察主题
    val representativeNotes: List<String> = emptyList()  // 代表性笔记
)

data class DialogueFrequency(
    val totalCount: Int = 0,
    val questionTypes: List<String> = emptyList(),  // 问题类型
    val engagementLevel: Int = 0,     // 参与度 0-100
    val representativeDialogues: List<String> = emptyList()  // 代表性对话
)

/**
 * 升级后的阅读报告 - 包含三维交互矩阵 + 丰富的书籍内容
 */
data class ReadingReport(
    val bookId: Int,
    val bookTitle: String,
    val bookAuthor: String = "",
    val bookSummary: String = "",
    // 原有的五维评分（保留兼容）
    val logic: Int,
    val empathy: Int,
    val critical: Int,
    val width: Int,
    val innovation: Int,
    // 新增三维交互矩阵
    val interactionMatrix: InteractionMatrix = InteractionMatrix(
        HighlightSemantics(),
        NoteDepth(),
        DialogueFrequency()
    ),
    // 丰富的内容字段
    val cognitiveIncrement: String,
    val motto: String,
    val highlightsList: List<String> = emptyList(),  // 划线列表
    val notesList: List<String> = emptyList(),       // 笔记列表
    val chatExcerpts: List<String> = emptyList(),    // 对话摘录
    val artImageUrl: String? = null,                 // 已生成的艺术长图 URL
    val timestamp: Long
)

data class KnowledgeSnippet(
    val bookId: Int,
    val sourceType: String,
    val sourceId: String,
    val text: String,
    val score: Float
)
