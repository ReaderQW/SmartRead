package com.example.data.repository

import com.example.data.remote.AigcRemoteDataSource
import com.example.data.remote.VivoImageApi
import com.example.data.remote.VivoImageParameters
import com.example.data.remote.VivoImageRequest
import com.example.domain.model.ReadingReport
import com.example.domain.repository.VivoImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * vivo AI 图像生成仓库实现 - 升级版
 * 使用丰富的阅读报告数据生成文艺手账风格长图
 */
class VivoImageRepositoryImpl(
    private val vivoApi: VivoImageApi,
    private val aigcDataSource: AigcRemoteDataSource
) : VivoImageRepository {

    // 实际开发中应从安全存储获取
    private val apiKey = "sk-xuanji-2026316046-SVRqbXNtWmdlaU1oUWlZSQ="

    override suspend fun generateArtImage(prompt: String, style: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestId = UUID.randomUUID().toString()
            val currentDate = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())

            val finalPrompt = if (prompt.contains("失败") || prompt.length < 5) {
                "莫兰迪低饱和柔色系，轻快高级文艺手帐拼贴风，竖版长图，画面丰富饱满。散落堆叠的复古精装书籍、翻开的书页（上有人工高亮批注与手写疑问），卷边羊皮纸卷轴、思维导图草稿、带有AI对话气泡的便签（气泡内文字：“你的洞见”→“认知转折”）、手写读书笔记便签（高频关键词如“为什么？”“原来如此”等）、认知转变前后对比示意图（两个柱状图或曲线箭头）、拼图块填补知识盲区的图示（一块拼图正在嵌入）。复古书签、火漆印章（印有“封存”字样）、羽毛钢笔、迷你盲盒摆件（盒子打开，飘出带有结构化总结的小纸条）、泛黄藏书票、古典石膏人像点缀背景。细碎金箔问号与灯泡装饰，手帐胶带边框、手撕纸张拼贴肌理，简约大气构图，细腻手绘质感，8K超清，温柔漫反射柔光。画面包含清晰准确的汉字文本：“$currentDate”与“阅读复盘”。整体传递“思维痕迹萃取+认知重塑可视化+思想封存仪式感”，阅读盲盒读书报告主题。"
            } else {
                "$prompt，莫兰迪柔和低饱和配色，轻快雅致高级大气，文艺阅读手帐拼贴插画，竖版长图。画面核心元素：堆叠各类读过的实体书籍（书页可见手写批注、荧光笔标记、折角），翻开读物旁散落着多个手写便签——包含你与AI的对话摘要（如“AI：这个观点很独特”→“你：但我认为…”）、高频困惑词云小标签、被标记为“极具洞见”的感悟气泡。穿插小型认知重塑可视化模型：比如两个大脑轮廓之间用箭头表示思维转变，或一个知识盲区补全的拼图动画，或一条认知曲线从“模糊”到“清晰”。同时突出“思想封存”仪式感元素：一个带有丝带和火漆印章的盲盒半开状态，里面涌出结构化的读书笔记长卷；或一张羊皮纸归档证书，上面有“阅读复盘·封存”字样和精准日期戳“$currentDate”。辅助元素包括羽毛笔、复古书签、手撕纸拼贴、手帐胶带边框、迷你石膏像背景。$style，简约超现实主义，温润肌理，8K高清，柔光渲染，读书报告盲盒配图，强调深度内化的个性化阅读复盘印记。要求汉字书写端正、清晰、无错别字。"
            }

            val request = VivoImageRequest(
                model = "Doubao-Seedream-5.0-lite",
                prompt = finalPrompt,
                parameters = VivoImageParameters(size = "1024x4096")
            )

            // 2. 按照文档要求传递 Query 参数
            val response = vivoApi.generateImage(
                authorization = "Bearer $apiKey",
                requestId = requestId,
                module = "aigc",
                systemTime = System.currentTimeMillis() / 1000,
                request = request
            )

            // 3. 结果判断
            if (response.code == 0 && response.data != null) {
                val imageUrl = response.data.images?.firstOrNull()?.url

                if (!imageUrl.isNullOrEmpty()) {
                    Result.success(imageUrl)
                } else {
                    Result.failure(Exception("API 响应成功但未返回有效图片链接"))
                }
            } else {
                val errorMsg = response.message.ifEmpty { "错误码: ${response.code}" }
                Result.failure(Exception("生成失败: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 使用丰富的阅读报告数据生成文艺手账风格长图
     */
    suspend fun generateArtImageFromReport(report: ReadingReport): Result<String> {
        // 构建三维矩阵描述文本
        val matrix = report.interactionMatrix
        val highlightSemanticsDesc = buildHighlightSemanticsDesc(matrix.highlightSemantics)
        val noteDepthDesc = buildNoteDepthDesc(matrix.noteDepth)
        val dialogueFreqDesc = buildDialogueFreqDesc(matrix.dialogueFrequency)

        val currentDate = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())

        // 调用 AI 生成绘画 Prompt
        val artPrompt = aigcDataSource.generateArtPrompt(
            bookTitle = report.bookTitle,
            bookAuthor = report.bookAuthor,
            bookSummary = report.bookSummary,
            cognitiveIncrement = report.cognitiveIncrement,
            motto = report.motto,
            highlightsList = report.highlightsList,
            notesList = report.notesList,
            chatExcerpts = report.chatExcerpts,
            highlightSemanticsDesc = highlightSemanticsDesc,
            noteDepthDesc = noteDepthDesc,
            dialogueFreqDesc = dialogueFreqDesc,
            date = currentDate
        )

        return generateArtImage(artPrompt, "文艺手账")
    }

    private fun buildHighlightSemanticsDesc(hs: com.example.domain.model.HighlightSemantics): String {
        return "共${hs.totalCount}条划线，高频概念：${hs.keyConcepts.joinToString("、")}，情感基调：${hs.emotionalTone}"
    }

    private fun buildNoteDepthDesc(nd: com.example.domain.model.NoteDepth): String {
        return "共${nd.totalCount}条笔记，深度评分${nd.depthScore}分，洞察主题：${nd.insightThemes.joinToString("、")}"
    }

    private fun buildDialogueFreqDesc(df: com.example.domain.model.DialogueFrequency): String {
        return "共${df.totalCount}次对话，问题类型：${df.questionTypes.joinToString("、")}，参与度${df.engagementLevel}分"
    }
}
