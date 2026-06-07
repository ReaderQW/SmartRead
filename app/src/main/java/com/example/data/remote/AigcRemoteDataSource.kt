package com.example.data.remote

import org.json.JSONObject
import java.util.UUID

class AigcRemoteDataSource(
    private val blueLmApi: BlueLmApi = BlueLmClient().api
) {
    private val appKey = "sk-xuanji-2026316046-SVRqbXNtWmdlaU1oUWlZSQ=="
    private val modelName = "Doubao-Seed-2.0-mini"

    suspend fun generate(prompt: String, systemPrompt: String? = null): String {
        val messages = mutableListOf<BlueLmMessage>()
        if (systemPrompt != null) {
            messages.add(BlueLmMessage(role = "system", content = systemPrompt))
        }
        messages.add(BlueLmMessage(role = "user", content = prompt))

        val request = BlueLmChatRequest(
            model = modelName,
            messages = messages,
            temperature = 0.7f,
            max_tokens = 1024
        )

        val requestId = UUID.randomUUID().toString()

        return try {
            val response = blueLmApi.chatCompletions("Bearer $appKey", requestId, request)
            if (response.error != null) {
                // 返回保底提示词，防止生图引擎崩溃
                "超现实主义艺术，深邃宇宙背景下漂浮的巨大书卷，水墨笔触与科技线条交织，冷色调，4k分辨率，哲学意蕴"
            } else {
                response.choices?.firstOrNull()?.message?.content ?: "超现实主义艺术，深邃宇宙背景下漂浮的巨大书卷"
            }
        } catch (e: Exception) {
            "超现实主义艺术，书卷与星空融合，水墨风格，哲学深邃感"
        }
    }

    suspend fun socraticReply(bookTitle: String, passage: String, userText: String, historyPrompt: String): String =
        generate(Prompts.socraticUserPrompt(bookTitle, passage, userText, historyPrompt), Prompts.socraticSystemPrompt)

    suspend fun noteInsight(originalText: String, userNote: String): String =
        generate(Prompts.noteInsightPrompt(originalText, userNote), Prompts.noteInsightSystemPrompt)

    suspend fun noteTags(originalText: String, userNote: String): String =
        generate(Prompts.noteTagsPrompt(originalText, userNote), Prompts.noteTagsSystemPrompt).replace(".", "").trim()

    suspend fun blindBoxReportJson(bookTitle: String, notesSummaryText: String, highlightsText: String, chatsText: String): JSONObject? {
        val raw = generate(Prompts.reportPrompt(bookTitle, highlightsText, notesSummaryText, chatsText), Prompts.reportSystemPrompt)
        val jsonString = try {
            val startIndex = raw.indexOf("{")
            val endIndex = raw.lastIndexOf("}")
            if (startIndex != -1 && endIndex != -1) raw.substring(startIndex, endIndex + 1) else raw
        } catch (e: Exception) { raw }
        return runCatching { JSONObject(jsonString) }.getOrNull()
    }

    suspend fun generateArtPrompt(bookTitle: String, cognitiveIncrement: String, motto: String): String =
        generate(Prompts.artPromptUserPrompt(bookTitle, cognitiveIncrement, motto), Prompts.artPromptSystemPrompt).trim()
}

object Prompts {
    const val socraticSystemPrompt = "你是 SmartRead 的智能伴读专家。你精通苏格拉底式启发教学，你的角色是读者的“思想陪跑者”。"
    
    fun socraticUserPrompt(bookTitle: String, passage: String, userText: String, historyPrompt: String) = """
        当前书籍：《$bookTitle》
        片段："$passage"
        读者："$userText"
        请给予深度回应并提出启发性问题。
    """.trimIndent()

    const val noteInsightSystemPrompt = "你是一个深度阅读思想剖析专家。"
    fun noteInsightPrompt(originalText: String, userNote: String) = "原文：$originalText\n笔记：$userNote\n请生成一段100字以内的深度剖析。"

    const val noteTagsSystemPrompt = "你是一个极简标签分类器，只返回标签。"
    fun noteTagsPrompt(originalText: String, userNote: String) = "为以下内容生成2个简短标签：\n$originalText\n$userNote"

    const val reportSystemPrompt = "你是一个高级阅读报告分析师。请务必返回纯 JSON 格式。"
    fun reportPrompt(bookTitle: String, highlightsText: String, notesSummaryText: String, chatsText: String) = """
        书籍：《$bookTitle》
        数据：$highlightsText\n$notesSummaryText\n$chatsText
        返回 JSON 包含评分(logic, empathy, critical, width, innovation)及 cognitiveIncrement, motto。
    """.trimIndent()

    const val artPromptSystemPrompt = "你是一位专业的 AI 绘画提示词架构师。"
    fun artPromptUserPrompt(bookTitle: String, cognitiveIncrement: String, motto: String) = """
        为书籍《$bookTitle》生成生图 Prompt。
        增量：$cognitiveIncrement
        金句：$motto
        要求：风格超现实主义水墨，直接返回 Prompt。
    """.trimIndent()
}
