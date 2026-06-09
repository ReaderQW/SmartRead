package com.example.data.remote

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AigcRemoteDataSource(
    private val blueLmApi: BlueLmApi = BlueLmClient().api
) {
    // 根据 AIGC 比赛官方文档配置
    private val appKey = "sk-xuanji-2026316046-SVRqbXNtWmdlaU1oUWlZSQ="
    private val modelName = "Doubao-Seed-2.0-mini" // 官方文档推荐模型

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
            // 按照官方示例使用 Bearer 认证和 request_id 查询参数
            val response = blueLmApi.chatCompletions("Bearer $appKey", requestId, request)
            
            if (response.error != null) {
                "对话失败: ${response.error.message ?: "未知错误"}"
            } else {
                response.choices?.firstOrNull()?.message?.content
                    ?: "大模型未返回有效信息。"
            }
        } catch (e: Exception) {
            "对话失败: ${e.localizedMessage}。请检查 API 配置和网络状态。"
        }
    }

    // --- 伴读业务逻辑 ---

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

    suspend fun inferRelationships(currentNote: String, recentNotes: List<String>): List<JSONObject> {
        val raw = generate(
            prompt = Prompts.inferRelationshipsPrompt(currentNote, recentNotes),
            systemPrompt = Prompts.inferRelationshipsSystemPrompt
        )
        val jsonString = try {
            val startIndex = raw.indexOf("[")
            val endIndex = raw.lastIndexOf("]")
            if (startIndex != -1 && endIndex != -1 && endIndex >= startIndex) {
                raw.substring(startIndex, endIndex + 1)
            } else {
                raw.trim()
            }
        } catch (e: Exception) {
            raw.trim()
        }

        return try {
            val array = JSONArray(jsonString)
            List(array.length()) { i -> array.getJSONObject(i) }
        } catch (e: Exception) {
            emptyList()
        }
    }

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
        // 增强的 JSON 提取逻辑，处理 AI 可能返回的 Markdown 代码块或文字干扰
        val jsonString = try {
            val startIndex = raw.indexOf("{")
            val endIndex = raw.lastIndexOf("}")
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                raw.substring(startIndex, endIndex + 1)
            } else {
                raw.trim()
            }
        } catch (e: Exception) {
            raw.trim()
        }

        return runCatching { JSONObject(jsonString) }.getOrNull()
    }

    suspend fun searchBooks(query: String): List<JSONObject> {
        val raw = generate(
            prompt = Prompts.searchBooksPrompt(query),
            systemPrompt = Prompts.searchBooksSystemPrompt
        )
        val jsonString = try {
            val startIndex = raw.indexOf("[")
            val endIndex = raw.lastIndexOf("]")
            if (startIndex != -1 && endIndex != -1 && endIndex >= startIndex) {
                raw.substring(startIndex, endIndex + 1)
            } else {
                raw.trim()
            }
        } catch (e: Exception) {
            raw.trim()
        }

        return try {
            val array = JSONArray(jsonString)
            List(array.length()) { i -> array.getJSONObject(i) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 根据阅读报告内容生成绘画 Prompt
     */
    suspend fun generateArtPrompt(bookTitle: String, cognitiveIncrement: String, motto: String): String =
        generate(
            prompt = Prompts.artPromptUserPrompt(bookTitle, cognitiveIncrement, motto),
            systemPrompt = Prompts.artPromptSystemPrompt
        )
}

object Prompts {
    const val socraticSystemPrompt =
        "你是 SmartRead 的智能伴读专家。你不仅精通苏格拉底式启发教学，还能提供深刻的知识洞察。你的角色是读者的“思想陪跑者”。当读者感到困惑时，你要提供多维度的引导和参考答案；当读者表达观点时，你要通过挑战性提问深化其思考。回复要博学、温和且具有穿透力。"

    fun socraticUserPrompt(bookTitle: String, passage: String, userText: String, historyPrompt: String) = """
        当前书籍：《$bookTitle》
        当前选中片段：
        "$passage"

        读者的思考/问题：
        "$userText"

        最近对话历史：
        $historyPrompt

        请按照以下逻辑给予回应：
        1. 简要肯定或梳理读者的观点。
        2. 提供一个基于文本背景的深度解读或"参考视角"。
        3. 提出 1-2 个能够引导读者继续向更深层次挖掘的启发性问题。
    """.trimIndent()

    const val noteInsightSystemPrompt =
        "你是一个深度阅读思想剖析专家。你的任务是针对读者的摘录和感悟，生成一段具有哲学高度或思维深度的'AI智慧深透剖析'。你的回复应当犀利、深邃，能够揭示文字背后的底层逻辑或认知价值。"

    fun noteInsightPrompt(originalText: String, userNote: String) = """
        原文摘录：
        "$originalText"

        读者的感悟：
        "$userNote"

        请基于以上内容，生成一段 100 字以内的'AI智慧深透剖析'，帮助读者提升认知维度。
    """.trimIndent()

    const val noteTagsSystemPrompt = "你是一个极简标签分类器，只返回标签。"

    fun noteTagsPrompt(originalText: String, userNote: String) =
        "根据以下原文和笔记生成 2 个简短标签，用英文逗号分隔，只返回标签：\n$originalText\n$userNote"

    const val inferRelationshipsSystemPrompt =
        "你是一个知识图谱专家。你的任务是分析当前笔记与之前笔记之间的潜在联系，并以 JSON 数组格式返回这些联系。每个联系应包含 'target' (目标笔记的简短描述) 和 'type' (关系类型，如 '补充'、'矛盾'、'因果'、'延伸'等)。"

    fun inferRelationshipsPrompt(currentNote: String, recentNotes: List<String>) = """
        当前笔记：
        "$currentNote"

        最近的其他笔记：
        ${recentNotes.joinToString("\n") { "- $it" }}

        请分析当前笔记与上述哪些笔记有显著关联，并返回 JSON 数组（最多 3 个关联）：
        [
          {
            "target": "目标笔记的核心内容摘要",
            "type": "关系类型"
          }
        ]
        如果没有任何显著关联，请返回空数组 []。请确保只返回 JSON。
    """.trimIndent()

    const val reportSystemPrompt =
        "你是一个高级阅读报告分析师。你的任务是根据读者的阅读数据（划线、笔记、对话）生成一份多维度的'阅读盲盒报告'。请务必返回可被 JSONObject 解析的纯 JSON 格式内容，不要包含任何 Markdown 标记。"

    fun reportPrompt(bookTitle: String, highlightsText: String, notesSummaryText: String, chatsText: String) = """
        书籍：《$bookTitle》
        阅读高亮（体现关注点）：
        $highlightsText

        阅读笔记（体现思考深度）：
        $notesSummaryText

        用户提问（体现怀疑精神）：
        $chatsText

        请综合分析以上数据，评估读者的阅读表现并返回 JSON：
        {
          "logic": 逻辑评分(50-100),
          "empathy": 共情评分(50-100),
          "critical": 批判思维评分(50-100),
          "width": 知识广度评分(50-100),
          "innovation": 创新启发评分(50-100),
          "cognitiveIncrement": "一段话总结本次阅读带来的认知增量，要求有洞察力",
          "motto": "一句激励读者的阅读金句"
        }
    """.trimIndent()

    const val searchBooksSystemPrompt =
        "你是一个博学且专业的图书推荐助手。请根据用户提供的关键词或描述，推荐 3-5 本相关的经典书籍或高价值读物。请务必返回可被 JSONArray 解析的纯 JSON 格式内容，不要包含任何 Markdown 标记或解释性文字。"

    fun searchBooksPrompt(query: String) = """
        搜索关键词或描述："$query"
        请按以下格式返回 JSON 数组：
        [
          {
            "title": "书名",
            "author": "作者",
            "summary": "一句话简介",
            "category": "分类标签"
          }
        ]
    """.trimIndent()

    const val artPromptSystemPrompt =
        "你是一个专业的艺术提示词专家。你的任务是将读者的阅读总结转化为一段高质量的、充满意境的绘画 Prompt，用于 AI 生成水墨风格的艺术长图。提示词应该是具象的、富有诗意的，且能体现书中的意境和读者的感悟。"

    fun artPromptUserPrompt(bookTitle: String, cognitiveIncrement: String, motto: String) = """
        书籍名称：《$bookTitle》
        认知增量：$cognitiveIncrement
        阅读金句：$motto

        请基于以上信息，生成一段 200 字以内的绘画 Prompt。
        要求：
        1. 强调中国传统"水墨"风格。
        2. 结合书籍的意象和阅读感悟，创造一个具象的视觉场景（如：高山流水、古人夜读、破茧成蝶等）。
        3. 描述场景的细节、色彩（以黑白灰为主，辅以少量点缀）、构图和情感氛围。
        4. 直接返回 Prompt 文本，不要有任何多余的解释。
    """.trimIndent()
}
