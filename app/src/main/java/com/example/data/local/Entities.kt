package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String,
    val coverResName: String,
    val summaryText: String,
    val category: String,
    val progress: Float = 0f
)

@Entity(tableName = "highlights")
data class Highlight(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val pageIndex: Int,
    val text: String,
    val comment: String? = null,
    val startX: Float = 0f,
    val startY: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val colorHex: String = "#FFEB3B", // Yellow default
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val originalText: String,
    val userNote: String,
    val aiSummary: String? = null,
    val tags: String = "", // comma-separated strings
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val sender: String, // "USER" or "AI"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "knowledge_nodes")
data class KnowledgeNode(
    @PrimaryKey val id: String,
    val label: String,
    val category: String, // "Book", "Concept", "Mindset", "Note"
    val size: Float = 1.0f
)

@Entity(tableName = "knowledge_edges")
data class KnowledgeEdge(
    @PrimaryKey val id: String, // e.g. source_target
    val source: String,
    val target: String,
    val relation: String
)
