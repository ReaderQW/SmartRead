package com.example.data.mapper

import com.example.data.local.BookEntity
import com.example.data.local.BookPageEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.HighlightEntity
import com.example.data.local.KnowledgeEdgeEntity
import com.example.data.local.KnowledgeNodeEntity
import com.example.data.local.NoteEntity
import com.example.domain.model.Book
import com.example.domain.model.BookPage
import com.example.domain.model.ChatMessage
import com.example.domain.model.ChatSession
import com.example.domain.model.Highlight
import com.example.domain.model.KnowledgeEdge
import com.example.domain.model.KnowledgeNode
import com.example.domain.model.Note
import com.example.domain.model.ReadingReport

fun BookEntity.toDomain() = Book(id, title, author, coverResName, summaryText, category, progress, type, fileUri, coverUri, totalPages, createdAt, updatedAt)
fun Book.toEntity() = BookEntity(id, title, author, coverResName, summaryText, category, progress, type, fileUri, coverUri, totalPages, createdAt, updatedAt)

fun BookPageEntity.toDomain() = BookPage(id, bookId, pageIndex, imageUri, width, height, extractedText)
fun BookPage.toEntity() = BookPageEntity(id, bookId, pageIndex, imageUri, width, height, extractedText)

fun HighlightEntity.toDomain() = Highlight(id, bookId, pageIndex, text, comment, startX, startY, endX, endY, colorHex, timestamp)
fun Highlight.toEntity() = HighlightEntity(id, bookId, pageIndex, text, comment, startX, startY, endX, endY, colorHex, null, timestamp)

fun NoteEntity.toDomain() = Note(id, bookId, pageIndex, highlightId, originalText, userNote, aiSummary, tags, screenshotUri, aiStatus, timestamp)
fun Note.toEntity() = NoteEntity(id, bookId, pageIndex, highlightId, originalText, userNote, aiSummary, tags, screenshotUri, aiStatus, timestamp)

fun ChatSessionEntity.toDomain() = ChatSession(id, bookId, title, createdAt)
fun ChatSession.toEntity() = ChatSessionEntity(id, bookId, title, createdAt)

fun ChatMessageEntity.toDomain() = ChatMessage(id, sessionId, bookId, sender, content, selectedText, timestamp)
fun ChatMessage.toEntity() = ChatMessageEntity(id, sessionId, bookId, sender, content, selectedText, timestamp)

fun KnowledgeNodeEntity.toDomain() = KnowledgeNode(id, bookId, label, category, size)
fun KnowledgeNode.toEntity() = KnowledgeNodeEntity(id, bookId, label, category, size)

fun KnowledgeEdgeEntity.toDomain() = KnowledgeEdge(id, bookId, source, target, relation, weight)
fun KnowledgeEdge.toEntity() = KnowledgeEdgeEntity(id, bookId, source, target, relation, weight)
