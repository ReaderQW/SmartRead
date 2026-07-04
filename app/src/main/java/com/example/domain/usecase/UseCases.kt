package com.example.domain.usecase

import com.example.domain.repository.ChatRepository
import com.example.domain.repository.NoteRepository
import com.example.domain.repository.ReaderRepository
import com.example.domain.repository.ReportRepository

class SaveHighlightUseCase(private val readerRepository: ReaderRepository) {
    suspend operator fun invoke(bookId: Int, pageIndex: Int, text: String, colorHex: String): Long =
        readerRepository.addHighlight(
            bookId = bookId,
            pageIndex = pageIndex,
            text = text,
            startX = (10..50).random().toFloat(),
            startY = (100..200).random().toFloat(),
            endX = (200..350).random().toFloat(),
            endY = (300..450).random().toFloat(),
            colorHex = colorHex
        )
}

class SaveNoteUseCase(private val noteRepository: NoteRepository) {
    suspend operator fun invoke(bookId: Int, originalText: String, userInsight: String, highlightId: Int? = null) =
        noteRepository.saveNoteWithAiInsight(bookId, originalText, userInsight, highlightId)
}

class SendSocraticMessageUseCase(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(bookId: Int, bookTitle: String, passage: String, userText: String) =
        chatRepository.sendSocraticMessage(bookId, bookTitle, passage, userText)
}

class GenerateReportUseCase(private val reportRepository: ReportRepository) {
    suspend operator fun invoke(bookId: Int, bookTitle: String) =
        reportRepository.generateBlindBoxReport(bookId, bookTitle)
}
