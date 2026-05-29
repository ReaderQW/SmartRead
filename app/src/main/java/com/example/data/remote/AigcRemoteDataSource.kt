package com.example.data.remote

import com.example.BuildConfig
import org.json.JSONObject

class AigcRemoteDataSource(
    private val api: GeminiApi = GeminiClient().api
) {
    private val modelName = "gemini-3.5-flash"

    suspend fun generate(prompt: String, systemPrompt: String? = null): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Error: Gemini API Key is placeholder. Please configure GEMINI_API_KEY in the .env file."
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = systemPrompt?.let { Content(parts = listOf(Part(text = it))) }
        )

        return try {
            val response = api.generateContent(modelName, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No AI reflection could be generated at this moment."
        } catch (e: Exception) {
            "Failure: ${e.localizedMessage}. Ensure connection is active and API key has proper permissions."
        }
    }

    suspend fun socraticReply(bookTitle: String, passage: String, userText: String, historyPrompt: String): String {
        val systemPrompt = Prompts.socraticSystemPrompt
        val prompt = Prompts.socraticUserPrompt(bookTitle, passage, userText, historyPrompt)
        return generate(prompt, systemPrompt)
    }

    suspend fun noteInsight(originalText: String, userNote: String): String =
        generate(Prompts.noteInsightPrompt(originalText, userNote), Prompts.noteInsightSystemPrompt)

    suspend fun noteTags(originalText: String, userNote: String): String =
        generate(Prompts.noteTagsPrompt(originalText, userNote), Prompts.noteTagsSystemPrompt)
            .replace(".", "")
            .trim()

    suspend fun blindBoxReportJson(
        bookTitle: String,
        notesSummaryText: String,
        highlightsText: String,
        chatsText: String
    ): JSONObject? {
        val raw = generate(
            prompt = Prompts.reportPrompt(bookTitle, highlightsText, notesSummaryText, chatsText),
            systemPrompt = Prompts.reportSystemPrompt
        )
        return runCatching { JSONObject(raw) }.getOrNull()
    }
}

object Prompts {
    const val socraticSystemPrompt =
        "你是 SmartRead 的苏格拉底式阅读伴读 AI。不要直接给答案或摘要，要通过提问、挑战、类比和关联，帮助读者自主思考。"

    fun socraticUserPrompt(bookTitle: String, passage: String, userText: String, historyPrompt: String) = """
        当前书籍：《$bookTitle》
        当前选中片段：
        "$passage"

        读者的问题或反思：
        "$userText"

        最近对话历史：
        $historyPrompt

        请给出一段引导式回应，优先提出能继续推进思考的问题。
    """.trimIndent()

    const val noteInsightSystemPrompt =
        "你是一个深度阅读笔记提炼器。根据摘录和读者笔记，生成简短、有洞察力的 AI 智慧摘要。"

    fun noteInsightPrompt(originalText: String, userNote: String) = """
        原文摘录：
        "$originalText"

        读者记录：
        "$userNote"

        请生成 80 字以内的洞察。
    """.trimIndent()

    const val noteTagsSystemPrompt = "你是一个极简标签分类器，只返回标签。"

    fun noteTagsPrompt(originalText: String, userNote: String) =
        "根据以下原文和笔记生成 2 个简短标签，用英文逗号分隔，只返回标签：\n$originalText\n$userNote"

    const val reportSystemPrompt =
        "你是 SmartRead 的阅读盲盒报告生成器。请返回可被 JSONObject 解析的纯 JSON。"

    fun reportPrompt(bookTitle: String, highlightsText: String, notesSummaryText: String, chatsText: String) = """
        书籍：《$bookTitle》
        高亮：
        $highlightsText

        笔记：
        $notesSummaryText

        用户提问：
        $chatsText

        返回 JSON 字段：logic, empathy, critical, width, innovation, cognitiveIncrement, motto。
        五个分数字段范围为 50-100。
    """.trimIndent()
}
