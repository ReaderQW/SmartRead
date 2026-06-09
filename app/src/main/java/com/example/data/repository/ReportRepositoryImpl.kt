package com.example.data.repository

import com.example.data.local.FileDataSource
import com.example.data.remote.AigcRemoteDataSource
import com.example.domain.model.DialogueFrequency
import com.example.domain.model.HighlightSemantics
import com.example.domain.model.InteractionMatrix
import com.example.domain.model.NoteDepth
import com.example.domain.model.ReadingReport
import com.example.domain.repository.ReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ReportRepositoryImpl(
    private val fileDataSource: FileDataSource,
    private val aigc: AigcRemoteDataSource
) : ReportRepository {
    override suspend fun generateBlindBoxReport(bookId: Int, bookTitle: String): ReadingReport = withContext(Dispatchers.IO) {
        val book = fileDataSource.getBookById(bookId)
        val notes = fileDataSource.getNotesForBook(bookId)
        val highlights = fileDataSource.getHighlightsForBook(bookId)
        val chats = fileDataSource.getMessagesForBook(bookId)

        val notesSummaryText = notes.joinToString("\n") { "摘录:${it.originalText} | 感悟:${it.userNote}" }
        val highlightsText = highlights.joinToString("\n") { it.text }
        val chatsText = chats.filter { it.sender == "USER" }.joinToString("\n") { it.content }
        
        val json = aigc.blindBoxReportJson(
            bookTitle = bookTitle,
            bookAuthor = book?.author ?: "",
            bookSummary = book?.summaryText ?: "",
            notesSummaryText = notesSummaryText,
            highlightsText = highlightsText,
            chatsText = chatsText
        )

        val report = if (json != null) {
            val matrix = parseInteractionMatrix(json.optJSONObject("interactionMatrix"))
            val highlightsList = parseJsonStringArray(json.optJSONArray("highlightsList"))
            val notesList = parseJsonStringArray(json.optJSONArray("notesList"))
            val chatExcerpts = parseJsonStringArray(json.optJSONArray("chatExcerpts"))

            ReadingReport(
                bookId = bookId,
                bookTitle = bookTitle,
                bookAuthor = book?.author ?: "",
                bookSummary = book?.summaryText ?: "",
                logic = json.optInt("logic", 80),
                empathy = json.optInt("empathy", 75),
                critical = json.optInt("critical", 85),
                width = json.optInt("width", 70),
                innovation = json.optInt("innovation", 80),
                interactionMatrix = matrix,
                cognitiveIncrement = json.optString("cognitiveIncrement", "本期阅读带来了新的认知连接。"),
                motto = json.optString("motto", "让问题继续照亮阅读。"),
                highlightsList = highlightsList,
                notesList = notesList,
                chatExcerpts = chatExcerpts,
                timestamp = System.currentTimeMillis()
            )
        } else {
            // Fallback: 基于本地数据生成三维矩阵
            val fallbackMatrix = buildFallbackMatrix(highlights, notes, chats)
            ReadingReport(
                bookId = bookId,
                bookTitle = bookTitle,
                bookAuthor = book?.author ?: "",
                bookSummary = book?.summaryText ?: "",
                logic = (75..95).random(),
                empathy = (70..90).random(),
                critical = (80..98).random(),
                width = (65..85).random(),
                innovation = (70..92).random(),
                interactionMatrix = fallbackMatrix,
                cognitiveIncrement = "通过对《$bookTitle》的高亮、批注与追问，你已经形成了更主动的阅读路径。",
                motto = "未被追问的概念，仍只是沉睡的知识。",
                highlightsList = highlights.take(5).map { it.text },
                notesList = notes.take(5).map { "「${it.originalText}」—— ${it.userNote}" },
                chatExcerpts = chats.filter { it.sender == "USER" }.take(3).map { it.content },
                timestamp = System.currentTimeMillis()
            )
        }
        fileDataSource.insertReport(report)
        report
    }

    override suspend fun saveReport(report: ReadingReport) {
        fileDataSource.insertReport(report)
    }

    private fun parseInteractionMatrix(obj: JSONObject?): InteractionMatrix {
        if (obj == null) return InteractionMatrix(
            HighlightSemantics(),
            NoteDepth(),
            DialogueFrequency()
        )

        val hs = obj.optJSONObject("highlightSemantics")
        val nd = obj.optJSONObject("noteDepth")
        val df = obj.optJSONObject("dialogueFrequency")

        return InteractionMatrix(
            highlightSemantics = HighlightSemantics(
                totalCount = hs?.optInt("totalCount", 0) ?: 0,
                keyConcepts = parseJsonStringArray(hs?.optJSONArray("keyConcepts")),
                emotionalTone = hs?.optString("emotionalTone", "") ?: "",
                representativeExcerpts = parseJsonStringArray(hs?.optJSONArray("representativeExcerpts"))
            ),
            noteDepth = NoteDepth(
                totalCount = nd?.optInt("totalCount", 0) ?: 0,
                depthScore = nd?.optInt("depthScore", 0) ?: 0,
                insightThemes = parseJsonStringArray(nd?.optJSONArray("insightThemes")),
                representativeNotes = parseJsonStringArray(nd?.optJSONArray("representativeNotes"))
            ),
            dialogueFrequency = DialogueFrequency(
                totalCount = df?.optInt("totalCount", 0) ?: 0,
                questionTypes = parseJsonStringArray(df?.optJSONArray("questionTypes")),
                engagementLevel = df?.optInt("engagementLevel", 0) ?: 0,
                representativeDialogues = parseJsonStringArray(df?.optJSONArray("representativeDialogues"))
            )
        )
    }

    private fun parseJsonStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { array.optString(it, "") }.filter { it.isNotEmpty() }
    }

    private fun buildFallbackMatrix(
        highlights: List<com.example.domain.model.Highlight>,
        notes: List<com.example.domain.model.Note>,
        chats: List<com.example.domain.model.ChatMessage>
    ): InteractionMatrix {
        val userChats = chats.filter { it.sender == "USER" }
        return InteractionMatrix(
            highlightSemantics = HighlightSemantics(
                totalCount = highlights.size,
                keyConcepts = highlights.take(3).map { 
                    val t = it.text
                    if (t.length > 10) t.take(10) + "…" else t
                },
                emotionalTone = "探索与思考",
                representativeExcerpts = highlights.take(3).map { it.text }
            ),
            noteDepth = NoteDepth(
                totalCount = notes.size,
                depthScore = (notes.size * 20).coerceAtMost(100),
                insightThemes = notes.take(3).map { n ->
                    n.tags.ifEmpty { 
                        if (n.userNote.length > 8) n.userNote.take(8) + "…" else n.userNote
                    }
                },
                representativeNotes = notes.take(3).map { "「${it.originalText}」—— ${it.userNote}" }
            ),
            dialogueFrequency = DialogueFrequency(
                totalCount = userChats.size,
                questionTypes = listOf("追问", "质疑", "联想"),
                engagementLevel = (userChats.size * 25).coerceAtMost(100),
                representativeDialogues = userChats.take(3).map { it.content }
            )
        )
    }
}
