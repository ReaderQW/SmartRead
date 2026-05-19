package com.example.smartread.data.mapper

import com.example.smartread.data.local.entities.BookEntity
import com.example.smartread.data.local.entities.ChatMessageEntity
import com.example.smartread.data.local.entities.ChatSessionEntity
import com.example.smartread.data.local.entities.EmbeddingEntity
import com.example.smartread.data.local.entities.HighlightEntity
import com.example.smartread.data.local.entities.KnowledgeEdgeEntity
import com.example.smartread.data.local.entities.KnowledgeNodeEntity
import com.example.smartread.data.local.entities.NoteEntity
import com.example.smartread.data.local.entities.ReadingReportEntity
import com.example.smartread.domain.model.Book
import com.example.smartread.domain.model.ChatMessage
import com.example.smartread.domain.model.ChatSession
import com.example.smartread.domain.model.Highlight
import com.example.smartread.domain.model.HighlightRect
import com.example.smartread.domain.model.KnowledgeEdge
import com.example.smartread.domain.model.KnowledgeNode
import com.example.smartread.domain.model.KnowledgeSnippet
import com.example.smartread.domain.model.Note
import com.example.smartread.domain.model.ReadingReport
import com.example.smartread.domain.model.ThinkingRadar
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
private val rectListType = Types.newParameterizedType(List::class.java, HighlightRect::class.java)
private val floatListType = Types.newParameterizedType(List::class.java, java.lang.Float::class.javaObjectType)
private val rectListAdapter = moshi.adapter<List<HighlightRect>>(rectListType)
private val floatListAdapter = moshi.adapter<List<Float>>(floatListType)
private val radarAdapter = moshi.adapter(ThinkingRadar::class.java)

fun BookEntity.toDomain() = Book(id, title, author, type, coverUri, fileUri, totalPages, createdAt, updatedAt)
fun Book.toEntity() = BookEntity(id, title, author, type, coverUri, fileUri, totalPages, createdAt, updatedAt)

fun HighlightEntity.toDomain() = Highlight(
    id = id,
    bookId = bookId,
    pageIndex = pageIndex,
    startOffset = startOffset,
    endOffset = endOffset,
    selectedText = selectedText,
    rects = rectListAdapter.fromJson(rectsJson).orEmpty(),
    color = color,
    createdAt = createdAt
)

fun Highlight.toEntity() = HighlightEntity(
    id = id,
    bookId = bookId,
    pageIndex = pageIndex,
    startOffset = startOffset,
    endOffset = endOffset,
    selectedText = selectedText,
    rectsJson = rectListAdapter.toJson(rects),
    color = color,
    createdAt = createdAt
)

fun NoteEntity.toDomain() = Note(id, bookId, pageIndex, highlightId, quoteText, userContent, screenshotUri, aiSummary, aiStatus, createdAt, updatedAt)
fun Note.toEntity() = NoteEntity(id, bookId, pageIndex, highlightId, quoteText, userContent, screenshotUri, aiSummary, aiStatus, createdAt, updatedAt)

fun ChatSessionEntity.toDomain() = ChatSession(id, bookId, title, createdAt)
fun ChatSession.toEntity() = ChatSessionEntity(id, bookId, title, createdAt)

fun ChatMessageEntity.toDomain() = ChatMessage(id, sessionId, role, content, selectedText, createdAt)
fun ChatMessage.toEntity() = ChatMessageEntity(id, sessionId, role, content, selectedText, createdAt)

fun KnowledgeNodeEntity.toDomain() = KnowledgeNode(id, bookId, label, type, weight)
fun KnowledgeNode.toEntity() = KnowledgeNodeEntity(id, bookId, label, type, weight)

fun KnowledgeEdgeEntity.toDomain() = KnowledgeEdge(id, bookId, fromNodeId, toNodeId, relation, weight)
fun KnowledgeEdge.toEntity() = KnowledgeEdgeEntity(id, bookId, fromNodeId, toNodeId, relation, weight)

fun ReadingReportEntity.toDomain() = ReadingReport(
    id = id,
    bookId = bookId,
    radar = radarAdapter.fromJson(radarJson) ?: ThinkingRadar(0, 0, 0, 0, 0),
    cognitionDelta = cognitionDelta,
    insightText = insightText,
    imageUri = imageUri,
    createdAt = createdAt
)

fun ReadingReport.toEntity() = ReadingReportEntity(
    id = id,
    bookId = bookId,
    radarJson = radarAdapter.toJson(radar),
    cognitionDelta = cognitionDelta,
    insightText = insightText,
    imageUri = imageUri,
    createdAt = createdAt
)

fun EmbeddingEntity.toSnippet(queryVector: List<Float>) = KnowledgeSnippet(
    sourceType = sourceType,
    sourceId = sourceId,
    bookId = bookId,
    text = text,
    score = cosineSimilarity(vectorJson.toFloatList(), queryVector)
)

fun List<Float>.toVectorJson(): String = floatListAdapter.toJson(this)

fun String.toFloatList(): List<Float> = floatListAdapter.fromJson(this).orEmpty()

private fun cosineSimilarity(left: List<Float>, right: List<Float>): Float {
    if (left.isEmpty() || right.isEmpty() || left.size != right.size) return 0f
    var dot = 0f
    var leftNorm = 0f
    var rightNorm = 0f
    left.indices.forEach { index ->
        dot += left[index] * right[index]
        leftNorm += left[index] * left[index]
        rightNorm += right[index] * right[index]
    }
    if (leftNorm == 0f || rightNorm == 0f) return 0f
    return (dot / kotlin.math.sqrt(leftNorm * rightNorm))
}
