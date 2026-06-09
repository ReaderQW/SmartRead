package com.example.domain.usecase

import com.example.data.remote.AigcRemoteDataSource
import com.example.domain.model.ReadingReport
import com.example.domain.repository.VivoImageRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 生成阅读盲盒艺术长图的 UseCase
 * 逻辑：读取报告数据 -> 调用 LLM 生成绘画 Prompt -> 调用 vivo API 生成图片
 */
class GenerateArtImageUseCase(
    private val vivoRepository: VivoImageRepository,
    private val aigcDataSource: AigcRemoteDataSource
) {
    suspend operator fun invoke(report: ReadingReport): Result<String> {
        val currentDate = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())

        // 1. 调用现有的 LLM 生成高质量的绘画 Prompt
        val artPrompt = aigcDataSource.generateArtPrompt(
            bookTitle = report.bookTitle,
            bookAuthor = report.bookAuthor,
            bookSummary = report.bookSummary,
            cognitiveIncrement = report.cognitiveIncrement,
            motto = report.motto,
            highlightsList = report.highlightsList,
            notesList = report.notesList,
            chatExcerpts = report.chatExcerpts,
            highlightSemanticsDesc = report.interactionMatrix.highlightSemantics.emotionalTone,
            noteDepthDesc = "深度评分:${report.interactionMatrix.noteDepth.depthScore}, 主题:${report.interactionMatrix.noteDepth.insightThemes.joinToString("/")}",
            dialogueFreqDesc = "参与度:${report.interactionMatrix.dialogueFrequency.engagementLevel}, 类型:${report.interactionMatrix.dialogueFrequency.questionTypes.joinToString("/")}",
            date = currentDate
        )

        // 2. 调用 vivo AI 文生图 API
        return vivoRepository.generateArtImage(
            prompt = artPrompt,
            style = "水墨" // 默认使用水墨风格，也可根据需要调整
        )
    }
}
