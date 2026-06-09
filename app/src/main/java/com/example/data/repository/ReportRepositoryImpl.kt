package com.example.data.repository

import com.example.data.local.FileDataSource
import com.example.data.remote.AigcRemoteDataSource
import com.example.domain.model.ReadingReport
import com.example.domain.repository.ReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportRepositoryImpl(
    private val fileDataSource: FileDataSource,
    private val aigc: AigcRemoteDataSource
) : ReportRepository {
    override suspend fun generateBlindBoxReport(bookId: Int, bookTitle: String): ReadingReport = withContext(Dispatchers.IO) {
        val notes = fileDataSource.getNotesForBook(bookId)
        val highlights = fileDataSource.getHighlightsForBook(bookId)
        val chats = fileDataSource.getMessagesForBook(bookId)

        val notesSummaryText = notes.joinToString("\n") { "摘录:${it.originalText} | 感悟:${it.userNote}" }
        val highlightsText = highlights.joinToString("\n") { it.text }
        val chatsText = chats.filter { it.sender == "USER" }.joinToString("\n") { it.content }
        val json = aigc.blindBoxReportJson(bookTitle, notesSummaryText, highlightsText, chatsText)

        val report = if (json != null) {
            ReadingReport(
                bookId = bookId,
                bookTitle = bookTitle,
                logic = json.optInt("logic", 80),
                empathy = json.optInt("empathy", 75),
                critical = json.optInt("critical", 85),
                width = json.optInt("width", 70),
                innovation = json.optInt("innovation", 80),
                cognitiveIncrement = json.optString("cognitiveIncrement", "本期阅读带来了新的认知连接。"),
                motto = json.optString("motto", "让问题继续照亮阅读。"),
                timestamp = System.currentTimeMillis()
            )
        } else {
            ReadingReport(
                bookId = bookId,
                bookTitle = bookTitle,
                logic = (75..95).random(),
                empathy = (70..90).random(),
                critical = (80..98).random(),
                width = (65..85).random(),
                innovation = (70..92).random(),
                cognitiveIncrement = "通过对《$bookTitle》的高亮、批注与追问，你已经形成了更主动的阅读路径。",
                motto = "未被追问的概念，仍只是沉睡的知识。",
                timestamp = System.currentTimeMillis()
            )
        }
        fileDataSource.insertReport(report)
        report
    }
}